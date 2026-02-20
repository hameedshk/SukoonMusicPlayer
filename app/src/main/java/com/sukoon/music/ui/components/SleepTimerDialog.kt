package com.sukoon.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sukoon.music.ui.theme.*

/**
 * Dialog for setting a sleep timer.
 */
@Composable
fun SleepTimerDialog(
    onTimerSelected: (Int) -> Unit,
    onEndOfTrackSelected: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomMinutesDialog by remember { mutableStateOf(false) }
    var customMinutesInput by remember { mutableStateOf("") }
    var customMinutesError by remember { mutableStateOf<String?>(null) }
    val customMinutesRangeError = androidx.compose.ui.res.stringResource(
        com.sukoon.music.R.string.sleep_timer_custom_minutes_error,
        1,
        360
    )

    val options = listOf(
        TimerOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.sleep_timer_option_off), 0, Icons.Default.TimerOff),
        TimerOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.sleep_timer_option_5_minutes), 5, Icons.Default.Timer),
        TimerOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.sleep_timer_option_10_minutes), 10, Icons.Default.Timer),
        TimerOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.sleep_timer_option_end_of_track), END_OF_TRACK, Icons.Default.Timer),
        TimerOption(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.sleep_timer_option_custom), null, Icons.Default.Edit)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_sleep_timer_title))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(options) { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (option.minutes == null) {
                                    showCustomMinutesDialog = true
                                } else if (option.minutes == END_OF_TRACK) {
                                    onEndOfTrackSelected()
                                } else {
                                    onTimerSelected(option.minutes)
                                }
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (option.minutes == 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_cancel))
            }
        },
        modifier = modifier
    )

    if (showCustomMinutesDialog) {
        AlertDialog(
            onDismissRequest = {
                showCustomMinutesDialog = false
                customMinutesError = null
            },
            title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.dialog_custom_timer_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customMinutesInput,
                        onValueChange = { value ->
                            customMinutesInput = value.filter { it.isDigit() }
                            customMinutesError = null
                        },
                        label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_minutes)) },
                        singleLine = true,
                        isError = customMinutesError != null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    customMinutesError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = customMinutesInput.toIntOrNull()
                    if (minutes == null || minutes <= 0 || minutes > 360) {
                        customMinutesError = customMinutesRangeError
                        return@TextButton
                    }
                    onTimerSelected(minutes)
                }) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_set))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCustomMinutesDialog = false
                    customMinutesError = null
                }) {
                    Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back))
                }
            }
        )
    }
}

private data class TimerOption(
    val label: String,
    val minutes: Int?,
    val icon: ImageVector
)

private const val END_OF_TRACK = -1

