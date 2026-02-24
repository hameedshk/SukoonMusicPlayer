package com.sukoon.music.util

import android.util.Log
import com.sukoon.music.BuildConfig

/**
 * Global logging utility for development purposes.
 * Toggle [isEnabled] to control logging across the entire app.
 */
object DevLogger {

    /**
     * Master switch for dev logging.
     * Automatically disabled in production builds (when BuildConfig.DEBUG = false).
     */
    var isEnabled: Boolean = BuildConfig.DEBUG

    /**
     * Optional prefix for all log tags (e.g., "Sukoon_")
     */
    var tagPrefix: String = "Sukoon_"

    /**
     * Log a debug message
     */
    fun d(tag: String, message: String) {
        if (isEnabled) {
            Log.d(tagPrefix + tag, message)
        }
    }

    /**
     * Log an info message
     */
    fun i(tag: String, message: String) {
        if (isEnabled) {
            Log.i(tagPrefix + tag, message)
        }
    }

    /**
     * Log a warning message
     */
    fun w(tag: String, message: String) {
        if (isEnabled) {
            Log.w(tagPrefix + tag, message)
        }
    }

    /**
     * Log an error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            if (throwable != null) {
                Log.e(tagPrefix + tag, message, throwable)
            } else {
                Log.e(tagPrefix + tag, message)
            }
        }
    }

    /**
     * Log a verbose message
     */
    fun v(tag: String, message: String) {
        if (isEnabled) {
            Log.v(tagPrefix + tag, message)
        }
    }

    /**
     * Log with automatic class name as tag
     */
    inline fun <reified T> d(message: String) {
        d(T::class.java.simpleName, message)
    }

    inline fun <reified T> i(message: String) {
        i(T::class.java.simpleName, message)
    }

    inline fun <reified T> w(message: String) {
        w(T::class.java.simpleName, message)
    }

    inline fun <reified T> e(message: String, throwable: Throwable? = null) {
        e(T::class.java.simpleName, message, throwable)
    }

    inline fun <reified T> v(message: String) {
        v(T::class.java.simpleName, message)
    }

    /**
     * Log click events with optional details
     */
    fun click(tag: String, elementName: String, handled: Boolean = true, extraInfo: String = "") {
        if (isEnabled) {
            val status = if (handled) "✓ HANDLED" else "✗ NOT HANDLED"
            val extra = if (extraInfo.isNotEmpty()) " | $extraInfo" else ""
            Log.d(tagPrefix + tag, "🖱️ CLICK: $elementName → $status$extra")
        }
    }

    /**
     * Log click events with automatic class name as tag
     */
    inline fun <reified T> click(elementName: String, handled: Boolean = true, extraInfo: String = "") {
        click(T::class.java.simpleName, elementName, handled, extraInfo)
    }
}
