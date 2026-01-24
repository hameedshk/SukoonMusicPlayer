package com.sukoon.music.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.theme.*

/**
 * Creates a share handler for songs.
 * Uses MediaStore content URI to share audio files via Android share sheet.
 */
@Composable
fun rememberShareHandler(): (Song) -> Unit {
    val context = LocalContext.current

    return remember {
        { song ->
            shareSong(context, song)
        }
    }
}

/**
 * Shares a song using Android share sheet.
 * @param context Android context
 * @param song Song to share
 */
fun shareSong(context: Context, song: Song) {
    try {
        val uri = android.net.Uri.parse(song.uri)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${song.title}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share file", Toast.LENGTH_SHORT).show()
    }
}
