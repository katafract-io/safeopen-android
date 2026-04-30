package com.katafract.safeopen.network

import android.content.Context
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

data class CreditsSnapshot(
    val balance: Int,
    val free_balance: Int,
    val next_refill_at: Int,
    val total_consumed: Int
)

data class Offer(
    val product_id: String,
    val base_credits: Int,
    val bonus_credits: Int,
    val bonus_type: String,
    val total_credits: Int
)

data class OffersResponse(
    val offers: List<Offer>
)

data class RedeemResponse(
    val balance: Int,
    val granted: Int,
    val product_id: String
)

class InspectionAPIClient(context: Context) {

    companion object {
        const val BASE_URL = "https://api.katafract.com"
        const val SERVICE_TOKEN = "3e27ee700e0b3ef336b4c7b5360af3fdb16410fb445e2b1889bf5da5b083b977"
        private const val PREF_NAME = "safeopen_device_id"
        private const val KEY_DEVICE_ID = "device_uuid"
    }

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val deviceId: String
        get() {
            var id = sharedPref.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                sharedPref.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    suspend fun getCredits(): CreditsSnapshot {
        val request = Request.Builder()
            .url("$BASE_URL/v1/safeopen/credits")
            .get()
            .addHeader("Authorization", "Bearer $SERVICE_TOKEN")
            .addHeader("X-Device-ID", deviceId)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get credits: ${response.code} ${response.message}")
        }

        return gson.fromJson(response.body!!.string(), CreditsSnapshot::class.java)
    }

    suspend fun getOffers(): List<Offer> {
        val request = Request.Builder()
            .url("$BASE_URL/v1/safeopen/credits/offers")
            .get()
            .addHeader("Authorization", "Bearer $SERVICE_TOKEN")
            .addHeader("X-Device-ID", deviceId)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get offers: ${response.code} ${response.message}")
        }

        return gson.fromJson(response.body!!.string(), OffersResponse::class.java).offers
    }

    suspend fun redeemPlayPurchase(productId: String, purchaseToken: String): RedeemResponse {
        val body = mapOf(
            "product_id" to productId,
            "purchase_token" to purchaseToken
        )
        val jsonBody = gson.toJson(body).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/v1/safeopen/credits/redeem-play")
            .post(jsonBody)
            .addHeader("Authorization", "Bearer $SERVICE_TOKEN")
            .addHeader("X-Device-ID", deviceId)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseData = response.body!!.string()

        return when (response.code) {
            200, 409 -> {
                gson.fromJson(responseData, RedeemResponse::class.java)
            }
            402 -> throw Exception("Invalid product or unverified purchase")
            500, 502, 503, 504 -> throw Exception("Server error: ${response.code}")
            else -> throw Exception("Redeem failed: ${response.code} $responseData")
        }
    }
}
