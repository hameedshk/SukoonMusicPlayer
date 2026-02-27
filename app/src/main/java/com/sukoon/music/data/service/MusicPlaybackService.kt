package com.sukoon.music.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.sukoon.music.MainActivity
import com.sukoon.music.R
import com.sukoon.music.di.ApplicationScope
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.media3.common.PlaybackException

@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var mediaSession: MediaSession

    @Inject
    lateinit var albumArtLoader: AlbumArtLoader

    @Inject
    lateinit var preferencesManager: com.sukoon.music.data.preferences.PreferencesManager

    @Inject
    lateinit var songAudioSettingsDao: com.sukoon.music.data.local.dao.SongAudioSettingsDao

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope
    private var notificationManager: PlayerNotificationManager? = null
    private var isForeground = false

    // Audio effects manager for equalizer
    @Volatile
    private var audioEffectManager: com.sukoon.music.data.audio.AudioEffectManager? = null
    private var equalizerFlowStarted = false
    private val audioEffectLock = Any() // Lock for synchronizing audio effect initialization

    // Audio noisy receiver for headphone unplug detection
    private var audioNoisyReceiver: BroadcastReceiver? = null

    // Preference: pause playback when audio becomes noisy (headphones unplugged)
    private var pauseOnAudioNoisy = true

    // Preference: resume playback when audio focus is regained
    private var resumeOnAudioFocus = false

    // Audio focus state tracking
    private enum class PauseReason {
        NONE,                    // Not paused or paused by user
        AUDIO_FOCUS_LOSS_TRANSIENT, // Paused due to transient audio focus loss (Android Auto handoff, prompts)
        AUDIO_FOCUS_LOSS_PERMANENT, // Paused due to permanent audio focus loss
        AUDIO_BECOMING_NOISY,    // Paused due to headphones unplugging
        USER_PAUSE               // Explicitly paused by user
    }

    private var currentPauseReason = PauseReason.NONE
    private var currentAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeFocusLoss = false
    private var lastPlayWhenReady = false
    private var lastAndroidAutoControllerSeenAtMs: Long = 0L
    private var lastExternalControllerSeenAtMs: Long = 0L
    private var lastForcedRecoveryAtMs: Long = 0L
    private var lastPostRecoveryRetryAtMs: Long = 0L
    private var lastTransientRecoveryAtMs: Long = 0L
    private var lastServiceStartAtMs: Long = 0L
    private var audioManager: AudioManager? = null

    // Volume state for ducking
    private var normalVolume = 1.0f
    private var isDucked = false

    // Notification visibility state (user preference)
    private var isNotificationVisible = true

    // Crossfade manager for smooth track transitions
    private var crossfadeManager: com.sukoon.music.data.audio.CrossfadeManager? = null
    private var currentCrossfadeDurationMs: Int = 0

    // Sleep timer job
    private var sleepTimerJob: Job? = null
    private var sleepTimerObserverJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playerListenersAttached = false
    private var sessionRegistered = false

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "sukoon_music_playback"
        private const val DUCK_VOLUME = 0.2f // 20% volume when ducked
        private const val ANDROID_AUTO_ACTIVE_WINDOW_MS = 15_000L
        private const val EXTERNAL_CONTROLLER_ACTIVE_WINDOW_MS = 20_000L
        private const val NOISY_ROUTE_SETTLE_DELAY_MS = 400L
        private const val FOCUS_RECOVERY_DELAY_MS = 500L
        private const val FORCED_RECOVERY_COOLDOWN_MS = 10_000L
        private const val SERVICE_START_RECOVERY_WINDOW_MS = 20_000L
        private const val POST_RECOVERY_RETRY_WINDOW_MS = 3_000L
        private const val POST_RECOVERY_RETRY_COOLDOWN_MS = 8_000L
        private const val POST_RECOVERY_RETRY_DELAY_MS = 700L
        private const val TRANSIENT_RECOVERY_DELAY_MS = 650L
        private const val TRANSIENT_RECOVERY_COOLDOWN_MS = 6_000L
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize AudioManager for focus change callbacks
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            createNotificationChannel()
            initializePlayer()

            // Register MediaSession with the service for Android Auto discovery
            if (!sessionRegistered) {
                addSession(mediaSession)
                sessionRegistered = true
            }

            registerAudioNoisyReceiver()
            observeResumeOnAudioFocusSetting()
            observeSleepTimer()
        } catch (e: Exception) {
            DevLogger.e("MusicPlaybackService", "Failed to initialize service", e)
            // If initialization fails, stop the service
            stopSelf()
        }
    }

    /**
     * Create notification channel for Android 8.0+.
     * Required for showing playback notifications.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_music_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_music_playback_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // ExoPlayer handles audio focus automatically when second param is true
        player.setAudioAttributes(audioAttributes, true)

        // Request audio focus with our listener to handle focus changes
        audioManager?.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        lastPlayWhenReady = player.playWhenReady
        attachPlayerListenersIfNeeded()

        // Initialize crossfade manager
        crossfadeManager = com.sukoon.music.data.audio.CrossfadeManager(player, scope)

        // Observe crossfade setting
        observeCrossfadeSetting()

        // Set session activity intent (MediaSession was injected by Hilt)
        val intent = Intent(this, MainActivity::class.java)
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession.setSessionActivity(sessionActivityIntent)

        // Create PlayerNotificationManager for media controls
        // The listener is set via the Builder in Media3 1.5.0
        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID,
            createDescriptionAdapter()
        )
            .setSmallIconResourceId(R.mipmap.ic_launcher)
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    // Trigger repository to sync latest state
                    scope.launch {
                        try {
                            val repo = com.sukoon.music.di.HiltHolder.getPlaybackRepository()
                            repo?.refreshPlaybackState()
                            DevLogger.d("MusicPlaybackService", "Playback state refreshed from notification")
                        } catch (e: Exception) {
                            DevLogger.e("MusicPlaybackService", "Failed to refresh playback state", e)
                        }
                    }

                    // Start foreground if notification should be visible
                    if (isNotificationVisible && !isForeground) {
                        startForegroundSafely(notificationId, notification)
                    } else if (ongoing && !isForeground) {
                        startForegroundSafely(notificationId, notification)
                    }
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    if (isForeground) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        isForeground = false
                    }
                    // Save playback state before stopping
                    if (dismissedByUser) {
                        scope.launch {
                            try {
                                val repo = com.sukoon.music.di.HiltHolder.getPlaybackRepository()
                                repo?.savePlaybackState()
                                DevLogger.d("MusicPlaybackService", "Playback state saved before notification dismissal")
                            } catch (e: Exception) {
                                DevLogger.e("MusicPlaybackService", "Failed to save playback state", e)
                            }
                            stopSelf()
                        }
                    }
                }
            })
            .build()

        // Configure the notification manager
        notificationManager?.apply {
            setUsePreviousAction(true)
            setUseNextAction(true)
            setUseFastForwardAction(false)
            setUseRewindAction(false)
        }

        // Reactive observer handles initial + ongoing state (single source of truth)
        observeNotificationVisibility()

        // Initialize audio effects (equalizer)
        initializeAudioEffects()

        // Observe pause on audio noisy preference
        observePauseOnAudioNoisySetting()
    }

    /**
     * Initialize AudioEffectManager and observe settings from DataStore.
     * Called after ExoPlayer is ready and has valid audio session ID.
     * Will be re-called when audio session ID becomes available if not available at first call.
     *
     * Thread-safe: Uses audioEffectLock to synchronize initialization and flow collection startup.
     */
    private fun initializeAudioEffects() {
        try {
            val audioSessionId = player.audioSessionId
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
                DevLogger.w("MusicPlaybackService", "Audio session ID not available yet, will retry on onAudioSessionIdChanged")
                return
            }

            val manager = com.sukoon.music.data.audio.AudioEffectManager(audioSessionId)
            if (!manager.initialize()) {
                DevLogger.e("MusicPlaybackService", "Failed to initialize audio effects")
                return
            }

            // Synchronize setting the manager to prevent races with flow collector
            synchronized(audioEffectLock) {
                audioEffectManager = manager
            }

            // Start flow collection only once (survives session ID changes via audioEffectManager ref update)
            // Protected by lock to prevent duplicate collectors
            synchronized(audioEffectLock) {
                if (!equalizerFlowStarted) {
                    equalizerFlowStarted = true
                    scope.launch {
                        preferencesManager.equalizerSettingsFlow.collect { settings ->
                            // Synchronize manager read to ensure we don't read a manager being released
                            synchronized(audioEffectLock) {
                                audioEffectManager?.applySettings(settings)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            DevLogger.e("MusicPlaybackService", "Error initializing audio effects", e)
        }
    }

    /**
     * Observe notification visibility preference and gate notification display.
     * When disabled, notification is hidden but playback continues.
     * When enabled, notification is shown with playback controls.
     */
    private fun observeNotificationVisibility() {
        android.util.Log.d("MusicPlaybackService", "observeNotificationVisibility: Starting flow collection")
        scope.launch {
            preferencesManager.showNotificationsFlow.collect { shouldShowNotification ->
                android.util.Log.d("MusicPlaybackService", ">>> FLOW EMITTED: $shouldShowNotification")
                try {
                    // Must run on Main thread for notification operations
                    withContext(Dispatchers.Main) {
                        android.util.Log.d("MusicPlaybackService", ">>> Inside withContext(Main), updating to: $shouldShowNotification")
                        isNotificationVisible = shouldShowNotification
                        DevLogger.d("MusicPlaybackService", "Notification preference changed: $shouldShowNotification")

                        if (shouldShowNotification) {
                            android.util.Log.d("MusicPlaybackService", ">>> Showing notification (setPlayer + invalidate)")
                            // Show notification - ensure player is bound and force refresh.
                            notificationManager?.setPlayer(player)
                            notificationManager?.invalidate()
                            android.util.Log.d("MusicPlaybackService", ">>> setPlayer(player) and invalidate() completed")
                            DevLogger.d("MusicPlaybackService", "Notification visibility enabled")
                        } else {
                            android.util.Log.d("MusicPlaybackService", ">>> Hiding notification (setPlayer null)")
                            // Hide notification and detach player.
                            notificationManager?.setPlayer(null)
                            val systemNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            systemNotificationManager.cancel(NOTIFICATION_ID)
                            if (isForeground) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                                isForeground = false
                            }
                            android.util.Log.d("MusicPlaybackService", ">>> Notification hidden successfully")
                            DevLogger.d("MusicPlaybackService", "Notification visibility disabled")
                        }
                    }
                } catch (e: Exception) {
                    // CRITICAL: Catch exceptions to prevent flow collection from dying
                    // Without this, a single exception kills notification toggle forever
                    android.util.Log.e("MusicPlaybackService", ">>> EXCEPTION caught: ${e.message}", e)
                    DevLogger.e(
                        "MusicPlaybackService",
                        "Error updating notification visibility to $shouldShowNotification",
                        e
                    )
                }
                android.util.Log.d("MusicPlaybackService", ">>> Flow collection cycle completed for: $shouldShowNotification")
            }
        }
    }

    /**
     * Observe crossfade setting and track current duration for transitions.
     * Crossfade works via volume animation at track boundaries.
     */
    private fun observeCrossfadeSetting() {
        scope.launch {
            preferencesManager.userPreferencesFlow.collect { preferences ->
                currentCrossfadeDurationMs = preferences.crossfadeDurationMs

                val isCrossfadeEnabled = currentCrossfadeDurationMs > 0
                if (isCrossfadeEnabled) {
                    DevLogger.d("MusicPlaybackService", "Crossfade enabled: ${currentCrossfadeDurationMs}ms")
                } else {
                    DevLogger.d("MusicPlaybackService", "Crossfade disabled")
                }
            }
        }
    }

    /**
     * Observe pause on audio noisy setting.
     * When headphones disconnect (ACTION_AUDIO_BECOMING_NOISY), playback will pause only if enabled.
     */
    private fun observePauseOnAudioNoisySetting() {
        scope.launch {
            preferencesManager.userPreferencesFlow.collect { preferences ->
                pauseOnAudioNoisy = preferences.pauseOnAudioNoisy
                DevLogger.d("MusicPlaybackService", "Pause on audio noisy: $pauseOnAudioNoisy")
            }
        }
    }

    /**
     * Observe resume on audio focus setting.
     * When enabled, playback will resume when audio focus is regained (if paused due to focus loss).
     */
    private fun observeResumeOnAudioFocusSetting() {
        scope.launch {
            preferencesManager.userPreferencesFlow.collect { preferences ->
                resumeOnAudioFocus = preferences.resumeOnAudioFocus
                DevLogger.d("MusicPlaybackService", "Resume on audio focus: $resumeOnAudioFocus")
            }
        }
    }

    /**
     * Observe sleep timer target time and schedule a pause.
     * This survives app restart because targetTime is persisted in DataStore.
     */
    private fun observeSleepTimer() {
        sleepTimerObserverJob?.cancel()
        sleepTimerObserverJob = serviceScope.launch {
            preferencesManager.userPreferencesFlow
                .map { it.sleepTimerTargetTimeMs }
                .distinctUntilChanged()
                .collect { targetTime ->
                    scheduleSleepTimer(targetTime)
                }
        }
    }

    private fun scheduleSleepTimer(targetTime: Long) {
        sleepTimerJob?.cancel()

        if (targetTime <= 0L) {
            DevLogger.d("MusicPlaybackService", "Sleep timer cleared")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (targetTime <= currentTime) {
            DevLogger.d("MusicPlaybackService", "Sleep timer target was in past, resetting")
            serviceScope.launch {
                preferencesManager.setSleepTimerTargetTime(0L)
            }
            return
        }

        val remainingMs = targetTime - currentTime
        DevLogger.d("MusicPlaybackService", "Sleep timer scheduled: pausing in ${remainingMs / 1000}s")
        sleepTimerJob = serviceScope.launch {
            delay(remainingMs)
            try {
                if (player.isPlaying || player.playWhenReady) {
                    DevLogger.d("MusicPlaybackService", "Sleep timer reached: pausing playback")
                    player.pause()
                } else {
                    DevLogger.d("MusicPlaybackService", "Sleep timer reached while already paused")
                }
            } catch (e: Exception) {
                DevLogger.e("MusicPlaybackService", "Sleep timer pause failed", e)
            } finally {
                // Always clear to avoid stale active state in UI.
                preferencesManager.setSleepTimerTargetTime(0L)
            }
        }
    }

    /**
     * Audio focus change listener to handle focus gain and resumption.
     * Only resumes if:
     * - Pause reason is transient focus loss (always) OR permanent focus loss (if setting enabled)
     * - Player is not already playing
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        currentAudioFocusState = focusChange
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDucked) {
                    player.volume = normalVolume
                    isDucked = false
                }

                val shouldResumeFromTransientLoss =
                    currentPauseReason == PauseReason.AUDIO_FOCUS_LOSS_TRANSIENT &&
                        wasPlayingBeforeFocusLoss &&
                        !player.isPlaying
                val shouldResumeFromPermanentLoss =
                    (resumeOnAudioFocus || isAndroidAutoRecentlyActive()) &&
                        currentPauseReason == PauseReason.AUDIO_FOCUS_LOSS_PERMANENT &&
                        !player.isPlaying

                if (shouldResumeFromTransientLoss || shouldResumeFromPermanentLoss) {
                    DevLogger.d(
                        "MusicPlaybackService",
                        "FOCUS_GAIN_RESUME: reason=${currentPauseReason.name}, wasPlayingBeforeLoss=$wasPlayingBeforeFocusLoss"
                    )
                    player.play()
                    currentPauseReason = PauseReason.NONE
                    wasPlayingBeforeFocusLoss = false
                } else {
                    DevLogger.d(
                        "MusicPlaybackService",
                        "FOCUS_GAIN_NO_RESUME: reason=${currentPauseReason.name}, resumeOnAudioFocus=$resumeOnAudioFocus, androidAutoRecent=${isAndroidAutoRecentlyActive()}, wasPlayingBeforeLoss=$wasPlayingBeforeFocusLoss, isPlaying=${player.isPlaying}"
                    )
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                wasPlayingBeforeFocusLoss = player.isPlaying || player.playWhenReady
                currentPauseReason = PauseReason.AUDIO_FOCUS_LOSS_TRANSIENT
                DevLogger.d(
                    "MusicPlaybackService",
                    "FOCUS_LOSS_TRANSIENT: wasPlayingBeforeLoss=$wasPlayingBeforeFocusLoss"
                )
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck - reduce volume instead of pausing
                DevLogger.d("MusicPlaybackService", "Can duck audio, reducing volume")
                isDucked = true
                if (player.isPlaying) {
                    player.volume = DUCK_VOLUME
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeFocusLoss = player.isPlaying || player.playWhenReady
                currentPauseReason = PauseReason.AUDIO_FOCUS_LOSS_PERMANENT
                DevLogger.d(
                    "MusicPlaybackService",
                    "FOCUS_LOSS_PERMANENT: wasPlayingBeforeLoss=$wasPlayingBeforeFocusLoss"
                )
            }
        }
    }

    /**
     * Player listener to track audio focus state changes and errors.
     * ExoPlayer automatically pauses on audio focus loss.
     * We track the state to conditionally resume when focus is regained.
     */
    private val audioFocusListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            when (reason) {
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                    if (!playWhenReady) {
                        val wasPlayingBeforePause = lastPlayWhenReady || player.isPlaying || wasPlayingBeforeFocusLoss
                        if (wasPlayingBeforePause) {
                            wasPlayingBeforeFocusLoss = true
                        }
                        val resolvedPauseReason = when (currentAudioFocusState) {
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> PauseReason.AUDIO_FOCUS_LOSS_TRANSIENT
                            AudioManager.AUDIOFOCUS_LOSS -> PauseReason.AUDIO_FOCUS_LOSS_PERMANENT
                            else -> if (wasPlayingBeforePause || isAndroidAutoRecentlyActive()) {
                                PauseReason.AUDIO_FOCUS_LOSS_TRANSIENT
                            } else {
                                PauseReason.AUDIO_FOCUS_LOSS_PERMANENT
                            }
                        }
                        currentPauseReason = resolvedPauseReason
                        DevLogger.d(
                            "MusicPlaybackService",
                            "PLAYER_FOCUS_PAUSE: reason=${resolvedPauseReason.name}, focusState=$currentAudioFocusState, wasPlayingBeforePause=$wasPlayingBeforePause, androidAutoRecent=${isAndroidAutoRecentlyActive()}"
                        )

                        if (resolvedPauseReason == PauseReason.AUDIO_FOCUS_LOSS_TRANSIENT &&
                            wasPlayingBeforePause &&
                            shouldRecoverAfterTransientFocusLoss()
                        ) {
                            lastTransientRecoveryAtMs = System.currentTimeMillis()
                            serviceScope.launch {
                                delay(TRANSIENT_RECOVERY_DELAY_MS)
                                if (!player.isPlaying &&
                                    currentPauseReason == PauseReason.AUDIO_FOCUS_LOSS_TRANSIENT &&
                                    shouldRecoverAfterTransientFocusLoss(ignoreCooldown = true)
                                ) {
                                    val focusRequested = requestAudioFocusForPlayback()
                                    DevLogger.d(
                                        "MusicPlaybackService",
                                        "TRANSIENT_FOCUS_RECOVERY_PLAY: focusRequested=$focusRequested"
                                    )
                                    player.play()
                                    currentPauseReason = PauseReason.NONE
                                    wasPlayingBeforeFocusLoss = false
                                }
                            }
                        } else if (resolvedPauseReason == PauseReason.AUDIO_FOCUS_LOSS_PERMANENT &&
                            wasPlayingBeforePause &&
                            shouldRecoverAfterPermanentFocusLoss()
                        ) {
                            serviceScope.launch {
                                delay(FOCUS_RECOVERY_DELAY_MS)
                                if (!player.isPlaying &&
                                    currentPauseReason == PauseReason.AUDIO_FOCUS_LOSS_PERMANENT &&
                                    shouldRecoverAfterPermanentFocusLoss()
                                ) {
                                    val focusRequested = requestAudioFocusForPlayback()
                                    lastForcedRecoveryAtMs = System.currentTimeMillis()
                                    DevLogger.d(
                                        "MusicPlaybackService",
                                        "FORCED_ANDROID_AUTO_RECOVERY_PLAY: focusRequested=$focusRequested"
                                    )
                                    player.play()
                                    currentPauseReason = PauseReason.NONE
                                    wasPlayingBeforeFocusLoss = false
                                }
                            }
                        } else if (resolvedPauseReason == PauseReason.AUDIO_FOCUS_LOSS_PERMANENT &&
                            wasPlayingBeforePause &&
                            isWithinPostRecoveryRetryWindow() &&
                            shouldRecoverAfterPermanentFocusLoss(ignoreCooldown = true) &&
                            canRunPostRecoveryRetry()
                        ) {
                            lastPostRecoveryRetryAtMs = System.currentTimeMillis()
                            serviceScope.launch {
                                delay(POST_RECOVERY_RETRY_DELAY_MS)
                                if (!player.isPlaying &&
                                    currentPauseReason == PauseReason.AUDIO_FOCUS_LOSS_PERMANENT &&
                                    shouldRecoverAfterPermanentFocusLoss(ignoreCooldown = true)
                                ) {
                                    val focusRequested = requestAudioFocusForPlayback()
                                    DevLogger.d(
                                        "MusicPlaybackService",
                                        "POST_RECOVERY_FOCUS_RETRY_PLAY: focusRequested=$focusRequested"
                                    )
                                    player.play()
                                    currentPauseReason = PauseReason.NONE
                                    wasPlayingBeforeFocusLoss = false
                                }
                            }
                        }
                    }
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                    // User explicitly played/paused - mark reason as user pause
                    if (!playWhenReady) {
                        currentPauseReason = PauseReason.USER_PAUSE
                        wasPlayingBeforeFocusLoss = false
                        DevLogger.d("MusicPlaybackService", "User paused playback")
                    } else {
                        currentPauseReason = PauseReason.NONE
                        wasPlayingBeforeFocusLoss = false
                        DevLogger.d("MusicPlaybackService", "User resumed playback")
                    }
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> {
                    if (playWhenReady) {
                        currentPauseReason = PauseReason.NONE
                        wasPlayingBeforeFocusLoss = false
                    }
                }
                else -> {
                    if (playWhenReady) {
                        currentPauseReason = PauseReason.NONE
                        wasPlayingBeforeFocusLoss = false
                    }
                }
            }
            lastPlayWhenReady = playWhenReady
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            error?.let {
                DevLogger.e("MusicPlaybackService", "Playback error: ${it.message}", it)
            }
        }

        override fun onVolumeChanged(volume: Float) {
            // Track volume changes (including ducking by ExoPlayer)
            if (!isDucked) {
                normalVolume = volume
            }
            DevLogger.d("MusicPlaybackService", "Volume changed: $volume (isDucked: $isDucked)")
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                DevLogger.d("MusicPlaybackService", "Audio session ID changed to $audioSessionId, re-initializing effects")
                // Synchronize release to prevent flow collector from accessing released manager
                synchronized(audioEffectLock) {
                    audioEffectManager?.release()
                    audioEffectManager = null
                }
                initializeAudioEffects()
            }
        }
    }

    /**
     * Player listener to trigger crossfade animation and apply per-song settings on media item transitions.
     */
    private val crossfadeListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            // Apply crossfade on natural track transitions (reason 0, 1, 2)
            // Skip only on explicit playlist changes or unknown reasons
            if (reason in 0..2) {
                crossfadeManager?.applyCrossfade(currentCrossfadeDurationMs)
            }

            // Load and apply per-song audio settings
            serviceScope.launch {
                try {
                    val songId = mediaItem?.mediaId?.toLongOrNull() ?: return@launch
                    val perSongSettings = songAudioSettingsDao.getSettings(songId)
                    if (perSongSettings != null && perSongSettings.isEnabled) {
                        audioEffectManager?.applyPerSongSettings(perSongSettings.toDomain())
                        player.playbackParameters = androidx.media3.common.PlaybackParameters(
                            perSongSettings.speed,
                            perSongSettings.pitch
                        )
                        DevLogger.d("MusicPlaybackService", "Applied per-song settings for song $songId")
                    } else {
                        // Revert to global EQ
                        audioEffectManager?.resetToDefaults()
                        player.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                    }
                } catch (e: Exception) {
                    DevLogger.e("MusicPlaybackService", "Failed to apply per-song settings", e)
                }
            }
        }
    }

    /**
     * Register BroadcastReceiver for ACTION_AUDIO_BECOMING_NOISY.
     * This handles headphone unplugging.
     * Only pauses playback if the preference is enabled AND playback is currently active.
     */
    private fun registerAudioNoisyReceiver() {
        audioNoisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    // Only pause if setting is enabled and playback is active
                    if (pauseOnAudioNoisy && player.isPlaying) {
                        serviceScope.launch {
                            // Give route handoff a short window (Android Auto/Bluetooth connect path)
                            delay(NOISY_ROUTE_SETTLE_DELAY_MS)
                            val externalOutputActive = hasExternalAudioOutput()
                            if (isAndroidAutoRecentlyActive() || externalOutputActive) {
                                DevLogger.d(
                                    "MusicPlaybackService",
                                    "NOISY_IGNORED_FOR_ROUTE_HANDOFF: androidAutoRecent=${isAndroidAutoRecentlyActive()}, externalOutputActive=$externalOutputActive"
                                )
                                return@launch
                            }
                            if (player.isPlaying) {
                                DevLogger.d("MusicPlaybackService", "Headphones disconnected, pausing playback")
                                player.pause()

                                // Mark pause reason as headphone unplug - DO NOT resume on this
                                currentPauseReason = PauseReason.AUDIO_BECOMING_NOISY
                                wasPlayingBeforeFocusLoss = false
                            }
                        }
                    } else if (!pauseOnAudioNoisy) {
                        DevLogger.d("MusicPlaybackService", "Headphones disconnected, but pause on audio noisy is disabled")
                    } else {
                        DevLogger.d("MusicPlaybackService", "Headphones disconnected, but playback already paused")
                    }
                }
            }
        }

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioNoisyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(audioNoisyReceiver, filter)
        }
    }

    /**
     * Unregister BroadcastReceiver when service is destroyed.
     */
    private fun unregisterAudioNoisyReceiver() {
        audioNoisyReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
        audioNoisyReceiver = null
    }

    private fun attachPlayerListenersIfNeeded() {
        if (playerListenersAttached) return
        player.addListener(audioFocusListener)
        player.addListener(crossfadeListener)
        playerListenersAttached = true
    }

    private fun detachPlayerListenersIfNeeded() {
        if (!playerListenersAttached) return
        player.removeListener(audioFocusListener)
        player.removeListener(crossfadeListener)
        playerListenersAttached = false
    }

    /**
     * Starts the service in foreground mode without crashing the app on restricted starts.
     * On Android 12+, this can throw when the app isn't allowed to start foreground service.
     */
    private fun startForegroundSafely(notificationId: Int, notification: Notification): Boolean {
        return try {
            startForeground(notificationId, notification)
            isForeground = true
            true
        } catch (e: RuntimeException) {
            val isForegroundStartNotAllowed =
                e.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException"
            if (isForegroundStartNotAllowed || e is IllegalStateException || e is SecurityException) {
                DevLogger.e(
                    "MusicPlaybackService",
                    "Foreground start blocked by system (will keep service alive without crashing)",
                    e
                )
                false
            } else {
                throw e
            }
        }
    }

    /**
     * Create MediaDescriptionAdapter for notification content.
     * Provides title, artist, album, and album art for the notification.
     */
    private fun createDescriptionAdapter(): PlayerNotificationManager.MediaDescriptionAdapter {
        return object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.currentMediaItem?.mediaMetadata?.title ?: getString(R.string.now_playing_unknown_song)
            }

            override fun createCurrentContentIntent(player: Player): PendingIntent? {
                val intent = Intent(this@MusicPlaybackService, MainActivity::class.java)
                return PendingIntent.getActivity(
                    this@MusicPlaybackService,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return player.currentMediaItem?.mediaMetadata?.artist
            }

            override fun getCurrentSubText(player: Player): CharSequence? {
                return player.currentMediaItem?.mediaMetadata?.albumTitle
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): Bitmap? {
                val artworkUri = player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()

                // Load album art asynchronously
                scope.launch {
                    val bitmap = albumArtLoader.loadForNotification(artworkUri)
                    bitmap?.let { callback.onBitmap(it) }
                }

                // Return null immediately, callback will be invoked when bitmap is loaded
                return null
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        markControllerActivity(controllerInfo.packageName)
        return mediaSession
    }

    private fun markControllerActivity(packageName: String?) {
        val pkg = packageName ?: return
        if (pkg == this.packageName) return

        lastExternalControllerSeenAtMs = System.currentTimeMillis()
        if (isAndroidAutoController(pkg)) {
            lastAndroidAutoControllerSeenAtMs = System.currentTimeMillis()
            AndroidAutoConnectionTracker.markInteraction()
            DevLogger.d(
                "MusicPlaybackService",
                "ANDROID_AUTO_CONTROLLER_CONNECTED: package=$pkg"
            )
        } else {
            DevLogger.d("MusicPlaybackService", "EXTERNAL_MEDIA_CONTROLLER_CONNECTED: package=$pkg")
        }
    }

    private fun isAndroidAutoController(packageName: String?): Boolean {
        val pkg = packageName ?: return false
        if (pkg == "com.google.android.projection.gearhead") return true
        if (pkg == "com.google.android.apps.automotive.media") return true
        if (pkg == "com.android.car.media") return true
        return pkg.contains("gearhead", ignoreCase = true) ||
            pkg.contains("automotive", ignoreCase = true) ||
            pkg.contains(".car.", ignoreCase = true)
    }

    private fun isAndroidAutoRecentlyActive(): Boolean {
        val localRecent = if (lastAndroidAutoControllerSeenAtMs <= 0L) {
            false
        } else {
            System.currentTimeMillis() - lastAndroidAutoControllerSeenAtMs <= ANDROID_AUTO_ACTIVE_WINDOW_MS
        }
        return localRecent || AndroidAutoConnectionTracker.isRecent(ANDROID_AUTO_ACTIVE_WINDOW_MS)
    }

    private fun isExternalControllerRecentlyActive(): Boolean {
        if (lastExternalControllerSeenAtMs <= 0L) return false
        return System.currentTimeMillis() - lastExternalControllerSeenAtMs <= EXTERNAL_CONTROLLER_ACTIVE_WINDOW_MS
    }

    private fun isRecentServiceStartActivity(): Boolean {
        if (lastServiceStartAtMs <= 0L) return false
        return System.currentTimeMillis() - lastServiceStartAtMs <= SERVICE_START_RECOVERY_WINDOW_MS
    }

    private fun shouldRecoverAfterPermanentFocusLoss(ignoreCooldown: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!ignoreCooldown && now - lastForcedRecoveryAtMs < FORCED_RECOVERY_COOLDOWN_MS) {
            DevLogger.d("MusicPlaybackService", "Skip forced recovery: cooldown active")
            return false
        }

        val hasControllerSignal = isAndroidAutoRecentlyActive() || isExternalControllerRecentlyActive()
        val hasServiceSignal = isRecentServiceStartActivity()

        val manager = audioManager ?: return true
        val mode = manager.mode
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
            DevLogger.d("MusicPlaybackService", "Skip forced recovery: in-call mode")
            return false
        }
        // Avoid fighting another active media app.
        if (manager.isMusicActive && !player.isPlaying && !hasControllerSignal && !hasServiceSignal) {
            DevLogger.d("MusicPlaybackService", "Skip forced recovery: another app is active")
            return false
        }
        if (currentPauseReason == PauseReason.USER_PAUSE || currentPauseReason == PauseReason.AUDIO_BECOMING_NOISY) {
            DevLogger.d("MusicPlaybackService", "Skip forced recovery: pause reason=${currentPauseReason.name}")
            return false
        }
        if (!hasControllerSignal && !hasServiceSignal) {
            DevLogger.d("MusicPlaybackService", "Forced recovery without explicit handoff marker (global fallback)")
        } else if (!hasControllerSignal && hasServiceSignal) {
            DevLogger.d("MusicPlaybackService", "Forced recovery using service-start handoff signal")
        }
        return true
    }

    private fun shouldRecoverAfterTransientFocusLoss(ignoreCooldown: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!ignoreCooldown && now - lastTransientRecoveryAtMs < TRANSIENT_RECOVERY_COOLDOWN_MS) {
            DevLogger.d("MusicPlaybackService", "Skip transient recovery: cooldown active")
            return false
        }

        val hasControllerSignal = isAndroidAutoRecentlyActive() || isExternalControllerRecentlyActive()
        val hasServiceSignal = isRecentServiceStartActivity()

        val manager = audioManager ?: return true
        val mode = manager.mode
        if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
            DevLogger.d("MusicPlaybackService", "Skip transient recovery: in-call mode")
            return false
        }
        if (manager.isMusicActive && !player.isPlaying && !hasControllerSignal && !hasServiceSignal) {
            DevLogger.d("MusicPlaybackService", "Skip transient recovery: another app is active")
            return false
        }
        if (currentPauseReason == PauseReason.USER_PAUSE || currentPauseReason == PauseReason.AUDIO_BECOMING_NOISY) {
            DevLogger.d("MusicPlaybackService", "Skip transient recovery: pause reason=${currentPauseReason.name}")
            return false
        }
        return true
    }

    private fun isWithinPostRecoveryRetryWindow(): Boolean {
        if (lastForcedRecoveryAtMs <= 0L) return false
        return System.currentTimeMillis() - lastForcedRecoveryAtMs <= POST_RECOVERY_RETRY_WINDOW_MS
    }

    private fun canRunPostRecoveryRetry(): Boolean {
        if (lastPostRecoveryRetryAtMs <= 0L) return true
        return System.currentTimeMillis() - lastPostRecoveryRetryAtMs > POST_RECOVERY_RETRY_COOLDOWN_MS
    }

    private fun requestAudioFocusForPlayback(): Boolean {
        val manager = audioManager ?: return false
        return try {
            val result = manager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (e: Exception) {
            DevLogger.e("MusicPlaybackService", "Audio focus request failed during recovery", e)
            false
        }
    }

    private fun hasExternalAudioOutput(): Boolean {
        val manager = audioManager ?: return false
        return try {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
                when (device.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_HDMI,
                    AudioDeviceInfo.TYPE_AUX_LINE,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER,
                    AudioDeviceInfo.TYPE_BLE_BROADCAST -> true
                    else -> false
                }
            }
        } catch (e: Exception) {
            DevLogger.e("MusicPlaybackService", "Failed to inspect audio output devices", e)
            false
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Only stop service if player is not playing
        if (!player.playWhenReady) {
            DevLogger.d("MusicPlaybackService", "Task removed and not playing, stopping service")
            stopSelf()
        } else {
            DevLogger.d("MusicPlaybackService", "Task removed but still playing, keeping service alive")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastServiceStartAtMs = System.currentTimeMillis()
        // Log service start for debugging
        DevLogger.d("MusicPlaybackService", "onStartCommand: action=${intent?.action}, flags=$flags, startId=$startId")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        DevLogger.w("MusicPlaybackService", "Low memory warning received")

        // Don't stop playback, but log for monitoring
        // The system may kill us anyway, but we shouldn't proactively stop
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        DevLogger.d("MusicPlaybackService", "onTrimMemory: level=$level")

        // Handle different trim memory levels
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                // System is running very low on memory, but keep playing if active
                DevLogger.w("MusicPlaybackService", "Critical memory pressure, but maintaining playback")
            }
        }
    }

    override fun onDestroy() {
        unregisterAudioNoisyReceiver()

        // Abandon audio focus
        audioManager?.abandonAudioFocus(audioFocusChangeListener)

        // Release audio effects
        audioEffectManager?.release()
        audioEffectManager = null

        // Release crossfade manager
        crossfadeManager?.release()
        crossfadeManager = null

        // Clean up notification manager
        notificationManager?.setPlayer(null)
        notificationManager = null

        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }

        try {
            detachPlayerListenersIfNeeded()
            if (sessionRegistered) {
                removeSession(mediaSession)
                sessionRegistered = false
            }
        } catch (e: Exception) {
            DevLogger.e("MusicPlaybackService", "Error during service cleanup", e)
        }

        sleepTimerObserverJob?.cancel()
        sleepTimerJob?.cancel()
        serviceScope.cancel()

        super.onDestroy()
    }
}

/**
 * Extension function to convert SongAudioSettingsEntity to domain model.
 */
private fun com.sukoon.music.data.local.entity.SongAudioSettingsEntity.toDomain(): com.sukoon.music.domain.model.SongAudioSettings {
    return com.sukoon.music.domain.model.SongAudioSettings(
        songId = songId,
        isEnabled = isEnabled,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        eqEnabled = eqEnabled,
        band60Hz = band60Hz,
        band230Hz = band230Hz,
        band910Hz = band910Hz,
        band3600Hz = band3600Hz,
        band14000Hz = band14000Hz,
        bassBoost = bassBoost,
        virtualizerStrength = virtualizerStrength,
        reverbPreset = reverbPreset,
        pitch = pitch,
        speed = speed,
        updatedAt = updatedAt
    )
}
