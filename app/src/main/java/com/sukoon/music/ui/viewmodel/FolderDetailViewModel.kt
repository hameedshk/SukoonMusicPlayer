package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderItem
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Folder Detail screen.
 *
 * Responsibilities:
 * - Load specific folder by ID or path
 * - Load subfolders and songs in the folder (hierarchical)
 * - Provide playback actions (play, shuffle, play song)
 * - Handle like/unlike songs
 */
@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    private var currentFolderId: Long = -1
    private var currentFolderPath: String = ""

    /**
     * Parent folder path for hierarchical navigation.
     * Null if at root level.
     */
    private val _parentFolderPath = MutableStateFlow<String?>(null)
    val parentFolderPath: StateFlow<String?> = _parentFolderPath

    /**
     * Current folder being displayed.
     * Updated when loadFolder() is called.
     */
    private var _folder: StateFlow<Folder?> = songRepository.getFolderById(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    val folder: StateFlow<Folder?> get() = _folder

    /**
     * Songs in the current folder.
     * Updated when loadFolder() is called.
     */
    private var _folderSongs: StateFlow<List<Song>> = songRepository.getSongsByFolderId(-1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val folderSongs: StateFlow<List<Song>> get() = _folderSongs

    /**
     * Mixed list of subfolders and songs in current folder.
     * Used for hierarchical navigation.
     */
    private val _folderItems = MutableStateFlow<List<FolderItem>>(emptyList())
    val folderItems: StateFlow<List<FolderItem>> = _folderItems

    /**
     * Current playback state for UI feedback.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    /**
     * Load folder and its songs by ID.
     * Call this from LaunchedEffect when screen opens.
     *
     * @param folderId The unique ID of the folder
     */
    fun loadFolder(folderId: Long) {
        if (currentFolderId == folderId) return // Already loaded

        currentFolderId = folderId

        _folder = songRepository.getFolderById(folderId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        _folderSongs = songRepository.getSongsByFolderId(folderId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Load folder contents (subfolders + songs) by path for hierarchical navigation.
     *
     * @param folderPath The file system path of the folder
     */
    fun loadFolderByPath(folderPath: String) {
        // Prevent navigation to storage root paths
        if (isStorageRootPath(folderPath)) {
            _folderItems.value = emptyList()
            return
        }

        if (currentFolderPath == folderPath) return // Already loaded

        currentFolderPath = folderPath
        _parentFolderPath.value = calculateParentPath(folderPath)

        viewModelScope.launch {
            songRepository.getSubfoldersAndSongsByPath(folderPath)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
                .collect { items ->
                    _folderItems.value = items
                }
        }
    }

    /**
     * Calculate parent folder path from current path.
     * Returns null if at root level or at storage root.
     */
    private fun calculateParentPath(path: String): String? {
        val normalizedPath = path.trimEnd('/')
        val lastSlashIndex = normalizedPath.lastIndexOf('/')

        if (lastSlashIndex <= 0) {
            return null // Root level
        }

        val parentPath = normalizedPath.substring(0, lastSlashIndex)

        // Don't allow navigation to storage root paths
        if (isStorageRootPath(parentPath)) {
            return null
        }

        return parentPath
    }

    /**
     * Check if a path is a storage root that should not be navigable.
     */
    private fun isStorageRootPath(path: String): Boolean {
        val normalizedPath = path.trimEnd('/')
        return normalizedPath.isEmpty() ||
                normalizedPath == "/storage" ||
                normalizedPath == "/storage/emulated" ||
                normalizedPath.matches(Regex("^/storage/emulated/\\d+$"))
    }

    /**
     * Play all songs in the folder from the beginning.
     *
     * @param folderSongs List of songs to play
     */
    fun playFolder(folderSongs: List<Song>) {
        if (folderSongs.isEmpty()) return
        viewModelScope.launch {
            playbackRepository.playQueue(folderSongs, startIndex = 0)
        }
    }

    /**
     * Shuffle and play all songs in the folder.
     *
     * @param folderSongs List of songs to shuffle and play
     */
    fun shuffleFolder(folderSongs: List<Song>) {
        if (folderSongs.isEmpty()) return
        viewModelScope.launch {
            val shuffledSongs = folderSongs.shuffled()
            playbackRepository.playQueue(shuffledSongs, startIndex = 0)
        }
    }

    /**
     * Play a specific song from the folder.
     * Sets the queue to all folder songs.
     *
     * @param song The song to play
     * @param folderSongs The full folder song list for queue context
     */
    fun playSong(song: Song, folderSongs: List<Song>) {
        viewModelScope.launch {
            val index = folderSongs.indexOf(song)
            if (index >= 0) {
                playbackRepository.playQueue(folderSongs, startIndex = index)
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
