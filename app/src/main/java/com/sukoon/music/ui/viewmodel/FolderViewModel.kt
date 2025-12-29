package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Folders screen.
 *
 * Responsibilities:
 * - Expose folder list from SongRepository
 * - Provide playback actions for entire folders
 * - Observe current playback state for UI highlighting
 *
 * Follows the architecture established by AlbumsViewModel and ArtistsViewModel.
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    /**
     * All folders grouped from song library.
     * Updates reactively when songs are added/removed or metadata changes.
     */
    val folders: StateFlow<List<Folder>> = songRepository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Current playback state for UI feedback.
     * Used to highlight currently playing folder.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Play all songs in a folder from the beginning.
     * Clears current queue and starts folder playback.
     *
     * @param folderId The unique ID of the folder to play
     */
    fun playFolder(folderId: Long) {
        viewModelScope.launch {
            // Get all songs in the folder
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { folderSongs ->
                    // Play queue from the beginning
                    playbackRepository.playQueue(folderSongs, startIndex = 0)
                }
        }
    }

    /**
     * Shuffle and play all songs in a folder.
     * Randomizes song order before playback.
     *
     * @param folderId The unique ID of the folder to shuffle
     */
    fun shuffleFolder(folderId: Long) {
        viewModelScope.launch {
            // Get all songs in the folder
            songRepository.getSongsByFolderId(folderId)
                .stateIn(viewModelScope)
                .value
                .takeIf { it.isNotEmpty() }
                ?.let { folderSongs ->
                    // Shuffle the songs and play queue
                    val shuffledSongs = folderSongs.shuffled()
                    playbackRepository.playQueue(shuffledSongs, startIndex = 0)
                }
        }
    }

    /**
     * Navigate to folder detail screen.
     * (Navigation logic handled by composable, this is a placeholder for future actions)
     */
    fun onFolderClick(folderId: Long) {
        // Navigation is handled by the screen composable
        // This method is reserved for future folder-specific actions
        // (e.g., analytics tracking, pre-loading folder metadata)
    }
}
