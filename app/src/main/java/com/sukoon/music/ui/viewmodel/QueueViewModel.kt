package com.sukoon.music.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukoon.music.domain.model.PlaybackState
import com.sukoon.music.domain.model.Queue
import com.sukoon.music.domain.model.Song
import com.sukoon.music.domain.repository.PlaybackRepository
import com.sukoon.music.domain.repository.QueueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.sukoon.music.ui.theme.*

/**
 * ViewModel for Queue management screens.
 *
 * Responsibilities:
 * - Expose queue data to UI (current queue, saved queues)
 * - Handle queue operations (save, load, delete, reorder)
 * - Coordinate with playback repository for queue playback
 * - Manage queue UI state
 *
 * State Management:
 * - All data exposed as StateFlow for lifecycle-aware observation
 * - Uses SharingStarted.WhileSubscribed(5000) to conserve resources
 */
@HiltViewModel
class QueueViewModel @Inject constructor(
    private val queueRepository: QueueRepository,
    val playbackRepository: PlaybackRepository
) : ViewModel() {

    // ============================================
    // QUEUE DATA
    // ============================================

    /**
     * All saved queues ordered by modification date.
     * Updates automatically when queues are added/removed/modified.
     */
    val savedQueues: StateFlow<List<Queue>> = queueRepository.getAllQueues()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Current playback state including the active queue.
     */
    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackState()
        )

    // ============================================
    // UI STATE
    // ============================================

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    // ============================================
    // QUEUE OPERATIONS
    // ============================================

    /**
     * Save the current playback queue with a custom name.
     */
    fun saveCurrentQueue(name: String) {
        viewModelScope.launch {
            try {
                val currentQueue = playbackState.value.queue
                if (currentQueue.isNotEmpty()) {
                    queueRepository.saveCurrentPlaybackQueue(
                        name = name,
                        songs = currentQueue,
                        markAsCurrent = false // Don't mark as current, just save as a snapshot
                    )
                    _uiState.value = _uiState.value.copy(
                        message = "Queue saved as \"$name\"",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to save queue: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Load a saved queue and start playing it.
     */
    fun loadQueue(queueId: Long) {
        viewModelScope.launch {
            try {
                val songs = queueRepository.restoreQueue(queueId)
                if (songs != null && songs.isNotEmpty()) {
                    val savedQueue = savedQueues.value.find { it.id == queueId }
                    val queueName = savedQueue?.name ?: "Saved Queue"
                    playbackRepository.playQueue(songs, startIndex = 0, queueName = "Queue: $queueName")
                    queueRepository.setCurrentQueue(queueId)
                    _uiState.value = _uiState.value.copy(
                        message = "Queue loaded",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to load queue: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Delete a saved queue.
     */
    fun deleteQueue(queueId: Long) {
        viewModelScope.launch {
            try {
                queueRepository.deleteQueue(queueId)
                _uiState.value = _uiState.value.copy(
                    message = "Queue deleted",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to delete queue: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Remove a song from the current playback queue.
     */
    fun removeFromQueue(index: Int) {
        viewModelScope.launch {
            try {
                playbackRepository.removeFromQueue(index)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to remove song: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Reorder songs in the current playback queue.
     * Used for drag-and-drop functionality.
     */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                // Get current queue
                val currentQueue = playbackState.value.queue.toMutableList()
                val currentIndex = playbackState.value.currentQueueIndex

                // Perform reorder
                val item = currentQueue.removeAt(fromIndex)
                currentQueue.add(toIndex, item)

                // Calculate new current index after reorder
                val newCurrentIndex = when {
                    currentIndex == fromIndex -> toIndex
                    fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex - 1
                    fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex + 1
                    else -> currentIndex
                }

                // Replace entire queue with reordered list
                playbackRepository.playQueue(currentQueue, startIndex = newCurrentIndex, queueName = playbackState.value.currentQueueName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to reorder queue: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Jump to a specific position in the queue.
     */
    fun playQueueItem(index: Int) {
        viewModelScope.launch {
            try {
                playbackRepository.seekToQueueIndex(index)
                playbackRepository.play()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to play song: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Clear the current playback queue.
     */
    fun clearQueue() {
        viewModelScope.launch {
            try {
                playbackRepository.playQueue(emptyList(), 0)
                _uiState.value = _uiState.value.copy(
                    message = "Queue cleared",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to clear queue: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Shuffle the current queue.
     */
    fun shuffleQueue() {
        viewModelScope.launch {
            try {
                val currentSong = playbackState.value.currentSong
                val queue = playbackState.value.queue.toMutableList()

                // Remove current song and shuffle the rest
                currentSong?.let { queue.remove(it) }
                queue.shuffle()

                // Put current song at the front
                currentSong?.let { queue.add(0, it) }

                playbackRepository.playQueue(queue, startIndex = 0, queueName = playbackState.value.currentQueueName)
                _uiState.value = _uiState.value.copy(
                    message = "Queue shuffled",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Failed to shuffle queue: ${e.message}",
                    isError = true
                )
            }
        }
    }

    /**
     * Clear any UI messages.
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

/**
 * UI state for queue screens.
 */
data class QueueUiState(
    val message: String? = null,
    val isError: Boolean = false
)
