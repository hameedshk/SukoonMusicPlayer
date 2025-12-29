package com.sukoon.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.ui.components.FolderViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Folders screen.
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = songRepository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sortMode: StateFlow<FolderSortMode> = preferencesManager.userPreferencesFlow
        .map { it.folderSortMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FolderSortMode.NAME_ASC
        )

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    private val _folderViewMode = MutableStateFlow(FolderViewMode.DIRECTORIES)
    val folderViewMode: StateFlow<FolderViewMode> = _folderViewMode.asStateFlow()

    val hiddenFolders: StateFlow<List<Folder>> = songRepository.getExcludedFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedFolderForPlaylist = MutableStateFlow<Long?>(null)
    val selectedFolderForPlaylist: StateFlow<Long?> = _selectedFolderForPlaylist.asStateFlow()

    private val _deleteResult = MutableStateFlow<DeleteHelper.DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteHelper.DeleteResult?> = _deleteResult.asStateFlow()

    fun setFolderViewMode(mode: FolderViewMode) {
        _folderViewMode.value = mode
    }

    fun unhideFolder(folderId: Long) {
        viewModelScope.launch {
            val folder = hiddenFolders.value.find { it.id == folderId }
            folder?.let {
                preferencesManager.removeExcludedFolderPath(it.path)
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            val songs = songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value

            if (songs.isEmpty()) {
                _deleteResult.value = DeleteHelper.DeleteResult.Success
                return@launch
            }

            val result = DeleteHelper.deleteSongs(context, songs)
            _deleteResult.value = result
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    fun showAddToPlaylistDialog(folderId: Long) {
        _selectedFolderForPlaylist.value = folderId
    }

    fun dismissPlaylistDialog() {
        _selectedFolderForPlaylist.value = null
    }

    fun addFolderToPlaylist(folderId: Long, playlistId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .forEach { song ->
                    songRepository.addSongToPlaylist(playlistId, song.id)
                }
            dismissPlaylistDialog()
        }
    }

    fun playFolder(folderId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { playbackRepository.playQueue(it, 0) }
        }
    }

    fun shuffleFolder(folderId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { playbackRepository.playQueue(it.shuffled(), 0) }
        }
    }

    fun addToQueue(folderId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { playbackRepository.addToQueue(it) }
        }
    }

    fun playNext(folderId: Long) {
        viewModelScope.launch {
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { playbackRepository.playNext(it) }
        }
    }

    fun excludeFolder(folderId: Long) {
        viewModelScope.launch {
            folders.value.find { it.id == folderId }?.let {
                preferencesManager.addExcludedFolderPath(it.path)
            }
        }
    }

    fun setSortMode(mode: FolderSortMode) {
        viewModelScope.launch {
            preferencesManager.setFolderSortMode(mode)
        }
    }
}
