package com.sukoon.music.data.mediastore

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sukoon.music.domain.model.Song

/**
 * Utility class to handle file deletion using MediaStore APIs.
 * Handles the complexities of scoped storage on Android 10+.
 */
object DeleteHelper {

    sealed class DeleteResult {
        data class RequiresPermission(val intentSender: IntentSender) : DeleteResult()
        object Success : DeleteResult()
        data class Error(val message: String) : DeleteResult()
    }

    /**
     * Request deletion of songs.
     * On Android 11+, this uses createDeleteRequest for a batch operation.
     */
    fun deleteSongs(context: Context, songs: List<Song>): DeleteResult {
        if (songs.isEmpty()) return DeleteResult.Success

        val uris = songs.map { Uri.parse(it.uri) }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ batch delete
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                DeleteResult.RequiresPermission(pendingIntent.intentSender)
            } else {
                // Fallback or legacy implementation could go here
                DeleteResult.Error("Bulk deletion is supported on Android 11 and above.")
            }
        } catch (e: Exception) {
            DeleteResult.Error(e.message ?: "Unknown error during deletion")
        }
    }
}
