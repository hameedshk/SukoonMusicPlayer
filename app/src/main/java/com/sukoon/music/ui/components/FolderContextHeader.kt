package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * View modes for the folders screen.
 */
enum class FolderViewMode {
    DIRECTORIES,
    HIDDEN
}

/**
 * Header component for toggling between "Directories" and "Hidden folders" views.
 * Uses Material 3 SegmentedButton for a clean toggle UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContextHeader(
    selectedMode: FolderViewMode,
    onModeChanged: (FolderViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = selectedMode == FolderViewMode.DIRECTORIES,
                onClick = { onModeChanged(FolderViewMode.DIRECTORIES) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Directories") }
            )
            SegmentedButton(
                selected = selectedMode == FolderViewMode.HIDDEN,
                onClick = { onModeChanged(FolderViewMode.HIDDEN) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Hidden folders") }
            )
        }
    }
}
