package com.sukoon.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sukoon.music.data.local.dao.DeletedPlaylistDao
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
import com.sukoon.music.data.local.dao.SongAudioSettingsDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.DeletedPlaylistEntity
import com.sukoon.music.data.local.entity.EqualizerPresetEntity
import com.sukoon.music.data.local.entity.GenreCoverEntity
import com.sukoon.music.data.local.entity.ListeningStatsArtistDailyEntity
import com.sukoon.music.data.local.entity.ListeningStatsBucketDailyEntity
import com.sukoon.music.data.local.entity.ListeningStatsEntity
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.local.entity.PlaylistEntity
import com.sukoon.music.data.local.entity.PlaylistSongCrossRef
import com.sukoon.music.data.local.entity.QueueEntity
import com.sukoon.music.data.local.entity.QueueItemEntity
import com.sukoon.music.data.local.entity.RecentlyPlayedAlbumEntity
import com.sukoon.music.data.local.entity.RecentlyPlayedArtistEntity
import com.sukoon.music.data.local.entity.RecentlyPlayedEntity
import com.sukoon.music.data.local.entity.SearchHistoryEntity
import com.sukoon.music.data.local.entity.SongAudioSettingsEntity
import com.sukoon.music.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        LyricsEntity::class,
        RecentlyPlayedEntity::class,
        RecentlyPlayedAlbumEntity::class,
        RecentlyPlayedArtistEntity::class,
        SearchHistoryEntity::class,
        EqualizerPresetEntity::class,
        DeletedPlaylistEntity::class,
        QueueEntity::class,
        QueueItemEntity::class,
        GenreCoverEntity::class,
        ListeningStatsEntity::class,
        ListeningStatsArtistDailyEntity::class,
        ListeningStatsBucketDailyEntity::class,
        SongAudioSettingsEntity::class
    ],
    version = 20,
    exportSchema = false
)
abstract class SukoonDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun recentlyPlayedAlbumDao(): RecentlyPlayedAlbumDao
    abstract fun recentlyPlayedArtistDao(): RecentlyPlayedArtistDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun equalizerPresetDao(): EqualizerPresetDao
    abstract fun deletedPlaylistDao(): DeletedPlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun genreCoverDao(): GenreCoverDao
    abstract fun listeningStatsDao(): ListeningStatsDao
    abstract fun songAudioSettingsDao(): SongAudioSettingsDao

    companion object {
        const val DATABASE_NAME = "sukoon_music_db"

        /**
         * Migration from version 2 to 3.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_song_cross_ref (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_song_cross_ref_songId " +
                    "ON playlist_song_cross_ref(songId)"
                )
            }
        }

        /**
         * Migration from version 3 to 4.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        query TEXT NOT NULL PRIMARY KEY,
                        timestamp INTEGER NOT NULL
                    )
                """)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_search_history_timestamp " +
                    "ON search_history(timestamp)"
                )
            }
        }

        /**
         * Migration from version 4 to 5.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE lyrics ADD COLUMN source TEXT NOT NULL DEFAULT 'UNKNOWN'"
                )
            }
        }

        /**
         * Migration from version 5 to 6.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS equalizer_presets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        band60Hz INTEGER NOT NULL,
                        band230Hz INTEGER NOT NULL,
                        band910Hz INTEGER NOT NULL,
                        band3600Hz INTEGER NOT NULL,
                        band14000Hz INTEGER NOT NULL,
                        bassBoost INTEGER NOT NULL,
                        virtualizerStrength INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        /**
         * Migration from version 6 to 7.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS deleted_playlists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        originalPlaylistId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        coverImageUri TEXT,
                        originalCreatedAt INTEGER NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        playlistDataJson TEXT NOT NULL
                    )
                """)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_deleted_playlists_deletedAt " +
                    "ON deleted_playlists(deletedAt)"
                )
            }
        }

        /**
         * Migration from version 7 to 8.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN folderPath TEXT"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_songs_folderPath " +
                    "ON songs(folderPath)"
                )
            }
        }

        /**
         * Migration from version 8 to 9.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recently_played_albums (
                        albumName TEXT NOT NULL PRIMARY KEY,
                        lastPlayedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        /**
         * Migration from version 9 to 10.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recently_played_artists (
                        artistName TEXT NOT NULL PRIMARY KEY,
                        lastPlayedAt INTEGER NOT NULL
                    )
                """)
            }
        }

        /**
         * Migration from version 10 to 11.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN genre TEXT NOT NULL DEFAULT 'Unknown Genre'"
                )
            }
        }

        /**
         * Migration from version 11 to 12.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS queues (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        modifiedAt INTEGER NOT NULL,
                        isCurrent INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS queue_items (
                        queueId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(queueId, position),
                        FOREIGN KEY(queueId) REFERENCES queues(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_queue_items_songId " +
                    "ON queue_items(songId)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_queue_items_queueId_position " +
                    "ON queue_items(queueId, position)"
                )
            }
        }

        /**
         * Migration from version 12 to 13.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS genre_covers (
                        genreId INTEGER PRIMARY KEY NOT NULL,
                        customArtworkUri TEXT NOT NULL
                    )
                """)
            }
        }

        /**
         * Migration from version 13 to 14.
         * Adds the listening_stats table for tracking daily listening statistics.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS listening_stats (
                        dateMs INTEGER PRIMARY KEY NOT NULL,
                        totalDurationMs INTEGER NOT NULL,
                        topArtist TEXT,
                        topArtistCount INTEGER NOT NULL,
                        peakTimeOfDay TEXT NOT NULL,
                        playCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_stats_dateMs " +
                    "ON listening_stats(dateMs)"
                )
            }
        }

        /**
         * Migration from version 14 to 15.
         * Changes songId foreign key in playlist_song_cross_ref from CASCADE to RESTRICT.
         * This prevents songs from being deleted if they're still in a playlist.
         * Preserves all existing playlist-song relationships.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Backup existing data
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_song_cross_ref_backup (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId)
                    )
                """)
                database.execSQL("INSERT INTO playlist_song_cross_ref_backup SELECT * FROM playlist_song_cross_ref")

                // Drop old table
                database.execSQL("DROP TABLE playlist_song_cross_ref")

                // Create new table with RESTRICT constraint on songId
                database.execSQL("""
                    CREATE TABLE playlist_song_cross_ref (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE RESTRICT
                    )
                """)

                // Restore data
                database.execSQL("INSERT INTO playlist_song_cross_ref SELECT * FROM playlist_song_cross_ref_backup")

                // Clean up backup
                database.execSQL("DROP TABLE playlist_song_cross_ref_backup")

                // Recreate index
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_song_cross_ref_songId " +
                    "ON playlist_song_cross_ref(songId)"
                )
            }
        }

        /**
         * Migration from version 15 to 16.
         * Reverts songId foreign key in playlist_song_cross_ref from RESTRICT back to CASCADE.
         * CASCADE deletion is required for proper media library scanning when songs are deleted.
         * Orphaned playlist entries are automatically cleaned up when their songs are removed.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Backup existing data
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_song_cross_ref_backup (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId)
                    )
                """)
                database.execSQL("INSERT INTO playlist_song_cross_ref_backup SELECT * FROM playlist_song_cross_ref")

                // Drop old table
                database.execSQL("DROP TABLE playlist_song_cross_ref")

                // Create new table with CASCADE constraint on songId
                database.execSQL("""
                    CREATE TABLE playlist_song_cross_ref (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE CASCADE
                    )
                """)

                // Restore data (this will succeed because CASCADE allows deletion)
                database.execSQL("INSERT INTO playlist_song_cross_ref SELECT * FROM playlist_song_cross_ref_backup")

                // Clean up backup
                database.execSQL("DROP TABLE playlist_song_cross_ref_backup")

                // Recreate index
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_song_cross_ref_songId " +
                    "ON playlist_song_cross_ref(songId)"
                )
            }
        }

        /**
         * Migration from version 16 to 17.
         * Changes songId foreign key in playlist_song_cross_ref from CASCADE to NO_ACTION.
         * This prevents automatic deletion of playlist entries when songs are removed during media scan.
         * Orphaned playlist-song references are preserved, allowing users to manually remove them.
         * Fixes bug: "Songs disappearing from playlists after media rescan"
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Backup existing data
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS playlist_song_cross_ref_backup (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId)
                    )
                """)
                database.execSQL("INSERT INTO playlist_song_cross_ref_backup SELECT * FROM playlist_song_cross_ref")

                // Drop old table
                database.execSQL("DROP TABLE playlist_song_cross_ref")

                // Create new table with NO_ACTION constraint on songId
                database.execSQL("""
                    CREATE TABLE playlist_song_cross_ref (
                        playlistId INTEGER NOT NULL,
                        songId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(playlistId, songId),
                        FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY(songId) REFERENCES songs(id) ON DELETE NO ACTION
                    )
                """)

                // Restore data
                database.execSQL("INSERT INTO playlist_song_cross_ref SELECT * FROM playlist_song_cross_ref_backup")

                // Clean up backup
                database.execSQL("DROP TABLE playlist_song_cross_ref_backup")

                // Recreate index
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_song_cross_ref_songId " +
                    "ON playlist_song_cross_ref(songId)"
                )
            }
        }

        /**
         * Migration from version 17 to 18.
         * Adds song_audio_settings table for per-song audio effects and playback settings.
         * Enables premium users to apply non-destructive per-song EQ, effects, pitch, speed, and trim.
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS song_audio_settings (
                        songId INTEGER PRIMARY KEY NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        trimStartMs INTEGER NOT NULL DEFAULT 0,
                        trimEndMs INTEGER NOT NULL DEFAULT -1,
                        eqEnabled INTEGER NOT NULL DEFAULT 0,
                        band60Hz INTEGER NOT NULL DEFAULT 0,
                        band230Hz INTEGER NOT NULL DEFAULT 0,
                        band910Hz INTEGER NOT NULL DEFAULT 0,
                        band3600Hz INTEGER NOT NULL DEFAULT 0,
                        band14000Hz INTEGER NOT NULL DEFAULT 0,
                        bassBoost INTEGER NOT NULL DEFAULT 0,
                        virtualizerStrength INTEGER NOT NULL DEFAULT 0,
                        reverbPreset INTEGER NOT NULL DEFAULT 0,
                        pitch REAL NOT NULL DEFAULT 1.0,
                        speed REAL NOT NULL DEFAULT 1.0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        /**
         * Migration from version 18 to 19.
         * Adds manual lyrics tracking columns to lyrics table.
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE lyrics ADD COLUMN isManual INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE lyrics ADD COLUMN manualUpdatedAt INTEGER"
                )
            }
        }

        /**
         * Migration from version 19 to 20.
         * Adds per-day artist/bucket aggregate tables and resets legacy listening_stats rows.
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_stats_artist_daily (
                        dateMs INTEGER NOT NULL,
                        artistName TEXT NOT NULL,
                        totalDurationMs INTEGER NOT NULL,
                        playCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(dateMs, artistName)
                    )
                    """
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS listening_stats_bucket_daily (
                        dateMs INTEGER NOT NULL,
                        bucket TEXT NOT NULL,
                        totalDurationMs INTEGER NOT NULL,
                        playCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(dateMs, bucket)
                    )
                    """
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_stats_artist_daily_dateMs " +
                        "ON listening_stats_artist_daily(dateMs)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_stats_artist_daily_artistName " +
                        "ON listening_stats_artist_daily(artistName)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_stats_bucket_daily_dateMs " +
                        "ON listening_stats_bucket_daily(dateMs)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_listening_stats_bucket_daily_bucket " +
                        "ON listening_stats_bucket_daily(bucket)"
                )

                // Reset legacy aggregates that were computed with inaccurate logic.
                database.execSQL("DELETE FROM listening_stats")
            }
        }
    }
}
