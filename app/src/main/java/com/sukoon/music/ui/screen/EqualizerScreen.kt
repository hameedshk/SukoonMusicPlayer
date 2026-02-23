package com.sukoon.music.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.R
import com.sukoon.music.domain.model.AppTheme
import com.sukoon.music.domain.model.EqualizerPreset
import com.sukoon.music.ui.theme.SukoonMusicPlayerTheme
import com.sukoon.music.ui.theme.accent
import com.sukoon.music.ui.util.hasUsableAlbumArt
import com.sukoon.music.ui.util.rememberAlbumPalette
import com.sukoon.music.ui.util.resolveNowPlayingAccentColors
import com.sukoon.music.ui.viewmodel.EqualizerViewModel
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Equalizer Screen - Premium audio effects control with dynamic color theming.
 *
 * Features:
 * - 5-band equalizer with animated frequency curve visualization
 * - Vertical band sliders with dB labels
 * - Dynamic album-art accent colors
 * - Preset selection with long-press to delete
 * - Bass boost and virtualizer controls
 * - Real-time haptic feedback
 * - Double-tap to reset individual bands
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
    val deleteConfirmPresetId by viewModel.deleteConfirmPresetId.collectAsStateWithLifecycle()
    val albumArtUri by viewModel.currentAlbumArtUri.collectAsStateWithLifecycle()

    // Dynamic color wiring
    val fallbackAccent = accent().primary
    val palette = rememberAlbumPalette(albumArtUri)
    val hasAlbumArt = remember(albumArtUri) { hasUsableAlbumArt(albumArtUri) }
    val resolvedColors = remember(palette, fallbackAccent, hasAlbumArt) {
        resolveNowPlayingAccentColors(palette, hasAlbumArt, fallbackAccent)
    }
    val accentColor by animateColorAsState(resolvedColors.controlsColor, tween(220), "eq_accent")

    // Animated band levels for synchronized slider and curve animation
    val animatedBandLevels = settings.bandLevels.mapIndexed { i, level ->
        val state = animateFloatAsState(
            targetValue = level.toFloat(),
            animationSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "eq_band_$i"
        )
        state.value
    }

    val bandLabels = listOf(
        androidx.compose.ui.res.stringResource(R.string.equalizer_band_60hz),
        androidx.compose.ui.res.stringResource(R.string.equalizer_band_230hz),
        androidx.compose.ui.res.stringResource(R.string.equalizer_band_910hz),
        androidx.compose.ui.res.stringResource(R.string.equalizer_band_3_6khz),
        androidx.compose.ui.res.stringResource(R.string.equalizer_band_14khz)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(R.string.label_equalizer)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            androidx.compose.ui.res.stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    // Reset button
                    TextButton(
                        onClick = { viewModel.resetToFlat() },
                        enabled = settings.isEnabled,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Reset")
                    }
                    // Save preset button
                    IconButton(
                        onClick = { viewModel.showSavePresetDialog() },
                        enabled = settings.isEnabled
                    ) {
                        Icon(
                            Icons.Default.Save,
                            androidx.compose.ui.res.stringResource(R.string.equalizer_cd_save_preset)
                        )
                    }
                    // Master toggle switch
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
            // EQ Curve Visualizer
            EqCurveVisualizer(
                bandLevels = animatedBandLevels,
                accentColor = accentColor,
                enabled = settings.isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Preset Chips
            EqPresetChipRow(
                presets = presets,
                selectedPresetId = settings.currentPresetId,
                accentColor = accentColor,
                enabled = settings.isEnabled,
                onPresetClick = { viewModel.applyPreset(it) },
                onPresetLongPress = { viewModel.showDeleteConfirmation(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5-Band EQ Sliders with dB Scale
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // dB scale column
                DbScaleColumn(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                )

                // 5 band sliders
                settings.bandLevels.forEachIndexed { index, level ->
                    VerticalEqBandSlider(
                        label = bandLabels[index],
                        animatedValue = animatedBandLevels[index],
                        rawValue = level,
                        enabled = settings.isEnabled,
                        accentColor = accentColor,
                        onValueChange = { viewModel.updateBandLevel(index, it) },
                        onReset = { viewModel.updateBandLevel(index, 0) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Effects Card (Bass Boost + Virtualizer)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EffectSliderRow(
                        label = androidx.compose.ui.res.stringResource(R.string.equalizer_section_bass_boost),
                        value = settings.bassBoost,
                        accentColor = accentColor,
                        enabled = settings.isEnabled,
                        onValueChange = { viewModel.updateBassBoost(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    EffectSliderRow(
                        label = androidx.compose.ui.res.stringResource(R.string.equalizer_section_virtualizer),
                        value = settings.virtualizerStrength,
                        accentColor = accentColor,
                        enabled = settings.isEnabled,
                        onValueChange = { viewModel.updateVirtualizer(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save Preset Dialog
        if (showSaveDialog) {
            SavePresetDialog(
                onDismiss = { viewModel.hideSavePresetDialog() },
                onSave = { name -> viewModel.saveAsPreset(name) }
            )
        }

        // Delete Preset Dialog
        if (deleteConfirmPresetId != null) {
            DeletePresetDialog(
                onConfirm = { viewModel.confirmDeletePreset() },
                onDismiss = { viewModel.dismissDeleteConfirmation() }
            )
        }
    }
}

/**
 * EQ Curve Visualizer - Animated frequency response curve with grid and glow.
 */
@Composable
fun EqCurveVisualizer(
    bandLevels: List<Float>,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Band frequencies in Hz
        val frequencies = listOf(60f, 230f, 910f, 3600f, 14000f)

        // Color selection based on enabled state
        val curveColor = if (enabled) accentColor else Color.White.copy(0.25f)

        // Draw grid lines (dB levels)
        val dbLevels = listOf(1500f, 750f, 0f, -750f, -1500f)
        dbLevels.forEach { db ->
            val y = centerY - (db / 1500f) * (height / 2f)
            val strokeWidth = if (db == 0f) 1.5f else 0.5f
            val alpha = if (db == 0f) 0.2f else 0.1f
            drawLine(
                color = Color.White.copy(alpha),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokeWidth
            )
        }

        // Calculate band points
        val bandPoints = bandLevels.mapIndexed { index, level ->
            val freq = frequencies[index]
            val x = xForFreq(freq, width)
            val y = centerY - (level / 1500f) * (height / 2f)
            Offset(x, y)
        }

        // Draw curve with Catmull-Rom smoothing
        if (bandPoints.size >= 2) {
            val path = Path()

            // Extend points for smooth curve at edges
            val extendedPoints = listOf(
                Offset(bandPoints.first().x, centerY)
            ) + bandPoints + listOf(
                Offset(bandPoints.last().x, centerY)
            )

            for (i in 1 until extendedPoints.size - 2) {
                val p0 = extendedPoints[i - 1]
                val p1 = extendedPoints[i]
                val p2 = extendedPoints[i + 1]
                val p3 = extendedPoints[i + 2]

                val cp1 = Offset(
                    p1.x + (p2.x - p0.x) / 6f,
                    p1.y + (p2.y - p0.y) / 6f
                )
                val cp2 = Offset(
                    p2.x - (p3.x - p1.x) / 6f,
                    p2.y - (p3.y - p1.y) / 6f
                )

                if (i == 1) {
                    path.moveTo(p1.x, p1.y)
                }
                path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
            }

            // Draw sharp curve on top
            drawPath(path, color = curveColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        // Draw band points as circles
        bandPoints.forEach { point ->
            drawCircle(
                color = curveColor,
                radius = with(density) { 6.dp.toPx() },
                center = point
            )
        }
    }
}

/**
 * Convert frequency (Hz) to X position (log scale).
 */
private fun xForFreq(freq: Float, width: Float): Float {
    val logFreq = log10(freq / 20f)
    val logMax = log10(20000f / 20f)
    return (logFreq / logMax) * width
}

/**
 * Vertical EQ Band Slider with dB label and double-tap reset.
 */
@Composable
fun VerticalEqBandSlider(
    label: String,
    animatedValue: Float,
    rawValue: Int,
    enabled: Boolean,
    accentColor: Color,
    onValueChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val trackHeightDp = 4.dp
    val thumbRadiusDp = 10.dp
    val trackColor = Color.White.copy(0.15f)
    val zeroLineColor = Color.White.copy(0.30f)
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // dB label at top
        Text(
            text = "${rawValue / 100} dB",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )

        // Canvas slider track
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .weight(1f)
                .width(52.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(onDoubleTap = { onReset() })
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val totalHeight = size.height.toFloat()
                            val deltaDb = (-dragAmount / totalHeight) * 3000f
                            val newValue = (rawValue + deltaDb).roundToInt().coerceIn(-1500, 1500)

                            // Haptic on zero-crossing
                            if (rawValue.sign != newValue.sign && rawValue != 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onValueChange(newValue)
                        }
                    )
                }
        ) {
            val trackWidth = with(density) { trackHeightDp.toPx() }
            val thumbRadius = with(density) { thumbRadiusDp.toPx() }
            val centerX = size.width / 2f
            val height = size.height
            val centerY = height / 2f

            // Track as a line (simple approach)
            drawLine(
                color = trackColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, height),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round
            )

            // Zero line
            drawLine(
                color = zeroLineColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = with(density) { 1.dp.toPx() }
            )

            // Animated thumb position
            val thumbY = centerY - (animatedValue / 1500f) * (height / 2f)

            // Active fill (above or below zero)
            if (animatedValue > 0) {
                drawLine(
                    color = accentColor,
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
            } else if (animatedValue < 0) {
                drawLine(
                    color = accentColor.copy(0.5f),
                    start = Offset(centerX, centerY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
            }

            // Thumb
            drawCircle(
                color = accentColor,
                radius = thumbRadius,
                center = Offset(centerX, thumbY)
            )
        }

        // Frequency label at bottom
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 10.sp
        )
    }
}

/**
 * dB Scale Column - left side label column for EQ sliders.
 */
@Composable
fun DbScaleColumn(
    modifier: Modifier = Modifier
) {
    val dbLabels = listOf("+15", "+10", "+5", "0", "-5", "-10", "-15")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        dbLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * EQ Preset Chip Row with long-press to delete.
 */
@Composable
fun EqPresetChipRow(
    presets: List<EqualizerPreset>,
    selectedPresetId: Long,
    accentColor: Color,
    enabled: Boolean,
    onPresetClick: (EqualizerPreset) -> Unit,
    onPresetLongPress: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        // Custom chip when currentPresetId is -999
        if (selectedPresetId == -999L && presets.none { it.id == -999L }) {
            item {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = {
                        Text(androidx.compose.ui.res.stringResource(R.string.equalizer_preset_custom))
                    },
                    enabled = enabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Preset chips
        items(presets) { preset ->
            val isSelected = preset.id == selectedPresetId
            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onPresetClick(preset) },
                label = { Text(preset.name) },
                enabled = enabled,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { if (preset.id > 0L) onPresetLongPress(preset.id) }
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.Black
                )
            )
        }
    }
}

/**
 * Effect Slider Row - horizontal slider for bass boost or virtualizer.
 */
@Composable
fun EffectSliderRow(
    label: String,
    value: Int,
    accentColor: Color,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${value / 10}%",
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..1000f,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
    }
}

/**
 * Save Preset Dialog - unchanged from original.
 */
@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(R.string.equalizer_save_custom_preset_title)) },
        text = {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text(androidx.compose.ui.res.stringResource(R.string.label_preset_name)) },
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
                Text(androidx.compose.ui.res.stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * Delete Preset Dialog - confirmation for deleting custom presets.
 */
@Composable
private fun DeletePresetDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(R.string.equalizer_delete_preset_title)) },
        text = { Text(androidx.compose.ui.res.stringResource(R.string.equalizer_delete_preset_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(androidx.compose.ui.res.stringResource(R.string.common_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.common_cancel))
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
