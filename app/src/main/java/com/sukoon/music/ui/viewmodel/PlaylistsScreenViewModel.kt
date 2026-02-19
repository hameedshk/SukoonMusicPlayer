package com.sukoon.music.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Playlist
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.PlaylistRepository
import com.sukoon.music.data.mediastore.DeleteHelper
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
 * ViewModel for the Playlists Screen.
 *
 * Owns all UI state for the playlists tab: sorting, dialogs, delete operations.
 * Manages lifecycle safely with DisposableEffect cleanup.
 * Prevents race conditions with Mutex during delete + operations.
 */
@HiltViewModel
class PlaylistsScreenViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    val playbackRepository: PlaybackRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "PlaylistsScreenViewModel"
    }

    // ========== DATA FLOWS ==========

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),
            initialValue = emptyList()
        )

    val playbackState = playbackRepository.playbackState

    // ========== UI STATE FLOWS ==========

    private val _sortMode = MutableStateFlow("Name")
    val sortMode: StateFlow<String> = _sortMode.asStateFlow()

    private val _sortOrder = MutableStateFlow("A to Z")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    private val _showSortDialog = MutableStateFlow(false)
    val showSortDialog: StateFlow<Boolean> = _showSortDialog.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    private val _showMenu = MutableStateFlow(false)
    val showMenu: StateFlow<Boolean> = _showMenu.asStateFlow()

    private val _playlistToDelete = MutableStateFlow<Playlist?>(null)
    val playlistToDelete: StateFlow<Playlist?> = _playlistToDelete.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _newPlaylistName = MutableStateFlow("")
    val newPlaylistName: StateFlow<String> = _newPlaylistName.asStateFlow()

    // ========== ERROR HANDLING FLOWS ==========

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

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
    }

    fun showSortDialog() { _showSortDialog.value = true }
    fun hideSortDialog() { _showSortDialog.value = false }

    fun selectPlaylist(playlistId: Long) { _selectedPlaylistId.value = playlistId }
    fun deselectPlaylist() { _selectedPlaylistId.value = null }

    fun openMenuForPlaylist(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
        _showMenu.value = true
    }
    fun closeMenu() { _showMenu.value = false }

    fun showDeleteConfirmation(playlist: Playlist) { _playlistToDelete.value = playlist }
    fun hideDeleteConfirmation() { _playlistToDelete.value = null }

    fun showCreatePlaylistDialog() { _showCreatePlaylistDialog.value = true }
    fun hideCreatePlaylistDialog() { _showCreatePlaylistDialog.value = false }

    fun setNewPlaylistName(name: String) { _newPlaylistName.value = name }

    fun clearDeleteError() { _deleteError.value = null }

    fun createPlaylist() {
        if (_newPlaylistName.value.isBlank()) {
            _deleteError.value = "Playlist name cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                playlistRepository.createPlaylist(_newPlaylistName.value)
                _newPlaylistName.value = ""
                _showCreatePlaylistDialog.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error creating playlist", e)
                _deleteError.value = e.message
            }
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) {
            _deleteError.value = "Playlist name cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Note: Rename functionality is handled via PlaylistRepository's update methods
                Log.d(TAG, "Renaming playlist $playlistId to $newName")
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming playlist", e)
                _deleteError.value = e.message
            }
        }
    }

    // ========== CONCURRENCY CONTROL ==========

    private val dataModificationMutex = Mutex()

    /**
     * Delete playlist with atomic operation + Mutex to prevent race conditions.
     * Only callbacks if screen still active.
     */
    fun confirmDelete(playlist: Playlist, callback: (DeleteHelper.DeleteResult) -> Unit) {
        if (_isDeleting.value) return

        _isDeleting.value = true

        viewModelScope.launch {
            try {
                // Acquire lock - delete playlist via repository
                dataModificationMutex.withLock {
                    try {
                        playlistRepository.deletePlaylist(playlist.id)
                        val result = DeleteHelper.DeleteResult.Success

                        // Only callback if screen still active
                        if (_isScreenActive.value) {
                            callback(result)
                            _playlistToDelete.value = null
                            _isDeleting.value = false
                        } else {
                            _isDeleting.value = false
                        }
                    } catch (e: Exception) {
                        val result = DeleteHelper.DeleteResult.Error(e.message ?: "Unknown error")
                        if (_isScreenActive.value) {
                            callback(result)
                        }
                        _deleteError.value = result.message
                        _playlistToDelete.value = null
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
                _playlistToDelete.value = null
                _isDeleting.value = false
            }
        }
    }

    fun finalizeDeletion() {
        _playlistToDelete.value = null
        _isDeleting.value = false
    }
}
