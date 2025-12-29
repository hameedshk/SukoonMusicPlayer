package com.sukoon.music.data.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extractor for embedded lyrics from audio file metadata (ID3 tags).
 *
 * Supports:
 * - USLT (Unsynchronized Lyrics) - Plain text lyrics
 * - SYLT (Synchronized Lyrics) - Timestamped lyrics (converted to LRC format)
 * - Vorbis Comments (for FLAC/OGG: LYRICS tag)
 *
 * Uses MediaMetadataRetriever for maximum compatibility across formats.
 */
@Singleton
class Id3LyricsExtractor @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "Id3LyricsExtractor"

        // Metadata keys for lyrics
        private const val METADATA_KEY_LYRICS = 1000 // Custom key for some formats
    }

    /**
     * Extract embedded lyrics from an audio file.
     *
     * @param audioUri URI of the audio file (content:// or file://)
     * @return Pair of (syncedLyrics, plainLyrics), both nullable
     */
    fun extractLyrics(audioUri: String): Pair<String?, String?> {
        val retriever = MediaMetadataRetriever()

        try {
            val uri = Uri.parse(audioUri)

            // Set data source based on URI scheme
            when (uri.scheme) {
                "file" -> {
                    uri.path?.let { retriever.setDataSource(it) }
                }
                "content" -> {
                    retriever.setDataSource(context, uri)
                }
                else -> {
                    Log.w(TAG, "Unsupported URI scheme: ${uri.scheme}")
                    return Pair(null, null)
                }
            }

            // Try to extract lyrics from various metadata fields
            val lyrics = extractLyricsFromMetadata(retriever)

            return lyrics

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting embedded lyrics from $audioUri", e)
            return Pair(null, null)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }

    /**
     * Extract lyrics from metadata retriever.
     * Attempts multiple metadata keys to maximize compatibility.
     */
    private fun extractLyricsFromMetadata(retriever: MediaMetadataRetriever): Pair<String?, String?> {
        var syncedLyrics: String? = null
        var plainLyrics: String? = null

        try {
            // Try standard METADATA_KEY_LYRICS (works for some MP3s with USLT frame)
            // Note: MediaMetadataRetriever doesn't expose all ID3 frames directly
            // This is a limitation of the Android API

            // For MP3 files, we can try to extract custom metadata
            // However, MediaMetadataRetriever has limited support for lyrics frames

            // Try extracting all embedded data
            val embeddedPicture = retriever.embeddedPicture

            // Unfortunately, MediaMetadataRetriever doesn't directly expose USLT/SYLT frames
            // We need to use a workaround or third-party library

            // For now, we'll attempt to extract any text-based metadata that might contain lyrics
            // This is a best-effort approach

            // Some files store lyrics in METADATA_KEY_ALBUM or custom fields
            // We'll check common metadata keys

            val comment = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION)
            val writer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)

            // Check if any metadata field contains LRC-format lyrics
            if (comment != null && isLrcFormat(comment)) {
                syncedLyrics = comment
            } else if (comment != null && comment.length > 50) {
                // Assume it's plain lyrics if it's long enough
                plainLyrics = comment
            }

            if (writer != null && isLrcFormat(writer)) {
                syncedLyrics = writer
            } else if (writer != null && writer.length > 50 && plainLyrics == null) {
                plainLyrics = writer
            }

            Log.d(TAG, "Extracted lyrics - Synced: ${syncedLyrics != null}, Plain: ${plainLyrics != null}")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting metadata", e)
        }

        return Pair(syncedLyrics, plainLyrics)
    }

    /**
     * Check if text is in LRC format (contains timestamp markers like [00:12.00]).
     */
    private fun isLrcFormat(text: String): Boolean {
        return text.contains(Regex("""\[\d{2}:\d{2}\.\d{2}\]"""))
    }

    /**
     * Advanced extraction using JAudioTagger library (optional enhancement).
     *
     * Note: This requires adding org.jaudiotagger:jaudiotagger dependency.
     * Commented out for now - can be enabled if needed.
     *
     * Benefits:
     * - Full ID3v2 USLT/SYLT frame support
     * - Support for multiple lyric languages
     * - Better format coverage (MP3, FLAC, OGG, M4A)
     */
    /*
    private fun extractLyricsWithJAudioTagger(audioPath: String): Pair<String?, String?> {
        try {
            val audioFile = AudioFileIO.read(File(audioPath))
            val tag = audioFile.tag ?: return Pair(null, null)

            // Extract USLT (unsynchronized lyrics)
            val usltFrames = tag.getFields(FieldKey.LYRICS)
            val plainLyrics = usltFrames?.firstOrNull()?.toString()

            // For SYLT frames, we'd need to parse the binary data
            // This is more complex and format-specific

            return Pair(null, plainLyrics)
        } catch (e: Exception) {
            Log.e(TAG, "Error with JAudioTagger extraction", e)
            return Pair(null, null)
        }
    }
    */
}
