package com.sukoon.music.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sukoon.music.ui.screen.*
import com.sukoon.music.ui.viewmodel.PlaylistViewModel
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.premium.PremiumManager
import dagger.hilt.android.EntryPointAccessors
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.sukoon.music.ui.theme.*

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
    modifier: Modifier = Modifier,
    startDestination: String = Routes.Home.route,
    userPreferences: com.sukoon.music.domain.model.UserPreferences? = null
) {
    val context = LocalContext.current

    // Get PreferencesManager via Hilt
    val preferencesManager = try {
        EntryPointAccessors.fromApplication(context, PreferencesManagerEntryPoint::class.java).preferencesManager()
    } catch (e: Exception) {
        null
    }

    // Get PremiumManager via Hilt
    val premiumManager = try {
        EntryPointAccessors.fromApplication(context, PremiumManagerEntryPoint::class.java).premiumManager()
    } catch (e: Exception) {
        null
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding Screen - Permission + Username setup
        composable(route = Routes.Onboarding.route) {
            if (preferencesManager != null) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    },
                    preferencesManager = preferencesManager
                )
            }
        }

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
                onNavigateToArtistDetail = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
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
                },
                onNavigateToGenres = {
                    navController.navigate(Routes.Genres.route)
                },
                onNavigateToPlaylistDetail = { playlistId ->
                    navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                },
                onNavigateToSmartPlaylist = { smartPlaylistType ->
                    navController.navigate(Routes.SmartPlaylistDetail.createRoute(smartPlaylistType.name))
                },
                onNavigateToRestorePlaylist = {
                    navController.navigate(Routes.RestorePlaylist.route)
                },
                onNavigateToFolderDetail = { folderId ->
                    navController.navigate(Routes.FolderDetail.createRoute(folderId))
                },
                onNavigateToAlbumDetail = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToGenreDetail = { genreId ->
                    navController.navigate(Routes.GenreDetail.createRoute(genreId))
                },
                onNavigateToSongSelection = {
                    navController.navigate(Routes.SongSelection.route)
                },
                onNavigateToAlbumSelection = {
                    navController.navigate(Routes.AlbumSelection.route)
                },
                onNavigateToArtistSelection = {
                    navController.navigate(Routes.ArtistSelection.route)
                },
                onNavigateToGenreSelection = {
                    navController.navigate(Routes.GenreSelection.route)
                },
                username = userPreferences?.username ?: ""
            )
        }

        // Now Playing Screen - Full player
        composable(route = Routes.NowPlaying.route) {
            NowPlayingScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToQueue = {
                    navController.navigate(Routes.Queue.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        // Song Selection Screen - Multi-select interface
        composable(route = Routes.SongSelection.route) {
            SongSelectionScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Album Selection Screen - Multi-select interface
        composable(route = Routes.AlbumSelection.route) {
            AlbumSelectionScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Artist Selection Screen - Multi-select interface
        composable(route = Routes.ArtistSelection.route) {
            ArtistSelectionScreen(
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Genre Selection Screen - Multi-select interface
        composable(route = Routes.GenreSelection.route) {
            GenreSelectionScreen(
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
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
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
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToAlbumDetail = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtistDetail = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
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
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        // Search Screen - Search local music library
        composable(route = Routes.Search.route) {
            SearchScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        // Songs Screen - All songs alphabetically with search
        composable(route = Routes.Songs.route) {
            SongsScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        // Settings Screen - App preferences
        composable(route = Routes.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToPlaylists = {
                    navController.navigate(Routes.Playlists.route)
                },
                onNavigateToSongs = {
                    navController.navigate(Routes.Songs.route)
                },
                onNavigateToEqualizer = {
                    navController.navigate(Routes.Equalizer.route)
                },
                onNavigateToExcludedFolders = {
                    navController.navigate(Routes.ExcludedFolders.route)
                },
                onNavigateToAbout = {
                    navController.navigate(Routes.About.route)
                },
                onNavigateToRestorePlaylist = {
                    navController.navigate(Routes.RestorePlaylist.route)
                },
                premiumManager = premiumManager
            )
        }

        // About Screen - App information and links
        composable(route = Routes.About.route) {
            AboutScreen(
                onBackClick = {
                    navController.navigateUp()
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
                },
                onNavigateToSmartPlaylist = { smartPlaylistType ->
                    navController.navigate(Routes.SmartPlaylistDetail.createRoute(smartPlaylistType.name))
                },
                onNavigateToAlbumSelection = {
                    navController.navigate(Routes.AlbumSelection.route)
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
                },
                navController = navController,
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        // Artists Screen - REMOVED: Now handled in HomeScreen Artists tab
        // Clicking on artist in HomeScreen navigates directly to ArtistDetailScreen

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
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                }
            )
        }

        // Genres Screen - All genres
        composable(route = Routes.Genres.route) {
            GenresScreen(
                onNavigateToGenre = { genreId ->
                    navController.navigate(Routes.GenreDetail.createRoute(genreId))
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }

        // Genre Detail Screen - Songs in a genre
        composable(
            route = Routes.GenreDetail.route,
            arguments = listOf(
                navArgument(Routes.ARG_GENRE_ID) { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val genreId = backStackEntry.arguments?.getLong(Routes.ARG_GENRE_ID) ?: return@composable
            GenreDetailScreen(
                genreId = genreId,
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
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
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
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
                },
                onNavigateToParent = {
                    navController.navigateUp()
                },
                onNavigateToSubfolder = { folderPath ->
                    navController.navigate(Routes.FolderDetailByPath.createRoute(folderPath))
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }

        // Folder Detail Screen (Hierarchical) - Subfolders and songs by path
        composable(
            route = Routes.FolderDetailByPath.route,
            arguments = listOf(
                navArgument("folderPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderPath = backStackEntry.arguments?.getString("folderPath")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: return@composable
            FolderDetailScreen(
                folderId = -1L,
                folderPath = folderPath,
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToParent = { parentPath ->
                    if (parentPath != null) {
                        navController.navigate(Routes.FolderDetailByPath.createRoute(parentPath)) {
                            popUpTo(Routes.FolderDetailByPath.route) { inclusive = true }
                        }
                    } else {
                        navController.navigateUp()
                    }
                },
                onNavigateToSubfolder = { subfolderPath ->
                    navController.navigate(Routes.FolderDetailByPath.createRoute(subfolderPath))
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
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

        // Queue Screen - Current and saved queues
        composable(route = Routes.Queue.route) {
            QueueScreen(
                onBackClick = {
                    navController.navigateUp()
                },
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NowPlaying.route)
                },
                onNavigateToAlbum = { albumId ->
                    navController.navigate(Routes.AlbumDetail.createRoute(albumId))
                },
                onNavigateToArtist = { artistId ->
                    navController.navigate(Routes.ArtistDetail.createRoute(artistId))
                }
            )
        }
    }
}

/**
 * Hilt entry point for accessing PreferencesManager from non-injected context.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PreferencesManagerEntryPoint {
    fun preferencesManager(): PreferencesManager
}

/**
 * Hilt entry point for accessing PremiumManager from non-injected context.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PremiumManagerEntryPoint {
    fun premiumManager(): PremiumManager
}

/**
 * Hilt entry point for accessing AdMobManager from non-injected context.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AdMobManagerEntryPoint {
    fun adMobManager(): com.sukoon.music.data.ads.AdMobManager
}

/**
 * Hilt entry point for accessing AdMobDecisionAgent from non-injected context.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AdMobDecisionAgentEntryPoint {
    fun adMobDecisionAgent(): com.sukoon.music.data.ads.AdMobDecisionAgent
}
