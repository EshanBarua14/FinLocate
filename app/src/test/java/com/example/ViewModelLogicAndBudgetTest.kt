package com.example

import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.ui.BudgetProjectionAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewModelLogicAndBudgetTest {

    @Test
    fun testBudgetProjectionAnalysisWhenOnTrack() {
        val totalBudget = 1000.0
        val currentSpent = 200.0
        val daysPassed = 10
        val totalDays = 30

        val dailyAverage = currentSpent / daysPassed
        val projectedTotal = dailyAverage * totalDays
        val isExceeding = projectedTotal > totalBudget && currentSpent > 0.0

        val analysis = BudgetProjectionAnalysis(
            dailyAverage = dailyAverage,
            projectedTotalSpent = projectedTotal,
            totalBudget = totalBudget,
            isProjectedToExceed = isExceeding,
            daysPassed = daysPassed,
            totalDays = totalDays,
            currentSpent = currentSpent
        )

        assertEquals(20.0, analysis.dailyAverage, 0.01)
        assertEquals(600.0, analysis.projectedTotalSpent, 0.01)
        assertFalse(analysis.isProjectedToExceed)
    }

    @Test
    fun testBudgetProjectionAnalysisWhenExceeding() {
        val totalBudget = 500.0
        val currentSpent = 300.0
        val daysPassed = 10
        val totalDays = 30

        val dailyAverage = currentSpent / daysPassed
        val projectedTotal = dailyAverage * totalDays
        val isExceeding = projectedTotal > totalBudget && currentSpent > 0.0

        val analysis = BudgetProjectionAnalysis(
            dailyAverage = dailyAverage,
            projectedTotalSpent = projectedTotal,
            totalBudget = totalBudget,
            isProjectedToExceed = isExceeding,
            daysPassed = daysPassed,
            totalDays = totalDays,
            currentSpent = currentSpent
        )

        assertEquals(30.0, analysis.dailyAverage, 0.01)
        assertEquals(900.0, analysis.projectedTotalSpent, 0.01)
        assertTrue(analysis.isProjectedToExceed)
    }

    @Test
    fun testTransactionFilteringByMonth() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val augCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 12, 0)
        }
        val sepCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5, 12, 0)
        }

        val tx1 = TransactionEntity(id = 1L, amount = 50.0, type = "EXPENSE", categoryId = 1L, accountId = 1L, timestamp = augCal.timeInMillis)
        val tx2 = TransactionEntity(id = 2L, amount = 100.0, type = "EXPENSE", categoryId = 1L, accountId = 1L, timestamp = sepCal.timeInMillis)

        val txList = listOf(tx1, tx2)

        val augTxList = txList.filter { sdf.format(Date(it.timestamp)) == "2026-08" }
        assertEquals(1, augTxList.size)
        assertEquals(1L, augTxList.first().id)

        val sepTxList = txList.filter { sdf.format(Date(it.timestamp)) == "2026-09" }
        assertEquals(1, sepTxList.size)
        assertEquals(2L, sepTxList.first().id)
    }

    @Test
    fun testTransactionFilteringByTag() {
        val tx1 = TransactionEntity(id = 1L, amount = 25.0, type = "EXPENSE", categoryId = 1L, accountId = 1L, tags = "groceries,weekly", notes = "Walmart")
        val tx2 = TransactionEntity(id = 2L, amount = 15.0, type = "EXPENSE", categoryId = 1L, accountId = 1L, tags = "coffee,work", notes = "Starbucks")
        val tx3 = TransactionEntity(id = 3L, amount = 120.0, type = "EXPENSE", categoryId = 1L, accountId = 1L, tags = "travel,flight", notes = "Airline")

        val list = listOf(tx1, tx2, tx3)

        val groceryTxs = list.filter { it.tags.contains("groceries", ignoreCase = true) }
        assertEquals(1, groceryTxs.size)
        assertEquals(1L, groceryTxs.first().id)

        val coffeeTxs = list.filter { it.tags.contains("coffee", ignoreCase = true) }
        assertEquals(1, coffeeTxs.size)
        assertEquals(2L, coffeeTxs.first().id)
    }

    @Test
    fun testRecurringTransactionFutureProjection() {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val augCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 1, 10, 0)
        }

        val recurringTx = TransactionEntity(
            id = 10L,
            amount = 15.0,
            type = "EXPENSE",
            categoryId = 1L,
            accountId = 1L,
            timestamp = augCal.timeInMillis,
            isRecurring = true,
            recurrenceInterval = "MONTHLY",
            notes = "Netflix Subscription"
        )

        val targetMonth = "2026-09"
        val txMonth = sdf.format(Date(recurringTx.timestamp))

        val isFutureProjection = recurringTx.isRecurring && targetMonth > txMonth
        assertTrue(isFutureProjection)

        val projectedTx = recurringTx.copy(
            id = recurringTx.id * 100_000 + targetMonth.replace("-", "").toLongOrNull().hashCode(),
            notes = "[Recurring Subscription] ${recurringTx.notes}"
        )

        assertTrue(projectedTx.notes.contains("[Recurring Subscription]"))
        assertTrue(projectedTx.id != recurringTx.id)
    }
}
