package com.sukoon.music.data.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
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
        /**
         * Test Ad Unit IDs from Google AdMob.
         * Replace with real IDs for production release.
         */
        const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"

        /**
         * Production Ad Unit IDs (replace with your actual IDs).
         */
        const val PROD_BANNER_AD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
        const val PROD_NATIVE_AD_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"

        /**
         * Flag to switch between test and production ads.
         * Set to false for production builds.
         */
        private const val USE_TEST_ADS = true // TODO: Set to false for production
    }

    /**
     * Initialize AdMob SDK.
     * Call this once at app startup (in Application class or MainActivity).
     */
    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            // Log initialization status
            val statusMap = initializationStatus.adapterStatusMap
            for (adapterClass in statusMap.keys) {
                val status = statusMap[adapterClass]
                android.util.Log.d(
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
        return if (USE_TEST_ADS) TEST_BANNER_AD_ID else PROD_BANNER_AD_ID
    }

    /**
     * Get Native Ad Unit ID (test or production based on flag).
     */
    fun getNativeAdId(): String {
        return if (USE_TEST_ADS) TEST_NATIVE_AD_ID else PROD_NATIVE_AD_ID
    }

    /**
     * Check if test ads are enabled.
     */
    fun isUsingTestAds(): Boolean = USE_TEST_ADS
}
