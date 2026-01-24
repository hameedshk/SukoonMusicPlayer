package com.sukoon.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.*

/**
 * Dialog for editing genre tags (renaming genre).
 */
@Composable
fun EditGenreTagsDialog(
    currentGenreName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var genreName by remember { mutableStateOf(currentGenreName) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Genre") },
        text = {
            Column {
                Text(
                    text = "Rename genre (updates all songs with this genre)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = genreName,
                    onValueChange = {
                        genreName = it
                        showError = false
                    },
                    label = { Text("Genre name") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Genre name cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (genreName.isBlank()) {
                        showError = true
                    } else {
                        onSave(genreName.trim())
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
