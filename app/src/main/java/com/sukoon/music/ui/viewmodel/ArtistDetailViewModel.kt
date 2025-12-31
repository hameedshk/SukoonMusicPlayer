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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    private var currentArtistId: Long = -1

    /**
     * Current artist being displayed.
     * Updated when loadArtist() is called.
     */
    private var _artist: StateFlow<Artist?> = songRepository.getArtistById(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    val artist: StateFlow<Artist?> get() = _artist

    /**
     * Songs by the current artist.
     * Updated when loadArtist() is called.
     */
    private var _artistSongs: StateFlow<List<Song>> = songRepository.getSongsByArtistId(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val artistSongs: StateFlow<List<Song>> get() = _artistSongs

    /**
     * Albums by the current artist.
     * Updated when loadArtist() is called.
     */
    private var _artistAlbums: StateFlow<List<Album>> = songRepository.getAlbumsByArtistId(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val artistAlbums: StateFlow<List<Album>> get() = _artistAlbums

    /**
     * Current playback state for UI feedback.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Load artist, songs, and albums by ID.
     * Call this from LaunchedEffect when screen opens.
     *
     * @param artistId The unique ID of the artist
     */
    fun loadArtist(artistId: Long) {
        if (currentArtistId == artistId) return // Already loaded

        currentArtistId = artistId

        _artist = songRepository.getArtistById(artistId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        _artistSongs = songRepository.getSongsByArtistId(artistId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        _artistAlbums = songRepository.getAlbumsByArtistId(artistId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        viewModelScope.launch {
            _artist.filterNotNull().first().let { artist ->
                logArtistInteraction(artist.id, artist.name)
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
}
