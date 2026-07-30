package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.AppDatabase
import com.example.data.model.InsightEntity
import com.example.data.service.BudgetNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BudgetCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val budgetDao = db.budgetDao()
            val categoryDao = db.categoryDao()
            val insightDao = db.insightDao()

            val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
            val budgets = budgetDao.getBudgetsForMonthStatic(currentMonth)
            val categories = categoryDao.getAllCategoriesStatic()

            Log.d("BudgetCheckWorker", "Running budget threshold audit for ${budgets.size} active budgets.")

            // Execute budget check & post local system notifications
            val alerts = BudgetNotificationService.checkBudgetsAndNotify(
                applicationContext,
                budgets,
                categories
            )

            // Save insight feed entries for each triggered alert
            val currentTime = System.currentTimeMillis()
            for (alert in alerts) {
                val insight = InsightEntity(
                    title = if (alert.isExceeded) "🚨 Budget Limit Exceeded (${alert.categoryName})" else "⚠️ Budget Limit Approached (${alert.categoryName})",
                    description = alert.message,
                    category = "BUDGET_WARNING",
                    severity = if (alert.isExceeded) "CRITICAL" else "WARNING",
                    timestamp = currentTime
                )
                insightDao.insertInsight(insight)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("BudgetCheckWorker", "Error executing budget check worker", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "budget_threshold_check_periodic_work"

        fun scheduleWorker(context: Context) {
            try {
                val workManager = WorkManager.getInstance(context)

                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()

                // Schedule periodic work every 6 hours
                val periodicWork = PeriodicWorkRequestBuilder<BudgetCheckWorker>(6, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )

                // Enqueue immediate initial check
                val immediateWork = OneTimeWorkRequestBuilder<BudgetCheckWorker>()
                    .build()
                workManager.enqueue(immediateWork)
            } catch (e: Exception) {
                Log.e("BudgetCheckWorker", "Failed to schedule BudgetCheckWorker", e)
            }
        }
    }
}
