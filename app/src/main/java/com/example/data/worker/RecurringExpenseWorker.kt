package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.AppDatabase
import com.example.data.model.ExpenseEntity
import com.example.data.model.InsightEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RecurringExpenseWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val recurringDao = db.recurringExpenseDao()
            val expenseDao = db.expenseDao()
            val insightDao = db.insightDao()

            val currentTime = System.currentTimeMillis()
            val dueExpenses = recurringDao.getDueRecurringExpenses(currentTime)

            Log.d("RecurringExpenseWorker", "Found ${dueExpenses.size} due recurring expenses")

            for (re in dueExpenses) {
                // 1. Generate Expense instance
                val newExpense = ExpenseEntity(
                    amount = re.amount,
                    currency = re.currency,
                    date = currentTime,
                    taxCategoryId = re.taxCategoryId,
                    merchant = re.merchant,
                    notes = "Auto-generated recurring payment (${re.frequency})",
                    createdAt = currentTime
                )
                expenseDao.insertExpense(newExpense)

                // 2. Compute next execution date
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = if (re.nextDueAt > 0) re.nextDueAt else currentTime

                when (re.frequency.uppercase()) {
                    "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                    else -> calendar.add(Calendar.MONTH, 1) // Default MONTHLY
                }

                val nextDue = calendar.timeInMillis
                val updatedRe = re.copy(
                    lastExecutedAt = currentTime,
                    nextDueAt = nextDue
                )
                recurringDao.updateRecurringExpense(updatedRe)

                // 3. Insert notification insight
                val insight = InsightEntity(
                    title = "Recurring Payment Processed",
                    description = "Processed recurring expense of ${re.currency} ${re.amount} for ${re.merchant.ifEmpty { "General" }}.",
                    category = "RECURRING_PAYMENT",
                    severity = "INFO",
                    timestamp = currentTime
                )
                insightDao.insertInsight(insight)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("RecurringExpenseWorker", "Error processing recurring expenses", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "recurring_expenses_periodic_work"

        fun scheduleWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            // Run every 12 hours
            val periodicWork = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )

            // Also trigger immediate one-time check on schedule
            val immediateWork = OneTimeWorkRequestBuilder<RecurringExpenseWorker>()
                .build()
            workManager.enqueue(immediateWork)
        }
    }
}
