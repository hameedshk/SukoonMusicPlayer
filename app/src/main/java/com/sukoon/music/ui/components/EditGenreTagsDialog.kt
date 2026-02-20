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
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.edit_genre_dialog_title)) },
        text = {
            Column {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.edit_genre_dialog_message),
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
                    label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.edit_genre_dialog_genre_name_label)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.edit_genre_dialog_genre_name_error)) }
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
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
            }
        }
    )
}

