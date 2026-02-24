package com.sukoon.music.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.sukoon.music.BuildConfig
import com.sukoon.music.util.DevLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for AdMob initialization and configuration.
 *
 * CRITICAL RULES:
 * - Only Banner and Native Advanced ads allowed
 * - NEVER use Interstitial or Rewarded ads (they pause music)
 * - Ads must not interfere with playback experience
 */
@Singleton
class AdMobManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private val USE_TEST_ADS = BuildConfig.USE_TEST_ADS
    }

    /**
     * Initialize AdMob SDK without consent flow (legacy, use initializeWithConsent() for Play Store compliance).
     * Call this once at app startup (in Application class or MainActivity).
     * @deprecated Use initializeWithConsent(activity) instead for GDPR/CCPA compliance
     */
    @Deprecated("Use initializeWithConsent(activity) instead for Play Store compliance")
    fun initialize() {
        // Production safety check
        if (USE_TEST_ADS && !BuildConfig.DEBUG) {
            DevLogger.e(
                "AdMobManager",
                "⚠️ CRITICAL: Test ads enabled in RELEASE build! Change USE_TEST_ADS to false in build.gradle"
            )
        }
        initializeAdMob()
    }

    /**
     * Initialize AdMob with UMP GDPR/CCPA consent flow (preferred for Play Store compliance).
     * Call this once at app startup from MainActivity.
     * Automatically handles consent form display and only initializes ads after consent is handled.
     *
     * TODO: Implement full UMP consent flow once UMP SDK is properly integrated.
     * For now, this initializes AdMob immediately. Update with proper consent handling when UMP is configured.
     */
    fun initializeWithConsent(activity: Activity) {
        DevLogger.d("AdMobManager", "Initializing AdMob with consent awareness")

        // TODO: Add proper UMP consent flow here:
        // 1. Create ConsentRequestParameters
        // 2. Request consent info update
        // 3. Load and show consent form if needed
        // 4. Only initialize MobileAds after consent is handled

        // For now, initialize ads immediately
        // This will be updated with proper UMP integration
        initializeAdMob()
    }

    /**
     * Internal helper to initialize MobileAds after consent is handled.
     */
    private fun initializeAdMob() {
        MobileAds.initialize(context) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                DevLogger.d(
                    "AdMobManager",
                    "Adapter: $adapterClass, Status: ${status?.initializationState}, Description: ${status?.description}"
                )
            }
        }

        // Configure test devices if using test ads
        if (USE_TEST_ADS) {
            val testDeviceIds = listOf(
                "33BE2250B43518CCDA7DE426D04EE231" // Add your test device IDs here
            )

            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()

            MobileAds.setRequestConfiguration(configuration)
        }
    }

    /**
     * Get Banner Ad Unit ID (test or production based on flag).
     */
    fun getBannerAdId(): String {
        return BuildConfig.ADMOB_BANNER_AD_UNIT_ID
    }

    /**
     * Get Native Ad Unit ID (test or production based on flag).
     */
    fun getNativeAdId(): String {
        return BuildConfig.ADMOB_NATIVE_AD_UNIT_ID
    }

    // NOTE: Interstitial, Rewarded, and App-Open ads are prohibited for music player apps
    // by Google Play Store policy (they interrupt playback). Only Banner and Native ads are allowed.

    /**
     * Check if test ads are enabled.
     */
    fun isUsingTestAds(): Boolean = USE_TEST_ADS
}
