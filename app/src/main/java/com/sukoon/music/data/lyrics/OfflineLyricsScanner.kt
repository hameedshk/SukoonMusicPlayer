package com.sukoon.music.data.lyrics

import android.content.Context
import android.net.Uri
import com.sukoon.music.util.DevLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner for finding local .lrc lyric files.
 *
 * Search Strategy:
 * 1. Check for exact match: "songname.lrc" in same directory as audio file
 * 2. Check for cleaned match: Remove special characters and try again
 * 3. Check for artist-title match: "Artist - Title.lrc"
 *
 * Supports scoped storage by using ContentResolver when needed.
 */
@Singleton
class OfflineLyricsScanner @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "OfflineLyricsScanner"
    }

    /**
     * Find local .lrc file for an audio file.
     *
     * @param audioUri URI of the audio file (content:// or file://)
     * @param title Song title for fallback matching
     * @param artist Artist name for fallback matching
     * @return Lyrics content from .lrc file, or null if not found
     */
    fun findLrcFile(audioUri: String, title: String, artist: String): String? {
        DevLogger.d("LYRICS_DEBUG", "Scanning LRC for uri=$audioUri title=$title artist=$artist")
        try {
            val uri = Uri.parse(audioUri)

            // Try to get file path from URI
            val filePath = when (uri.scheme) {
                "file" -> uri.path
                "content" -> getPathFromContentUri(uri)
                else -> null
            }

            if (filePath == null) {
                DevLogger.d(TAG, "Could not resolve file path from URI: $audioUri")
                return null
            }

            val audioFile = File(filePath)
            if (!audioFile.exists()) {
                DevLogger.d(TAG, "Audio file does not exist: $filePath")
                return null
            }

            val parentDir = audioFile.parentFile ?: return null
            val audioNameWithoutExt = audioFile.nameWithoutExtension

            // Strategy 1: Exact match - "songname.lrc"
            val exactMatch = File(parentDir, "$audioNameWithoutExt.lrc")
            if (exactMatch.exists() && exactMatch.canRead()) {
                DevLogger.d(TAG, "Found exact match .lrc file: ${exactMatch.absolutePath}")
                return exactMatch.readText()
            }

            // Strategy 2: Cleaned match - Remove special characters
            val cleanedName = cleanFileName(audioNameWithoutExt)
            val cleanedMatch = File(parentDir, "$cleanedName.lrc")
            if (cleanedMatch.exists() && cleanedMatch.canRead()) {
                DevLogger.d(TAG, "Found cleaned match .lrc file: ${cleanedMatch.absolutePath}")
                return cleanedMatch.readText()
            }

            // Strategy 3: Artist - Title match
            val artistTitleName = "$artist - $title"
            val cleanedArtistTitle = cleanFileName(artistTitleName)
            val artistTitleMatch = File(parentDir, "$cleanedArtistTitle.lrc")
            if (artistTitleMatch.exists() && artistTitleMatch.canRead()) {
                DevLogger.d(TAG, "Found artist-title match .lrc file: ${artistTitleMatch.absolutePath}")
                return artistTitleMatch.readText()
            }

            // Strategy 4: Scan all .lrc files in directory and fuzzy match
            val lrcFiles = parentDir.listFiles { file ->
                file.extension.equals("lrc", ignoreCase = true)
            }

            if (!lrcFiles.isNullOrEmpty()) {
                val bestMatch = lrcFiles.firstOrNull { lrcFile ->
                    val lrcName = lrcFile.nameWithoutExtension.lowercase()
                    val titleLower = title.lowercase()
                    lrcName.contains(titleLower) || titleLower.contains(lrcName)
                }

                if (bestMatch != null && bestMatch.canRead()) {
                    DevLogger.d(TAG, "Found fuzzy match .lrc file: ${bestMatch.absolutePath}")
                    return bestMatch.readText()
                }
            }

            DevLogger.d(TAG, "No .lrc file found for: $audioNameWithoutExt")
            return null

        } catch (e: Exception) {
            DevLogger.e(TAG, "Error scanning for .lrc file", e)
            return null
        }
    }

    /**
     * Get file path from content:// URI using ContentResolver.
     * This is a best-effort approach for scoped storage.
     */
    private fun getPathFromContentUri(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                    if (columnIndex != -1) {
                        cursor.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            DevLogger.e(TAG, "Error getting path from content URI", e)
            null
        }
    }

    /**
     * Clean filename by removing special characters and extra spaces.
     * Helps match files with different naming conventions.
     */
    private fun cleanFileName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9\\s-]"), "") // Remove special chars
            .replace(Regex("\\s+"), " ")             // Normalize spaces
            .trim()
    }
}
