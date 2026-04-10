package com.katafract.safeopen.models

data class ScannedPayload(
    val id: String,
    val rawValue: String,
    val type: PayloadType,
    val normalizedValue: String,
    val scannedAt: Long,
    val source: PayloadSource
) {
    enum class PayloadSource {
        QR_CODE,
        PASTED_TEXT,
        SHARED_LINK
    }
}
