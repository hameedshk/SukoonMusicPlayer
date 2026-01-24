package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Album
import com.sukoon.music.domain.model.Artist
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * Sort modes for artist songs.
 */
enum class ArtistSongSortMode {
    TITLE,
    ARTIST,
    ALBUM,
    DURATION
}

/**
 * ViewModel for Artist Detail screen.
 *
 * Responsibilities:
 * - Load specific artist by ID
 * - Load songs by the artist
 * - Load albums by the artist
 * - Provide playback actions (play, shuffle, play song)
 * - Handle like/unlike songs
 */
@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    private val _artistId = MutableStateFlow<Long?>(null)

    /**
     * Current artist being displayed.
     */
    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    /**
     * Songs by the current artist.
     */
    private val _artistSongs = MutableStateFlow<List<Song>>(emptyList())
    val artistSongs: StateFlow<List<Song>> = _artistSongs.asStateFlow()

    /**
     * Albums by the current artist.
     */
    private val _artistAlbums = MutableStateFlow<List<Album>>(emptyList())
    val artistAlbums: StateFlow<List<Album>> = _artistAlbums.asStateFlow()

    /**
     * Current playback state for UI feedback.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Selection mode state for multi-select functionality.
     */
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    /**
     * Selected song IDs for batch operations.
     */
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    /**
     * Sort mode for songs.
     */
    private val _sortMode = MutableStateFlow(ArtistSongSortMode.TITLE)
    val sortMode: StateFlow<ArtistSongSortMode> = _sortMode.asStateFlow()

    /**
     * Load artist, songs, and albums by ID.
     * Call this from LaunchedEffect when screen opens.
     *
     * @param artistId The unique ID of the artist
     */
    fun loadArtist(artistId: Long) {
        if (_artistId.value == artistId) return // Already loaded

        _artistId.value = artistId

        // Load artist
        viewModelScope.launch {
            songRepository.getArtistById(artistId)
                .collect { artist ->
                    _artist.value = artist
                    artist?.let {
                        logArtistInteraction(it.id, it.name)
                    }
                }
        }

        // Load artist songs
        viewModelScope.launch {
            songRepository.getSongsByArtistId(artistId)
                .stateIn(viewModelScope)
                .collect { songs ->
                    _artistSongs.value = songs
                }
        }

        // Load artist albums
        viewModelScope.launch {
            songRepository.getAlbumsByArtistId(artistId)
                .stateIn(viewModelScope)
                .collect { albums ->
                    _artistAlbums.value = albums
                }
        }
    }

    /**
     * Log artist interaction to tracking.
     */
    private fun logArtistInteraction(artistId: Long, artistName: String) {
        viewModelScope.launch {
            songRepository.logArtistPlay(artistName)
        }
    }

    /**
     * Play all songs by the artist from the beginning.
     *
     * @param artistSongs List of songs to play
     */
    fun playArtist(artistSongs: List<Song>) {
        if (artistSongs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.playQueue(artistSongs, startIndex = 0)
        }
    }

    /**
     * Shuffle and play all songs by the artist.
     *
     * @param artistSongs List of songs to shuffle and play
     */
    fun shuffleArtist(artistSongs: List<Song>) {
        if (artistSongs.isEmpty()) return
        viewModelScope.launch {
            val shuffledSongs = artistSongs.shuffled()
            playbackRepository.playQueue(shuffledSongs, startIndex = 0)
        }
    }

    /**
     * Play a specific song by the artist.
     * Sets the queue to all artist songs.
     *
     * @param song The song to play
     * @param artistSongs The full artist song list for queue context
     */
    fun playSong(song: Song, artistSongs: List<Song>) {
        viewModelScope.launch {
            val index = artistSongs.indexOf(song)
            if (index >= 0) {
                playbackRepository.playQueue(artistSongs, startIndex = index)
            }
        }
    }

    /**
     * Play all songs in an album by the artist.
     *
     * @param albumId The unique ID of the album to play
     */
    fun playAlbum(albumId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByAlbumId(albumId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { albumSongs ->
                    playbackRepository.playQueue(albumSongs, startIndex = 0)
                }
        }
    }

    /**
     * Toggle like status of a song.
     *
     * @param songId The unique ID of the song
     * @param isLiked Current like status (will be toggled)
     */
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !isLiked)
        }
    }

    /**
     * Toggle play/pause for current playback.
     */
    fun playPause() {
        viewModelScope.launch {
            playbackRepository.playPause()
        }
    }

    /**
     * Skip to next track in queue.
     */
    fun seekToNext() {
        viewModelScope.launch {
            playbackRepository.seekToNext()
        }
    }

    /**
     * Toggle selection mode on/off.
     */
    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedSongIds.value = emptySet()
        }
    }

    /**
     * Toggle selection of a specific song.
     */
    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value
        _selectedSongIds.value = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
    }

    /**
     * Select all songs by the artist.
     */
    fun selectAllSongs(songs: List<Song>) {
        _selectedSongIds.value = songs.map { it.id }.toSet()
    }

    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    /**
     * Play selected songs from the beginning.
     */
    fun playSelectedSongs(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                playbackRepository.playQueue(selectedSongs, startIndex = 0)
                toggleSelectionMode(false)
            }
        }
    }

    /**
     * Play selected songs next in queue.
     */
    fun playSelectedSongsNext(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                playbackRepository.playNext(selectedSongs)
            }
        }
    }

    /**
     * Add selected songs to queue.
     */
    fun addSelectedSongsToQueueBatch(allSongs: List<Song>) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            selectedSongs.forEach { song ->
                playbackRepository.addToQueue(song)
            }
            toggleSelectionMode(false)
        }
    }

    /**
     * Delete selected songs with result handling for permissions.
     */
    fun deleteSelectedSongsWithResult(allSongs: List<Song>, context: android.content.Context, onResult: (com.sukoon.music.data.mediastore.DeleteHelper.DeleteResult) -> Unit) {
        val ids = _selectedSongIds.value
        if (ids.isEmpty()) return

        viewModelScope.launch {
            val selectedSongs = allSongs.filter { ids.contains(it.id) }
            if (selectedSongs.isNotEmpty()) {
                val result = com.sukoon.music.data.mediastore.DeleteHelper.deleteSongs(
                    context,
                    selectedSongs
                )
                onResult(result)
                toggleSelectionMode(false)
            }
        }
    }

    /**
     * Set the sort mode for songs.
     */
    fun setSortMode(mode: ArtistSongSortMode) {
        _sortMode.value = mode
    }
}
