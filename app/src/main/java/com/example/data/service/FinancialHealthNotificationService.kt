package com.example.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.BudgetEntity
import com.example.data.model.InsightEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserDebtEntity
import java.util.Calendar

object FinancialHealthNotificationService {

    data class WeeklyHealthSummary(
        val healthScore: Int, // 0 to 100
        val summaryTitle: String,
        val summaryText: String,
        val habitNudge: String,
        val totalSpentThisWeek: Double,
        val savingsRatePercentage: Int
    )

    fun evaluateWeeklyFinancialHealth(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        debts: List<UserDebtEntity>,
        currencySymbol: String = "$"
    ): WeeklyHealthSummary {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000L)

        val recentWeekTxs = transactions.filter { it.timestamp in oneWeekAgo..now }
        val previousWeekTxs = transactions.filter { it.timestamp in twoWeeksAgo until oneWeekAgo }

        val weekOutflow = recentWeekTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val prevWeekOutflow = previousWeekTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val weekInflow = recentWeekTxs.filter { it.type == "INCOME" }.sumOf { it.amount }

        val totalBudget = budgets.sumOf { it.amount }
        val totalSpentBudget = budgets.sumOf { it.spent }

        // Compute Savings Rate
        val savingsRatePercentage = if (weekInflow > 0) {
            (((weekInflow - weekOutflow) / weekInflow) * 100).toInt().coerceIn(0, 100)
        } else {
            if (weekOutflow == 0.0) 50 else 10
        }

        // Compute Health Score (0-100)
        var score = 70
        if (weekOutflow < prevWeekOutflow) score += 10 else if (weekOutflow > prevWeekOutflow * 1.2 && prevWeekOutflow > 0) score -= 10
        if (savingsRatePercentage >= 20) score += 10
        if (totalBudget > 0 && totalSpentBudget > totalBudget) score -= 20
        if (debts.isNotEmpty()) score -= (debts.size * 3).coerceAtMost(15)
        val finalScore = score.coerceIn(10, 100)

        val deltaPercent = if (prevWeekOutflow > 0) {
            (((weekOutflow - prevWeekOutflow) / prevWeekOutflow) * 100).toInt()
        } else 0

        val trendMsg = if (deltaPercent < 0) {
            "Great news! Your weekly spending decreased by ${-deltaPercent}% compared to last week."
        } else if (deltaPercent > 0) {
            "Notice: Your weekly spending increased by ${deltaPercent}% ($currencySymbol${String.format("%.2f", weekOutflow)})."
        } else {
            "Your weekly spending remained steady at $currencySymbol${String.format("%.2f", weekOutflow)}."
        }

        val nudge = when {
            finalScore >= 80 -> "🌟 Outstanding financial discipline! Consider auto-allocating 15% of your net income directly into high-yield savings or investment funds."
            finalScore >= 60 -> "💡 Good progress! Automating recurring bill payments and trimming non-essential dining expenses could raise your savings rate by 8% this month."
            else -> "⚠️ Financial Nudge: You're close to exceeding monthly targets. Set spending alerts on major categories to keep your savings goals on track."
        }

        val title = "Financial Health Score: $finalScore/100"
        val fullSummary = "$trendMsg $nudge Total spent this week: $currencySymbol${String.format("%.2f", weekOutflow)}."

        return WeeklyHealthSummary(
            healthScore = finalScore,
            summaryTitle = title,
            summaryText = fullSummary,
            habitNudge = nudge,
            totalSpentThisWeek = weekOutflow,
            savingsRatePercentage = savingsRatePercentage
        )
    }

    fun triggerWeeklyHealthNotification(
        context: Context,
        summary: WeeklyHealthSummary
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelId = "wealthflow_financial_health_summary"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Weekly Financial Health Summaries & Nudges",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Weekly AI summary and savings habit nudges based on spending trends"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(summary.summaryTitle)
                .setContentText(summary.summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summary.summaryText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            notificationManager.notify(8888, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun buildInsightEntity(summary: WeeklyHealthSummary): InsightEntity {
        return InsightEntity(
            title = summary.summaryTitle,
            description = summary.summaryText,
            category = "SAVINGS_IMPROVED",
            severity = if (summary.healthScore >= 70) "SUCCESS" else if (summary.healthScore >= 50) "INFO" else "WARNING",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
    }
}
