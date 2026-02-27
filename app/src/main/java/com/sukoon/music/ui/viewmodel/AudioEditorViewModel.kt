package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.SongAudioSettings
import com.sukoon.music.domain.repository.AudioEditorRepository
import com.sukoon.music.util.DevLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AudioEditor screen.
 *
 * Responsibilities:
 * - Load per-song audio settings from database
 * - Provide live preview via AudioEffectManager
 * - Save/delete per-song settings
 * - Compute waveform data for trim visualization
 */
@HiltViewModel
class AudioEditorViewModel @Inject constructor(
    private val audioEditorRepository: AudioEditorRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val TAG = "AudioEditorViewModel"

    // Extracted from nav args
    val songId: Long = savedStateHandle["songId"] ?: -1L

    // Current audio settings state
    private val _settings = MutableStateFlow(SongAudioSettings(songId = songId))
    val settings: StateFlow<SongAudioSettings> = _settings.asStateFlow()

    // Waveform data for visualization (0.0-1.0 amplitude values)
    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData: StateFlow<List<Float>> = _waveformData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load existing settings from database.
     */
    private fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loaded = audioEditorRepository.getSettings(songId)
                _settings.value = loaded ?: SongAudioSettings(songId = songId)
                _isLoading.value = false
                DevLogger.d(TAG, "Loaded audio settings for song $songId")
            } catch (e: Exception) {
                DevLogger.e(TAG, "Failed to load audio settings", e)
                _isLoading.value = false
            }
        }
    }

    /**
     * Update settings with live preview.
     * Called whenever user adjusts a slider or toggle.
     *
     * @param updated Updated audio settings
     */
    fun updateSettings(updated: SongAudioSettings) {
        _settings.value = updated
        // Live preview is handled by the service observing via room listeners or manual trigger
    }

    /**
     * Save settings to database and exit editor.
     */
    fun saveAndExit(onExit: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val settingsToSave = _settings.value.copy(
                    updatedAt = System.currentTimeMillis()
                )
                audioEditorRepository.saveSettings(settingsToSave)
                DevLogger.d(TAG, "Saved audio settings for song $songId")
                onExit()
            } catch (e: Exception) {
                DevLogger.e(TAG, "Failed to save audio settings", e)
            }
        }
    }

    /**
     * Reset all settings to defaults and delete from database.
     */
    fun resetSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                audioEditorRepository.deleteSettings(songId)
                _settings.value = SongAudioSettings(songId = songId)
                DevLogger.d(TAG, "Reset audio settings for song $songId")
            } catch (e: Exception) {
                DevLogger.e(TAG, "Failed to reset audio settings", e)
            }
        }
    }

    /**
     * Compute waveform data asynchronously.
     * In a real implementation, this would use MediaExtractor/MediaCodec to sample audio.
     * For now, we'll provide a placeholder implementation.
     *
     * @param songUri The URI of the song file
     */
    fun computeWaveform(songUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // TODO: Implement actual waveform extraction using MediaExtractor + MediaCodec
                // For now, generate a simple placeholder waveform
                val waveform = generatePlaceholderWaveform()
                _waveformData.value = waveform
                DevLogger.d(TAG, "Generated waveform with ${waveform.size} samples")
            } catch (e: Exception) {
                DevLogger.e(TAG, "Failed to compute waveform", e)
            }
        }
    }

    /**
     * Generate a placeholder waveform for UI preview.
     * In production, this would be replaced with actual audio sampling.
     */
    private fun generatePlaceholderWaveform(): List<Float> {
        // Generate 200 samples of a simple sine wave pattern
        return (0..199).map { i ->
            val angle = (i / 200f) * Math.PI * 4
            ((Math.sin(angle) + 1) / 2).toFloat()
        }
    }
}
