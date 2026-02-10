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

/**
 * Shares multiple songs using Android share sheet.
 * @param context Android context
 * @param songs List of songs to share
 */
fun shareMultipleSongs(context: Context, songs: List<Song>) {
    if (songs.isEmpty()) {
        Toast.makeText(context, "No songs to share", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uris = ArrayList<android.net.Uri>()
        songs.forEach { song ->
            uris.add(android.net.Uri.parse(song.uri))
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareTitle = if (songs.size == 1) {
            "Share ${songs[0].title}"
        } else {
            "Share ${songs.size} songs"
        }

        context.startActivity(Intent.createChooser(intent, shareTitle))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share files: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
