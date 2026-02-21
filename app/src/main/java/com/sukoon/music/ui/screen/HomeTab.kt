package com.sukoon.music.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.R
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.ui.components.ContinueListeningCard
import com.sukoon.music.ui.components.LastAddedSection
import com.sukoon.music.ui.components.LibraryNavigationCards
import com.sukoon.music.ui.components.ListeningStatsCard
import com.sukoon.music.ui.components.RatingBanner
import com.sukoon.music.ui.components.RecentlyPlayedScrollSection
import com.sukoon.music.ui.components.RediscoverAlbumsSection
import com.sukoon.music.ui.model.HomeTabKey
import com.sukoon.music.ui.theme.SectionSpacing
import com.sukoon.music.ui.theme.SpacingLarge
import com.sukoon.music.ui.theme.SpacingMedium
import com.sukoon.music.ui.theme.SpacingSmall
import com.sukoon.music.ui.theme.homeSectionHeader
import com.sukoon.music.ui.viewmodel.HomeViewModel

@Composable
fun HomeTabScreen(
    songs: List<Song>,
    recentlyPlayed: List<Song>,
    rediscoverAlbums: List<Album>,
    playbackState: PlaybackState,
    viewModel: HomeViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToAlbumDetail: (Long) -> Unit,
    onNavigateToSmartPlaylist: (SmartPlaylistType) -> Unit
) {
    val listeningStats = viewModel.listeningStats.collectAsStateWithLifecycle().value
    val sessionState = viewModel.sessionState.collectAsStateWithLifecycle().value
    val shouldShowRatingBanner = viewModel.shouldShowRatingBanner.collectAsStateWithLifecycle().value
    val appContext = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = SpacingSmall,
            start = 0.dp,
            end = 0.dp
        ),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        if (playbackState.currentSong != null) {
            item {
                Text(
                    text = stringResource(R.string.home_continue_listening),
                    style = MaterialTheme.typography.homeSectionHeader,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingSmall)
                )

                ContinueListeningCard(
                    song = playbackState.currentSong,
                    isPlaying = playbackState.isPlaying,
                    onPlayClick = {
                        viewModel.onHomeResumeTap()
                        viewModel.playPause()
                    },
                    onClick = onNavigateToNowPlaying,
                    horizontalPadding = SpacingLarge
                )
            }
        } else if (songs.isNotEmpty()) {
            item {
                HomeQuickStartActions(
                    onShuffleClick = {
                        viewModel.onHomeShuffleTap()
                        viewModel.shuffleAll()
                    },
                    onLastAddedClick = {
                        onNavigateToSmartPlaylist(SmartPlaylistType.LAST_ADDED)
                    }
                )
            }
        }

        if (recentlyPlayed.isNotEmpty() && !sessionState.isActive) {
            item {
                RecentlyPlayedScrollSection(
                    songs = recentlyPlayed,
                    onItemClick = { song: Song ->
                        if (playbackState.currentSong?.id != song.id) {
                            viewModel.onHomeSectionPlayTap("recently_played")
                            viewModel.playSong(song)
                        } else {
                            onNavigateToNowPlaying()
                        }
                    }
                )
            }
        }

        item {
            LibraryNavigationCards(
                onSongsClick = { viewModel.setSelectedTab(HomeTabKey.SONGS) },
                onPlaylistsClick = { viewModel.setSelectedTab(HomeTabKey.PLAYLISTS) },
                onAlbumsClick = { viewModel.setSelectedTab(HomeTabKey.ALBUMS) },
                onFoldersClick = { viewModel.setSelectedTab(HomeTabKey.FOLDERS) }
            )
        }

        if (shouldShowRatingBanner) {
            item {
                RatingBanner(
                    onDismiss = { viewModel.dismissRatingBanner() },
                    onClick = {
                        val activity = appContext as? ComponentActivity
                        if (activity != null) {
                            viewModel.triggerInAppReview(activity)
                        }
                    },
                    modifier = Modifier.padding(SpacingSmall)
                )
            }
        }

        if (!sessionState.isActive && listeningStats != null) {
            item {
                ListeningStatsCard(stats = listeningStats)
            }
        }

        if (songs.isNotEmpty()) {
            item {
                LastAddedSection(
                    songs = songs,
                    onSongClick = { song ->
                        val index = songs.indexOf(song)
                        if (playbackState.currentSong?.id != song.id) {
                            viewModel.onHomeSectionPlayTap("last_added")
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

@Composable
private fun HomeQuickStartActions(
    onShuffleClick: () -> Unit,
    onLastAddedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        Button(
            onClick = onShuffleClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(R.string.common_ui_action_shuffle))
        }
        OutlinedButton(
            onClick = onLastAddedClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(R.string.library_screens_b_last_added))
        }
    }
}
