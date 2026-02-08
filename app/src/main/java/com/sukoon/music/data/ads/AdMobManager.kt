package com.sukoon.music.data.ads

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
     * Initialize AdMob SDK.
     * Call this once at app startup (in Application class or MainActivity).
     */
    fun initialize() {
        // Production safety check
        if (USE_TEST_ADS && !BuildConfig.DEBUG) {
            android.util.Log.e(
                "AdMobManager",
                "⚠️ CRITICAL: Test ads enabled in RELEASE build! Change USE_TEST_ADS to false in build.gradle"
            )
        }

        MobileAds.initialize(context) { initializationStatus ->
            // Log initialization status
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

    /**
     * Get Interstitial Ad Unit ID (test or production based on flag).
     */
    fun getInterstitialAdId(): String {
        return BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
    }

    /**
     * Get Rewarded Ad Unit ID (test or production based on flag).
     */
    fun getRewardedAdId(): String {
        return BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
    }

    /**
     * Get App Open Ad Unit ID (test or production based on flag).
     */
    fun getAppOpenAdId(): String {
        return BuildConfig.ADMOB_APP_OPEN_AD_UNIT_ID
    }

    /**
     * Check if test ads are enabled.
     */
    fun isUsingTestAds(): Boolean = USE_TEST_ADS
}
