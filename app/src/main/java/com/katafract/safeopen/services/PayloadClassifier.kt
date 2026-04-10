package com.katafract.safeopen.services

import com.katafract.safeopen.models.PayloadType

object PayloadClassifier {
    fun classify(rawValue: String): PayloadType {
        val trimmed = rawValue.trim()

        return when {
            // WiFi networks
            trimmed.startsWith("WIFI:", ignoreCase = true) -> PayloadType.WIFI

            // SMS
            trimmed.startsWith("smsto:", ignoreCase = true) ||
            trimmed.startsWith("sms:", ignoreCase = true) -> PayloadType.SMS

            // Email
            trimmed.startsWith("mailto:", ignoreCase = true) ||
            (trimmed.contains("@") && !trimmed.contains("/")) -> PayloadType.EMAIL

            // Phone
            trimmed.startsWith("tel:", ignoreCase = true) ||
            trimmed.startsWith("callto:", ignoreCase = true) -> PayloadType.PHONE

            // Short URLs (common shortening services)
            isShortUrl(trimmed) -> PayloadType.SHORT_URL

            // Full URLs
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> PayloadType.URL

            // Fallback for URLs without scheme
            trimmed.contains(".") && !trimmed.contains(" ") &&
            !trimmed.startsWith("//") -> {
                try {
                    // Try to parse as URL
                    android.net.Uri.parse("https://$trimmed")
                    PayloadType.URL
                } catch (e: Exception) {
                    PayloadType.TEXT
                }
            }

            // Plain text
            else -> PayloadType.TEXT
        }
    }

    private fun isShortUrl(url: String): Boolean {
        val shortServices = listOf(
            "bit.ly", "bitly.com",
            "tinyurl.com",
            "t.co",
            "short.link",
            "ow.ly",
            "adf.ly",
            "goo.gl",
            "is.gd"
        )

        return shortServices.any { service ->
            url.contains(service, ignoreCase = true)
        }
    }
}
