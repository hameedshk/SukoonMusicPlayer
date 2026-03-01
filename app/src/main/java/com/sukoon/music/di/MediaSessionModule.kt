package com.sukoon.music.di

import android.content.Context
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.sukoon.music.data.playback.PerSongSettingsCommand
import com.sukoon.music.data.playback.PlaybackCustomCommandBridge
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean
import com.sukoon.music.util.DevLogger

@Module
@InstallIn(SingletonComponent::class)
object MediaSessionModule {

    // Guard against double initialization from multiple services
    private val mediaSessionInitialized = AtomicBoolean(false)

    @Singleton
    @Provides
    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @Singleton
    @Provides
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        audioAttributes: AudioAttributes
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    @Singleton
    @Provides
    fun provideMediaSession(
        @ApplicationContext context: Context,
        player: ExoPlayer,
        customCommandBridge: PlaybackCustomCommandBridge
    ): MediaSession {
        // Ensure single MediaSession creation even if multiple services request it
        if (mediaSessionInitialized.getAndSet(true)) {
            DevLogger.d("MediaSessionModule", "MediaSession already initialized, reusing singleton instance")
        }
        return MediaSession.Builder(context, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val baseResult = super.onConnect(session, controller)
                    val sessionCommands = baseResult.availableSessionCommands
                        .buildUpon()
                        .add(SessionCommand(PerSongSettingsCommand.ACTION_APPLY, Bundle.EMPTY))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(baseResult.availablePlayerCommands)
                        .setAvailableSessionCommands(sessionCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    val resultCode = customCommandBridge.dispatch(customCommand.customAction, args)
                    return if (resultCode != null) {
                        Futures.immediateFuture(SessionResult(resultCode))
                    } else {
                        super.onCustomCommand(session, controller, customCommand, args)
                    }
                }
            })
            .build()
    }
}
