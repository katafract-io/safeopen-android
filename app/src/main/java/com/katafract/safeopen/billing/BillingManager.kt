package com.katafract.safeopen.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class BillingManager(
    context: Context,
    private val onPurchaseSuccess: (purchaseToken: String, productId: String) -> Unit = { _, _ -> }
) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    companion object {
        const val PROD_CREDITS_STARTER = "com.katafract.safeopen.credits_starter"
        const val PROD_CREDITS_STANDARD = "com.katafract.safeopen.credits_standard"
        const val PROD_CREDITS_POWER = "com.katafract.safeopen.credits_power"
        private val CONSUMABLE_PRODUCTS = listOf(
            PROD_CREDITS_STARTER,
            PROD_CREDITS_STANDARD,
            PROD_CREDITS_POWER
        )
    }

    fun connect() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    restorePurchases()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    fun disconnect() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { _, details ->
            val detail = details.firstOrNull() ?: return@queryProductDetailsAsync

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

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) return

        purchases?.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                val productId = purchase.products.firstOrNull() ?: return@forEach
                // Hand purchase token to caller for redeem POST before consuming
                onPurchaseSuccess(purchase.purchaseToken, productId)
            }
        }
    }

    fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // Consumed successfully
            }
        }
    }

    private fun restorePurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.filter { p ->
                    p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged &&
                    p.products.any { it in CONSUMABLE_PRODUCTS }
                }.forEach { purchase ->
                    val productId = purchase.products.firstOrNull() ?: return@forEach
                    onPurchaseSuccess(purchase.purchaseToken, productId)
                }
            }
        }
    }
}
