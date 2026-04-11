package com.katafract.safeopen.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    context: Context,
    private val onPurchaseSuccess: (purchaseToken: String, productId: String) -> Unit = { _, _ -> },
    private val onPurchaseError: (message: String) -> Unit = { }
) : PurchasesUpdatedListener {

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    companion object {
        const val PROD_PRO = "com.katafract.safeopen.pro"
    }

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    checkExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Optionally try to reconnect
            }
        })
    }

    private fun checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPro = purchases.any { purchase ->
                    purchase.products.contains(PROD_PRO) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPro.value = hasPro

                // Acknowledge any unacknowledged purchases
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
                    .forEach { acknowledgePurchase(it) }
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PROD_PRO)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { _, details ->
            val detail = details.firstOrNull() ?: run {
                onPurchaseError("Product not found")
                return@queryProductDetailsAsync
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(detail)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    fun restorePurchases() {
        checkExistingPurchases()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                    purchase.products.forEach { pid ->
                        onPurchaseSuccess(purchase.purchaseToken, pid)
                    }
                    if (purchase.products.contains(PROD_PRO)) {
                        _isPro.value = true
                    }
                }
            }
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseError("Purchase failed: ${result.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            ) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    onPurchaseError("Failed to acknowledge purchase")
                }
            }
        }
    }

    fun disconnect() {
        billingClient.endConnection()
    }
}
