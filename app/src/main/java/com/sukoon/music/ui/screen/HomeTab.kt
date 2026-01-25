package com.sukoon.music.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
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
import androidx.compose.foundation.layout.Spacer

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
        contentPadding = PaddingValues(vertical = SpacingLarge)
    ) {
        item {
            ActionButtonGrid(
                onShuffleAllClick = { viewModel.shuffleAll() },
                onPlayAllClick = { viewModel.playAll() },
                onScanClick = { viewModel.scanLocalMusic() },
                onSettingsClick = onSettingsClick
            )
        }
        item {
            Spacer(modifier = Modifier.height(SectionSpacing))
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
        item {
            Spacer(modifier = Modifier.height(SectionSpacing))
        }
        // Listening Stats Card - appears above Recently Played (hidden when private session is active)
        if (!sessionState.isActive) {
            item {
                ListeningStatsCard(stats = listeningStats)
            }
            item {
                Spacer(modifier = Modifier.height(SectionSpacing))
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
        item {
            Spacer(modifier = Modifier.height(SectionSpacing))
        }
        if (rediscoverAlbums.isNotEmpty()) {
            item {
                val isVisibleState = remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    // Delay animation until after initial composition
                    delay(300)
                    isVisibleState.value = true
                }

                AnimatedVisibility(
                    visible = isVisibleState.value,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                ) {
                    RediscoverAlbumsSection(
                        albums = rediscoverAlbums,
                        onAlbumClick = onNavigateToAlbumDetail
                    )
                }
            }
        }
    }
}
