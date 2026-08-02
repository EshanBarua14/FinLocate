package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.AppDatabase
import com.example.data.model.ExchangeRateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ExchangeRateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val rateCount = fetchAndUpdateRates(applicationContext)
            Log.d("ExchangeRateWorker", "Successfully fetched and updated $rateCount exchange rates.")
            Result.success(workDataOf("ratesUpdated" to rateCount))
        } catch (e: Exception) {
            Log.e("ExchangeRateWorker", "Failed to fetch exchange rates", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "PeriodicExchangeRateWorker"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<ExchangeRateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        suspend fun fetchAndUpdateRates(context: Context): Int = withContext(Dispatchers.IO) {
            try {
                val apiResponse = com.example.data.api.ExchangeRateApiClient.service.getLatestRates()
                val newRates = apiResponse.rates
                if (!newRates.isNullOrEmpty() && newRates.containsKey("USD")) {
                    val db = AppDatabase.getDatabase(context)
                    val entities = newRates.map { (cur, rateVal) ->
                        ExchangeRateEntity(
                            currency = cur,
                            rate = rateVal,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    db.exchangeRateDao().insertAllRates(entities)
                    return@withContext entities.size
                }
            } catch (e: Exception) {
                Log.w("ExchangeRateWorker", "Retrofit fetch failed, falling back to URLConnection: ${e.message}")
            }

            val url = URL("https://open.er-api.com/v6/latest/USD")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val regex = "\"([A-Z]{3})\":\\s*([0-9\\.]+)".toRegex()
                val matches = regex.findAll(responseText)
                val newRates = matches.associate {
                    it.groupValues[1] to (it.groupValues[2].toDoubleOrNull() ?: 1.0)
                }

                if (newRates.isNotEmpty() && newRates.containsKey("USD")) {
                    val db = AppDatabase.getDatabase(context)
                    val entities = newRates.map { (cur, rateVal) ->
                        ExchangeRateEntity(
                            currency = cur,
                            rate = rateVal,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    db.exchangeRateDao().insertAllRates(entities)
                    return@withContext entities.size
                }
            }
            return@withContext 0
        }
    }
}
