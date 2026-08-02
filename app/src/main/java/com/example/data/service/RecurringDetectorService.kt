package com.example.data.service

import com.example.data.model.TransactionEntity
import com.example.data.model.RecurringTransactionEntity
import kotlin.math.abs

data class RecurringSuggestion(
    val merchant: String,
    val categoryId: Long,
    val accountId: Long,
    val estimatedAmount: Double,
    val frequency: String, // "MONTHLY", "WEEKLY", "YEARLY"
    val occurrenceCount: Int,
    val lastDate: Long,
    val sampleNotes: String
)

object RecurringDetectorService {

    fun analyzeTransactions(
        transactions: List<TransactionEntity>,
        existingRecurring: List<RecurringTransactionEntity>
    ): List<RecurringSuggestion> {
        val existingMerchants = existingRecurring.map { it.merchant.trim().lowercase() }.toSet()

        val candidateMap = mutableMapOf<String, MutableList<TransactionEntity>>()

        for (tx in transactions) {
            if (tx.isRecurring) continue
            val name = (tx.merchant.ifBlank { tx.notes }).trim()
            if (name.length < 2) continue
            val normalizedKey = name.lowercase()

            if (existingMerchants.contains(normalizedKey)) continue

            candidateMap.getOrPut(normalizedKey) { mutableListOf() }.add(tx)
        }

        val suggestions = mutableListOf<RecurringSuggestion>()

        for ((_, txList) in candidateMap) {
            if (txList.size < 2) continue

            val sorted = txList.sortedBy { it.timestamp }
            val amounts = sorted.map { abs(it.amount) }
            val avgAmount = amounts.average()

            // Check if amounts are within 25% of average (similar price point)
            val isSimilarAmount = amounts.all { abs(it - avgAmount) / (avgAmount.coerceAtLeast(1.0)) < 0.25 }
            if (!isSimilarAmount) continue

            // Calculate intervals
            val intervals = mutableListOf<Long>()
            for (i in 0 until sorted.size - 1) {
                val daysDiff = (sorted[i + 1].timestamp - sorted[i].timestamp) / (1000 * 60 * 60 * 24)
                intervals.add(daysDiff)
            }

            val avgInterval = if (intervals.isNotEmpty()) intervals.average() else 30.0
            val frequency = when {
                avgInterval in 5.0..10.0 -> "WEEKLY"
                avgInterval in 22.0..36.0 -> "MONTHLY"
                avgInterval in 340.0..380.0 -> "YEARLY"
                else -> "MONTHLY"
            }

            val latestTx = sorted.last()
            val merchantDisplayName = latestTx.merchant.ifBlank { latestTx.notes }

            suggestions.add(
                RecurringSuggestion(
                    merchant = merchantDisplayName,
                    categoryId = latestTx.categoryId,
                    accountId = latestTx.accountId,
                    estimatedAmount = avgAmount,
                    frequency = frequency,
                    occurrenceCount = txList.size,
                    lastDate = latestTx.timestamp,
                    sampleNotes = "Detected $frequency pattern across ${txList.size} transactions"
                )
            )
        }

        return suggestions.sortedByDescending { it.occurrenceCount }
    }
}
