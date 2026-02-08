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
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.media3.common.PlaybackException

@UnstableApi
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    // CRITICAL: ExoPlayer is created and owned by this service, NOT injected.
    // This follows CLAUDE.md requirement: "ExoPlayer lives exclusively inside MediaSessionService"
    private lateinit var player: ExoPlayer

    @Inject
    lateinit var albumArtLoader: AlbumArtLoader

    @Inject
    lateinit var preferencesManager: com.sukoon.music.data.preferences.PreferencesManager

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    private var mediaSession: MediaSession? = null
    private var notificationManager: PlayerNotificationManager? = null
    private var isForeground = false

    // Audio effects manager for equalizer
    private var audioEffectManager: com.sukoon.music.data.audio.AudioEffectManager? = null

    // Audio noisy receiver for headphone unplug detection
    private var audioNoisyReceiver: BroadcastReceiver? = null

    // Preference: pause playback when audio becomes noisy (headphones unplugged)
    private var pauseOnAudioNoisy = true

    // Preference: resume playback when audio focus is regained
    private var resumeOnAudioFocus = false

    // Audio focus state tracking
    private enum class PauseReason {
        NONE,                    // Not paused or paused by user
        AUDIO_FOCUS_LOSS,        // Paused due to permanent audio focus loss
        AUDIO_BECOMING_NOISY,    // Paused due to headphones unplugging
        USER_PAUSE               // Explicitly paused by user
    }

    private var currentPauseReason = PauseReason.NONE
    private var currentAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var audioManager: AudioManager? = null

    // Volume state for ducking
    private var normalVolume = 1.0f
    private var isDucked = false

    // Notification visibility state
    private var isNotificationVisible = true

    // Crossfade manager for smooth track transitions
    private var crossfadeManager: com.sukoon.music.data.audio.CrossfadeManager? = null
    private var currentCrossfadeDurationMs: Int = 0

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "sukoon_music_playback"
        private const val DUCK_VOLUME = 0.2f // 20% volume when ducked
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize AudioManager for focus change callbacks
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // Create ExoPlayer instance - this MUST happen in the service, not via DI
            player = ExoPlayer.Builder(this).build()

            createNotificationChannel()
            initializePlayer()
            registerAudioNoisyReceiver()
            observeResumeOnAudioFocusSetting()
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
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
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

        // Add listener to track audio focus changes
        player.addListener(audioFocusListener)

        // Initialize crossfade manager
        crossfadeManager = com.sukoon.music.data.audio.CrossfadeManager(player, scope)

        // Add listener for crossfade transitions
        player.addListener(crossfadeListener)

        // Observe crossfade setting
        observeCrossfadeSetting()

        // Create MediaSession with callback for activity intent
        val intent = Intent(this, MainActivity::class.java)
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

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
                    if (ongoing && !isForeground) {
                        startForeground(notificationId, notification)
                        isForeground = true
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
                    // Only stop service if user dismissed; if programmatically cancelled
                    // (e.g., setPlayer(null) from notification visibility toggle), keep service alive
                    if (dismissedByUser) {
                        stopSelf()
                    }
                }
            })
            .build()

        // Configure the notification manager
        notificationManager?.apply {
            // Don't set player here - let observeNotificationVisibility() handle it based on preference
            setUsePreviousAction(true)
            setUseNextAction(true)
            setUseFastForwardAction(false)
            setUseRewindAction(false)
        }

        // Observe notification visibility preference FIRST (before setting player)
        observeNotificationVisibility()

        // Initialize audio effects (equalizer)
        initializeAudioEffects()

        // Observe pause on audio noisy preference
        observePauseOnAudioNoisySetting()
    }

    /**
     * Initialize AudioEffectManager and observe settings from DataStore.
     * Called after ExoPlayer is ready and has valid audio session ID.
     */
    private fun initializeAudioEffects() {
        try {
            // Get audio session ID from ExoPlayer
            val audioSessionId = player.audioSessionId
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
                DevLogger.w("MusicPlaybackService", "Audio session ID not available, skipping effects")
                return
            }

            // Create and initialize AudioEffectManager
            audioEffectManager = com.sukoon.music.data.audio.AudioEffectManager(audioSessionId).apply {
                val initialized = initialize()
                if (!initialized) {
                    DevLogger.e("MusicPlaybackService", "Failed to initialize audio effects")
                    return
                }
            }

            // Observe equalizer settings and apply them
            scope.launch {
                preferencesManager.equalizerSettingsFlow.collect { settings ->
                    audioEffectManager?.applySettings(settings)
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
        scope.launch {
            preferencesManager.showNotificationsFlow.collect { shouldShowNotification ->
                isNotificationVisible = shouldShowNotification
                notificationManager?.let { manager ->
                    if (shouldShowNotification) {
                        // Show notification by binding player
                        manager.setPlayer(player)
                        // Force notification refresh if player has media loaded
                        if (player.mediaItemCount > 0) {
                            manager.invalidate()
                        }
                        DevLogger.d("MusicPlaybackService", "Notification visibility enabled")
                    } else {
                        // Hide notification by unbinding player (playback continues)
                        manager.setPlayer(null)
                        DevLogger.d("MusicPlaybackService", "Notification visibility disabled")
                    }
                }
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
     * Audio focus change listener to handle focus gain and resumption.
     * Only resumes if:
     * - Feature is enabled
     * - Pause reason was AUDIO_FOCUS_LOSS (not user pause or headphone unplug)
     * - Player is not already playing
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Audio focus regained
                if (resumeOnAudioFocus &&
                    currentPauseReason == PauseReason.AUDIO_FOCUS_LOSS &&
                    !player.isPlaying) {
                    DevLogger.d("MusicPlaybackService", "Audio focus regained, resuming playback")
                    player.play()
                    currentPauseReason = PauseReason.NONE
                } else {
                    val reason = if (!resumeOnAudioFocus) "feature disabled"
                                 else if (currentPauseReason != PauseReason.AUDIO_FOCUS_LOSS) "pause reason is ${currentPauseReason.name}"
                                 else "already playing"
                    DevLogger.d("MusicPlaybackService", "Audio focus regained but not resuming ($reason)")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Transient focus loss - ignore for resume purposes
                DevLogger.d("MusicPlaybackService", "Transient audio focus loss, ignoring")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck - reduce volume instead of pausing
                DevLogger.d("MusicPlaybackService", "Can duck audio, reducing volume")
                isDucked = true
                if (player.isPlaying) {
                    player.volume = DUCK_VOLUME
                }
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
                    // Audio focus was lost - set pause reason to enable conditional resume
                    currentPauseReason = PauseReason.AUDIO_FOCUS_LOSS
                    DevLogger.d("MusicPlaybackService", "Audio focus lost, set pause reason to AUDIO_FOCUS_LOSS")
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                    // User explicitly played/paused - mark reason as user pause
                    if (!playWhenReady) {
                        currentPauseReason = PauseReason.USER_PAUSE
                        DevLogger.d("MusicPlaybackService", "User paused playback")
                    } else {
                        currentPauseReason = PauseReason.NONE
                        DevLogger.d("MusicPlaybackService", "User resumed playback")
                    }
                }
            }
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
    }

    /**
     * Player listener to trigger crossfade animation on media item transitions.
     */
    private val crossfadeListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            // Apply crossfade on natural track transitions (reason 0, 1, 2)
            // Skip only on explicit playlist changes or unknown reasons
            if (reason in 0..2) {
                crossfadeManager?.applyCrossfade(currentCrossfadeDurationMs)
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
                        DevLogger.d("MusicPlaybackService", "Headphones disconnected, pausing playback")
                        player.pause()

                        // Mark pause reason as headphone unplug - DO NOT resume on this
                        currentPauseReason = PauseReason.AUDIO_BECOMING_NOISY
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

    /**
     * Create MediaDescriptionAdapter for notification content.
     * Provides title, artist, album, and album art for the notification.
     */
    private fun createDescriptionAdapter(): PlayerNotificationManager.MediaDescriptionAdapter {
        return object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.currentMediaItem?.mediaMetadata?.title ?: "Unknown"
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
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Only stop service if player is not playing
        val player = mediaSession?.player
        if (player?.playWhenReady == false || player == null) {
            DevLogger.d("MusicPlaybackService", "Task removed and not playing, stopping service")
            stopSelf()
        } else {
            DevLogger.d("MusicPlaybackService", "Task removed but still playing, keeping service alive")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

        mediaSession?.run {
            player.removeListener(audioFocusListener)
            player.removeListener(crossfadeListener)
            player.release()
            release()
            mediaSession = null
        }

        super.onDestroy()
    }
}
