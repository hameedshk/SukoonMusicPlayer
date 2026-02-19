package com.sukoon.music.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Song
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * ViewModel for the Songs Screen.
 *
 * Owns all UI state for the songs tab: sorting, dialogs, delete operations.
 * Manages lifecycle safely with DisposableEffect cleanup.
 * Prevents race conditions with Mutex during delete + scan operations.
 */
@HiltViewModel
class SongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "SongsViewModel"
    }

    // ========== DATA FLOWS ==========

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),  // 1s not 5s (Bug #18)
            initialValue = emptyList()
        )

    val playbackState = playbackRepository.playbackState

    // ========== UI STATE FLOWS ==========

    private val _sortMode = MutableStateFlow("Song name")
    val sortMode: StateFlow<String> = _sortMode.asStateFlow()

    private val _sortOrder = MutableStateFlow("A to Z")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog: StateFlow<Boolean> = _showSortDialog.asStateFlow()

    private val _selectedSongId = MutableStateFlow<Long?>(null)
    val selectedSongId: StateFlow<Long?> = _selectedSongId.asStateFlow()

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu.asStateFlow()

    private val _songToDelete = MutableStateFlow<Song?>(null)
    val songToDelete: StateFlow<Song?> = _songToDelete.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _showInfoForSong = MutableStateFlow<Song?>(null)
    val showInfoForSong: StateFlow<Song?> = _showInfoForSong.asStateFlow()

    private val _scrollPosition = MutableStateFlow(0)
    val scrollPosition: StateFlow<Int> = _scrollPosition.asStateFlow()

    // ========== ERROR HANDLING FLOWS ==========

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _requiresDeletePermission = MutableStateFlow<android.content.IntentSender?>(null)
    val requiresDeletePermission: StateFlow<android.content.IntentSender?> = _requiresDeletePermission.asStateFlow()

    // ========== LIFECYCLE FLOWS ==========

    private val _isScreenActive = MutableStateFlow(true)
    val isScreenActive: StateFlow<Boolean> = _isScreenActive.asStateFlow()

    override fun onCleared() {
        _isScreenActive.value = false
        super.onCleared()
    }

    // ========== PUBLIC METHODS ==========

    fun setScreenActive(active: Boolean) {
        _isScreenActive.value = active
    }

    fun setSortMode(mode: String, order: String) {
        _sortMode.value = mode
        _sortOrder.value = order
        // TODO: Persist sort preferences when SettingsRepository method available
    }

    fun showSortDialog() { _showSortDialog.value = true }
    fun hideSortDialog() { _showSortDialog.value = false }

    fun selectSong(songId: Long) { _selectedSongId.value = songId }
    fun deselectSong() { _selectedSongId.value = null }

    fun openMenuForSong(songId: Long) {
        _selectedSongId.value = songId
        _showMenu.value = true
    }
    fun closeMenu() { _showMenu.value = false }

    fun showDeleteConfirmation(song: Song) { _songToDelete.value = song }
    fun hideDeleteConfirmation() { _songToDelete.value = null }

    fun showSongInfo(song: Song) { _showInfoForSong.value = song }
    fun hideSongInfo() { _showInfoForSong.value = null }

    fun saveScrollPosition(index: Int) { _scrollPosition.value = index }

    fun clearDeleteError() { _deleteError.value = null }

    fun playQueue(songs: List<Song>, index: Int) {
        viewModelScope.launch {
            playbackRepository.playQueue(songs, index)
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            playbackRepository.shuffleAndPlayQueue(songs.value)
        }
    }

    fun playAll() {
        viewModelScope.launch {
            playbackRepository.playQueue(songs.value, 0)
        }
    }

    fun toggleLike(songId: Long, isLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, isLiked)
        }
    }

    // ========== CONCURRENCY CONTROL ==========

    private val dataModificationMutex = Mutex()

    /**
     * Delete song with atomic operation + Mutex to prevent race with scan (Bug #17).
     * Pauses playback if deleting currently playing song (Bug #14).
     * Only callbacks if screen still active (Bug #2).
     */
    fun confirmDelete(song: Song, callback: (DeleteHelper.DeleteResult) -> Unit) {
        if (_isDeleting.value) return  // Prevent duplicate deletes

        _isDeleting.value = true

        viewModelScope.launch {
            try {
                // Check if deleting currently playing song (Bug #14 fix)
                val isCurrentlyPlaying = playbackState.value.currentSong?.id == song.id
                if (isCurrentlyPlaying) {
                    playbackRepository.pause()
                }

                // Acquire lock - prevents race with scan (Bug #17 fix)
                dataModificationMutex.withLock {
                    val result = DeleteHelper.deleteSongs(context, listOf(song))

                    // Only callback if screen still active (Bug #2 fix)
                    if (_isScreenActive.value) {
                        callback(result)

                        when (result) {
                            is DeleteHelper.DeleteResult.Success -> {
                                _songToDelete.value = null
                                _isDeleting.value = false
                                // Trigger refresh across all screens
                                songRepository.scanLocalMusic()
                            }
                            is DeleteHelper.DeleteResult.RequiresPermission -> {
                                _requiresDeletePermission.value = result.intentSender
                                _isDeleting.value = false
                            }
                            is DeleteHelper.DeleteResult.Error -> {
                                _deleteError.value = result.message
                                _songToDelete.value = null
                                _isDeleting.value = false
                            }
                        }
                    } else {
                        _isDeleting.value = false
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.w(TAG, "Delete cancelled mid-operation", e)
                _isDeleting.value = false
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Delete failed", e)
                _deleteError.value = e.message
                _songToDelete.value = null
                _isDeleting.value = false
            }
        }
    }

    fun finalizeDeletion() {
        _songToDelete.value = null
        _isDeleting.value = false
    }
}
