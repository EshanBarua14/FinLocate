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

data class GeminiPart(
    val text: String? = null
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
}
