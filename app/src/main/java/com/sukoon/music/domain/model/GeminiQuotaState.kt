package com.sukoon.music.domain.model

/**
 * Runtime status of Gemini API availability for metadata correction.
 */
enum class GeminiQuotaState {
    AVAILABLE,
    LIMITED,
    NOT_CONFIGURED
}
