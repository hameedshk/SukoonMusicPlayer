package com.sukoon.music.data.ads

/**
 * Represents a decision by the AdMob decision agent about whether to show an ad.
 *
 * @param shouldShow Whether the ad should be shown
 * @param format The ad format (BANNER, NATIVE, INTERSTITIAL)
 * @param reason Human-readable reason for the decision (for debugging/logging)
 * @param recheckAfterMs Optional delay before retrying (in milliseconds), if suppressed
 */
data class AdDecision(
    val shouldShow: Boolean,
    val format: AdFormat,
    val reason: String,
    val recheckAfterMs: Long? = null
)

/**
 * Ad format types supported by the decision agent.
 */
enum class AdFormat {
    BANNER,
    NATIVE,
    INTERSTITIAL
}
