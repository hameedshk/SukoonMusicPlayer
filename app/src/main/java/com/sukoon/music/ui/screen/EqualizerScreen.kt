package com.sukoon.music.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.domain.model.EqualizerPreset
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.viewmodel.EqualizerViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.sukoon.music.ui.theme.*

/**
 * Equalizer Screen - Audio effects control.
 *
 * Features:
 * - 5-band equalizer with sliders
 * - Preset selection dropdown
 * - Bass boost control
 * - Virtualizer control
 * - Save custom presets
 * - Master enable/disable switch
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBackClick: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val settings by viewModel.equalizerSettings.collectAsStateWithLifecycle()
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val showSaveDialog by viewModel.showSavePresetDialog.collectAsStateWithLifecycle()

    var showPresetMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_equalizer)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.common_back)
                        )
                    }
                },
                actions = {
                    // Save preset button
                    IconButton(
                        onClick = { viewModel.showSavePresetDialog() },
                        enabled = settings.isEnabled
                    ) {
                        Icon(
                            Icons.Default.Save,
                            androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_cd_save_preset)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Master Enable/Disable Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_master_title),
                            style = MaterialTheme.typography.cardTitle
                        )
                        Text(
                            text = if (settings.isEnabled) {
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_status_active)
                            } else {
                                androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_status_disabled)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset Selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_section_preset),
                        style = MaterialTheme.typography.sectionHeader
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { showPresetMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = settings.isEnabled
                        ) {
                            val currentPreset = presets.find { it.id == settings.currentPresetId }
                            Text(
                                currentPreset?.name
                                    ?: androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_preset_custom)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }

                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false }
                        ) {
                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(preset.name)
                                            if (preset.id == settings.currentPresetId) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.applyPreset(preset)
                                        showPresetMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5-Band Equalizer
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_section_five_band),
                        style = MaterialTheme.typography.sectionHeader
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val bandLabels = listOf(
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_band_60hz),
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_band_230hz),
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_band_910hz),
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_band_3_6khz),
                        androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_band_14khz)
                    )
                    settings.bandLevels.forEachIndexed { index, level ->
                        EqualizerBandSlider(
                            label = bandLabels.getOrElse(index) { "${index + 1}" },
                            value = level,
                            enabled = settings.isEnabled,
                            onValueChange = { newValue ->
                                viewModel.updateBandLevel(index, newValue.toInt())
                            }
                        )
                        if (index < 4) Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bass Boost
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_section_bass_boost),
                            style = MaterialTheme.typography.sectionHeader
                        )
                        Text(
                            text = "${settings.bassBoost / 10}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.bassBoost.toFloat(),
                        onValueChange = { viewModel.updateBassBoost(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = settings.isEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Virtualizer
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_section_virtualizer),
                            style = MaterialTheme.typography.sectionHeader
                        )
                        Text(
                            text = "${settings.virtualizerStrength / 10}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.virtualizerStrength.toFloat(),
                        onValueChange = { viewModel.updateVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = settings.isEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset Button
            OutlinedButton(
                onClick = { viewModel.resetToFlat() },
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.isEnabled
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_reset_to_flat))
            }
        }

        // Save Preset Dialog
        if (showSaveDialog) {
            SavePresetDialog(
                onDismiss = { viewModel.hideSavePresetDialog() },
                onSave = { name -> viewModel.saveAsPreset(name) }
            )
        }
    }
}

@Composable
private fun EqualizerBandSlider(
    label: String,
    value: Int,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = androidx.compose.ui.res.stringResource(
                    com.sukoon.music.R.string.equalizer_band_db_value,
                    value / 100
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = -1500f..1500f,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.equalizer_save_custom_preset_title)) },
        text = {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text(androidx.compose.ui.res.stringResource(com.sukoon.music.R.string.label_preset_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (presetName.isNotBlank()) {
                        onSave(presetName)
                    }
                },
                enabled = presetName.isNotBlank()
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

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun EqualizerScreenPreview() {
    SukoonMusicPlayerTheme(theme = AppTheme.DARK) {
        EqualizerScreen(onBackClick = {})
    }
}

