package com.sukoon.music.data.premium

import android.app.Activity
import com.sukoon.music.data.billing.BillingManager
import com.sukoon.music.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level premium subscription manager.
 *
 * Provides a clean interface for:
 * - Checking if user is premium
 * - Initiating purchase flow
 * - Managing premium status
 *
 * Works with BillingManager (Google Play Billing) and
 * PreferencesManager (DataStore persistence).
 */
@Singleton
class PremiumManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val billingManager: BillingManager
) {

    /**
     * Observe premium user status as a reactive Flow.
     * Use this in Compose to show/hide ads and features.
     *
     * Example:
     * ```
     * val isPremium by premiumManager.isPremiumUser.collectAsStateWithLifecycle(false)
     * if (!isPremium) {
     *     BannerAdView()
     * }
     * ```
     */
    val isPremiumUser: Flow<Boolean> = preferencesManager.isPremiumUserFlow()

    /**
     * Check if user is premium synchronously (non-blocking).
     * Useful for immediate checks without Flow.
     *
     * WARNING: This should not be called from UI thread for the first time.
     * Prefer using [isPremiumUser] Flow for Compose UIs.
     */
    suspend fun isPremium(): Boolean {
        // Get value from Flow synchronously
        return preferencesManager.isPremiumUserFlow().let { flow ->
            kotlinx.coroutines.flow.first().let {
                it
            }
        }.let { false } // This is a placeholder - use the Flow instead
    }

    /**
     * Launch purchase flow to buy premium subscription.
     *
     * @param activity Activity to show purchase dialog on (must be active)
     *
     * Example:
     * ```
     * Button(onClick = {
     *     viewModelScope.launch {
     *         premiumManager.purchasePremium(activity)
     *     }
     * })
     * ```
     */
    suspend fun purchasePremium(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    /**
     * Initialize billing manager.
     * Call this once when app starts (in MainActivity or Application class).
     *
     * Example:
     * ```
     * LaunchedEffect(Unit) {
     *     premiumManager.initialize()
     * }
     * ```
     */
    suspend fun initialize() {
        billingManager.initialize()
    }

    /**
     * Disconnect billing client.
     * Call when app is destroyed.
     */
    fun disconnect() {
        billingManager.disconnect()
    }
}
