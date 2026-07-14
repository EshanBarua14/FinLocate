package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabasePrunerUtility {
    private const val TAG = "DatabasePrunerUtility"

    /**
     * Prunes old, read insights, deletes ancient exchange rate caches,
     * and runs SQLite VACUUM to reclaim storage and optimize indexes.
     */
    suspend fun pruneAndOptimize(context: Context, appDatabase: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting periodic SQLite storage optimization...")
            
            // 1. Prune read insights older than 3 days
            val threshold = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            appDatabase.insightDao().pruneOldReadInsights(threshold)
            Log.d(TAG, "Pruned non-essential temporary insights cache.")

            // 2. Prune old exchange rates that are older than 30 days
            val rateThreshold = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            appDatabase.exchangeRateDao().pruneOldRates(rateThreshold)
            Log.d(TAG, "Pruned outdated exchange rates cache.")

            // 3. Run VACUUM to reclaim space and rebuild database structure
            appDatabase.openHelper.writableDatabase.execSQL("VACUUM")
            Log.d(TAG, "SQLite VACUUM execution completed successfully. Database fully optimized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run SQLite database optimization", e)
        }
    }
}
