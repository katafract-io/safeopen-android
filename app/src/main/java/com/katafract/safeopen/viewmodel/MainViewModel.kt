package com.katafract.safeopen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.katafract.safeopen.models.InspectionResult
import com.katafract.safeopen.models.ScannedPayload
import com.katafract.safeopen.services.SafeOpenService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _history = MutableStateFlow<List<InspectionResult>>(emptyList())
    val history: StateFlow<List<InspectionResult>> = _history.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _scannerActive = MutableStateFlow(true)
    val scannerActive: StateFlow<Boolean> = _scannerActive.asStateFlow()

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
                _history.value = listOf(result) + _history.value.take(99) // Keep last 100
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

    fun clearHistory() {
        _history.value = emptyList()
    }
}
