package com.sukoon.music.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sukoon.music.ui.screen.AlbumDetailScreen
import com.sukoon.music.ui.screen.AlbumsScreen
import com.sukoon.music.ui.screen.ArtistDetailScreen
import com.sukoon.music.ui.screen.ArtistsScreen
import com.sukoon.music.ui.screen.EqualizerScreen
import com.sukoon.music.ui.screen.ExcludedFoldersScreen
import com.sukoon.music.ui.screen.FolderDetailScreen
import com.sukoon.music.ui.screen.FoldersScreen
import com.sukoon.music.ui.screen.HomeScreen
import com.sukoon.music.ui.screen.LikedSongsScreen
import com.sukoon.music.ui.screen.NowPlayingScreen
import com.sukoon.music.ui.screen.PlaylistDetailScreen
import com.sukoon.music.ui.screen.PlaylistsScreen
import com.sukoon.music.ui.screen.RestorePlaylistScreen
import com.sukoon.music.ui.screen.SearchScreen
import com.sukoon.music.ui.screen.SettingsScreen
import com.sukoon.music.ui.screen.SmartPlaylistDetailScreen
import com.sukoon.music.ui.screen.SongsScreen

/**
 * Main navigation graph for Sukoon Music app.
 *
 * Sets up NavHost with all app screens and handles navigation between them.
 * Uses type-safe Routes sealed class for compile-time route checking.
 *
 * @param navController Navigation controller to manage app navigation
 * @param modifier Optional modifier for the NavHost
 */
@Composable
fun SukoonNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier
    ) {
        // Home Screen - Main entry point
        composable(route = Routes.Home.route) {
            HomeScreen(
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Routes.Search.route)
                },
                onNavigateToLikedSongs = {
                    navController.navigate(Routes.LikedSongs.route)
                },
                onNavigateToAlbums = {
                    navController.navigate(Routes.Albums.route)
                },
                onNavigateToArtists = {
                    navController.navigate(Routes.Artists.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route)
                },
                onNavigateToSongs = {
                    navController.navigate(Routes.Songs.route)
                },
                onNavigateToPlaylists = {
                    navController.navigate(Routes.Playlists.route)
                },
                onNavigateToFolders = {
                    navController.navigate(Routes.Folders.route)
                }
            )
        }

        // Now Playing Screen - Full player
        composable(route = Routes.NowPlaying.route) {
            NowPlayingScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Playlists Screen - All playlists
        composable(route = Routes.Playlists.route) {
            PlaylistsScreen(
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                },
                onNavigateToSmartPlaylist = { smartPlaylistType ->
                    navController.navigate(Routes.SmartPlaylistDetail.createRoute(smartPlaylistType.name))
                },
                onNavigateToRestore = {
                    navController.navigate(Routes.RestorePlaylist.route)
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Playlist Detail Screen - Songs in a playlist
        composable(
            route = Routes.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Smart Playlist Detail Screen - Songs in a smart playlist
        composable(
            route = Routes.SmartPlaylistDetail.route,
            arguments = listOf(
                navArgument("smartPlaylistType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val smartPlaylistTypeString = backStackEntry.arguments?.getString("smartPlaylistType") ?: return@composable
            SmartPlaylistDetailScreen(
                smartPlaylistType = smartPlaylistTypeString,
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Restore Playlist Screen - Shows deleted playlists
        composable(route = Routes.RestorePlaylist.route) {
            RestorePlaylistScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Liked Songs Screen - User's favorited songs
        composable(route = Routes.LikedSongs.route) {
            LikedSongsScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Search Screen - Search local music library
        composable(route = Routes.Search.route) {
            SearchScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Songs Screen - All songs alphabetically with search
        composable(route = Routes.Songs.route) {
            SongsScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Settings Screen - App preferences
        composable(route = Routes.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToEqualizer = {
                    navController.navigate(Routes.Equalizer.route)
                },
                onNavigateToExcludedFolders = {
                    navController.navigate(Routes.ExcludedFolders.route)
                }
            )
        }

        // Equalizer Screen - Audio effects control
        composable(route = Routes.Equalizer.route) {
            EqualizerScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Albums Screen - All albums
        composable(route = Routes.Albums.route) {
            AlbumsScreen(
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Album Detail Screen - Songs in an album
        composable(
            route = Routes.AlbumDetail.route,
            arguments = listOf(
                navArgument("albumId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Artists Screen - All artists
        composable(route = Routes.Artists.route) {
            ArtistsScreen(
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Artist Detail Screen - Songs and albums by an artist
        composable(
            route = Routes.ArtistDetail.route,
            arguments = listOf(
                navArgument("artistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getLong("artistId") ?: return@composable
            ArtistDetailScreen(
                artistId = artistId,
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                }
            )
        }

        // Folders Screen - All folders
        composable(route = Routes.Folders.route) {
            FoldersScreen(
                onNavigateToFolder = { folderId ->
                    navController.navigate(Routes.FolderDetail.createRoute(folderId))
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Folder Detail Screen - Songs in a folder
        composable(
            route = Routes.FolderDetail.route,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            FolderDetailScreen(
                folderId = folderId,
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Excluded Folders Screen - Manage hidden folders
        composable(route = Routes.ExcludedFolders.route) {
            ExcludedFoldersScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
    }
}
