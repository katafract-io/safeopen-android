package com.katafract.safeopen.models

enum class RiskLevel(val displayTitle: String) {
    LOW("Safe"),
    CAUTION("Caution"),
    HIGH("Dangerous"),
    UNKNOWN("Unknown");

    override fun toString(): String = displayTitle
}
