package com.katafract.safeopen.credits

import android.content.Context
import com.katafract.safeopen.network.InspectionAPIClient
import com.katafract.safeopen.network.CreditsSnapshot
import com.katafract.safeopen.network.Offer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CreditsState(
    val balance: Int = 0,
    val freeBalance: Int = 0,
    val nextRefillAt: Int = 0,
    val totalConsumed: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CreditsRepository(
    context: Context,
    private val billingManager: com.katafract.safeopen.billing.BillingManager
) {
    private val apiClient = InspectionAPIClient(context)

    private val _creditsState = MutableStateFlow(CreditsState())
    val creditsState: StateFlow<CreditsState> = _creditsState.asStateFlow()

    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()

    suspend fun refreshBalance() {
        try {
            _creditsState.value = _creditsState.value.copy(isLoading = true)
            val snapshot = apiClient.getCredits()
            _creditsState.value = CreditsState(
                balance = snapshot.balance,
                freeBalance = snapshot.free_balance,
                nextRefillAt = snapshot.next_refill_at,
                totalConsumed = snapshot.total_consumed,
                isLoading = false
            )
        } catch (e: Exception) {
            _creditsState.value = _creditsState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to load credits"
            )
        }
    }

    suspend fun refreshOffers() {
        try {
            val newOffers = apiClient.getOffers()
            _offers.value = newOffers
        } catch (e: Exception) {
            _creditsState.value = _creditsState.value.copy(
                error = e.message ?: "Failed to load offers"
            )
        }
    }

    suspend fun redeemPurchase(purchaseToken: String, productId: String) {
        try {
            val response = apiClient.redeemPlayPurchase(productId, purchaseToken)
            if (response.granted > 0 || response.balance >= 0) {
                // Success: consume the purchase
                billingManager.consumePurchaseByToken(purchaseToken)
                // Update balance
                _creditsState.value = _creditsState.value.copy(
                    balance = response.balance,
                    error = null
                )
            }
        } catch (e: Exception) {
            _creditsState.value = _creditsState.value.copy(
                error = e.message ?: "Failed to redeem purchase"
            )
            throw e
        }
    }
}
