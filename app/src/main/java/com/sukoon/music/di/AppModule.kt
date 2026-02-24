package com.sukoon.music.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.sukoon.music.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.sukoon.music.data.local.SukoonDatabase
import com.sukoon.music.data.local.dao.EqualizerPresetDao
import com.sukoon.music.data.local.dao.GenreCoverDao
import com.sukoon.music.data.local.dao.ListeningStatsDao
import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.dao.PlaylistDao
import com.sukoon.music.data.local.dao.QueueDao
import com.sukoon.music.data.local.dao.RecentlyPlayedAlbumDao
import com.sukoon.music.data.local.dao.RecentlyPlayedArtistDao
import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SearchHistoryDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.remote.LyricsApi
import com.sukoon.music.data.repository.AudioEffectRepositoryImpl
import com.sukoon.music.data.repository.FeedbackRepositoryImpl
import com.sukoon.music.data.repository.PlaybackRepositoryImpl
import com.sukoon.music.data.repository.QueueRepositoryImpl
import com.sukoon.music.data.repository.SearchHistoryRepositoryImpl
import com.sukoon.music.data.repository.SongRepositoryImpl
import com.sukoon.music.data.service.AlbumArtLoader
import com.sukoon.music.data.source.MediaStoreScanner
import com.sukoon.music.domain.repository.AudioEffectRepository
import com.sukoon.music.domain.repository.FeedbackRepository
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.QueueRepository
import com.sukoon.music.domain.repository.SearchHistoryRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlbumArtLoader(
        @ApplicationContext context: Context
    ): AlbumArtLoader {
        return AlbumArtLoader(context)
    }

    @Provides
    @Singleton
    fun provideSukoonDatabase(
        @ApplicationContext context: Context
    ): SukoonDatabase {
        return Room.databaseBuilder(
            context,
            SukoonDatabase::class.java,
            SukoonDatabase.DATABASE_NAME
        )
        .addMigrations(
            SukoonDatabase.MIGRATION_2_3,
            SukoonDatabase.MIGRATION_3_4,
            SukoonDatabase.MIGRATION_4_5,
            SukoonDatabase.MIGRATION_5_6,
            SukoonDatabase.MIGRATION_6_7,
            SukoonDatabase.MIGRATION_7_8,
            SukoonDatabase.MIGRATION_8_9,
            SukoonDatabase.MIGRATION_9_10,
            SukoonDatabase.MIGRATION_10_11,
            SukoonDatabase.MIGRATION_11_12,
            SukoonDatabase.MIGRATION_12_13,
            SukoonDatabase.MIGRATION_13_14,
            SukoonDatabase.MIGRATION_14_15,
            SukoonDatabase.MIGRATION_15_16,
            SukoonDatabase.MIGRATION_16_17
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(database: SukoonDatabase) = database.songDao()

    @Provides
    @Singleton
    fun provideLyricsDao(database: SukoonDatabase) = database.lyricsDao()

    @Provides
    @Singleton
    fun provideRecentlyPlayedDao(database: SukoonDatabase) = database.recentlyPlayedDao()

    @Provides
    @Singleton
    fun provideRecentlyPlayedArtistDao(database: SukoonDatabase) = database.recentlyPlayedArtistDao()

    @Provides
    @Singleton
    fun provideRecentlyPlayedAlbumDao(database: SukoonDatabase) = database.recentlyPlayedAlbumDao()

    @Provides
    @Singleton
    fun providePlaylistDao(database: SukoonDatabase) = database.playlistDao()

    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: SukoonDatabase) = database.searchHistoryDao()

    @Provides
    @Singleton
    fun provideEqualizerPresetDao(database: SukoonDatabase) = database.equalizerPresetDao()

    @Provides
    @Singleton
    fun provideDeletedPlaylistDao(database: SukoonDatabase) = database.deletedPlaylistDao()

    @Provides
    @Singleton
    fun provideQueueDao(database: SukoonDatabase) = database.queueDao()

    @Provides
    @Singleton
    fun provideGenreCoverDao(database: SukoonDatabase) = database.genreCoverDao()

    @Provides
    @Singleton
    fun provideListeningStatsDao(database: SukoonDatabase) = database.listeningStatsDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        val tlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(
                TlsVersion.TLS_1_2,
                TlsVersion.TLS_1_3
            )
            .allEnabledCipherSuites()
            .build()

        return OkHttpClient.Builder()
            .connectionSpecs(
                listOf(tlsSpec)
            )  // CLEARTEXT removed: not needed for HTTPS-only APIs (LRCLIB, Gemini)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Sukoon Music Player/1.0.0 (Android)")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideLyricsApi(okHttpClient: OkHttpClient): LyricsApi {
        return Retrofit.Builder()
            .baseUrl(LyricsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LyricsApi::class.java)
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    @UnstableApi
    @Provides
    @Singleton
    fun providePlaybackRepository(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        songRepository: SongRepository,
        preferencesManager: com.sukoon.music.data.preferences.PreferencesManager,
        queueRepository: QueueRepository,
        listeningStatsRepository: com.sukoon.music.domain.repository.ListeningStatsRepository,
        sessionController: com.sukoon.music.domain.usecase.SessionController
    ): PlaybackRepository {
        return PlaybackRepositoryImpl(context, scope, songRepository, preferencesManager, queueRepository, listeningStatsRepository, sessionController)
    }

    @Provides
    @Singleton
    fun provideMediaStoreScanner(
        @ApplicationContext context: Context
    ): MediaStoreScanner {
        return MediaStoreScanner(context)
    }

    @Provides
    @Singleton
    fun provideSongRepository(
        songDao: SongDao,
        recentlyPlayedDao: RecentlyPlayedDao,
        recentlyPlayedArtistDao: RecentlyPlayedArtistDao,
        recentlyPlayedAlbumDao: RecentlyPlayedAlbumDao,
        playlistDao: PlaylistDao,
        genreCoverDao: GenreCoverDao,
        mediaStoreScanner: MediaStoreScanner,
        preferencesManager: com.sukoon.music.data.preferences.PreferencesManager,
        @ApplicationScope scope: CoroutineScope
    ): SongRepository {
        return SongRepositoryImpl(
            songDao,
            recentlyPlayedDao,
            recentlyPlayedArtistDao,
            recentlyPlayedAlbumDao,
            playlistDao,
            genreCoverDao,
            mediaStoreScanner,
            preferencesManager,
            scope
        )
    }

    @Provides
    @Singleton
    fun provideOfflineLyricsScanner(
        @ApplicationContext context: Context
    ): com.sukoon.music.data.lyrics.OfflineLyricsScanner {
        return com.sukoon.music.data.lyrics.OfflineLyricsScanner(context)
    }

    @Provides
    @Singleton
    fun provideId3LyricsExtractor(
        @ApplicationContext context: Context
    ): com.sukoon.music.data.lyrics.Id3LyricsExtractor {
        return com.sukoon.music.data.lyrics.Id3LyricsExtractor(context)
    }

    // Gemini AI providers (for metadata correction)
    @Provides
    @Singleton
    @Named("GeminiClient")
    fun provideGeminiOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)  // No auto-retry for Gemini
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiApi(@Named("GeminiClient") okHttpClient: OkHttpClient): com.sukoon.music.data.remote.GeminiApi {
        return Retrofit.Builder()
            .baseUrl(com.sukoon.music.data.remote.GeminiApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.sukoon.music.data.remote.GeminiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiMetadataCorrector(
        geminiApi: com.sukoon.music.data.remote.GeminiApi,
        @ApplicationContext context: Context,
        preferencesManager: com.sukoon.music.data.preferences.PreferencesManager
    ): com.sukoon.music.data.metadata.GeminiMetadataCorrector {
        return com.sukoon.music.data.metadata.GeminiMetadataCorrector(geminiApi, context, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideLyricsRepository(
        lyricsDao: LyricsDao,
        lyricsApi: LyricsApi,
        offlineLyricsScanner: com.sukoon.music.data.lyrics.OfflineLyricsScanner,
        id3LyricsExtractor: com.sukoon.music.data.lyrics.Id3LyricsExtractor,
        geminiMetadataCorrector: com.sukoon.music.data.metadata.GeminiMetadataCorrector
    ): com.sukoon.music.domain.repository.LyricsRepository {
        return com.sukoon.music.data.repository.LyricsRepositoryImpl(
            lyricsDao,
            lyricsApi,
            offlineLyricsScanner,
            id3LyricsExtractor,
            geminiMetadataCorrector
        )
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(
        playlistDao: PlaylistDao,
        songDao: SongDao,
        deletedPlaylistDao: com.sukoon.music.data.local.dao.DeletedPlaylistDao,
        @ApplicationScope scope: CoroutineScope
    ): com.sukoon.music.domain.repository.PlaylistRepository {
        return com.sukoon.music.data.repository.PlaylistRepositoryImpl(playlistDao, songDao, deletedPlaylistDao, scope)
    }

    @Provides
    @Singleton
    fun provideQueueRepository(
        queueDao: QueueDao,
        songDao: SongDao,
        @ApplicationScope scope: CoroutineScope
    ): QueueRepository {
        return QueueRepositoryImpl(queueDao, songDao, scope)
    }

    @Provides
    @Singleton
    fun provideSearchHistoryRepository(
        searchHistoryDao: SearchHistoryDao
    ): SearchHistoryRepository {
        return SearchHistoryRepositoryImpl(searchHistoryDao)
    }

    @Provides
    @Singleton
    fun provideAudioEffectRepository(
        preferencesManager: com.sukoon.music.data.preferences.PreferencesManager,
        equalizerPresetDao: EqualizerPresetDao
    ): AudioEffectRepository {
        return AudioEffectRepositoryImpl(preferencesManager, equalizerPresetDao)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): com.sukoon.music.data.preferences.PreferencesManager {
        return com.sukoon.music.data.preferences.PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context
    ): coil.ImageLoader {
        return coil.ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context,
        preferencesManager: com.sukoon.music.data.preferences.PreferencesManager,
        database: SukoonDatabase,
        imageLoader: coil.ImageLoader,
        songDao: com.sukoon.music.data.local.dao.SongDao
    ): com.sukoon.music.domain.repository.SettingsRepository {
        return com.sukoon.music.data.repository.SettingsRepositoryImpl(
            context,
            preferencesManager,
            database,
            imageLoader,
            songDao
        )
    }

    @Provides
    @Singleton
    fun provideAdMobManager(
        @ApplicationContext context: Context
    ): com.sukoon.music.data.ads.AdMobManager {
        val adMobManager = com.sukoon.music.data.ads.AdMobManager(context)
        adMobManager.initialize()
        return adMobManager
    }

    @Provides
    @Singleton
    fun provideSessionController(): com.sukoon.music.domain.usecase.SessionController {
        return com.sukoon.music.data.usecase.SessionControllerImpl()
    }

    @Provides
    @Singleton
    fun provideListeningStatsRepository(
        listeningStatsDao: ListeningStatsDao
    ): com.sukoon.music.domain.repository.ListeningStatsRepository {
        return com.sukoon.music.data.repository.ListeningStatsRepositoryImpl(listeningStatsDao)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance().apply {
            try {
                firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
            } catch (e: Exception) {
                // Settings already configured, skip
            }
        }
    }

    @Provides
    @Singleton
    fun provideFeedbackRepository(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context,
        preferencesManager: com.sukoon.music.data.preferences.PreferencesManager
    ): FeedbackRepository {
        return FeedbackRepositoryImpl(firestore, context, preferencesManager)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
