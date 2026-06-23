package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiPriceService {
    private const val TAG = "GeminiPriceService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchOnlinePrice(productName: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: API Key is not configured in Secrets. Please add GEMINI_API_KEY."
        }

        val url = "$BASE_URL?key=$apiKey"

        val prompt = """
            You are a helpful retail business assistant for a shopkeeper (dukan dar) in Pakistan/South Asia.
            The user wants to check the current online/retail/wholesale market price for the product: "$productName".
            
            Please provide a realistic, professional estimate of the market rates.
            Provide the output clearly in simple Urdu and English.
            Include:
            1. Estimated Retail Price Range (Rs / PKR)
            2. Estimated Wholesale Price (if relevant)
            3. Smart advice/tip for the dukan dar to maximize profit or source it.
            
            Format the response with bullet points and short blocks of text so it's super easy to read on a mobile screen. Keep it clean, professional, and friendly.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed: ${response.code} $bodyString")
                    return@withContext "Error: API call failed with code ${response.code}. Please ensure your API key in Secrets is valid and active."
                }
                
                if (bodyString.isNullOrEmpty()) {
                    return@withContext "Error: Empty response from AI service."
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No content text found.")
                    }
                }
                return@withContext "Error: Failed to parse price prediction."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during fetchOnlinePrice", e)
            return@withContext "Network/Connection Error: ${e.localizedMessage ?: "Please check internet connectivity and try again."}"
        }
    }
}
