package com.sukoon.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sukoon.music.data.local.dao.DeletedPlaylistDao
import com.sukoon.music.data.local.dao.EqualizerPresetDao
import com.sukoon.music.data.local.dao.LyricsDao
import com.sukoon.music.data.local.dao.PlaylistDao
import com.sukoon.music.data.local.dao.RecentlyPlayedDao
import com.sukoon.music.data.local.dao.SearchHistoryDao
import com.sukoon.music.data.local.dao.SongDao
import com.sukoon.music.data.local.entity.DeletedPlaylistEntity
import com.sukoon.music.data.local.entity.EqualizerPresetEntity
import com.sukoon.music.data.local.entity.LyricsEntity
import com.sukoon.music.data.local.entity.PlaylistEntity
import com.sukoon.music.data.local.entity.PlaylistSongCrossRef
import com.sukoon.music.data.local.entity.RecentlyPlayedEntity
import com.sukoon.music.data.local.entity.SearchHistoryEntity
import com.sukoon.music.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        LyricsEntity::class,
        RecentlyPlayedEntity::class,
        SearchHistoryEntity::class,
        EqualizerPresetEntity::class,
        DeletedPlaylistEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class SukoonDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun equalizerPresetDao(): EqualizerPresetDao
    abstract fun deletedPlaylistDao(): DeletedPlaylistDao

    companion object {
        const val DATABASE_NAME = "sukoon_music_db"

        /**
         * Migration from version 2 to 3.
         * Adds the playlist_song_cross_ref junction table for many-to-many
         * relationship between playlists and songs.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create playlist_song_cross_ref table
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

                // Create index on songId for faster reverse lookups
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_playlist_song_cross_ref_songId " +
                    "ON playlist_song_cross_ref(songId)"
                )
            }
        }

        /**
         * Migration from version 3 to 4.
         * Adds the search_history table for storing search queries.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create search_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        query TEXT NOT NULL PRIMARY KEY,
                        timestamp INTEGER NOT NULL
                    )
                """)

                // Create index on timestamp for efficient ORDER BY queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_search_history_timestamp " +
                    "ON search_history(timestamp)"
                )
            }
        }

        /**
         * Migration from version 4 to 5.
         * Adds source column to lyrics table for tracking lyrics origin.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add source column to lyrics table with default value "UNKNOWN"
                database.execSQL(
                    "ALTER TABLE lyrics ADD COLUMN source TEXT NOT NULL DEFAULT 'UNKNOWN'"
                )
            }
        }

        /**
         * Migration from version 5 to 6.
         * Adds equalizer_presets table for storing custom EQ presets.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create equalizer_presets table
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
         * Adds deleted_playlists table for trash/restore functionality.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create deleted_playlists table
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

                // Create index on deletedAt for efficient cleanup queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_deleted_playlists_deletedAt " +
                    "ON deleted_playlists(deletedAt)"
                )
            }
        }

        /**
         * Migration from version 7 to 8.
         * Adds folderPath column to songs table for folder grouping.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add folderPath column to songs table
                database.execSQL(
                    "ALTER TABLE songs ADD COLUMN folderPath TEXT"
                )

                // Create index on folderPath for efficient folder queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_songs_folderPath " +
                    "ON songs(folderPath)"
                )
            }
        }
    }
}
