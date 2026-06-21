package com.example.data.service

import com.example.data.model.TransactionEntity
import com.example.data.model.AccountEntity
import java.util.Calendar
import kotlin.math.abs

enum class AnomalyType {
    SUSPICIOUS_HIGH_AMOUNT,
    RAPID_DUPLICATE_ENTRY,
    SUDDEN_SPENDING_BURST
}

data class AnomalyReport(
    val transactionId: Long,
    val transaction: TransactionEntity,
    val type: AnomalyType,
    val confidence: Double, // 0.0 to 1.0 confidence score
    val description: String
)

object AnomalyDetectionService {

    /**
     * Analyzes bank and MFS transaction lists.
     * Flags:
     * 1. Duplicate entries (same amount, same merchant, within 24 hours).
     * 2. Suspiciously high amounts (outlier: greater than 3x the average of that category or > 5000 in absolute terms).
     * 3. Sudden spending bursts (several transactions for same category/merchant within details).
     */
    fun analyzeTransactions(
        transactions: List<TransactionEntity>,
        accounts: List<AccountEntity>
    ): List<AnomalyReport> {
        val anomalies = mutableListOf<AnomalyReport>()
        if (transactions.isEmpty()) return anomalies

        val expenses = transactions.filter { it.type == "EXPENSE" }

        // --- 1. SUSPICIOUS OUTLIER HIGH AMOUNTS ---
        // Calculate category averages
        val categoryGroups = expenses.groupBy { it.categoryId }
        categoryGroups.forEach { (catId, txList) ->
            if (txList.size >= 3) {
                val average = txList.map { it.amount }.average()
                txList.forEach { tx ->
                    // If greater than 3x the average of this category and larger than 100
                    if (tx.amount > 3 * average && tx.amount > 100.0) {
                        anomalies.add(
                            AnomalyReport(
                                transactionId = tx.id,
                                transaction = tx,
                                type = AnomalyType.SUSPICIOUS_HIGH_AMOUNT,
                                confidence = 0.85,
                                description = "Unusually high amount for this category. Current is ${tx.amount}, category average is %.1f.".format(average)
                            )
                        )
                    }
                }
            } else {
                // Individual absolutes
                txList.forEach { tx ->
                    if (tx.amount >= 2000.0) {
                        anomalies.add(
                            AnomalyReport(
                                transactionId = tx.id,
                                transaction = tx,
                                type = AnomalyType.SUSPICIOUS_HIGH_AMOUNT,
                                confidence = 0.70,
                                description = "Absolute high outlier of ${tx.amount} detected."
                            )
                        )
                    }
                }
            }
        }

        // --- 2. RAPID DUPLICATE ENTRIES ---
        // Duplicate defined as: same absolute amount, same merchant, and within 12 hours
        for (i in expenses.indices) {
            for (j in i + 1 until expenses.size) {
                val tx1 = expenses[i]
                val tx2 = expenses[j]
                
                if (tx1.id != tx2.id &&
                    tx1.accountId == tx2.accountId &&
                    abs(tx1.amount - tx2.amount) < 0.01 &&
                    tx1.merchant.isNotEmpty() &&
                    tx1.merchant.equals(tx2.merchant, ignoreCase = true)
                ) {
                    val diffMs = abs(tx1.timestamp - tx2.timestamp)
                    val diffHours = diffMs / (1000.0 * 60.0 * 60.0)
                    if (diffHours < 24.0) {
                        val formattedHours = "%.1f".format(diffHours)
                        val duplicateDesc = "Potential duplicate transaction of ${tx1.amount} with '${tx1.merchant}' within $formattedHours hours."
                        
                        // Add for parent or both
                        val containsOne = anomalies.any { it.transactionId == tx1.id || it.transactionId == tx2.id }
                        if (!containsOne) {
                            anomalies.add(
                                AnomalyReport(
                                    transactionId = tx1.id,
                                    transaction = tx1,
                                    type = AnomalyType.RAPID_DUPLICATE_ENTRY,
                                    confidence = 0.90,
                                    description = duplicateDesc
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- 3. SUDDEN SPENDING BURST ---
        // Having 3 or more transactions at the exact same merchant or within same category in a 12 hour period
        val sortedExpenses = expenses.sortedBy { it.timestamp }
        for (i in 0 until sortedExpenses.size - 2) {
            val tx1 = sortedExpenses[i]
            val tx3 = sortedExpenses[i+2]
            
            val diffMs = tx3.timestamp - tx1.timestamp
            val diffHours = diffMs / (1000.0 * 60.0 * 60.0)
            if (diffHours < 12.0) {
                // check if they are same category
                val tx2 = sortedExpenses[i+1]
                if (tx1.categoryId == tx2.categoryId && tx2.categoryId == tx3.categoryId) {
                    val containsAny = anomalies.any { 
                        it.transactionId == tx1.id || 
                        it.transactionId == tx2.id || 
                        it.transactionId == tx3.id 
                    }
                    if (!containsAny) {
                        anomalies.add(
                            AnomalyReport(
                                transactionId = tx1.id,
                                transaction = tx1,
                                type = AnomalyType.SUDDEN_SPENDING_BURST,
                                confidence = 0.75,
                                description = "Rapid spending burst: 3+ expenses inside same category in %.1f hours.".format(diffHours)
                            )
                        )
                    }
                }
            }
        }

        return anomalies
    }
}
