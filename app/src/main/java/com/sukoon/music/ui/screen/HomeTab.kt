package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.ActionButtonGrid
import com.sukoon.music.ui.components.LastAddedSection
import com.sukoon.music.ui.components.ListeningStatsCard
import com.sukoon.music.ui.components.RecentlyPlayedSection
import com.sukoon.music.ui.components.RecentlyPlayedSongCard
import com.sukoon.music.ui.components.RediscoverAlbumsSection
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.ui.theme.*

@Composable
fun HomeTab(
    songs: List<Song>,
    recentlyPlayed: List<Song>,
    rediscoverAlbums: List<Album>,
    playbackState: PlaybackState,
    viewModel: HomeViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    // Collect listening stats from ViewModel
    val listeningStats = viewModel.listeningStats.collectAsStateWithLifecycle().value

    // Collect private session state
    val sessionState = viewModel.sessionState.collectAsStateWithLifecycle().value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            ActionButtonGrid(
                onShuffleAllClick = { viewModel.shuffleAll() },
                onPlayAllClick = { viewModel.playAll() },
                onScanClick = { viewModel.scanLocalMusic() },
                onSettingsClick = onSettingsClick
            )
        }
        if (songs.isNotEmpty()) {
            item {
                LastAddedSection(
                    songs = songs,
                    onSongClick = { song ->
                        val index = songs.indexOf(song)
                        if (playbackState.currentSong?.id != song.id) {
                            viewModel.playQueue(songs, index)
                        } else {
                            onNavigateToNowPlaying()
                        }
                    },
                    onHeaderClick = {
                        onNavigateToSmartPlaylist(SmartPlaylistType.LAST_ADDED)
                    }
                )
            }
        }
        // Listening Stats Card - appears above Recently Played (hidden when private session is active)
        if (!sessionState.isActive) {
            item {
                ListeningStatsCard(stats = listeningStats)
            }
        }

        // Recently Played Grid - hidden when private session is active
        if (recentlyPlayed.isNotEmpty() && !sessionState.isActive) {
            item {
                RecentlyPlayedSection(
                    items = recentlyPlayed,
                    onItemClick = { song ->
                        if (playbackState.currentSong?.id != song.id) {
                            viewModel.playSong(song)
                        } else {
                            onNavigateToNowPlaying()
                        }
                    },
                    onHeaderClick = {
                        onNavigateToSmartPlaylist(SmartPlaylistType.RECENTLY_PLAYED)
                    }
                ) { song, onClick ->
                    RecentlyPlayedSongCard(song = song, onClick = onClick)
                }
            }
        }
        if (rediscoverAlbums.isNotEmpty()) {
            item {
                RediscoverAlbumsSection(
                    albums = rediscoverAlbums,
                    onAlbumClick = onNavigateToAlbumDetail
                )
            }
        }
    }
}
