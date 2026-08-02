package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

data class ExchangeRateApiResponse(
    val result: String? = null,
    val provider: String? = null,
    val base_code: String? = null,
    val rates: Map<String, Double>? = null
)

interface ExchangeRateApiService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateApiResponse
}

object ExchangeRateApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://open.er-api.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: ExchangeRateApiService by lazy {
        retrofit.create(ExchangeRateApiService::class.java)
    }
}
