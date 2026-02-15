package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Add these to your imports in HomeTab.kt
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.ActionButtonGrid
import com.sukoon.music.ui.components.ContinueListeningCard
import com.sukoon.music.ui.components.LastAddedSection
import com.sukoon.music.ui.components.LibraryNavigationCards
import com.sukoon.music.ui.components.ListeningStatsCard
import com.sukoon.music.ui.components.RecentlyPlayedScrollSection
import com.sukoon.music.ui.components.RecentlyPlayedSection
import com.sukoon.music.ui.components.RecentlyPlayedSongCard
import com.sukoon.music.ui.components.RediscoverAlbumsSection
import com.sukoon.music.ui.model.HomeTabKey
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
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,  // No top padding - handled by Scaffold topBar
            bottom = MiniPlayerHeight + SpacingSmall,  // Space for mini player overlay (64dp + 8dp)
            start = 0.dp,
            end = 0.dp
        ),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        // PRIMARY: Continue Listening Card (album art + one-tap resume)
        if (playbackState.currentSong != null && recentlyPlayed.isNotEmpty()) {
            item {
                // Section header
                Text(
                    text = "Continue listening",
                    style = MaterialTheme.typography.sectionHeader,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                ContinueListeningCard(
                    song = playbackState.currentSong,
                    onPlayClick = {
                        // Simply toggle play/pause on current song
                        viewModel.playPause()
                    },
                    onClick = onNavigateToNowPlaying
                )
            }
        }

        // SECONDARY: Recently Played Horizontal Scroll
        if (recentlyPlayed.isNotEmpty() && !sessionState.isActive) {
            item {
                RecentlyPlayedScrollSection(
                    songs = recentlyPlayed,
                    onItemClick = { song: Song ->
                        if (playbackState.currentSong?.id != song.id) {
                            viewModel.playSong(song)
                        } else {
                            onNavigateToNowPlaying()
                        }
                    }
                )
            }
        }

        // TERTIARY: Library Navigation Cards
        item {
            LibraryNavigationCards(
                onSongsClick = { viewModel.setSelectedTab(HomeTabKey.SONGS) },
                onPlaylistsClick = { viewModel.setSelectedTab(HomeTabKey.PLAYLISTS) },
                onAlbumsClick = { viewModel.setSelectedTab(HomeTabKey.ALBUMS) },
                onFoldersClick = { viewModel.setSelectedTab(HomeTabKey.FOLDERS) }
            )
        }

        // OPTIONAL: Listening Stats (below primary content when not private session)
        if (!sessionState.isActive && listeningStats != null) {
            item {
                ListeningStatsCard(stats = listeningStats)
            }
        }

        // Additional sections (scrollable below fold)
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
