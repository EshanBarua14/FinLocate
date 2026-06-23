package com.example.data.service

import android.content.Context
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object RecurringTransactionScheduler {

    /**
     * Scans all stored recurring transaction definitions, automatically inserts due transactions,
     * and advances their next scheduled dates. Runs fully on Dispatchers.IO.
     * Returns the list of newly posted transaction messages to notify.
     */
    suspend fun checkAndProcessRecurring(repository: FinanceRepository): List<String> = withContext(Dispatchers.IO) {
        val notifications = mutableListOf<String>()
        val currentTime = System.currentTimeMillis()
        
        try {
            val recurringList = repository.getActiveRecurringStatic()
            
            recurringList.forEach { schedule ->
                var nextExecution = schedule.nextExecutionTimestamp
                
                // If the next execution date has passed or is now
                if (currentTime >= nextExecution && schedule.isActive) {
                    var lastExec = nextExecution
                    var updatedNextExec = calculateNextOccurrence(nextExecution, schedule.recurrenceInterval)
                    
                    // Safe guard: if the next occurrence is still in the past, keep forwarding it so we don't loop endlessly
                    while (currentTime >= updatedNextExec) {
                        lastExec = updatedNextExec
                        updatedNextExec = calculateNextOccurrence(updatedNextExec, schedule.recurrenceInterval)
                    }

                    // 1. Create and insert the transaction
                    val spawnedTx = TransactionEntity(
                        amount = schedule.amount,
                        type = schedule.type,
                        categoryId = schedule.categoryId,
                        accountId = schedule.accountId,
                        toAccountId = schedule.toAccountId,
                        timestamp = nextExecution, // Executed precisely on scheduled time
                        merchant = schedule.merchant,
                        isTaxDeductible = schedule.isTaxDeductible,
                        taxRate = schedule.taxRate,
                        notes = schedule.notes.ifEmpty { "Automatically generated recurring entry" },
                        isRecurring = true,
                        recurrenceInterval = schedule.recurrenceInterval,
                        userEmail = schedule.userEmail
                    )
                    
                    // Perform actual insertion
                    repository.insertTransaction(spawnedTx)

                    // 2. Update the scheduler state with next execution date
                    val updatedSchedule = schedule.copy(
                        lastExecutionTimestamp = lastExec,
                        nextExecutionTimestamp = updatedNextExec
                    )
                    repository.updateRecurring(updatedSchedule)

                    notifications.add(
                        "📅 Scheduled Posting: Auto-inserted recurring ${schedule.type.lowercase()} for '${schedule.merchant}' (${schedule.recurrenceInterval})."
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext notifications
    }

    private fun calculateNextOccurrence(current: Long, interval: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = current
        
        when (interval.uppercase()) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            else -> calendar.add(Calendar.MONTH, 1) // default fallback
        }
        
        return calendar.timeInMillis
    }
}
