package com.sukoon.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.ads.AdMobManager
import com.sukoon.music.data.mediastore.DeleteHelper
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.domain.model.Folder
import com.sukoon.music.domain.model.FolderSortMode
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.SongRepository
import com.sukoon.music.ui.components.FolderViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FolderBrowserItem {
    data class SubFolder(
        val name: String,
        val path: String,
        val songCount: Int,
        val totalDuration: Long,
        val albumArtUri: String?
    ) : FolderBrowserItem()
    data class SongItem(val song: Song) : FolderBrowserItem()
}

/**
 * ViewModel for Folders screen with Recursive Browsing.
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val songRepository: SongRepository,
    val playbackRepository: PlaybackRepository,
    private val preferencesManager: PreferencesManager,
    val adMobManager: AdMobManager,
    @ApplicationContext private val context: Context

) : ViewModel() {

    private fun String.stripAndroidStoragePrefix(): String {
        val prefix = "/storage/emulated/0/"
        return if (this.startsWith(prefix)) this.removePrefix(prefix) else this
    }
    private val allFolders: StateFlow<List<Folder>> = songRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allSongs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sortMode: StateFlow<FolderSortMode> = preferencesManager.userPreferencesFlow
        .map { it.folderSortMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FolderSortMode.NAME_ASC)

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState

    private val _folderViewMode = MutableStateFlow(FolderViewMode.DIRECTORIES)
    val folderViewMode: StateFlow<FolderViewMode> = _folderViewMode.asStateFlow()

    val hiddenFolders: StateFlow<List<Folder>> = songRepository.getExcludedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPath = MutableStateFlow<String?>(null)
    val currentPath: StateFlow<String?> = _currentPath.asStateFlow()

    /**
     * Hierarchical content based on currentPath.
     */
    val browsingContent: StateFlow<List<FolderBrowserItem>> = combine(
        allFolders,
        allSongs,
        _currentPath,
        sortMode
    ) { folders, songs, currentPath, sort ->
        val items = mutableListOf<FolderBrowserItem>()

        if (currentPath == null) {
            // Show root folders (top-level segments of all folder paths)
            val rootPaths = folders.map { it.path.stripAndroidStoragePrefix().split("/").filter { s -> s.isNotEmpty() }.firstOrNull() ?: "" }
                .distinct()
            
            rootPaths.forEach { root ->
                val fullPath = root
                val foldersUnderRoot = folders.filter { it.path.stripAndroidStoragePrefix().startsWith(fullPath) }
                val songCount = foldersUnderRoot.sumOf { it.songCount }
                items.add(FolderBrowserItem.SubFolder(
                    name = root,
                    path = "/storage/emulated/0/$root",
                    songCount = songCount,
                    totalDuration = foldersUnderRoot.sumOf { it.totalDuration },
                    albumArtUri = foldersUnderRoot.firstOrNull { it.albumArtUri != null }?.albumArtUri
                ))
            }
        } else {
            // Show direct sub-folders and songs in currentPath
            val subFoldersMap = mutableMapOf<String, MutableList<Folder>>()
            
            folders.forEach { folder ->
                if (folder.path.startsWith(currentPath) && folder.path != currentPath) {
                    val relativePath = folder.path.removePrefix(currentPath).removePrefix("/")
                    val firstSegment = relativePath.split("/").firstOrNull()
                    if (firstSegment != null) {
                        subFoldersMap.getOrPut(firstSegment) { mutableListOf() }.add(folder)
                    }
                }
            }

            subFoldersMap.forEach { (name, foldersInSub) ->
                val fullPath = if (currentPath.endsWith("/")) "$currentPath$name" else "$currentPath/$name"
                items.add(FolderBrowserItem.SubFolder(
                    name = name,
                    path = fullPath,
                    songCount = foldersInSub.sumOf { it.songCount },
                    totalDuration = foldersInSub.sumOf { it.totalDuration },
                    albumArtUri = foldersInSub.firstOrNull { it.albumArtUri != null }?.albumArtUri
                ))
            }

            // Add songs directly in this path
            val songsInPath = songs.filter { 
                // We need to derive the folder path from song URI or have it in model
                // Assuming leaf folder path in Repository matches
                val songFolder = folders.find { f -> f.songIds.contains(it.id) }
                songFolder?.path == currentPath
            }
            items.addAll(songsInPath.map { FolderBrowserItem.SongItem(it) })
        }

        // Apply basic sorting (Folders first, then Songs)
        items.sortedWith(compareBy({ it is FolderBrowserItem.SongItem }, { 
            when (it) {
                is FolderBrowserItem.SubFolder -> it.name.lowercase()
                is FolderBrowserItem.SongItem -> it.song.title.lowercase()
            }
        }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = combine(allFolders, sortMode) { folders, sort ->
        when (sort) {
            FolderSortMode.NAME_ASC -> folders.sortedBy { it.name.lowercase() }
            FolderSortMode.NAME_DESC -> folders.sortedByDescending { it.name.lowercase() }
            FolderSortMode.TRACK_COUNT -> folders.sortedByDescending { it.songCount }
            FolderSortMode.RECENTLY_MODIFIED -> folders.sortedByDescending { it.totalDuration }
            FolderSortMode.DURATION -> folders.sortedByDescending { it.totalDuration }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFolderForPlaylist = MutableStateFlow<Long?>(null)
    val selectedFolderForPlaylist: StateFlow<Long?> = _selectedFolderForPlaylist.asStateFlow()

    private val _deleteResult = MutableStateFlow<DeleteHelper.DeleteResult?>(null)
    val deleteResult: StateFlow<DeleteHelper.DeleteResult?> = _deleteResult.asStateFlow()

    fun navigateToFolder(path: String) {
        _currentPath.value = path
    }

    fun navigateUp() {
        val path = _currentPath.value ?: return
        val parentPath = path.substringBeforeLast('/').ifEmpty { null }

        // Check if current path is a root-level folder (e.g., /storage/emulated/0/Music)
        // by checking if parent is the Android storage base
        val strippedParent = parentPath?.stripAndroidStoragePrefix()
        if (strippedParent.isNullOrEmpty() || strippedParent == "/storage/emulated/0") {
            _currentPath.value = null // Go back to root folder list
        } else {
            _currentPath.value = parentPath
        }
    }

    fun setFolderViewMode(mode: FolderViewMode) {
        _folderViewMode.value = mode
        if (mode == FolderViewMode.HIDDEN) {
            _currentPath.value = null // Reset browsing when viewing hidden
        }
    }

    fun unhideFolder(path: String) {
        viewModelScope.launch {
            preferencesManager.removeExcludedFolderPath(path)
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

    fun playFolder(path: String) {
        viewModelScope.launch {
            val folderSongs = allFolders.value
                .filter { it.path.startsWith(path) }
                .flatMap { folder ->
                    allSongs.value.filter { folder.songIds.contains(it.id) }
                }
            
            if (folderSongs.isNotEmpty()) {
                playbackRepository.playQueue(folderSongs, 0)
            }
        }
    }

    fun playSong(song: Song, contextSongs: List<Song>) {
        viewModelScope.launch {
            playbackRepository.playQueue(contextSongs, contextSongs.indexOf(song).coerceAtLeast(0))
        }
    }

    fun playNext(path: String) {
        viewModelScope.launch {
            val folderSongs = allFolders.value
                .filter { it.path.startsWith(path) }
                .flatMap { folder ->
                    allSongs.value.filter { folder.songIds.contains(it.id) }
                }
            if (folderSongs.isNotEmpty()) {
                playbackRepository.playNext(folderSongs)
            }
        }
    }

    fun addToQueue(path: String) {
        viewModelScope.launch {
            val folderSongs = allFolders.value
                .filter { it.path.startsWith(path) }
                .flatMap { folder ->
                    allSongs.value.filter { folder.songIds.contains(it.id) }
                }
            if (folderSongs.isNotEmpty()) {
                playbackRepository.addToQueue(folderSongs)
            }
        }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch {
            preferencesManager.addExcludedFolderPath(path)
        }
    }

    fun setSortMode(mode: FolderSortMode) {
        viewModelScope.launch {
            preferencesManager.setFolderSortMode(mode)
        }
    }

    fun toggleLike(songId: Long, currentLiked: Boolean) {
        viewModelScope.launch {
            songRepository.updateLikeStatus(songId, !currentLiked)
        }
    }
}
