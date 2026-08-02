package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DatabaseArchiveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val bytesArchived = performArchive(applicationContext)
            Log.d(TAG, "Database archival completed successfully. Archived $bytesArchived bytes.")
            Result.success(workDataOf("bytesArchived" to bytesArchived))
        } catch (e: Exception) {
            Log.e(TAG, "Database archival failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "DailyDatabaseArchiveWorker"
        private const val TAG = "DatabaseArchiveWorker"
        private const val MAX_ARCHIVES_TO_KEEP = 7

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<DatabaseArchiveWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Scheduled daily WorkManager database archive trigger.")
        }

        fun performArchive(context: Context): Long {
            val dbFile = context.getDatabasePath("app_database")
            if (!dbFile.exists()) {
                Log.w(TAG, "Room database file does not exist yet at ${dbFile.absolutePath}")
                return 0L
            }

            // Force WAL checkpoint before backup if database instance exists
            try {
                val db = AppDatabase.getDatabase(context)
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL);").close()
            } catch (e: Exception) {
                Log.w(TAG, "WAL checkpoint warning: ${e.message}")
            }

            val archiveDir = File(context.filesDir, "db_archives")
            if (!archiveDir.exists()) {
                archiveDir.mkdirs()
            }

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetBackupFile = File(archiveDir, "app_database_archive_$dateStr.db")

            var totalBytesCopied = copyFile(dbFile, targetBackupFile)

            // Also backup WAL file if present
            val walFile = File(dbFile.parentFile, "app_database-wal")
            if (walFile.exists()) {
                val targetWalFile = File(archiveDir, "app_database_archive_$dateStr.db-wal")
                totalBytesCopied += copyFile(walFile, targetWalFile)
            }

            pruneOldArchives(archiveDir)

            return totalBytesCopied
        }

        private fun copyFile(source: File, destination: File): Long {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    return input.copyTo(output)
                }
            }
        }

        private fun pruneOldArchives(directory: File) {
            val archives = directory.listFiles { file ->
                file.isFile && file.name.startsWith("app_database_archive_") && file.name.endsWith(".db")
            }?.sortedByDescending { it.lastModified() } ?: return

            if (archives.size > MAX_ARCHIVES_TO_KEEP) {
                for (i in MAX_ARCHIVES_TO_KEEP until archives.size) {
                    val fileToDelete = archives[i]
                    val walToDelete = File(directory, "${fileToDelete.name}-wal")
                    fileToDelete.delete()
                    if (walToDelete.exists()) walToDelete.delete()
                    Log.d(TAG, "Pruned old database archive: ${fileToDelete.name}")
                }
            }
        }
    }
}
