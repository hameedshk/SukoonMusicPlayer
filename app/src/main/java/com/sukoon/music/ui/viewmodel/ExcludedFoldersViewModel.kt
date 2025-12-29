package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing excluded folders.
 *
 * Responsibilities:
 * - Expose the list of currently excluded folder paths.
 * - Provide functionality to remove a folder from the exclusion list.
 */
@HiltViewModel
class ExcludedFoldersViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    /**
     * List of excluded folders with their display names and full paths.
     */
    val excludedFolders: StateFlow<List<ExcludedFolderItem>> = preferencesManager.userPreferencesFlow
        .map { prefs ->
            prefs.excludedFolderPaths.map { path ->
                ExcludedFolderItem(
                    path = path,
                    name = path.substringAfterLast('/').ifEmpty { path }
                )
            }.sortedBy { it.name.lowercase() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Remove a folder path from the exclusion list.
     * This will cause the library to rescan or include songs from this folder.
     *
     * @param folderPath The full path of the folder to remove from exclusions.
     */
    fun removeExclusion(folderPath: String) {
        viewModelScope.launch {
            preferencesManager.removeExcludedFolderPath(folderPath)
        }
    }
}

/**
 * Data class representing an item in the excluded folders list.
 */
data class ExcludedFolderItem(val path: String, val name: String)
