package com.katafract.safeopen.services

import android.net.Uri
import com.katafract.safeopen.models.PayloadType
import com.katafract.safeopen.models.RiskLevel

object RiskScoringService {
    fun scorePayload(rawValue: String, type: PayloadType): Pair<RiskLevel, List<String>> {
        val factors = mutableListOf<String>()
        var score = 0

        return when (type) {
            PayloadType.URL, PayloadType.SHORT_URL -> scoreUrl(rawValue, factors)
            PayloadType.SHORT_URL -> {
                factors.add("Shortened URL - destination unknown")
                Pair(RiskLevel.CAUTION, factors)
            }
            PayloadType.WIFI -> Pair(RiskLevel.UNKNOWN, listOf("WiFi QR code - verify network name"))
            PayloadType.SMS -> Pair(RiskLevel.UNKNOWN, listOf("SMS link - verify before responding"))
            PayloadType.EMAIL -> Pair(RiskLevel.UNKNOWN, listOf("Email address - verify sender before contacting"))
            PayloadType.PHONE -> Pair(RiskLevel.UNKNOWN, listOf("Phone number - verify before calling"))
            PayloadType.TEXT -> Pair(RiskLevel.UNKNOWN, listOf("Plain text - unknown type"))
            PayloadType.UNKNOWN -> Pair(RiskLevel.UNKNOWN, listOf("Unknown payload type"))
        }
    }

    private fun scoreUrl(urlString: String, factors: MutableList<String>): Pair<RiskLevel, List<String>> {
        var riskScore = 0

        // Parse the URL
        val uri = try {
            Uri.parse(urlString)
        } catch (e: Exception) {
            factors.add("Invalid URL format")
            return Pair(RiskLevel.HIGH, factors)
        }

        // Check for suspicious patterns
        val host = uri.host ?: ""
        val scheme = uri.scheme ?: ""

        // Check for IP-only URLs (suspicious)
        if (isIpAddress(host)) {
            factors.add("Direct IP address - no domain")
            riskScore += 40
        }

        // Check for very long URLs
        if (urlString.length > 2000) {
            factors.add("Unusually long URL")
            riskScore += 20
        }

        // Check for suspicious TLDs
        val suspiciousTlds = listOf(".xyz", ".tk", ".ml", ".ga", ".cf", ".zip", ".bank", ".adult")
        if (suspiciousTlds.any { host.endsWith(it, ignoreCase = true) }) {
            factors.add("Suspicious top-level domain")
            riskScore += 30
        }

        // Check for non-HTTP(S) schemes
        if (scheme !in listOf("http", "https", "")) {
            factors.add("Non-standard protocol: $scheme")
            riskScore += 20
        }

        // Check for phishing patterns
        if (containsPhishingPatterns(urlString)) {
            factors.add("URL contains potential phishing indicators")
            riskScore += 50
        }

        // Check for encoded characters
        if (urlString.contains("%") && !isCommonEncoding(urlString)) {
            factors.add("URL contains unusual encoding")
            riskScore += 15
        }

        // Well-known legitimate domains
        if (isWellKnownSafe(host)) {
            riskScore = (riskScore - 40).coerceAtLeast(0)
            factors.add("Recognized legitimate domain")
        }

        // Determine risk level
        val level = when {
            riskScore >= 60 -> RiskLevel.HIGH
            riskScore >= 30 -> RiskLevel.CAUTION
            riskScore >= 0 -> RiskLevel.LOW
            else -> RiskLevel.UNKNOWN
        }

        if (factors.isEmpty()) {
            factors.add("URL appears safe")
        }

        return Pair(level, factors)
    }

    private fun isIpAddress(host: String): Boolean {
        val parts = host.split(".")
        return parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
    }

    private fun containsPhishingPatterns(url: String): Boolean {
        val phishingPatterns = listOf(
            "login", "signin", "account", "verify", "confirm",
            "update", "suspended", "urgent", "alert"
        )
        val lowerUrl = url.lowercase()
        return phishingPatterns.count { lowerUrl.contains(it) } >= 2
    }

    private fun isCommonEncoding(url: String): Boolean {
        val commonEncodings = listOf(
            "%20", "%2F", "%3A", "%3D", "%26", "%3F", "%2C", "%27"
        )
        return commonEncodings.any { url.contains(it) }
    }

    private fun isWellKnownSafe(host: String): Boolean {
        val safeDomains = listOf(
            "google.com", "gmail.com",
            "github.com", "stackoverflow.com",
            "youtube.com", "wikipedia.org",
            "apple.com", "microsoft.com",
            "amazon.com", "facebook.com",
            "twitter.com", "reddit.com"
        )
        return safeDomains.any { safe ->
            host.endsWith(safe, ignoreCase = true)
        }
    }
}
