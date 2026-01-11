package com.sukoon.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.model.Genre
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Genre Detail screen.
 */
@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    @ApplicationContext private val context: Context,
    val adMobManager: com.sukoon.music.data.ads.AdMobManager
) : ViewModel() {

    private val _genreId = MutableStateFlow<Long>(-1)

    private val _deleteResult = MutableStateFlow<DeleteHelper.DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteHelper.DeleteResult?> = _deleteResult.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val genre: StateFlow<Genre?> = _genreId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(null)
            else songRepository.getGenreById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val genreSongs: StateFlow<List<Song>> = _genreId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(emptyList())
            else songRepository.getSongsByGenreId(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    fun loadGenre(genreId: Long) {
        _genreId.value = genreId
    }

    /**
     * Play all songs in the genre from the beginning.
     */
    fun playGenre(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.playQueue(songs, startIndex = 0)
        }
    }

    /**
     * Shuffle and play all songs in the genre.
     */
    fun shuffleGenre(songs: List<Song>) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.shuffleAndPlayQueue(songs)
        }
    }

    /**
     * Play a specific song from the genre.
     */
    fun playSong(song: Song, songs: List<Song>) {
        viewModelScope.launch {
            val index = songs.indexOf(song)
            if (index >= 0) {
                playbackRepository.playQueue(songs, startIndex = index)
            }
        }
    }

    fun playNext(song: Song) {
        viewModelScope.launch {
            playbackRepository.playNext(song)
        }
    }

    fun addToQueue(song: Song) {
        viewModelScope.launch {
            playbackRepository.addToQueue(song)
        }
    }

    /**
     * Toggle like status of a song.
     */
    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !isLiked)
        }
    }

    /**
     * Request deletion of a song.
     */
    fun deleteSong(song: Song) {
        _deleteResult.value = DeleteHelper.deleteSongs(context, listOf(song))
    }

    fun resetDeleteResult() {
        _deleteResult.value = null
    }
}
