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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.sukoon.music.ui.components.HeroHeaderCard
import com.sukoon.music.ui.components.LastAddedSection
import com.sukoon.music.ui.components.ListeningStatsCard
import com.sukoon.music.ui.components.QuickActionsRow
import com.sukoon.music.ui.components.RecentlyPlayedScrollSection
import com.sukoon.music.ui.components.RecentlyPlayedSection
import com.sukoon.music.ui.components.RecentlyPlayedSongCard
import com.sukoon.music.ui.components.RediscoverAlbumsSection
import com.sukoon.music.ui.components.SleepTimerDialog
import com.sukoon.music.ui.model.HomeTabKey
import com.sukoon.music.ui.viewmodel.HomeViewModel
import com.sukoon.music.ui.theme.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.ui.components.RatingBanner

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
    onNavigateToLikedSongs: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    // Collect stats from ViewModel
    val listeningStats = viewModel.listeningStats.collectAsStateWithLifecycle().value
    val currentAlbumArtUri = viewModel.currentAlbumArtUri.collectAsStateWithLifecycle().value
    val sleepTimerState = viewModel.sleepTimerState.collectAsStateWithLifecycle().value
    val likedSongsCount = viewModel.likedSongsCount.collectAsStateWithLifecycle().value
    val username = viewModel.username.collectAsStateWithLifecycle().value
    val scanState = viewModel.scanState.collectAsStateWithLifecycle().value

    // Private session and rating banner state
    val sessionState = viewModel.sessionState.collectAsStateWithLifecycle().value
    val shouldShowRatingBanner = viewModel.shouldShowRatingBanner.collectAsStateWithLifecycle().value
    val appContext = LocalContext.current

    // Sleep timer dialog state
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentState = sleepTimerState,
            onDismiss = { showSleepTimerDialog = false },
            onSetTimer = { minutes -> viewModel.setSleepTimer(minutes) },
            onCancelTimer = { viewModel.cancelSleepTimer() }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = MiniPlayerHeight + SpacingSmall,
            start = 0.dp,
            end = 0.dp
        ),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        // HERO: Premium header with greeting + weekly stats
        item {
            HeroHeaderCard(
                username = username,
                stats = listeningStats.takeIf { !sessionState.isActive },
                albumArtUri = currentAlbumArtUri,
                isPrivateSession = sessionState.isActive,
                emptyState = songs.isEmpty(),
                modifier = Modifier.padding(horizontal = SpacingSmall)
            )
        }

        // QUICK ACTIONS: Shuffle, Sleep Timer, Scan, Liked Songs
        item {
            QuickActionsRow(
                onShuffleAll = { viewModel.shuffleAll() },
                onSleepTimer = { showSleepTimerDialog = true },
                onScanMusic = { viewModel.scanLocalMusic() },
                onLikedSongs = onNavigateToLikedSongs,
                shuffleEnabled = songs.isNotEmpty(),
                sleepTimerState = sleepTimerState,
                scanState = scanState,
                likedSongsCount = likedSongsCount,
                modifier = Modifier.padding(horizontal = SpacingSmall)
            )
        }

        // Continue Listening Card
        if (playbackState.currentSong != null && recentlyPlayed.isNotEmpty()) {
            item {
                Text(
                    text = "Continue listening",
                    style = MaterialTheme.typography.sectionHeader,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingSmall)
                )
                ContinueListeningCard(
                    song = playbackState.currentSong,
                    onPlayClick = { viewModel.playPause() },
                    onClick = onNavigateToNowPlaying
                )
            }
        }

        // Recently Played Horizontal Scroll
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

        // Rating Banner
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

        // Last Added Section
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

        // Rediscover Albums Section
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
