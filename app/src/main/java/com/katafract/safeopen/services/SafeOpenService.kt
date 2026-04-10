package com.katafract.safeopen.services

import android.net.Uri
import com.katafract.safeopen.models.InspectionResult
import com.katafract.safeopen.models.PayloadType
import com.katafract.safeopen.models.RiskLevel
import com.katafract.safeopen.models.ScannedPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit

object SafeOpenService {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    suspend fun inspectPayload(
        rawValue: String,
        source: ScannedPayload.PayloadSource
    ): InspectionResult = withContext(Dispatchers.Default) {
        // Step 1: Classify
        val type = PayloadClassifier.classify(rawValue)

        // Step 2: Normalize
        val normalized = normalizePayload(rawValue, type)

        // Step 3: Create payload object
        val payload = ScannedPayload(
            id = UUID.randomUUID().toString(),
            rawValue = rawValue,
            type = type,
            normalizedValue = normalized,
            scannedAt = System.currentTimeMillis(),
            source = source
        )

        // Step 4: Score locally
        val (riskLevel, riskFactors) = RiskScoringService.scorePayload(rawValue, type)

        // Step 5: Follow redirects if URL
        val (finalUrl, redirectHops) = if (type == PayloadType.URL || type == PayloadType.SHORT_URL) {
            try {
                followRedirects(normalized)
            } catch (e: Exception) {
                Pair(null, emptyList())
            }
        } else {
            Pair(null, emptyList())
        }

        // Step 6: Build result
        val recommendedAction = when (riskLevel) {
            RiskLevel.LOW -> InspectionResult.RecommendedAction.OPEN
            RiskLevel.CAUTION -> InspectionResult.RecommendedAction.CAUTION
            RiskLevel.HIGH, RiskLevel.UNKNOWN -> InspectionResult.RecommendedAction.BLOCK
        }

        InspectionResult(
            payload = payload,
            title = buildTitle(type, riskLevel),
            summary = buildSummary(type, riskLevel, riskFactors),
            riskLevel = riskLevel,
            riskFactors = riskFactors,
            recommendedAction = recommendedAction,
            finalUrl = finalUrl ?: normalized,
            redirectHops = redirectHops,
            canOpenSafely = riskLevel == RiskLevel.LOW
        )
    }

    private fun normalizePayload(rawValue: String, type: PayloadType): String {
        return when (type) {
            PayloadType.URL, PayloadType.SHORT_URL -> {
                val trimmed = rawValue.trim()
                if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true)
                ) {
                    trimmed
                } else {
                    "https://$trimmed"
                }
            }
            PayloadType.EMAIL -> rawValue.trim().removePrefix("mailto:")
            PayloadType.PHONE -> rawValue.trim().removePrefix("tel:").removePrefix("callto:")
            PayloadType.SMS -> rawValue.trim().removePrefix("sms:").removePrefix("smsto:")
            PayloadType.WIFI -> rawValue.trim()
            else -> rawValue.trim()
        }
    }

    suspend fun followRedirects(
        urlString: String,
        maxHops: Int = 5
    ): Pair<String?, List<String>> = withContext(Dispatchers.IO) {
        val hops = mutableListOf<String>()
        var currentUrl = urlString
        var finalUrl: String? = null

        for (i in 0 until maxHops) {
            try {
                val request = Request.Builder()
                    .url(currentUrl)
                    .head()
                    .build()

                val response = httpClient.newCall(request).execute()
                hops.add("$currentUrl (${response.code})")

                val location = response.header("Location")
                finalUrl = currentUrl

                if (location != null && response.code in 300..399) {
                    currentUrl = if (location.startsWith("http")) {
                        location
                    } else {
                        Uri.parse(currentUrl).scheme + "://" + Uri.parse(currentUrl).host + location
                    }
                } else {
                    break
                }

                response.close()
            } catch (e: Exception) {
                hops.add("$currentUrl (Error: ${e.message})")
                finalUrl = currentUrl
                break
            }
        }

        Pair(finalUrl, hops)
    }

    private fun buildTitle(type: PayloadType, riskLevel: RiskLevel): String {
        return "${type.toString()} - ${riskLevel.displayTitle}"
    }

    private fun buildSummary(
        type: PayloadType,
        riskLevel: RiskLevel,
        riskFactors: List<String>
    ): String {
        return when (riskLevel) {
            RiskLevel.LOW -> "This ${type.toString().lowercase()} appears to be safe."
            RiskLevel.CAUTION -> "This ${type.toString().lowercase()} has some suspicious characteristics. Review before opening."
            RiskLevel.HIGH -> "This ${type.toString().lowercase()} contains high-risk indicators. Recommend not opening."
            RiskLevel.UNKNOWN -> "Unable to determine safety. Be cautious with this ${type.toString().lowercase()}."
        }
    }
}
