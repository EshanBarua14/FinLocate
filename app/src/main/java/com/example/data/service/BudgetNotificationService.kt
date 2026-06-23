package com.example.data.service

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

            if (percentage >= 100) {
                alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        isExceeded = true,
                        percentage = percentage,
                        spent = budget.spent,
                        limit = budget.amount,
                        message = "⚠️ Alert: Exceeded budget for '$categoryName'! Spent $spent of $limit ($percentage%)."
                    )
                )
            } else if (percentage >= 85) {
                alerts.add(
                    BudgetAlert(
                        categoryId = budget.categoryId,
                        categoryName = categoryName,
                        isExceeded = false,
                        percentage = percentage,
                        spent = budget.spent,
                        limit = budget.amount,
                        message = "⚠️ Warning: Approaching budget limit for '$categoryName'. Used $percentage% ($spent of $limit)."
                    )
                )
            }
        }

        return alerts
    }
}
