package com.sukoon.music.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Centralized app locale handling using AndroidX per-app locale APIs.
 */
object AppLocaleManager {

    const val LANGUAGE_SYSTEM = "system"

    private val supportedLanguageTags = setOf(
        LANGUAGE_SYSTEM,
        "en",
        "hi-IN",
        "pt-BR"
    )

    fun applyLanguage(context: Context, languageTag: String?) {
        val normalizedTag = normalizeLanguageTag(languageTag)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager != null) {
                localeManager.applicationLocales = if (normalizedTag == LANGUAGE_SYSTEM) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(normalizedTag)
                }
            }
        }

        val locales = if (normalizedTag == LANGUAGE_SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(normalizedTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun normalizeLanguageTag(languageTag: String?): String {
        if (languageTag.isNullOrBlank()) return LANGUAGE_SYSTEM
        return if (supportedLanguageTags.contains(languageTag)) {
            languageTag
        } else {
            LANGUAGE_SYSTEM
        }
    }
}
