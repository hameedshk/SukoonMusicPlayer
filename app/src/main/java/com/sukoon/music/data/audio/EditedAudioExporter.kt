package com.sukoon.music.data.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SongAudioSettings
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class ExportedAudioResult(
    val contentUri: String,
    val displayName: String,
    val partialApplied: Boolean
)

@Singleton
class EditedAudioExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "EditedAudioExporter"

    suspend fun exportEditedCopy(
        song: Song,
        settings: SongAudioSettings
    ): Result<ExportedAudioResult> = withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File.createTempFile("edited_${song.id}_", ".m4a", context.cacheDir)
            try {
                val trimStartMs = if (settings.isEnabled) settings.trimStartMs.coerceAtLeast(0L) else 0L
                val trimEndMs = if (settings.isEnabled) settings.trimEndMs else -1L
                remuxAudio(
                    inputUri = Uri.parse(song.uri),
                    outputFile = tempFile,
                    trimStartMs = trimStartMs,
                    trimEndMs = trimEndMs
                )

                val displayName = buildFileName(song.title)
                val exportedUri = copyToMediaStore(tempFile, displayName)
                val partialApplied = hasUnsupportedBakedSettings(settings)
                ExportedAudioResult(
                    contentUri = exportedUri.toString(),
                    displayName = displayName,
                    partialApplied = partialApplied
                )
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun remuxAudio(
        inputUri: Uri,
        outputFile: File,
        trimStartMs: Long,
        trimEndMs: Long
    ) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            context.contentResolver.openAssetFileDescriptor(inputUri, "r")?.use { afd ->
                extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            } ?: extractor.setDataSource(context, inputUri, emptyMap())

            val audioTrackIndex = findAudioTrackIndex(extractor)
            require(audioTrackIndex >= 0) { "No audio track found in source." }

            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)
            require(inputFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                "Unsupported source mime type."
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputTrackIndex = muxer.addTrack(inputFormat)
            muxer.start()

            val startUs = trimStartMs.coerceAtLeast(0L) * 1000L
            val endUs = if (trimEndMs > trimStartMs) trimEndMs * 1000L else Long.MAX_VALUE
            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val bufferSize = if (inputFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                inputFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(256 * 1024)
            } else {
                256 * 1024
            }
            val buffer = java.nio.ByteBuffer.allocate(bufferSize)
            val info = MediaCodec.BufferInfo()
            var wroteAnySample = false

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0L) break
                if (sampleTimeUs < startUs) {
                    extractor.advance()
                    continue
                }
                if (endUs != Long.MAX_VALUE && sampleTimeUs > endUs) break

                info.offset = 0
                info.size = sampleSize
                info.presentationTimeUs = sampleTimeUs - startUs
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(outputTrackIndex, buffer, info)
                wroteAnySample = true
                extractor.advance()
            }

            require(wroteAnySample) { "Nothing to export for the selected trim range." }
        } finally {
            try {
                muxer?.stop()
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun findAudioTrackIndex(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun copyToMediaStore(tempFile: File, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Sukoon Edited")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Failed to create MediaStore entry.")

        try {
            resolver.openOutputStream(uri, "w")?.use { out ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(out)
                }
            } ?: error("Failed to open output stream.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            DevLogger.e(tag, "Failed to write exported file to MediaStore", e)
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun buildFileName(title: String): String {
        val safeTitle = title.ifBlank { "song" }
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .trim()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "${safeTitle}_edited_$timestamp.m4a"
    }

    private fun hasUnsupportedBakedSettings(settings: SongAudioSettings): Boolean {
        if (!settings.isEnabled) return false
        val speedChanged = kotlin.math.abs(settings.speed - 1.0f) > 0.001f
        val pitchChanged = kotlin.math.abs(settings.pitch - 1.0f) > 0.001f
        return settings.eqEnabled ||
            settings.bassBoost != 0 ||
            settings.virtualizerStrength != 0 ||
            settings.reverbPreset != 0.toShort() ||
            speedChanged ||
            pitchChanged
    }
}
