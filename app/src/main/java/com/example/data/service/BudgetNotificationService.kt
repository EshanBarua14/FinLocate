package com.example.data.service

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity

object BudgetNotificationService {

    data class BudgetAlert(
        val categoryId: Long,
        val categoryName: String,
        val isExceeded: Boolean,
        val percentage: Int,
        val spent: Double,
        val limit: Double,
        val message: String
    )

    /**
     * Checks budgets for potential limit warnings or overruns.
     * Returns a list of structured [BudgetAlert] alerts.
     */
    fun checkBudgets(
        context: Context,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>
    ): List<BudgetAlert> {
        val alerts = mutableListOf<BudgetAlert>()

        budgets.forEach { budget ->
            if (budget.amount <= 0.0) return@forEach
            val spent = budget.spent
            val limit = budget.amount
            val percentage = ((spent / limit) * 100).toInt()
            val categoryName = categories.find { it.id == budget.categoryId }?.name ?: "Unknown"

            val sharedPrefs = context.getSharedPreferences("wealthflow_budget_alerts_prefs", Context.MODE_PRIVATE)

            val alert50Enabled = sharedPrefs.getBoolean("category_${budget.categoryId}_alert_50", true)
            val alert75Enabled = sharedPrefs.getBoolean("category_${budget.categoryId}_alert_75", true)
            val alert90Enabled = sharedPrefs.getBoolean("category_${budget.categoryId}_alert_90", true)
            val alert100Enabled = sharedPrefs.getBoolean("category_${budget.categoryId}_alert_100", true)

            if (percentage >= 100 && alert100Enabled) {
                alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        isExceeded = true,
                        percentage = percentage,
                        spent = budget.spent,
                        limit = budget.amount,
                        message = "🚨 Budget Alert (100% Reached): You have fully used your monthly budget for '$categoryName'! Spent $spent of $limit ($percentage%)."
                    )
                )
            } else if (percentage >= 90 && alert90Enabled) {
                alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        isExceeded = false,
                        percentage = percentage,
                        spent = budget.spent,
                        limit = budget.amount,
                        message = "🚨 Budget Urgent Warning (90% Reached): You have used $percentage% of your budget limit for '$categoryName'. Spent $spent of $limit."
                    )
                )
            } else if (percentage >= 75 && alert75Enabled) {
                alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        isExceeded = false,
                        percentage = percentage,
                        spent = budget.spent,
                        limit = budget.amount,
                        message = "⚠️ Budget Caution (75% Reached): You have reached 75% of your budget limit for '$categoryName'. Spent $spent of $limit."
                    )
                )
            } else if (percentage >= 50 && alert50Enabled) {
                alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        isExceeded = false,
                        percentage = percentage,
                        spent = budget.spent,
                        limit = budget.amount,
                        message = "🟢 Budget Halfway Milestone (50% Reached): You have used 50% of your budget limit for '$categoryName'. Spent $spent of $limit."
                    )
                )
            }
        }

        return alerts
    }

    /**
     * Checks budgets and posts local Android system notifications when spending thresholds (80% or 100%) are reached.
     */
    fun checkBudgetsAndNotify(
        context: Context,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>
    ): List<BudgetAlert> {
        val alerts = checkBudgets(context, budgets, categories)
        alerts.forEach { alert ->
            triggerLocalSystemNotification(context, alert)
        }
        return alerts
    }

    /**
     * Triggers a local Android system notification for a specific budget threshold alert.
     */
    fun triggerLocalSystemNotification(context: Context, alert: BudgetAlert) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelId = "wealthflow_budget_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Monthly Budget Threshold Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "System notifications triggered when category spending reaches defined monthly budget limits"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(if (alert.isExceeded) "🚨 Budget Limit Exceeded: ${alert.categoryName}" else "⚠️ Budget Warning: ${alert.categoryName}")
                .setContentText(alert.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(alert.categoryId.toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

