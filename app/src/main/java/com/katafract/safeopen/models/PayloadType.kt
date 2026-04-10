package com.katafract.safeopen.models

enum class PayloadType {
    URL,
    SHORT_URL,
    WIFI,
    SMS,
    EMAIL,
    PHONE,
    TEXT,
    UNKNOWN;

    override fun toString(): String = when (this) {
        URL -> "URL"
        SHORT_URL -> "Shortened URL"
        WIFI -> "WiFi Network"
        SMS -> "SMS"
        EMAIL -> "Email"
        PHONE -> "Phone Number"
        TEXT -> "Text"
        UNKNOWN -> "Unknown"
    }
}
