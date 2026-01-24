package com.sukoon.music.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import dagger.hilt.android.qualifiers.ApplicationContext
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetailsParams
import com.sukoon.music.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages Google Play in-app purchases for premium subscription.
 *
 * Responsibilities:
 * - Initialize billing client
 * - Query available products
 * - Launch purchase flow
 * - Acknowledge purchases
 * - Update premium status in DataStore
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val preferencesManager: PreferencesManager
) : PurchasesUpdatedListener {

    companion object {
        // Product ID for premium subscription (must match Play Console)
        const val PREMIUM_PRODUCT_ID = "sukoon_premium_monthly"

        // Test product IDs for testing
        const val TEST_PRODUCT_ID = "android.test.purchased"
    }

    private val appContext = context.applicationContext
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState

    private var isConnected = false
    private var cachedProductDetails: com.android.billingclient.api.ProductDetails? = null

    /**
     * Initialize billing client.
     * Must be called before any other operations.
     */
    suspend fun initialize() {
        if (isConnected) return

        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isConnected = true
                        // Query existing purchases
                        queryPremiumPurchases()
                        continuation.resume(Unit)
                    } else {
                        _billingState.value = BillingState.Error("Setup failed: ${billingResult.debugMessage}")
                        continuation.resume(Unit)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnected = false
                }
            })
        }
    }

    /**
     * Query available premium product from Play Console.
     * Returns product details needed for purchase flow.
     */
    suspend fun queryPremiumProduct(): ProductDetails? {
        if (!isConnected) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PREMIUM_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    cachedProductDetails = productDetails
                    continuation.resume(
                        ProductDetails(
                            productId = productDetails.productId,
                            title = productDetails.title,
                            description = productDetails.description,
                            price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: "$9.99"
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Launch purchase flow for premium subscription.
     * User will be shown Google Play purchase dialog.
     *
     * @param activity Activity to show purchase dialog on
     */
    suspend fun launchPurchaseFlow(activity: Activity) {
        if (!isConnected) {
            _billingState.value = BillingState.Error("Billing not initialized")
            return
        }

        _billingState.value = BillingState.Loading

        // Use cached product details or query new ones
        if (cachedProductDetails == null) {
            queryPremiumProduct()
        }

        val cachedDetails = cachedProductDetails
        if (cachedDetails == null) {
            _billingState.value = BillingState.Error("Failed to load product details")
            return
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(cachedDetails)
                        .build()
                )
            )
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingState.value = BillingState.Error("Launch failed: ${billingResult.debugMessage}")
        }
    }

    /**
     * Query existing purchases to check if user already bought premium.
     * Updates DataStore if premium purchase is found.
     */
    private fun queryPremiumPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val premiumPurchase = purchasesList.find {
                    it.products.contains(PREMIUM_PRODUCT_ID)
                }

                val isPremium = premiumPurchase != null && premiumPurchase.isAcknowledged

                if (isPremium) {
                    _billingState.value = BillingState.Success("Premium activated!")
                } else {
                    _billingState.value = BillingState.Idle
                }
            }
        }
    }

    /**
     * Called when purchase state changes.
     * Handles successful purchases and acknowledges them.
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<com.android.billingclient.api.Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PREMIUM_PRODUCT_ID) && purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
                    // Acknowledge purchase
                    acknowledgePurchase(purchase)
                }
            }
        } else {
            _billingState.value = BillingState.Error("Purchase cancelled or failed")
        }
    }

    /**
     * Acknowledge purchase to inform Google Play the purchase was handled.
     * Required for non-consumable purchases.
     */
    private fun acknowledgePurchase(purchase: com.android.billingclient.api.Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _billingState.value = BillingState.Success("Premium activated!")
            } else {
                _billingState.value = BillingState.Error("Failed to activate premium")
            }
        }
    }

    /**
     * Disconnect billing client.
     * Call this when app is destroyed.
     */
    fun disconnect() {
        if (isConnected) {
            billingClient.endConnection()
            isConnected = false
        }
    }
}

/**
 * State of billing operations.
 */
sealed class BillingState {
    data object Idle : BillingState()
    data object Loading : BillingState()
    data class Success(val message: String) : BillingState()
    data class Error(val message: String) : BillingState()
}

/**
 * Product details from Play Console.
 */
data class ProductDetails(
    val productId: String,
    val title: String,
    val description: String,
    val price: String
)
