package com.sukoon.music.data.service

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide tracker for recent Android Auto interactions.
 * Used by playback service to interpret focus/noisy transitions during car handoff.
 */
object AndroidAutoConnectionTracker {
    private val lastInteractionElapsedMs = AtomicLong(0L)

    fun markInteraction() {
        lastInteractionElapsedMs.set(SystemClock.elapsedRealtime())
    }

    fun isRecent(windowMs: Long): Boolean {
        val last = lastInteractionElapsedMs.get()
        if (last <= 0L) return false
        return SystemClock.elapsedRealtime() - last <= windowMs
    }
}
