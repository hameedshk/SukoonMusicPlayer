package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.*

/**
 * Reusable component showing item count and a sort dropdown menu.
 *
 * Features:
 * - Left badge: "$itemCount $itemLabel" (e.g., "7 folders")
 * - Right button: Sort dropdown with checkmark on active mode
 * - Material 3 styling
 */
@Composable
fun <T : Enum<T>> CategoryPillRow(
    itemCount: Int,
    itemLabel: String,
    sortOptions: List<T>,
    currentSortMode: T,
    onSortModeChanged: (T) -> Unit,
    sortModeToDisplayName: (T) -> String,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Item Count Badge
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "$itemCount $itemLabel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Right: Sort Dropdown
        Box {
            OutlinedButton(
                onClick = { showSortMenu = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = sortModeToDisplayName(currentSortMode),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                sortOptions.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(sortModeToDisplayName(mode)) },
                        onClick = {
                            onSortModeChanged(mode)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (currentSortMode == mode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
