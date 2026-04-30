package com.katafract.safeopen.credits

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.katafract.safeopen.billing.BillingManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreditsViewModel(context: Context) : ViewModel() {

    private val billingManager = BillingManager(context) { token, productId ->
        onPurchaseSuccess(token, productId)
    }
    private val repository = CreditsRepository(context, billingManager)

    val creditsBalance: StateFlow<Int>
        get() = kotlinx.coroutines.flow.MutableStateFlow(repository.creditsState.value.balance)

    val offers: StateFlow<List<com.katafract.safeopen.network.Offer>>
        get() = repository.offers

    private fun onPurchaseSuccess(token: String, productId: String) {
        viewModelScope.launch {
            try {
                repository.refreshBalance()
            } catch (e: Exception) {
                // Error already in state
            }
        }
    }

    fun loadCreditsAndOffers() {
        viewModelScope.launch {
            repository.refreshBalance()
            repository.refreshOffers()
        }
    }

    fun purchaseCredits(activity: Activity, productId: String) {
        billingManager.launchPurchase(activity, productId)
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.disconnect()
    }
}
