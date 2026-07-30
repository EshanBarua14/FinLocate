package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class CacheCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val bytesFreed = performCleanup(applicationContext)
            Log.d("CacheCleanupWorker", "Cleared cache & temp files. Freed $bytesFreed bytes.")
            Result.success(workDataOf("bytesFreed" to bytesFreed))
        } catch (e: Exception) {
            Log.e("CacheCleanupWorker", "Error during cache cleanup", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "PeriodicCacheCleanupWorker"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<CacheCleanupWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun performCleanup(context: Context): Long {
            var totalFreed = 0L

            // 1. Clear internal cache dir
            context.cacheDir?.let { cacheDir ->
                totalFreed += deleteExpiredFiles(cacheDir)
            }

            // 2. Clear external cache dir if accessible
            context.externalCacheDir?.let { extCacheDir ->
                totalFreed += deleteExpiredFiles(extCacheDir)
            }

            // 3. Clear temporary receipt scan image files or exports in filesDir
            val tempDir = File(context.filesDir, "temp_receipts")
            if (tempDir.exists()) {
                totalFreed += deleteExpiredFiles(tempDir)
            }

            val exportDir = File(context.filesDir, "exports")
            if (exportDir.exists()) {
                // Delete exports older than 3 days
                totalFreed += deleteExpiredFiles(exportDir, maxAgeMillis = 3 * 24 * 60 * 60 * 1000L)
            }

            return totalFreed
        }

        private fun deleteExpiredFiles(directory: File, maxAgeMillis: Long = 24 * 60 * 60 * 1000L): Long {
            var freed = 0L
            if (!directory.exists()) return 0L

            val currentTime = System.currentTimeMillis()
            val files = directory.listFiles() ?: return 0L

            for (file in files) {
                if (file.isDirectory) {
                    freed += deleteExpiredFiles(file, maxAgeMillis)
                } else {
                    // Delete files older than maxAgeMillis, or temp files
                    if (file.name.startsWith("temp_") || file.name.endsWith(".tmp") || (currentTime - file.lastModified()) > maxAgeMillis) {
                        val length = file.length()
                        if (file.delete()) {
                            freed += length
                        }
                    }
                }
            }
            return freed
        }
    }
}
