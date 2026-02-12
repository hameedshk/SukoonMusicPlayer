package com.sukoon.music.data.source

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.sukoon.music.data.local.entity.SongEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * MediaStore data source for querying local audio files.
 *
 * This class is responsible for:
 * - Checking READ_MEDIA_AUDIO permissions
 * - Querying MediaStore.Audio.Media ContentProvider
 * - Extracting audio metadata from cursor
 * - Mapping MediaStore data to SongEntity
 *
 * CRITICAL: This class performs read-only operations.
 * It should only be invoked from foreground contexts.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Check if the app has permission to read audio files.
     * Handles Android version differences:
     * - Android 13+ (API 33+): Requires READ_MEDIA_AUDIO
     * - Android 12- (API 32-): Requires READ_EXTERNAL_STORAGE
     *
     * @return True if permission is granted, false otherwise
     */
    fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 and below
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Trigger a media scan for common music directories.
     * This forces Android to update MediaStore with recent file changes.
     */
    private suspend fun triggerMediaScan() {
        suspendCancellableCoroutine<Unit> { continuation ->
            val musicDirs = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Environment.getExternalStorageDirectory()
                } else {
                    @Suppress("DEPRECATION")
                    Environment.getExternalStorageDirectory()
                }
            ).filter { it.exists() && it.isDirectory }

            if (musicDirs.isEmpty()) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            // Scan all music directories - callback is called for each path
            val paths = musicDirs.map { it.absolutePath }.toTypedArray()
            var scannedCount = 0

            MediaScannerConnection.scanFile(
                context,
                paths,
                null
            ) { _, _ ->
                // Callback is invoked for each path
                scannedCount++
                if (scannedCount >= paths.size && continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
    }

    /**
     * Scan MediaStore for all audio files.
     *
     * Query Strategy:
     * 1. Trigger media scan to update MediaStore with recent file changes
     * 2. Query MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
     * 3. Filter by IS_MUSIC = 1 (unless showAllAudioFiles is true)
     * 4. Sort by DATE_ADDED DESC (newest first)
     *
     * @param showAllAudioFiles If true, includes ringtones, notifications, and system sounds
     * @param onProgress Callback invoked for each song scanned with (count, title)
     * @return List of SongEntity objects representing scanned audio files
     * @throws SecurityException if permissions are not granted
     */
    suspend fun scanAudioFiles(
        showAllAudioFiles: Boolean = false,
        onProgress: (scannedCount: Int, songTitle: String) -> Unit = { _, _ -> }
    ): List<SongEntity> {
        // Permission check
        if (!hasAudioPermission()) {
            throw SecurityException("READ_MEDIA_AUDIO or READ_EXTERNAL_STORAGE permission not granted")
        }

        // Force MediaStore update by triggering media scan
        triggerMediaScan()

        // Brief delay to let MediaStore propagate changes
        kotlinx.coroutines.delay(300)

        val songs = mutableListOf<SongEntity>()
        val contentResolver: ContentResolver = context.contentResolver

        // MediaStore projection (columns to retrieve)
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,  // File path
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.YEAR
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        // Selection criteria: Only music files by default, all audio files if showAllAudioFiles is true
        val selection = if (showAllAudioFiles) {
            null  // No filter - include all audio files
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} = 1"  // Only music files
        }

        // Sort order: Newest first
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        // Execute query
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            // Column indices
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            } else -1

            var scannedCount = 0

            // Iterate through cursor
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val data = cursor.getString(dataColumn) ?: continue  // Skip if no file path
                val albumId = cursor.getLong(albumIdColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val size = cursor.getLong(sizeColumn)
                val year = cursor.getInt(yearColumn)
                
                val rawGenre = if (genreColumn != -1) {
                    cursor.getString(genreColumn)
                } else null

                val genre = rawGenre?.takeIf { it.isNotBlank() }?.let { name ->
                    // Normalize to Title Case for consistent grouping
                    name.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { it.uppercase() }
                    }
                } ?: "Unknown Genre"

                // Build content URI for the audio file
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                // Build album art URI
                val albumArtUri = getAlbumArtUri(albumId)

                // Extract folder path from file path
                val folderPath = extractFolderPath(data)

                // Create SongEntity
                val songEntity = SongEntity(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    uri = uri,
                    albumArtUri = albumArtUri,
                    dateAdded = dateAdded,
                    isLiked = false,  // Default to not liked
                    folderPath = folderPath,
                    genre = genre,
                    year = year,
                    size = size
                )

                songs.add(songEntity)
                scannedCount++

                // Report progress
                onProgress(scannedCount, title)
            }
        }

        return songs
    }

    /**
     * Construct album art URI from album ID.
     *
     * Android provides album art via MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI.
     * We use ContentUris to append the album ID to this base URI.
     *
     * @param albumId The album ID from MediaStore
     * @return Album art URI string, or null if unavailable
     */
    private fun getAlbumArtUri(albumId: Long): String? {
        return if (albumId > 0) {
            ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            ).toString()
        } else {
            null
        }
    }

    /**
     * Extract folder path from file path.
     *
     * Extracts the parent directory path from the full file path.
     * Example: /storage/emulated/0/Music/Rock/song.mp3 -> /storage/emulated/0/Music/Rock
     *
     * @param filePath The full file path from MediaStore.Audio.Media.DATA
     * @return The folder path, or null if extraction fails
     */
    private fun extractFolderPath(filePath: String?): String? {
        if (filePath == null) return null

        // Find the last path separator
        val lastSeparatorIndex = filePath.lastIndexOf('/')
        if (lastSeparatorIndex == -1) return null

        // Return everything before the last separator (the folder path)
        return filePath.substring(0, lastSeparatorIndex)
    }

    /**
     * Notify MediaStore that a file has been deleted.
     *
     * This triggers Android's MediaScanner to re-index the deleted file,
     * ensuring MediaStore stays in sync with the physical storage.
     *
     * @param songUri The content URI of the deleted song
     */
    fun notifyMediaStoreChanged(songUri: String) {
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
            data = Uri.parse(songUri)
        })
    }
}
