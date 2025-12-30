package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.domain.model.SmartPlaylist
import com.sukoon.music.domain.model.SmartPlaylistType
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.PlaylistRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Playlist screens.
 *
 * Responsibilities:
 * - Expose playlists data to UI
 * - Handle playlist CRUD operations
 * - Manage playlist-song relationships
 * - Coordinate with playback repository for playing playlists
 *
 * State Management:
 * - All data exposed as StateFlow for lifecycle-aware observation
 * - Uses SharingStarted.WhileSubscribed(5000) to conserve resources
 * - Transforms repository Flows to StateFlows for UI consumption
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val playbackRepository: PlaybackRepository,
    private val songRepository: SongRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    // ============================================
    // PLAYLIST DATA
    // ============================================

    /**
     * All playlists with their metadata and song counts.
     * Updates automatically when playlists are added/removed/modified.
     */
    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Smart playlists (My favourite, Last added, Recently played, Most played).
     * These are dynamically generated based on user behavior.
     */
    val smartPlaylists: StateFlow<List<SmartPlaylist>> = combine(
        songRepository.getLikedSongsCount(),
        songRepository.getAllSongs(),  // For "Last added" count
        songRepository.getRecentlyPlayedCount(),
        songRepository.getMostPlayedCount()
    ) { likedCount, allSongs, recentCount, mostPlayedCount ->
        listOf(
            SmartPlaylist(
                type = SmartPlaylistType.MY_FAVOURITE,
                title = SmartPlaylist.getDisplayName(SmartPlaylistType.MY_FAVOURITE),
                songCount = likedCount
            ),
            SmartPlaylist(
                type = SmartPlaylistType.LAST_ADDED,
                title = SmartPlaylist.getDisplayName(SmartPlaylistType.LAST_ADDED),
                songCount = allSongs.size
            ),
            SmartPlaylist(
                type = SmartPlaylistType.RECENTLY_PLAYED,
                title = SmartPlaylist.getDisplayName(SmartPlaylistType.RECENTLY_PLAYED),
                songCount = recentCount
            ),
            SmartPlaylist(
                type = SmartPlaylistType.MOST_PLAYED,
                title = SmartPlaylist.getDisplayName(SmartPlaylistType.MOST_PLAYED),
                songCount = mostPlayedCount
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Currently selected playlist ID.
     * Set via loadPlaylist() when user navigates to a playlist detail screen.
     */
    private val _currentPlaylistId = MutableStateFlow<Long?>(null)
    val currentPlaylistId: StateFlow<Long?> = _currentPlaylistId.asStateFlow()

    /**
     * Songs in the currently selected playlist.
     * Updates automatically when songs are added/removed/reordered.
     */
    val currentPlaylistSongs: StateFlow<List<Song>> = _currentPlaylistId
        .flatMapLatest { playlistId ->
            if (playlistId != null) {
                playlistRepository.getSongsInPlaylist(playlistId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * All available songs in the library.
     * Used for song selection when adding to playlists.
     */
    val availableSongs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Songs that can be added to the current playlist.
     * Filters out songs already in the playlist.
     */
    val songsNotInPlaylist: StateFlow<List<Song>> = combine(
        availableSongs,
        currentPlaylistSongs
    ) { allSongs, playlistSongs ->
        val playlistSongIds = playlistSongs.map { it.id }.toSet()
        allSongs.filter { it.id !in playlistSongIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Filter state for playlists screen (null: All, true: Smart, false: My Playlists)
     */
    private val _playlistFilter = MutableStateFlow<Boolean?>(null)
    val playlistFilter: StateFlow<Boolean?> = _playlistFilter.asStateFlow()

    fun setPlaylistFilter(filter: Boolean?) {
        _playlistFilter.value = filter
    }

    // ============================================
    // PLAYBACK STATE
    // ============================================

    /**
     * Current playback state (playing song, queue, shuffle, repeat, etc.).
     * Observed by UI to show playback controls and mini-player.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    // ============================================
    // PLAYLIST ACTIONS
    // ============================================

    /**
     * Load a playlist by ID.
     * Call this when navigating to playlist detail screen.
     */
    fun loadPlaylist(playlistId: Long) {
        _currentPlaylistId.value = playlistId
    }

    /**
     * Create a new playlist.
     * @param name Playlist name (required)
     * @param description Playlist description (optional)
     * @param onCreated Callback with the newly created playlist ID
     */
    fun createPlaylist(name: String, description: String? = null, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name, description)
            onCreated?.invoke(playlistId)
        }
    }

    /**
     * Update an existing playlist's metadata.
     * @param playlist Updated playlist data
     */
    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.updatePlaylist(playlist)
        }
    }

    /**
     * Delete a playlist and all its song associations.
     * @param playlistId ID of the playlist to delete
     */
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    // ============================================
    // PLAYLIST-SONG MANAGEMENT
    // ============================================

    /**
     * Add a song to a playlist.
     * If the song is already in the playlist, this operation is ignored.
     *
     * @param playlistId ID of the playlist
     * @param songId ID of the song to add
     */
    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    /**
     * Remove a song from a playlist.
     * @param playlistId ID of the playlist
     * @param songId ID of the song to remove
     */
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    /**
     * Check if a song is in a playlist.
     * @param playlistId ID of the playlist
     * @param songId ID of the song
     * @return Flow emitting true if song is in playlist, false otherwise
     */
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean {
        return playlistRepository.isSongInPlaylist(playlistId, songId)
    }

    /**
     * Update the position of a song within a playlist.
     * Used for drag-and-drop reordering.
     *
     * @param playlistId ID of the playlist
     * @param songId ID of the song to move
     * @param newPosition New position (0-based index)
     */
    fun updateSongPosition(playlistId: Long, songId: Long, newPosition: Int) {
        viewModelScope.launch {
            playlistRepository.updateSongPosition(playlistId, songId, newPosition)
        }
    }

    // ============================================
    // PLAYBACK ACTIONS
    // ============================================

    /**
     * Play a single song immediately.
     * @param song Song to play
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            playbackRepository.playSong(song)
        }
    }

    /**
     * Play all songs in a playlist starting from the first song.
     * @param playlistId ID of the playlist to play
     */
    fun playPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val songs = playlistRepository.getSongsInPlaylist(playlistId)
                .stateIn(viewModelScope)
                .value

            if (songs.isNotEmpty()) {
                playbackRepository.playQueue(songs, startIndex = 0)
            }
        }
    }

    /**
     * Play all songs in a playlist with shuffle enabled.
     * @param playlistId ID of the playlist to shuffle and play
     */
    fun shufflePlaylist(playlistId: Long) {
        viewModelScope.launch {
            val songs = playlistRepository.getSongsInPlaylist(playlistId)
                .stateIn(viewModelScope)
                .value

            if (songs.isNotEmpty()) {
                // Enable shuffle first
                playbackRepository.setShuffleEnabled(true)
                // Then play the queue
                playbackRepository.playQueue(songs, startIndex = 0)
            }
        }
    }

    /**
     * Toggle between play and pause.
     */
    fun playPause() {
        viewModelScope.launch {
            playbackRepository.playPause()
        }
    }

    /**
     * Skip to the next track.
     */
    fun seekToNext() {
        viewModelScope.launch {
            playbackRepository.seekToNext()
        }
    }

    /**
     * Skip to the previous track.
     */
    fun seekToPrevious() {
        viewModelScope.launch {
            playbackRepository.seekToPrevious()
        }
    }

    // ============================================
    // IMPORT/EXPORT ACTIONS
    // ============================================

    /**
     * Export all playlists to JSON format.
     * @return JSON string containing all playlists
     */
    suspend fun exportAllPlaylists(): String {
        return playlistRepository.exportAllPlaylists()
    }

    /**
     * Export a single playlist to JSON format.
     * @param playlistId ID of the playlist to export
     * @return JSON string containing the playlist, or null if not found
     */
    suspend fun exportPlaylist(playlistId: Long): String? {
        return playlistRepository.exportPlaylist(playlistId)
    }

    /**
     * Import playlists from JSON string.
     * @param json JSON string containing playlist data
     * @return Number of playlists successfully imported
     */
    suspend fun importPlaylists(json: String): Int {
        return playlistRepository.importPlaylists(json)
    }

    // ============================================
    // TRASH/RESTORE ACTIONS
    // ============================================

    /**
     * Get all deleted playlists from trash.
     */
    val deletedPlaylists: StateFlow<List<com.sukoon.music.domain.model.DeletedPlaylist>> =
        playlistRepository.getDeletedPlaylists()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Get count of deleted playlists.
     */
    val deletedPlaylistsCount: StateFlow<Int> =
        playlistRepository.getDeletedPlaylistsCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    /**
     * Restore a deleted playlist from trash.
     */
    suspend fun restorePlaylist(deletedPlaylistId: Long): Boolean {
        return playlistRepository.restorePlaylist(deletedPlaylistId)
    }

    /**
     * Permanently delete a playlist from trash.
     */
    fun permanentlyDeletePlaylist(deletedPlaylistId: Long) {
        viewModelScope.launch {
            playlistRepository.permanentlyDeletePlaylist(deletedPlaylistId)
        }
    }

    /**
     * Clear all deleted playlists from trash.
     */
    fun clearTrash() {
        viewModelScope.launch {
            playlistRepository.clearTrash()
        }
    }
}
