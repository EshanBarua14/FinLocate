package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class QuickExpenseWidgetProvider : AppWidgetProvider() {

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
            val prefs = context.getSharedPreferences("wealthflow_widget_prefs", Context.MODE_PRIVATE)
            val spending = prefs.getFloat("current_month_spending", 0f)
            val currencySymbol = prefs.getString("currency_symbol", "$") ?: "$"
            val formattedSpending = "This Month: $currencySymbol${String.format("%.2f", spending)}"

            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_QUICK_ADD_EXPENSE"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.quick_expense_widget).apply {
                setTextViewText(R.id.widget_spending_text, formattedSpending)
                setOnClickPendingIntent(R.id.widget_quick_add_btn, pendingIntent)
                setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateSpending(context: Context, amount: Double, currencySymbol: String = "$") {
            val prefs = context.getSharedPreferences("wealthflow_widget_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("current_month_spending", amount.toFloat())
                .putString("currency_symbol", currencySymbol)
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, QuickExpenseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
