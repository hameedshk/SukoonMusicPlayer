package com.sukoon.music.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
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

    // Track if we were playing before audio focus loss
    private var wasPlayingBeforeAudioFocusLoss = false

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "sukoon_music_playback"
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // Create ExoPlayer instance - this MUST happen in the service, not via DI
            player = ExoPlayer.Builder(this).build()

            createNotificationChannel()
            initializePlayer()
            registerAudioNoisyReceiver()
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

        // Add listener to track audio focus changes
        player.addListener(audioFocusListener)

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
                    stopSelf()
                }
            })
            .build()

        // Configure the notification manager
        notificationManager?.apply {
            setPlayer(player)
            setUsePreviousAction(true)
            setUseNextAction(true)
            setUseFastForwardAction(false)
            setUseRewindAction(false)
        }

        // Initialize audio effects (equalizer)
        initializeAudioEffects()
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
     * Player listener to track audio focus state changes.
     * ExoPlayer automatically pauses on audio focus loss.
     * We track the state to prevent auto-resume.
     */
    private val audioFocusListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            when (reason) {
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                    // Audio focus was lost - track that we should not auto-resume
                    wasPlayingBeforeAudioFocusLoss = player.isPlaying
                }
            }
        }
    }

    /**
     * Register BroadcastReceiver for ACTION_AUDIO_BECOMING_NOISY.
     * This handles headphone unplugging.
     */
    private fun registerAudioNoisyReceiver() {
        audioNoisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    // Pause playback immediately
                    player.pause()

                    // Mark that we should NOT auto-resume
                    wasPlayingBeforeAudioFocusLoss = false
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
        val player = mediaSession?.player
        if (player?.playWhenReady == false) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        unregisterAudioNoisyReceiver()

        // Release audio effects
        audioEffectManager?.release()
        audioEffectManager = null

        // Clean up notification manager
        notificationManager?.setPlayer(null)
        notificationManager = null

        if (isForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isForeground = false
        }

        mediaSession?.run {
            player.removeListener(audioFocusListener)
            player.release()
            release()
            mediaSession = null
        }

        super.onDestroy()
    }
}
