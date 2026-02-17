package com.sukoon.music.data.premium

import android.app.Activity
import com.sukoon.music.data.billing.BillingManager
import com.sukoon.music.data.billing.BillingState
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
     * Observe billing state (Loading, Success, Error, Idle).
     * Use this in purchase dialogs to show user feedback.
     */
    val billingState: Flow<BillingState> = billingManager.billingState

    /**
     * Check if user is premium (non-blocking).
     * This returns a Flow - collect it in a Composable with collectAsStateWithLifecycle.
     * For synchronous checks, collect the [isPremiumUser] Flow instead.
     */
    fun checkPremiumStatus() = isPremiumUser

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

    /**
     * Reset billing state to Idle.
     * Call when dismissing purchase dialogs.
     */
    fun resetBillingState() {
        billingManager.resetBillingState()
    }

    /**
     * Restore purchases from Google Play.
     * For users who reinstalled the app or switched devices.
     */
    suspend fun restorePurchases() {
        billingManager.queryAndRestorePremiumPurchases()
    }
}
