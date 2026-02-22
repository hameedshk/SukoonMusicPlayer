package com.sukoon.music.ui.navigation

/**
 * Navigation routes for the Sukoon Music app.
 *
 * Uses sealed class pattern for type-safe navigation with compile-time route checking.
 * Each route represents a screen in the app's navigation graph.
 */
sealed class Routes(val route: String) {
    /**
     * Onboarding screen - Permission grant + optional username setup.
     */
    data object Onboarding : Routes("onboarding")

    /**
     * Home screen - Main entry point showing song list, scan button, and mini player.
     */
    data object Home : Routes("home")

    /**
     * Now Playing screen - Full-screen player with lyrics, controls, and album art.
     */
    data object NowPlaying : Routes("now_playing")

    /**
     * Liked Songs screen - Filtered view of user's liked/favorited songs.
     */
    data object LikedSongs : Routes("liked_songs")

    /**
     * Playlists screen - Shows all user-created playlists.
     */
    data object Playlists : Routes("playlists")

    /**
     * Playlist Detail screen - Shows songs in a specific playlist.
     * Requires playlistId parameter.
     */
    data object PlaylistDetail : Routes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    /**
     * Smart Playlist Detail screen - Shows songs in a smart playlist.
     * Requires smartPlaylistType parameter (MY_FAVOURITE, LAST_ADDED, RECENTLY_PLAYED, MOST_PLAYED).
     */
    data object SmartPlaylistDetail : Routes("smart_playlist/{smartPlaylistType}") {
        fun createRoute(smartPlaylistType: String) = "smart_playlist/$smartPlaylistType"
    }

    /**
     * Restore Playlist screen - Shows deleted playlists and allows restoration.
     */
    data object RestorePlaylist : Routes("restore_playlist")

    /**
     * Search screen - Search and filter local music library.
     */
    data object Search : Routes("search")

    /**
     * Songs screen - All songs in alphabetical order with search.
     */
    data object Songs : Routes("songs")

    /**
     * Song Selection screen - Full-screen multi-select interface for songs.
     */
    data object SongSelection : Routes("song_selection")

    /**
     * Album Selection screen - Full-screen multi-select interface for albums.
     */
    data object AlbumSelection : Routes("album_selection")

    /**
     * Artist Selection screen - Full-screen multi-select interface for artists.
     */
    data object ArtistSelection : Routes("artist_selection")

    /**
     * Genre Selection screen - Full-screen multi-select interface for genres.
     */
    data object GenreSelection : Routes("genre_selection")

    /**
     * Settings screen - App preferences and configuration.
     */
    data object Settings : Routes("settings?openPremium={openPremium}") {
        fun createRoute(openPremiumDialog: Boolean = false) = "settings?openPremium=$openPremiumDialog"
    }

    /**
     * Feedback report screen - user can submit issues/suggestions.
     */
    data object FeedbackReport : Routes("feedback_report")

    /**
     * Equalizer screen - Audio effects and 5-band equalizer control.
     */
    data object Equalizer : Routes("equalizer")

    /**
     * Albums screen - Shows all albums grouped from song library.
     */
    data object Albums : Routes("albums")

    /**
     * Album Detail screen - Shows songs in a specific album.
     * Requires albumId parameter.
     */
    data object AlbumDetail : Routes("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }

    /**
     * Artists screen - Shows all artists grouped from song library.
     */
    data object Artists : Routes("artists")

    /**
     * Artist Detail screen - Shows songs and albums by a specific artist.
     * Requires artistId parameter.
     */
    data object ArtistDetail : Routes("artist/{artistId}") {
        fun createRoute(artistId: Long) = "artist/$artistId"
    }

    /**
     * Genres screen - Shows all genres grouped from song library.
     */
    data object Genres : Routes("genres")

    /**
     * Genre Detail screen - Shows songs in a specific genre.
     * Requires genreId parameter.
     */
    data object GenreDetail : Routes("genre/{genreId}") {
        fun createRoute(genreId: Long) = "genre/$genreId"
    }

    /**
     * Folders screen - Shows all folders grouped from song library.
     */
    data object Folders : Routes("folders")

    /**
     * Folder Detail screen - Shows songs in a specific folder.
     * Requires folderId parameter.
     */
    data object FolderDetail : Routes("folder/{folderId}") {
        fun createRoute(folderId: Long) = "folder/$folderId"
    }

    /**
     * Folder Detail By Path screen - Shows subfolders and songs in a folder (hierarchical).
     * Requires folderPath parameter (URL-encoded).
     */
    data object FolderDetailByPath : Routes("folder_path/{folderPath}") {
        fun createRoute(folderPath: String): String {
            val encoded = java.net.URLEncoder.encode(folderPath, "UTF-8")
            return "folder_path/$encoded"
        }
    }

    /**
     * Folder Selection screen - Full-screen multi-select interface for folders.
     */
    data object FolderSelection : Routes("folder_selection")

    /**
     * Excluded Folders screen - Manage folders excluded from the music library.
     */
    data object ExcludedFolders : Routes("excluded_folders")

    /**
     * About screen - App information, version, and links.
     */
    data object About : Routes("about")

    /**
     * Queue screen - Shows current playback queue with reordering and saved queues.
     */
    data object Queue : Routes("queue")

    /**
     * Queue Detail screen - Shows songs in a specific saved queue.
     * Requires queueId parameter.
     */
    data object QueueDetail : Routes("queue/{queueId}") {
        fun createRoute(queueId: Long) = "queue/$queueId"
    }

    companion object {
        const val ARG_GENRE_ID = "genreId"
        const val ARG_OPEN_PREMIUM = "openPremium"
    }
}
