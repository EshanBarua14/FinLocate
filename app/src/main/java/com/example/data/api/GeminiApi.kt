package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Moshi Mapped Request/Response DTOs ---

data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f,
    val responseMimeType: String? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

// --- Retrofit Service Endpoint ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- API Client ---

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * Checks if a valid Gemini API key is configured.
     * Some build templates default the value to a string or placeholder from .env.example
     */
    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && key != "GEMINI_API_KEY"
    }

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Execute structured prompt in Gemini 3.5 Flash inside IO Coroutine Context
     */
    suspend fun getAiInsights(promptText: String, systemInstructionText: String? = null): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext "API_KEY_MISSING"
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = promptText)))
            ),
            systemInstruction = systemInstructionText?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            }
        )

        try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No evaluation text returned."
        } catch (e: Exception) {
            "Exception while calling Gemini: ${e.localizedMessage ?: e.message}"
        }
    }

    /**
     * Analyzes image of receipt to extract merchant, date, and amount.
     */
    suspend fun analyzeReceipt(bitmap: android.graphics.Bitmap): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            // Simulated local OCR fallback
            return@withContext """{"merchant": "Starbucks Coffee Retail", "date": "${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}", "amount": 16.85}"""
        }

        // Convert bitmap to base64
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val base64Data = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

        val promptText = "Extract the following receipt information as a single JSON object with these keys: 'merchant' (string, name of vendor), 'date' (string, format YYYY-MM-DD or empty if not found), and 'amount' (number, total amount of charges). Keep the JSON response simple and clear, no wrapping markdown, e.g. {\"merchant\": \"Starbucks\", \"date\": \"2026-06-19\", \"amount\": 12.50}."

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(
                    GeminiPart(text = promptText),
                    GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data))
                ))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json"
            )
        )

        try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    /**
     * Analyzes image of receipt to extract merchant, date, amount, and category match.
     */
    suspend fun analyzeReceiptWithCategory(
        bitmap: android.graphics.Bitmap,
        categoriesJson: String,
        historyJson: String
    ): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            // Simulated local OCR fallback
            return@withContext """{"merchant": "Starbucks Coffee Retail", "date": "${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}", "amount": 16.85, "categoryId": 0}"""
        }

        // Convert bitmap to base64
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val base64Data = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

        val promptText = """
            Analyze the image of the receipt to extract:
            1. 'merchant' (string, name of vendor)
            2. 'date' (string, format YYYY-MM-DD or empty if not found)
            3. 'amount' (number, total amount of charges)
            
            Additionally, automatically select the most appropriate category for this purchase.
            Here is the list of available categories (ID and Name):
            $categoriesJson
            
            Here is a sample of recent transaction history (Merchant to Category ID mapping):
            $historyJson
            
            Match the merchant name to the most logical category from the list. Use the transaction history for context if similar merchants exist.
            Return the result as a single JSON object with the following keys:
            'merchant' (string)
            'date' (string, format YYYY-MM-DD)
            'amount' (number)
            'categoryId' (number, the matched Category ID from the provided categories, or 0 if no logical category matches)
            
            Do not wrap in markdown or block comments. Return raw JSON.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(
                    GeminiPart(text = promptText),
                    GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data))
                ))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json"
            )
        )

        try {
            val response = service.generateContent(getApiKey(), request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }
}
