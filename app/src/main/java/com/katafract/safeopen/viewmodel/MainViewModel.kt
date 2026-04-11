package com.katafract.safeopen.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.katafract.safeopen.billing.BillingManager
import com.katafract.safeopen.data.ScanHistoryDatabase
import com.katafract.safeopen.data.ScanHistoryEntity
import com.katafract.safeopen.models.InspectionResult
import com.katafract.safeopen.models.ScannedPayload
import com.katafract.safeopen.services.SafeOpenService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.google.gson.Gson

class MainViewModel(application: Application) : AndroidViewModel(application) {

    enum class Screen {
        SCANNER,
        RESULT,
        HISTORY
    }

    private val _screen = MutableStateFlow(Screen.SCANNER)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _currentResult = MutableStateFlow<InspectionResult?>(null)
    val currentResult: StateFlow<InspectionResult?> = _currentResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _scannerActive = MutableStateFlow(true)
    val scannerActive: StateFlow<Boolean> = _scannerActive.asStateFlow()

    // Database
    private val db = ScanHistoryDatabase.getInstance(application)
    private val dao = db.dao()
    private val gson = Gson()

    // Billing
    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    val billingManager = BillingManager(
        context = application,
        onPurchaseSuccess = { _, _ ->
            _isPro.value = true
        },
        onPurchaseError = { }
    )

    // History flow from DB
    val historyFlow: Flow<List<InspectionResult>> = dao.getAllScans().map { entities ->
        entities.mapNotNull { entity ->
            try {
                InspectionResult(
                    id = entity.id.toString(),
                    payload = ScannedPayload(
                        id = entity.id.toString(),
                        rawValue = entity.rawValue,
                        type = com.katafract.safeopen.models.PayloadType.values()
                            .firstOrNull { it.toString() == entity.payloadType }
                            ?: com.katafract.safeopen.models.PayloadType.UNKNOWN,
                        normalizedValue = entity.resolvedUrl ?: entity.rawValue,
                        scannedAt = entity.scannedAt,
                        source = ScannedPayload.PayloadSource.values()
                            .firstOrNull { it.toString() == entity.source }
                            ?: ScannedPayload.PayloadSource.QR_CODE
                    ),
                    title = "${entity.payloadType} - ${entity.riskLevel}",
                    summary = entity.summary,
                    riskLevel = com.katafract.safeopen.models.RiskLevel.values()
                        .firstOrNull { it.toString() == entity.riskLevel }
                        ?: com.katafract.safeopen.models.RiskLevel.UNKNOWN,
                    riskFactors = try {
                        gson.fromJson(entity.signalsJson, Array<String>::class.java).toList()
                    } catch (e: Exception) {
                        emptyList()
                    },
                    recommendedAction = when (entity.riskLevel) {
                        "LOW" -> InspectionResult.RecommendedAction.OPEN
                        "CAUTION" -> InspectionResult.RecommendedAction.CAUTION
                        else -> InspectionResult.RecommendedAction.BLOCK
                    },
                    finalUrl = entity.resolvedUrl,
                    redirectHops = emptyList(),
                    canOpenSafely = entity.riskLevel == "LOW",
                    inspectedAt = entity.scannedAt
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    init {
        // Initialize billing
        billingManager.connect()
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.disconnect()
    }

    fun onQrScanned(rawValue: String) {
        if (rawValue.isBlank()) return
        inspectPayload(rawValue, ScannedPayload.PayloadSource.QR_CODE)
    }

    fun inspectPasted(text: String) {
        if (text.isBlank()) return
        inspectPayload(text, ScannedPayload.PayloadSource.PASTED_TEXT)
    }

    fun onShareReceived(text: String) {
        if (text.isBlank()) return
        inspectPayload(text, ScannedPayload.PayloadSource.SHARED_LINK)
    }

    fun reInspectPayload(result: InspectionResult) {
        inspectPayload(result.payload.rawValue, result.payload.source)
    }

    private fun inspectPayload(rawValue: String, source: ScannedPayload.PayloadSource) {
        viewModelScope.launch {
            _isLoading.value = true
            _scannerActive.value = false

            try {
                val result = SafeOpenService.inspectPayload(rawValue, source)
                _currentResult.value = result

                // Save to database
                val entity = ScanHistoryEntity(
                    rawValue = result.payload.rawValue,
                    payloadType = result.payload.type.toString(),
                    riskLevel = result.riskLevel.displayTitle,
                    riskScore = when (result.riskLevel) {
                        com.katafract.safeopen.models.RiskLevel.LOW -> 20
                        com.katafract.safeopen.models.RiskLevel.CAUTION -> 50
                        com.katafract.safeopen.models.RiskLevel.HIGH -> 80
                        com.katafract.safeopen.models.RiskLevel.UNKNOWN -> 0
                    },
                    resolvedUrl = result.finalUrl,
                    redirectCount = result.redirectHops.size,
                    source = source.toString(),
                    signalsJson = gson.toJson(result.riskFactors),
                    summary = result.summary
                )
                dao.insert(entity)

                _screen.value = Screen.RESULT
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun navigateToScanner() {
        _screen.value = Screen.SCANNER
        _scannerActive.value = true
        _currentResult.value = null
    }

    fun navigateToHistory() {
        _screen.value = Screen.HISTORY
        _scannerActive.value = false
    }

    fun navigateToResult() {
        _screen.value = Screen.RESULT
        _scannerActive.value = false
    }

    fun deleteScan(entity: ScanHistoryEntity) {
        viewModelScope.launch {
            dao.delete(entity)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun launchBilling(activity: Activity) {
        billingManager.launchPurchase(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }
}
