package com.sukoon.music.data.metadata

import com.sukoon.music.data.remote.dto.CorrectedMetadata

/**
 * Outcome of a Gemini metadata correction attempt.
 */
sealed interface MetadataCorrectionResult {
    data class Corrected(val metadata: CorrectedMetadata) : MetadataCorrectionResult

    object Unchanged : MetadataCorrectionResult

    object Disabled : MetadataCorrectionResult

    object NotConfigured : MetadataCorrectionResult

    data class QuotaLimited(
        val httpCode: Int,
        val reason: String,
        val cooldownUntilMs: Long
    ) : MetadataCorrectionResult

    object NetworkError : MetadataCorrectionResult

    object ParseError : MetadataCorrectionResult
}
