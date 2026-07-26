package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaxCategoryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_tax_category).apply {
                setOnClickPendingIntent(R.id.widget_tax_container, pendingIntent)
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val categories = db.taxCategoryDao().getAllTaxCategoriesStatic()
                    val expenses = db.expenseDao().getAllExpensesStatic()

                    val cal = java.util.Calendar.getInstance()
                    val curYear = cal.get(java.util.Calendar.YEAR)
                    val curMonth = cal.get(java.util.Calendar.MONTH)

                    val monthExps = expenses.filter { exp ->
                        val expCal = java.util.Calendar.getInstance().apply { timeInMillis = exp.date }
                        expCal.get(java.util.Calendar.YEAR) == curYear && expCal.get(java.util.Calendar.MONTH) == curMonth
                    }

                    if (categories.isNotEmpty()) {
                        val cat1 = categories[0]
                        val spent1 = monthExps.filter { it.taxCategoryId == cat1.id }.sumOf { it.amount }
                        val ratio1 = if (cat1.monthlyCap > 0) (spent1 / cat1.monthlyCap * 100).toInt().coerceIn(0, 100) else 0

                        views.setTextViewText(R.id.widget_cat1_title, "${cat1.name}: $spent1 / ${cat1.monthlyCap.toInt()}")
                        views.setProgressBar(R.id.widget_cat1_progress, 100, ratio1, false)

                        if (categories.size > 1) {
                            val cat2 = categories[1]
                            val spent2 = monthExps.filter { it.taxCategoryId == cat2.id }.sumOf { it.amount }
                            val ratio2 = if (cat2.monthlyCap > 0) (spent2 / cat2.monthlyCap * 100).toInt().coerceIn(0, 100) else 0

                            views.setTextViewText(R.id.widget_cat2_title, "${cat2.name}: $spent2 / ${cat2.monthlyCap.toInt()}")
                            views.setProgressBar(R.id.widget_cat2_progress, 100, ratio2, false)
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
