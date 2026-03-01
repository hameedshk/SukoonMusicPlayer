package com.sukoon.music.data.lyrics

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
 * 4. Check fuzzy title match in sibling .lrc files
 *
 * Supports scoped storage by querying MediaStore when absolute filesystem paths are unavailable.
 */
@Singleton
class OfflineLyricsScanner @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "OfflineLyricsScanner"
    }

    private data class LrcCandidate(
        val nameWithoutExtension: String,
        val readText: () -> String?
    )

    private data class ContentAudioInfo(
        val absolutePath: String?,
        val displayName: String?,
        val relativePath: String?
    )

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
        return try {
            val uri = Uri.parse(audioUri)
            when (uri.scheme) {
                "file" -> {
                    val path = uri.path
                    if (path.isNullOrBlank()) {
                        DevLogger.d(TAG, "lyrics_stage=local_file_path_missing uri=$audioUri")
                        null
                    } else {
                        findLrcViaFilesystem(path, title, artist)
                    }
                }

                "content" -> {
                    val info = getContentAudioInfo(uri)

                    val fromAbsolutePath = info.absolutePath?.let { findLrcViaFilesystem(it, title, artist) }
                    if (fromAbsolutePath != null) {
                        DevLogger.d(TAG, "lyrics_stage=local_file_hit_via_content_path")
                        return fromAbsolutePath
                    }

                    if (info.relativePath.isNullOrBlank()) {
                        DevLogger.d(TAG, "lyrics_stage=content_relative_path_missing uri=$audioUri")
                        null
                    } else {
                        val displayNameWithoutExt = info.displayName?.substringBeforeLast(".")
                        val fromMediaStore = findLrcInMediaStoreDirectory(
                            relativePath = info.relativePath,
                            audioNameWithoutExt = displayNameWithoutExt,
                            title = title,
                            artist = artist
                        )
                        if (fromMediaStore == null) {
                            DevLogger.d(TAG, "lyrics_stage=local_file_miss")
                        }
                        fromMediaStore
                    }
                }

                else -> {
                    DevLogger.d(TAG, "Unsupported URI scheme for LRC scan: ${uri.scheme}")
                    null
                }
            }
        } catch (e: Exception) {
            DevLogger.e(TAG, "Error scanning for .lrc file", e)
            null
        }
    }

    private fun findLrcViaFilesystem(audioFilePath: String, title: String, artist: String): String? {
        val audioFile = File(audioFilePath)
        if (!audioFile.exists()) {
            DevLogger.d(TAG, "Audio file does not exist: $audioFilePath")
            return null
        }

        val parentDir = audioFile.parentFile ?: return null
        val candidates = parentDir
            .listFiles { file -> file.extension.equals("lrc", ignoreCase = true) && file.canRead() }
            ?.map { file ->
                LrcCandidate(
                    nameWithoutExtension = file.nameWithoutExtension,
                    readText = {
                        try {
                            file.readText()
                        } catch (e: Exception) {
                            DevLogger.e(TAG, "Failed reading local .lrc: ${file.absolutePath}", e)
                            null
                        }
                    }
                )
            }
            .orEmpty()

        return findBestCandidateText(
            candidates = candidates,
            audioNameWithoutExt = audioFile.nameWithoutExtension,
            title = title,
            artist = artist
        )
    }

    /**
     * Query MediaStore for sibling .lrc files in the same RELATIVE_PATH directory.
     */
    private fun findLrcInMediaStoreDirectory(
        relativePath: String,
        audioNameWithoutExt: String?,
        title: String,
        artist: String
    ): String? {
        return try {
            val filesUri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
            )
            val selection =
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND LOWER(${MediaStore.MediaColumns.DISPLAY_NAME}) LIKE ?"
            val selectionArgs = arrayOf(relativePath, "%.lrc")

            val candidates = mutableListOf<LrcCandidate>()
            context.contentResolver.query(
                filesUri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    if (idIndex == -1 || nameIndex == -1) continue
                    val id = cursor.getLong(idIndex)
                    val displayName = cursor.getString(nameIndex) ?: continue
                    val contentUri = Uri.withAppendedPath(filesUri, id.toString())

                    candidates += LrcCandidate(
                        nameWithoutExtension = displayName.substringBeforeLast("."),
                        readText = {
                            try {
                                context.contentResolver
                                    .openInputStream(contentUri)
                                    ?.bufferedReader()
                                    ?.use { reader -> reader.readText() }
                            } catch (e: Exception) {
                                DevLogger.e(TAG, "Failed reading MediaStore .lrc: $contentUri", e)
                                null
                            }
                        }
                    )
                }
            }

            findBestCandidateText(
                candidates = candidates,
                audioNameWithoutExt = audioNameWithoutExt.orEmpty(),
                title = title,
                artist = artist
            )
        } catch (e: Exception) {
            DevLogger.e(TAG, "Error scanning MediaStore for sibling .lrc", e)
            null
        }
    }

    /**
     * Resolve content metadata for audio Uri.
     * Uses absolute DATA path when available (legacy), otherwise DISPLAY_NAME + RELATIVE_PATH.
     */
    @Suppress("DEPRECATION")
    private fun getContentAudioInfo(uri: Uri): ContentAudioInfo {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use ContentAudioInfo(null, null, null)
                }

                val dataPath = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    .takeIf { it != -1 }
                    ?.let { cursor.getString(it) }
                val displayName = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    .takeIf { it != -1 }
                    ?.let { cursor.getString(it) }
                val relativePath = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    .takeIf { it != -1 }
                    ?.let { cursor.getString(it) }

                ContentAudioInfo(dataPath, displayName, relativePath)
            } ?: ContentAudioInfo(null, null, null)
        } catch (e: Exception) {
            DevLogger.e(TAG, "Error resolving content audio info", e)
            ContentAudioInfo(null, null, null)
        }
    }

    private fun findBestCandidateText(
        candidates: List<LrcCandidate>,
        audioNameWithoutExt: String,
        title: String,
        artist: String
    ): String? {
        if (candidates.isEmpty()) {
            return null
        }

        val exactMatch = candidates.firstOrNull {
            it.nameWithoutExtension.equals(audioNameWithoutExt, ignoreCase = true)
        }
        if (exactMatch != null) {
            DevLogger.d(TAG, "Found exact match .lrc candidate")
            return exactMatch.readText()
        }

        val cleanedAudioName = cleanFileName(audioNameWithoutExt)
        val cleanedMatch = candidates.firstOrNull {
            cleanFileName(it.nameWithoutExtension).equals(cleanedAudioName, ignoreCase = true)
        }
        if (cleanedMatch != null) {
            DevLogger.d(TAG, "Found cleaned-name match .lrc candidate")
            return cleanedMatch.readText()
        }

        val cleanedArtistTitle = cleanFileName("$artist - $title")
        val artistTitleMatch = candidates.firstOrNull {
            cleanFileName(it.nameWithoutExtension).equals(cleanedArtistTitle, ignoreCase = true)
        }
        if (artistTitleMatch != null) {
            DevLogger.d(TAG, "Found artist-title match .lrc candidate")
            return artistTitleMatch.readText()
        }

        val titleLower = title.lowercase().trim()
        val fuzzyMatch = if (titleLower.isNotBlank()) {
            candidates.firstOrNull { candidate ->
                val lrcName = candidate.nameWithoutExtension.lowercase()
                lrcName.contains(titleLower) || titleLower.contains(lrcName)
            }
        } else {
            null
        }
        if (fuzzyMatch != null) {
            DevLogger.d(TAG, "Found fuzzy-title match .lrc candidate")
            return fuzzyMatch.readText()
        }

        return null
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
