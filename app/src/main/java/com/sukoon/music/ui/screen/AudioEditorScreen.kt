package com.sukoon.music.ui.screen

import android.media.audiofx.PresetReverb
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukoon.music.R
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.model.SongAudioSettings
import com.sukoon.music.ui.components.AlbumArtWithFallback
import com.sukoon.music.ui.viewmodel.AudioEditorViewModel
import com.sukoon.music.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEditorScreen(
    songId: Long,
    onBack: () -> Unit,
    viewModel: AudioEditorViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val waveformData by viewModel.waveformData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val songs by homeViewModel.songs.collectAsStateWithLifecycle()
    val song = remember(songs, songId) { songs.firstOrNull { it.id == songId } }
    val accentColor = MaterialTheme.colorScheme.primary
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(song?.uri) {
        song?.uri?.let(viewModel::computeWaveform)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    androidx.compose.material3.TextButton(onClick = viewModel::resetSettings) {
                        Text(stringResource(R.string.audio_editor_reset))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveAndExit {
                            coroutineScope.launch {
                                onBack()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.audio_editor_apply))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SongHeaderCard(
                    song = song,
                    settings = settings,
                    enabled = !isLoading,
                    onEnabledChange = { enabled ->
                        viewModel.updateSettings(settings.copy(isEnabled = enabled))
                    }
                )
            }

            item {
                AudioSectionCard(title = stringResource(R.string.audio_editor_trim_section)) {
                    TrimSection(
                        song = song,
                        settings = settings,
                        waveformData = waveformData,
                        enabled = settings.isEnabled && !isLoading,
                        onSettingsChange = viewModel::updateSettings
                    )
                }
            }

            item {
                AudioSectionCard(title = stringResource(R.string.audio_editor_eq_section)) {
                    EqualizerSection(
                        settings = settings,
                        accentColor = accentColor,
                        enabled = settings.isEnabled && !isLoading,
                        onSettingsChange = viewModel::updateSettings
                    )
                }
            }

            item {
                AudioSectionCard(title = stringResource(R.string.audio_editor_effects_section)) {
                    EffectsSection(
                        settings = settings,
                        accentColor = accentColor,
                        enabled = settings.isEnabled && !isLoading,
                        onSettingsChange = viewModel::updateSettings
                    )
                }
            }

            item {
                AudioSectionCard(title = stringResource(R.string.audio_editor_playback_section)) {
                    PlaybackSection(
                        settings = settings,
                        accentColor = accentColor,
                        enabled = settings.isEnabled && !isLoading,
                        onSettingsChange = viewModel::updateSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun SongHeaderCard(
    song: Song?,
    settings: SongAudioSettings,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (song != null) {
                    AlbumArtWithFallback(
                        song = song,
                        size = 72.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.artist.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.durationFormatted().orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.audio_editor_master_toggle),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
private fun AudioSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun TrimSection(
    song: Song?,
    settings: SongAudioSettings,
    waveformData: List<Float>,
    enabled: Boolean,
    onSettingsChange: (SongAudioSettings) -> Unit
) {
    val durationMs = (song?.duration ?: 0L).coerceAtLeast(1L)
    val effectiveEnd = if (settings.trimEndMs <= 0L) durationMs else settings.trimEndMs.coerceAtMost(durationMs)
    val startMs = settings.trimStartMs.coerceIn(0L, durationMs)
    val safeEndMs = effectiveEnd.coerceAtLeast(startMs)
    var range by remember(settings.trimStartMs, settings.trimEndMs, durationMs) {
        mutableStateOf(startMs.toFloat()..safeEndMs.toFloat())
    }

    LaunchedEffect(startMs, safeEndMs, durationMs) {
        range = startMs.toFloat()..safeEndMs.toFloat()
    }

    val samples = if (waveformData.isNotEmpty()) waveformData else remember {
        List(64) { index ->
            (((index % 12) + 1) / 12f).coerceIn(0.1f, 1f)
        }
    }

    WaveformPreview(
        waveformData = samples,
        selectionStartRatio = (range.start / durationMs).coerceIn(0f, 1f),
        selectionEndRatio = (range.endInclusive / durationMs).coerceIn(0f, 1f),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
    )

    RangeSlider(
        value = range,
        onValueChange = { newRange ->
            val clampedStart = newRange.start.coerceIn(0f, durationMs.toFloat())
            val clampedEnd = newRange.endInclusive.coerceIn(clampedStart, durationMs.toFloat())
            range = clampedStart..clampedEnd

            val endValue = if (clampedEnd >= durationMs - 50f) -1L else clampedEnd.roundToInt().toLong()
            onSettingsChange(
                settings.copy(
                    trimStartMs = clampedStart.roundToInt().toLong(),
                    trimEndMs = endValue
                )
            )
        },
        valueRange = 0f..durationMs.toFloat(),
        enabled = enabled
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(R.string.audio_editor_trim_start),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(formatMs(range.start.roundToInt().toLong()), style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.audio_editor_trim_end),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(formatMs(range.endInclusive.roundToInt().toLong()), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WaveformPreview(
    waveformData: List<Float>,
    selectionStartRatio: Float,
    selectionEndRatio: Float,
    modifier: Modifier = Modifier
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        if (waveformData.isEmpty()) return@Canvas

        val barCount = waveformData.size
        val gap = 3f
        val barWidth = ((size.width - ((barCount - 1) * gap)) / barCount).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val startXSelected = size.width * selectionStartRatio
        val endXSelected = size.width * selectionEndRatio

        waveformData.forEachIndexed { index, value ->
            val x = index * (barWidth + gap)
            val amplitude = value.coerceIn(0.05f, 1f)
            val barHeight = amplitude * size.height
            val top = centerY - barHeight / 2f
            val isSelected = (x + barWidth / 2f) in startXSelected..endXSelected
            drawRoundRect(
                color = if (isSelected) activeColor else inactiveColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun EqualizerSection(
    settings: SongAudioSettings,
    accentColor: Color,
    enabled: Boolean,
    onSettingsChange: (SongAudioSettings) -> Unit
) {
    val bandSpecs = listOf(
        60 to settings.band60Hz,
        230 to settings.band230Hz,
        910 to settings.band910Hz,
        3600 to settings.band3600Hz,
        14000 to settings.band14000Hz
    )
    val animatedValues = bandSpecs.map { (_, value) ->
        animateFloatAsState(targetValue = value.toFloat(), label = "audio_editor_eq_$value").value
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DbScaleColumn(
            modifier = Modifier
                .width(24.dp)
                .fillMaxSize()
        )

        bandSpecs.forEachIndexed { index, (frequency, value) ->
            VerticalEqBandSlider(
                label = formatFrequencyLabel(frequency),
                animatedValue = animatedValues[index],
                rawValue = value,
                enabled = enabled,
                accentColor = accentColor,
                onValueChange = { newValue ->
                    onSettingsChange(
                        settings.copy(
                            eqEnabled = true,
                            band60Hz = if (index == 0) newValue else settings.band60Hz,
                            band230Hz = if (index == 1) newValue else settings.band230Hz,
                            band910Hz = if (index == 2) newValue else settings.band910Hz,
                            band3600Hz = if (index == 3) newValue else settings.band3600Hz,
                            band14000Hz = if (index == 4) newValue else settings.band14000Hz
                        )
                    )
                },
                onReset = {
                    onSettingsChange(
                        settings.copy(
                            eqEnabled = true,
                            band60Hz = if (index == 0) 0 else settings.band60Hz,
                            band230Hz = if (index == 1) 0 else settings.band230Hz,
                            band910Hz = if (index == 2) 0 else settings.band910Hz,
                            band3600Hz = if (index == 3) 0 else settings.band3600Hz,
                            band14000Hz = if (index == 4) 0 else settings.band14000Hz
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EffectsSection(
    settings: SongAudioSettings,
    accentColor: Color,
    enabled: Boolean,
    onSettingsChange: (SongAudioSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EffectSliderRow(
            label = stringResource(R.string.audio_editor_bass_boost),
            value = settings.bassBoost,
            accentColor = accentColor,
            enabled = enabled,
            onValueChange = { onSettingsChange(settings.copy(bassBoost = it)) },
            modifier = Modifier.fillMaxWidth()
        )

        EffectSliderRow(
            label = stringResource(R.string.audio_editor_stereo_width),
            value = settings.virtualizerStrength,
            accentColor = accentColor,
            enabled = enabled,
            onValueChange = { onSettingsChange(settings.copy(virtualizerStrength = it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.audio_editor_reverb),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )

        val reverbOptions = listOf(
            PresetReverb.PRESET_NONE.toShort() to stringResource(R.string.audio_editor_reverb_none),
            PresetReverb.PRESET_SMALLROOM.toShort() to stringResource(R.string.audio_editor_reverb_small_room),
            PresetReverb.PRESET_MEDIUMROOM.toShort() to stringResource(R.string.audio_editor_reverb_medium_room),
            PresetReverb.PRESET_LARGEROOM.toShort() to stringResource(R.string.audio_editor_reverb_large_room),
            PresetReverb.PRESET_LARGEHALL.toShort() to stringResource(R.string.audio_editor_reverb_hall),
            PresetReverb.PRESET_PLATE.toShort() to stringResource(R.string.audio_editor_reverb_plate)
        )

        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reverbOptions) { (value, label) ->
                FilterChip(
                    selected = settings.reverbPreset == value,
                    onClick = { onSettingsChange(settings.copy(reverbPreset = value)) },
                    enabled = enabled,
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
private fun PlaybackSection(
    settings: SongAudioSettings,
    accentColor: Color,
    enabled: Boolean,
    onSettingsChange: (SongAudioSettings) -> Unit
) {
    val selectedPitchSemitones = remember(settings.pitch) { pitchToSemitones(settings.pitch) }
    val speedOptions = remember { listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }
    val selectedSpeed = remember(settings.speed) {
        speedOptions.minByOrNull { kotlin.math.abs(it - settings.speed) } ?: 1.0f
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SelectionChipRow(
            label = stringResource(R.string.audio_editor_pitch),
            accentColor = accentColor,
            valueLabel = formatSemitoneLabel(selectedPitchSemitones),
            enabled = enabled,
            options = (-2..2).map { semitones ->
                ChipOption(
                    key = semitones,
                    label = formatSemitoneLabel(semitones),
                    selected = selectedPitchSemitones == semitones,
                    onClick = {
                        onSettingsChange(settings.copy(pitch = semitonesToPitch(semitones)))
                    }
                )
            }
        )

        SelectionChipRow(
            label = stringResource(R.string.audio_editor_speed),
            accentColor = accentColor,
            valueLabel = formatSpeedLabel(selectedSpeed),
            enabled = enabled,
            options = speedOptions.map { speed ->
                ChipOption(
                    key = speed,
                    label = formatSpeedLabel(speed),
                    selected = kotlin.math.abs(settings.speed - speed) < 0.001f,
                    onClick = { onSettingsChange(settings.copy(speed = speed)) }
                )
            }
        )
    }
}

@Composable
private fun SelectionChipRow(
    label: String,
    valueLabel: String,
    enabled: Boolean,
    accentColor: Color,
    options: List<ChipOption<*>>
) {
    Column {
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
                text = valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                FilterChip(
                    selected = option.selected,
                    onClick = option.onClick,
                    enabled = enabled,
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }
    }
}

private data class ChipOption<T>(
    val key: T,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

private fun formatFrequencyLabel(freqHz: Int): String {
    return when {
        freqHz >= 1000 && freqHz % 1000 == 0 -> "${freqHz / 1000}kHz"
        freqHz >= 1000 -> "${(freqHz / 100f) / 10f}kHz"
        else -> "${freqHz}Hz"
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun semitonesToPitch(semitones: Int): Float {
    return 2f.pow(semitones / 12f)
}

private fun pitchToSemitones(pitch: Float): Int {
    if (pitch <= 0f) return 0
    return ((12f * (ln(pitch) / ln(2f))).roundToInt()).coerceIn(-2, 2)
}

private fun formatSemitoneLabel(semitones: Int): String {
    return when {
        semitones > 0 -> "+${semitones} st"
        semitones < 0 -> "${semitones} st"
        else -> "0 st"
    }
}

private fun formatSpeedLabel(speed: Float): String {
    return "${"%.2f".format(speed)}x"
}
