package com.sukoon.music.ui.search

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

/**
 * Builds speech recognizer intents with Google-first preference.
 */
object VoiceSearchIntentFactory {
    private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"

    fun create(context: Context): Intent? {
        val packageManager = context.packageManager
        val fallbackIntent = createBaseIntent()
        val googleIntent = Intent(fallbackIntent).setPackage(GOOGLE_APP_PACKAGE)

        return when {
            googleIntent.resolveActivity(packageManager) != null -> googleIntent
            fallbackIntent.resolveActivity(packageManager) != null -> fallbackIntent
            else -> null
        }
    }

    private fun createBaseIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }
}
