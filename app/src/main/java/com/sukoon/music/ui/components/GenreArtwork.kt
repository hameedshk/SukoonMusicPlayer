package com.sukoon.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Unified artwork component for Genres.
 * Handles custom artwork URIs with a consistent fallback to [PlaceholderAlbumArt].
 */
@Composable
fun GenreArtwork(
    genreName: String,
    genreId: Long,
    artworkUri: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    androidx.compose.runtime.key(artworkUri, genreId) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUri != null) {
                SubcomposeAsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = PlaceholderAlbumArt.generateSeed(
                                albumName = genreName,
                                albumId = genreId
                            )
                        )
                    },
                    error = {
                        PlaceholderAlbumArt.Placeholder(
                            seed = PlaceholderAlbumArt.generateSeed(
                                albumName = genreName,
                                albumId = genreId
                            )
                        )
                    }
                )
            } else {
                PlaceholderAlbumArt.Placeholder(
                    seed = PlaceholderAlbumArt.generateSeed(
                        albumName = genreName,
                        albumId = genreId
                    )
                )
            }
        }
    }
}
