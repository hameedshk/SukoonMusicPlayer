package com.sukoon.music.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukoon.music.domain.manager.SleepTimerState

@Composable
fun SleepTimerDialog(currentState: SleepTimerState, onDismiss: () -> Unit, onSetTimer: (minutes: Int) -> Unit, onCancelTimer: () -> Unit) {
    var customMinutes by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when { currentState is SleepTimerState.Active -> "Sleep Timer Active"; showCustomInput -> "Custom Duration"; else -> "Set Sleep Timer" }) },
        text = { Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                currentState is SleepTimerState.Active && !showCustomInput -> ActiveTimerContent(state = currentState, onCancel = onCancelTimer)
                showCustomInput -> CustomTimerInput(value = customMinutes, onValueChange = { customMinutes = it }, onSetTimer = { minutes -> if (minutes > 0) { onSetTimer(minutes); onDismiss() } }, onBack = { showCustomInput = false; customMinutes = "" })
                else -> PresetOptions(onPresetSelected = { minutes -> onSetTimer(minutes); onDismiss() }, onCustomSelected = { showCustomInput = true })
            }
        } },
        confirmButton = { if (currentState is SleepTimerState.Active && !showCustomInput) { Button(onClick = onDismiss) { Text("Done") } } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ActiveTimerContent(state: SleepTimerState.Active, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Playback will pause in:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatTimerDisplay(state.remainingMinutes), fontSize = 32.sp, color = MaterialTheme.colorScheme.primary)
        Text("Total duration: ${state.totalMinutes} minutes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel Timer") }
    }
}

@Composable
private fun PresetOptions(onPresetSelected: (minutes: Int) -> Unit, onCustomSelected: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetButton("15 minutes", 15, onPresetSelected)
        PresetButton("30 minutes", 30, onPresetSelected)
        PresetButton("1 hour", 60, onPresetSelected)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onCustomSelected, modifier = Modifier.fillMaxWidth()) { Text("Custom Duration") }
    }
}

@Composable
private fun PresetButton(label: String, minutes: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = { onSelected(minutes) }, modifier = modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun CustomTimerInput(value: String, onValueChange: (String) -> Unit, onSetTimer: (minutes: Int) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Enter duration in minutes:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextField(value = value, onValueChange = { newValue -> if (newValue.all { it.isDigit() }) { onValueChange(newValue) } }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        val minutes = value.toIntOrNull() ?: 0
        if (minutes <= 0 && value.isNotEmpty()) { Text("Please enter a number greater than 0", fontSize = 12.sp, color = MaterialTheme.colorScheme.error) }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = { val timerMinutes = value.toIntOrNull() ?: 0; if (timerMinutes > 0) { onSetTimer(timerMinutes) } }, enabled = minutes > 0, modifier = Modifier.weight(1f)) { Text("Set") }
        }
    }
}

private fun formatTimerDisplay(minutes: Int): String = when {
    minutes < 1 -> "Expiring soon"
    minutes < 60 -> "$minutes min"
    else -> {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
    }
}
