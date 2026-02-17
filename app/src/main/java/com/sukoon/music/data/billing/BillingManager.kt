package com.sukoon.music.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sukoon.music.data.preferences.PreferencesManager
import com.sukoon.music.data.analytics.AnalyticsTracker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val preferencesManager: PreferencesManager,
    private val analyticsTracker: AnalyticsTracker? = null
) : PurchasesUpdatedListener {

    companion object {
        // STATIC TEST → "android.test.purchased"
        // PRODUCTION → "sukoon_premium_lifetime"
        const val PREMIUM_PRODUCT_ID = "sukoon_premium_lifetime"
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var isConnected = false
    private var cachedProductDetails: ProductDetails? = null

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState

    // ------------------ INIT ------------------

    suspend fun initialize() {
        if (isConnected) return

        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d("BillingManager", "Billing connected")
                        isConnected = true
                        queryExistingPurchases()
                    } else {
                        Log.e("BillingManager", "Billing setup failed: ${result.debugMessage}")
                    }
                    cont.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    Log.w("BillingManager", "Billing disconnected → retrying")
                    isConnected = false
                    scope.launch { initialize() }
                }
            })

            cont.invokeOnCancellation { }
        }
    }

    // ------------------ QUERY PRODUCT ------------------

    suspend fun queryPremiumProduct(): UiProduct? {
        if (!isConnected) return null

        return suspendCancellableCoroutine<UiProduct?> { cont ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PREMIUM_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {

                    if (list.isNotEmpty()) {
                        cachedProductDetails = list[0]
                        val details = list[0]

                        cont.resume(
                            UiProduct(
                                id = details.productId,
                                title = details.title,
                                description = details.description,
                                price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                            )
                        )
                    } else {
                        // Static test fallback
                        Log.w("BillingManager", "Static test mode: No ProductDetails returned")

                        cont.resume(
                            UiProduct(
                                id = PREMIUM_PRODUCT_ID,
                                title = "Premium (Test)",
                                description = "Static purchase test",
                                price = ""
                            )
                        )
                    }
                } else {
                    Log.e("BillingManager", "Product query failed: ${result.debugMessage}")
                    cont.resume(null)
                }
            }

            cont.invokeOnCancellation { }
        }
    }

    // ------------------ PURCHASE FLOW ------------------

    suspend fun launchPurchaseFlow(activity: Activity) {
        if (!isConnected) return
        if (_billingState.value == BillingState.Loading) return

        _billingState.value = BillingState.Loading

        if (cachedProductDetails == null) {
            queryPremiumProduct()
        }

        val details = cachedProductDetails

        // DEBUG ONLY: Simulate purchase for testing without Google Play
        if (details == null && PREMIUM_PRODUCT_ID.startsWith("android.test")) {
            Log.d("BillingManager", "Simulating purchase (test mode)")
            _billingState.value = BillingState.Loading
            scope.launch {
                delay(1000) // Simulate API delay
                _billingState.value = BillingState.Success("Test purchase complete")
                grantPremium()
            }
            return
        }

        if (details == null) {
            _billingState.value = BillingState.Error("Product not loaded")
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    // ------------------ PURCHASE CALLBACK ------------------

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        acknowledgePurchase(purchase)
                    }
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingState.value = BillingState.Idle
            }

            else -> {
                _billingState.value = BillingState.Error(result.debugMessage ?: "Billing error")
            }
        }
    }

    // ------------------ ACKNOWLEDGE ------------------

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) {
            grantPremium()
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                grantPremium()
            } else {
                _billingState.value = BillingState.Error("Acknowledge failed")
            }
        }
    }

    // ------------------ RESTORE ------------------

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    if (purchase.products.contains(PREMIUM_PRODUCT_ID)) {
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        } else {
                            grantPremium()
                        }
                    }
                }
            }
        }
    }

    suspend fun restorePurchases() {
        _billingState.value = BillingState.Loading
        queryExistingPurchases()
        _billingState.value = BillingState.Idle
    }

    fun resetBillingState() {
        _billingState.value = BillingState.Idle
    }

    // ------------------ PREMIUM GRANT ------------------

    private fun grantPremium() {
        scope.launch {
            preferencesManager.setIsPremiumUser(true)
        }
        _billingState.value = BillingState.Success("Premium activated")
        analyticsTracker?.logEvent("premium_unlocked", emptyMap())
    }

    // ------------------ CLEANUP ------------------

    fun disconnect() {
        if (isConnected) billingClient.endConnection()
        scope.cancel()
    }
}

// ------------------ STATE ------------------

sealed class BillingState {
    data object Idle : BillingState()
    data object Loading : BillingState()
    data class Success(val msg: String) : BillingState()
    data class Error(val msg: String) : BillingState()
}

// ------------------ UI MODEL ------------------

data class UiProduct(
    val id: String,
    val title: String,
    val description: String,
    val price: String
)
