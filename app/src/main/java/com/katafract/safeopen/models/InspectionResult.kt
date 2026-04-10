package com.katafract.safeopen.models

import java.util.UUID

data class InspectionResult(
    val id: String = UUID.randomUUID().toString(),
    val payload: ScannedPayload,
    val title: String,
    val summary: String,
    val riskLevel: RiskLevel,
    val riskFactors: List<String>,
    val recommendedAction: RecommendedAction,
    val finalUrl: String? = null,
    val redirectHops: List<String> = emptyList(),
    val canOpenSafely: Boolean = riskLevel == RiskLevel.LOW,
    val inspectedAt: Long = System.currentTimeMillis()
) {
    enum class RecommendedAction {
        OPEN,
        CAUTION,
        BLOCK
    }
}
