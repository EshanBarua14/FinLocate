package com.example.data.repository

import android.content.Context
import com.example.data.model.CountryConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class CountryConfigProviderService(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val adapter = moshi.adapter(CountryConfig::class.java)

    /**
     * Loads the CountryConfig JSON data dynamically from assets matching the selected country name.
     * Fallbacks to USA config automatically in case of errors.
     */
    fun loadConfigForCountry(countryName: String): CountryConfig {
        val fileName = when (countryName.trim().lowercase()) {
            "usa" -> "usa.json"
            "bangladesh" -> "bangladesh.json"
            "india" -> "india.json"
            "germany" -> "germany.json"
            else -> "usa.json"
        }
        
        return try {
            context.assets.open("countries/$fileName").use { inputStream ->
                val jsonText = inputStream.bufferedReader().use { it.readText() }
                adapter.fromJson(jsonText) ?: CountryConfig.find(countryName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful memory model fallback
            CountryConfig.find(countryName)
        }
    }
}
