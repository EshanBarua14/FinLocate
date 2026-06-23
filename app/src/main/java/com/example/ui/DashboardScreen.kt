package com.example.ui

import kotlinx.coroutines.launch
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import java.util.Calendar
import java.util.Date
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.InsightEntity
import com.example.data.model.TransactionEntity
import com.example.data.service.AnomalyReport
import com.example.data.service.AnomalyType
import com.example.ui.theme.*
import kotlin.math.min
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardContext = androidx.compose.ui.platform.LocalContext.current
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showControlCenter by remember { mutableStateOf(false) }
    var showYearlyReport by remember { mutableStateOf(false) }
    val isDark by viewModel.isDarkTheme.collectAsState()
    val showBackupReminder by viewModel.showBackupReminder.collectAsState()
    val txCountSinceLastExport by viewModel.txCountSinceLastExport.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkBackupReminder()
    }
    val totalBalance by viewModel.totalBalance.collectAsState()
    val inflow by viewModel.currentInflow.collectAsState()
    val outflow by viewModel.currentOutflow.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val rawTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val insights by viewModel.smartInsights.collectAsState()
    val burnRateText by viewModel.burnRateAndRunway.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthToMonthSpending by viewModel.monthToMonthSpending.collectAsState()
    val countryConfig by viewModel.activeCountryConfig.collectAsState()
    val budgetProjection by viewModel.budgetProjection.collectAsState()
    val netWorthSummary by viewModel.netWorthSummary.collectAsState()
    val matchingRules by viewModel.matchingRules.collectAsState()
    val anomalies by viewModel.detectedAnomalies.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("quick_add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Quick Add Expense")
            }
        },
        containerColor = Color.Transparent
    ) { padValue ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValue)
                .testTag("dashboard_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showBackupReminder) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_reminder_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alert",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "BACKUP REMINDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You have added $txCountSinceLastExport unbacked-up expense ledger modifications. Export your financial records to local database backups for offline data security.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.exportReportToCsv(dashboardContext) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).testTag("backup_export_csv_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportReportToPdf(dashboardContext) },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).testTag("backup_export_pdf_btn")
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 1. COMMAND INDEX BANNER ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("command_banner_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "TOTAL NET WORTH",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatCurrency(netWorthSummary.totalNetWorth),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("total_balance_text")
                            )
                        }
                        IconButton(
                            onClick = { viewModel.syncAllActiveAccounts() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(48.dp)
                                .testTag("quick_refresh_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync All Bank Gateway Accounts",
                                tint = MaterialTheme.colorScheme.background
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Manual entries",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "MANUAL CASH",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatCurrency(netWorthSummary.manualNetInLocalCurrency),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("manual_net_worth_text")
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Synced accounts",
                                        tint = FintechGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SYNCED BANK/MFS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatCurrency(netWorthSummary.syncedNetInLocalCurrency),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("synced_net_worth_text")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // INFLOW vs OUTFLOW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Inflow",
                                tint = FintechGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "INFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = viewModel.formatCurrency(inflow),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FintechGreen
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = "Outflow",
                                tint = ExpenseRose,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "OUTFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = viewModel.formatCurrency(outflow),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRose
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 1.12. YEARLY FISCAL TRENDS & ANNUAL REPORT ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showYearlyReport = true }
                    .testTag("dashboard_yearly_report_button_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Yearly Report Icon",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Column {
                            Text(
                                "Annual Fiscal Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Aggregate monthly spend trends & categories breakdown",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open report",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 1.25. ANOMALY DETECTION AND FRAUD WARNINGS ---
        if (anomalies.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anomaly_alert_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SUSPICIOUS PATTERN DEVIATIONS (${anomalies.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WealthFlow real-time risk algorithms have flagged the following potential duplicate records or transaction outliers for prompt review:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            anomalies.take(3).forEach { report ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = report.transaction.merchant.ifEmpty { "Indirect Ledger Entry" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = viewModel.formatCurrency(report.transaction.amount),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = report.description,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            lineHeight = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Reliability Confidence: ${(report.confidence * 100).toInt()}% • Group: ${report.type.name.replace("_", " ")}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            if (anomalies.size > 3) {
                                Text(
                                    text = "+ ${anomalies.size - 3} additional suspicious records audited.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 1.5. SMART SPENDING PROJECTION AND VELOCITY ALERT ---
        if (budgetProjection.totalBudget > 0.0) {
            item {
                val isWarning = budgetProjection.isProjectedToExceed
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWarning) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isWarning) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_projection_trend_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = "Trend Alert Symbol",
                                tint = if (isWarning) MaterialTheme.colorScheme.error else FintechGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWarning) "BUDGET OVERRUN ALERT (TREND-PROJECTED)" else "SAFE VELOCITY PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWarning) MaterialTheme.colorScheme.error else FintechGreen,
                                letterSpacing = 0.8.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Daily Spend Avg",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = viewModel.formatCurrency(budgetProjection.dailyAverage),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Projected Month-End Finish",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${viewModel.formatCurrency(budgetProjection.projectedTotalSpent)} / ${viewModel.formatCurrency(budgetProjection.totalBudget)} limit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                val ratio = (budgetProjection.projectedTotalSpent / budgetProjection.totalBudget).toFloat()
                                ratio.coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isWarning) MaterialTheme.colorScheme.error else FintechGreen,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isWarning) {
                                "⚠️ Warning: Based on your average of ${viewModel.formatCurrency(budgetProjection.dailyAverage)} over ${budgetProjection.daysPassed} days, you are projected to exceed your current budget. We recommend immediate restraint."
                            } else {
                                "✅ Dynamic Trend Safe: Spending is well regulated (~${viewModel.formatCurrency(budgetProjection.dailyAverage)} daily). If speed is maintained, you will finish within limits."
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // --- 2. RUNWAY BURN RATE WIDGET ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "Runway Alert",
                        tint = AccentGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "FINANCIAL RUNWAY STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = burnRateText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // --- 2.2 CATEGORY BUDGET PROGRESS GAUGE ---
        item {
            val activeBudgetsSnapshot by viewModel.activeBudgets.collectAsState()
            val themeCategories by viewModel.categories.collectAsState()
            
            if (activeBudgetsSnapshot.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_budget_progress_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CATEGORY BUDGET PROGRESS GAUGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeBudgetsSnapshot.forEach { bud ->
                                val cat = themeCategories.find { it.id == bud.categoryId }
                                val pct = if (bud.amount > 0.0) (bud.spent / bud.amount).toFloat().coerceIn(0f, 1f) else 0f
                                val barColor = when {
                                    pct >= 1.0f -> ExpenseRose
                                    pct >= 0.85f -> AccentGold
                                    else -> FintechGreen
                                }
                                
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat?.name ?: "Category Limit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${viewModel.formatCurrency(bud.spent)} / ${viewModel.formatCurrency(bud.amount)} (${(pct * 100).toInt()}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (pct >= 1.0f) ExpenseRose else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ESTABLISH SPENDING CAPS PER CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No spending limits set this month. Use the Category Limits page to customize caps & track overruns.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- 2.5 DEDICATED GEMINI AI SUMMARY & SPENDING ADVICE CARD ---
        item {
            val aiReport by viewModel.aiReport.collectAsState()
            val aiLoading by viewModel.aiInsightsLoading.collectAsState()
            val activeCountryConfig by viewModel.activeCountryConfig.collectAsState()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_ai_summary_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini Spark",
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI ADVISOR INSIGHTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.triggerGeminiEvaluation() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("gemini_recompute_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload predictions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (aiLoading) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "Strategizing tax-saving advice tailored for ${activeCountryConfig.country}...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else if (aiReport != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RenderMarkdownStructuredText(markdown = aiReport!!)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Get personalized, instant budget optimization tips and legal tax deductions based on ${activeCountryConfig.country} guidelines.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.triggerGeminiEvaluation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("gemini_card_trigger_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "RUN GEMINI FINANCIAL AUDIT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. ACCOUNTS SNAPSHOTS (HORIZONTAL SCROLLER SIMULATOR) ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNTS & MOBILE WALLETS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { showControlCenter = true },
                        modifier = Modifier.size(24.dp).testTag("accounts_control_panel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Inter-App Bank Controller",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (accounts.isEmpty()) {
                    Text(
                        text = "No accounts configured. Click settings gear icon to link banks & mobile wallets.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        accounts.take(3).forEach { account ->
                            AccountCard(
                                account = account,
                                formatter = { viewModel.formatCurrency(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- 3B. LINKED INTEGRATION MODULES (ACTIVE CHANNELS) ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("linked_accounts_dashboard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LINKED BANK & MFS SERVICE CHANNELS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.syncAllActiveAccounts() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("dashboard_bulk_sync_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FintechGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BULK SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val integrations = accounts.filter { it.type == "BANK" || it.type == "MOBILE_WALLET" }
                    if (integrations.isEmpty()) {
                        Text(
                            text = "No linked external banking/MFS gateways active. Configure connections inside the setup gear panel.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            integrations.forEach { account ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (account.type == "BANK") Icons.Default.AccountBalance else Icons.Default.PhonelinkRing,
                                                        contentDescription = null,
                                                        tint = Color(android.graphics.Color.parseColor(account.accountColorHex)),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = account.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${account.provider} Gateway • ${if (account.isSyncEnabled) "ACTIVE SYNCHRONIZATION" else "LOCAL ACCOUNT ONLY"}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (account.isSyncEnabled) FintechGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }

                                            Text(
                                                text = viewModel.formatCurrency(account.balance),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.testTag("linked_acc_balance_${account.id}")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = account.isSyncEnabled,
                                                    onCheckedChange = { viewModel.toggleAccountSync(account) },
                                                    modifier = Modifier.size(24.dp).testTag("sync_toggle_chk_${account.id}")
                                                )
                                                Text(
                                                    text = "Automated API Syncing",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.syncAccountInstance(account) },
                                                enabled = account.isSyncEnabled,
                                                modifier = Modifier
                                                    .background(
                                                        if (account.isSyncEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                        CircleShape
                                                    )
                                                    .size(32.dp)
                                                    .testTag("api_sync_refresh_btn_${account.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Sync transaction logs",
                                                    tint = if (account.isSyncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. DATA VISUALIZATION CHART ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spending_chart_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var chartSelectionState by remember { mutableIntStateOf(0) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VISUAL ANALYSIS PLATFORM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Three-Way Selector
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val choices = listOf("Native Pie", "D3 Pie", "Recharts Trend")
                        choices.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = chartSelectionState == index,
                                onClick = { chartSelectionState = index },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                                modifier = Modifier.testTag("chart_btn_$index")
                            ) {
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val expenseTransactions = rawTransactions.filter { it.type == "EXPENSE" }
                    if (expenseTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(130.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Analytics,
                                    contentDescription = "No Expenses",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No expenses recorded this month.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        // Math for categorization percentage
                        val totalExpense = expenseTransactions.sumOf { it.amount }
                        val categoriesGrouped = expenseTransactions.groupBy { it.categoryId }
                        val sortedCategories = categoriesGrouped.map { (catId, txs) ->
                            val cat = categories.find { it.id == catId }
                            val catName = cat?.name ?: "Other"
                            val amount = txs.sumOf { it.amount }
                            val pct = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                            CategoryShare(catName, amount, pct)
                        }.sortedByDescending { it.amount }

                        if (chartSelectionState == 2) {
                            // Recharts custom line area & bar graphs
                            val calendar = java.util.Calendar.getInstance()
                            val activeYearMonth = selectedMonth // "YYYY-MM"
                            val daysInMonth = try {
                                val parts = activeYearMonth.split("-")
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.YEAR, parts[0].toInt())
                                cal.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                                cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                            } catch (e: Exception) {
                                30
                            }

                            val dayDailyTotals = DoubleArray(daysInMonth + 1)
                            expenseTransactions.forEach { tx ->
                                calendar.timeInMillis = tx.timestamp
                                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                if (day in 1..daysInMonth) {
                                    dayDailyTotals[day] += tx.amount
                                }
                            }

                            var runSum = 0.0
                            val trendJson = (1..daysInMonth).joinToString(",") { day ->
                                runSum += dayDailyTotals[day]
                                "{\"day\": $day, \"Spent\": $runSum}"
                            }

                            val activeBudgetsSnapshot by viewModel.activeBudgets.collectAsState()
                            val budgetBarJson = activeBudgetsSnapshot.joinToString(",") { bud ->
                                val catName = categories.find { it.id == bud.categoryId }?.name ?: "Unknown"
                                "{\"category\": \"${catName.replace("\"", "\\\"")}\", \"Limit\": ${bud.amount}, \"Spent\": ${bud.spent}}"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(750.dp)
                            ) {
                                RechartsTrendWebView(
                                    trendDataJson = trendJson,
                                    budgetBarJson = budgetBarJson,
                                    monthToMonthDataJson = monthToMonthSpending,
                                    currencySymbol = countryConfig.currencySymbol,
                                    isDark = isDark,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Circular Canvas chart
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = selectedMonth,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.85f)) togetherWith
                                                    (fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 0.85f))
                                        },
                                        label = "chart_month_transition"
                                    ) { targetMonth ->
                                        if (chartSelectionState == 1) {
                                            D3ChartWebView(
                                                shares = sortedCategories,
                                                currencySymbol = countryConfig.currencySymbol,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            PieChartContainer(
                                                shares = sortedCategories,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                // Breakdown Indicators
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    sortedCategories.take(4).forEachIndexed { index, share ->
                                        val col = getStaticChartColor(index)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(col)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = share.categoryName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = String.format(java.util.Locale.US, "%.0f%%", share.fraction * 100f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. ALERTS AND COMPLIANCE FEED ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE ALERTS & ADVISORIES",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                if (insights.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { viewModel.clearNotificationFeedStr() }
                            .padding(4.dp)
                    )
                }
            }
        }

        if (insights.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Alerts Clear",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No warning flags detected. Financial compliance is optimal.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            items(insights.take(3), key = { it.id }) { alert ->
                AlertItemCard(alert = alert, onDismiss = { viewModel.dismissSingleInsight(alert.id) })
            }
        }
    }
    }

    if (showQuickAddDialog) {
        QuickAddExpenseDialog(
            viewModel = viewModel,
            onDismiss = { showQuickAddDialog = false }
        )
    }

    if (showYearlyReport) {
        YearlyReportDialog(
            viewModel = viewModel,
            onDismissRequest = { showYearlyReport = false }
        )
    }

    if (showControlCenter) {
        AlertDialog(
            onDismissRequest = { showControlCenter = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Inter-App Bank link icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("BANK & MFS CONTROL CENTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                var selectedTab by remember { mutableStateOf(0) } // 0: Link, 1: Control
                var name by remember { mutableStateOf("") }
                var balanceStr by remember { mutableStateOf("") }
                var accountType by remember { mutableStateOf("BANK") }
                var provider by remember { mutableStateOf("Chase") }
                var expandedProvider by remember { mutableStateOf(false) }
                
                var activeAuditLog by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
                var activeAuditLoading by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Link App", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("App Controller", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Auto-Match Rules", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                    
                    if (selectedTab == 0) {
                        Text("Connect External Bank or Mobile Financial Service (MFS)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("App Label Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("link_acc_name_input")
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("BANK", "MOBILE_WALLET").forEach { type ->
                                FilterChip(
                                    selected = accountType == type,
                                    onClick = { 
                                        accountType = type
                                        provider = if (type == "BANK") "Chase" else "bKash"
                                    },
                                    label = { Text(if (type == "BANK") "🏦 BANK APP" else "📱 MFS WALLET", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        val providerList = if (accountType == "BANK") {
                            listOf("Chase", "Bank of America", "HSBC", "Citi", "State Bank of India", "Barclays")
                        } else {
                            listOf("bKash", "SSL Wallet", "Nagad", "Rocket", "Upay")
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedProvider = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Provider API Gateway: $provider", fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedProvider,
                                onDismissRequest = { expandedProvider = false }
                            ) {
                                providerList.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov, fontSize = 11.sp) },
                                        onClick = {
                                            provider = prov
                                            expandedProvider = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = balanceStr,
                            onValueChange = { balanceStr = it },
                            label = { Text("Starter Connection Balance", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("link_acc_balance_input")
                        )
                        
                        Button(
                            onClick = {
                                val bal = balanceStr.toDoubleOrNull() ?: 0.0
                                if (name.isNotEmpty()) {
                                    viewModel.addAccount(
                                        name = name,
                                        type = accountType,
                                        startingBalance = bal,
                                        provider = provider,
                                        colorHex = if (accountType == "BANK") "#0D9488" else "#DB2777"
                                    )
                                    android.widget.Toast.makeText(context, "$provider app integrated!", android.widget.Toast.LENGTH_SHORT).show()
                                    name = ""
                                    balanceStr = ""
                                    selectedTab = 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("link_institution_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LINK SECURE APP INSTANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (selectedTab == 1) {
                        if (accounts.isEmpty()) {
                            Text("No linked apps. Link accounts in first tab.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 240.dp).fillMaxWidth()
                            ) {
                                items(accounts.size) { index ->
                                    val acc = accounts[index]
                                    val isChecking = activeAuditLoading[acc.id] ?: false
                                    val auditLog = activeAuditLog[acc.id] ?: "API Idle. Secure token registered."
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${acc.provider} [${acc.type}]", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(viewModel.formatCurrency(acc.balance), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            }
                                            
                                            Text(
                                                text = "Log: $auditLog",
                                                fontSize = 9.sp,
                                                color = if (isChecking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        activeAuditLoading = activeAuditLoading + (acc.id to true)
                                                        activeAuditLog = activeAuditLog + (acc.id to "Handshaking secure API Plaid credentials...")
                                                        coroutineScope.launch {
                                                            kotlinx.coroutines.delay(1000)
                                                            activeAuditLoading = activeAuditLoading + (acc.id to false)
                                                            activeAuditLog = activeAuditLog + (acc.id to "Success! Plaid Visa token verified. Balance synchronized.")
                                                            android.widget.Toast.makeText(context, "${acc.name} state synced!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    enabled = !isChecking,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("SYNC API", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        activeAuditLoading = activeAuditLoading + (acc.id to true)
                                                        activeAuditLog = activeAuditLog + (acc.id to "Validating routing, SSL handshake, compliance encryption...")
                                                        coroutineScope.launch {
                                                            kotlinx.coroutines.delay(1200)
                                                            activeAuditLoading = activeAuditLoading + (acc.id to false)
                                                            activeAuditLog = activeAuditLog + (acc.id to "Cleared: Encryption block secure (SHA-256 routing verified).")
                                                            android.widget.Toast.makeText(context, "${acc.name} deep audit completed!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    enabled = !isChecking,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = FintechGreen.copy(alpha = 0.12f),
                                                        contentColor = FintechGreen
                                                    ),
                                                    modifier = Modifier.weight(1.1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("AUDIT", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        activeAuditLog = activeAuditLog + (acc.id to "Deep-link intent generated. Launcher redirected safely.")
                                                        android.widget.Toast.makeText(context, "Launched external secure app!", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    enabled = !isChecking,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("LAUNCH", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 2: Custom auto match rule mapping controls!
                        var ruleKeyword by remember { mutableStateOf("") }
                        var ruleCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
                        var isTaxDeductible by remember { mutableStateOf(false) }
                        var taxRateStr by remember { mutableStateOf("8.25") }
                        var expandedCatDropdown by remember { mutableStateOf(false) }

                        Text("Configure patterns to auto-assign category & tax tags", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = ruleKeyword,
                            onValueChange = { ruleKeyword = it },
                            label = { Text("Keyword Pattern (e.g., Walmart)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("rule_keyword_input")
                        )

                        // Category Dropdown selector
                        val matchedCat = categories.find { it.id == ruleCategoryId } ?: categories.firstOrNull()
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedCatDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mapped Category: ${matchedCat?.name ?: "Select Box"}", fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedCatDropdown,
                                onDismissRequest = { expandedCatDropdown = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name, fontSize = 11.sp) },
                                        onClick = {
                                            ruleCategoryId = cat.id
                                            expandedCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isTaxDeductible,
                                    onCheckedChange = { isTaxDeductible = it },
                                    modifier = Modifier.size(32.dp).testTag("rule_tax_checkbox")
                                )
                                Text("Mark Tax Deductible", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                            }

                            if (isTaxDeductible) {
                                OutlinedTextField(
                                    value = taxRateStr,
                                    onValueChange = { taxRateStr = it },
                                    label = { Text("Tax Rate %", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp).testTag("rule_tax_rate_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (ruleKeyword.isNotEmpty()) {
                                    val rateVal = taxRateStr.toDoubleOrNull() ?: 0.0
                                    viewModel.addMatchingRule(ruleKeyword, ruleCategoryId, isTaxDeductible, rateVal)
                                    ruleKeyword = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_matching_rule_btn")
                        ) {
                            Text("SAVE AUTO-MATCHING RULE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        Text("ACTIVE MAPPING TEMPLATES (${matchingRules.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth()
                        ) {
                            items(matchingRules.size) { index ->
                                val r = matchingRules[index]
                                val rCatName = categories.find { it.id == r.categoryId }?.name ?: "Unknown"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("\"${r.keyword}\" ➜ $rCatName", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(
                                            text = if (r.isTaxDeductible) "Tax Deductible (${r.taxRate}%)" else "Non-deductible Ledger",
                                            fontSize = 9.sp,
                                            color = if (r.isTaxDeductible) FintechGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMatchingRule(r) },
                                        modifier = Modifier.size(24.dp).testTag("delete_rule_btn_${r.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete mapping pattern Rule",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showControlCenter = false }) {
                    Text("Close", fontSize = 11.sp)
                }
            }
        )
    }

    val showRatePrompt by viewModel.showRatePrompt.collectAsState()
    if (showRatePrompt) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.dismissRatePrompt() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Exchange rates update prompt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Currency Rates")
                }
            },
            text = {
                Text(
                    text = "Would you like to update your currency rates using the latest online prices (USD base currency) to guarantee absolute multi-currency portfolio accuracy?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                var loading by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        loading = true
                        viewModel.triggerExchangeRatesFetch(
                            onSuccess = { msg ->
                                loading = false
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            },
                            onFailure = { err ->
                                loading = false
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.testTag("rate_prompt_confirm_btn")
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissRatePrompt() },
                    modifier = Modifier.testTag("rate_prompt_dismiss_btn")
                ) {
                    Text("Skip")
                }
            }
        )
    }
}

data class CategoryShare(
    val categoryName: String,
    val amount: Double,
    val fraction: Float
)

@Composable
fun PieChartContainer(
    shares: List<CategoryShare>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        var startAngle = 0f
        shares.forEachIndexed { index, share ->
            val sweepAngle = share.fraction * 360f
            val color = getStaticChartColor(index)
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

fun getStaticChartColor(index: Int): Color {
    val list = listOf(
        FintechGreen,
        Color(0xFF3B82F6), // Ocean Blue
        AccentGold,        // Canary Gold
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF43F5E), // Rose Coral
        Color(0xFF06B6D4)  // Cyan
    )
    return list[index % list.size]
}

@Composable
fun AccountCard(
    account: AccountEntity,
    formatter: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(android.graphics.Color.parseColor(account.accountColorHex)).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Color(android.graphics.Color.parseColor(account.accountColorHex)).copy(alpha = 0.15f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = account.type,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(account.accountColorHex))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = account.name,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = account.provider,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatter(account.balance),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (account.balance >= 0) FintechGreen else ExpenseRose
            )
        }
    }
}

@Composable
fun AlertItemCard(
    alert: InsightEntity,
    onDismiss: () -> Unit
) {
    val containerCol = when (alert.severity) {
        "ALERT" -> ExpenseRose.copy(alpha = 0.12f)
        "WARNING" -> AccentGold.copy(alpha = 0.12f)
        "SUCCESS" -> FintechGreen.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }

    val iconColor = when (alert.severity) {
        "ALERT" -> ExpenseRose
        "WARNING" -> AccentGold
        "SUCCESS" -> FintechGreen
        else -> MaterialTheme.colorScheme.primary
    }

    val icon = when (alert.severity) {
        "ALERT" -> Icons.Default.Dangerous
        "WARNING" -> Icons.Default.Warning
        "SUCCESS" -> Icons.Default.CheckCircle
        else -> Icons.Default.Lightbulb
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerCol),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alert_item_${alert.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = alert.severity,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = alert.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = alert.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Notification",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun D3ChartWebView(
    shares: List<CategoryShare>,
    currencySymbol: String,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rawJson = shares.joinToString(",") { share ->
        "{\"name\":\"${share.categoryName.replace("\"", "\\\"")}\", \"value\": ${share.amount}}"
    }

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body {
                    background-color: ${if (isDark) "#141B2D" else "#FFFFFF"};
                    color: ${if (isDark) "#F8FAFC" else "#1F2937"};
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    margin: 0;
                    padding: 0;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    overflow: hidden;
                    width: 100vw;
                    height: 100vh;
                }
                #chart-container {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    width: 100%;
                    height: 100%;
                }
                text {
                    font-family: inherit;
                }
                path {
                    cursor: pointer;
                }
            </style>
            <script src="https://d3js.org/d3.v7.min.js"></script>
        </head>
        <body>
            <div id="chart-container"></div>
            <script>
                const data = [ $rawJson ];
                const width = 120;
                const height = 120;
                const radius = Math.min(width, height) / 2 - 4;

                const svg = d3.select("#chart-container")
                    .append("svg")
                    .attr("width", width)
                    .attr("height", height)
                    .append("g")
                    .attr("transform", "translate(" + (width / 2) + "," + (height / 2) + ")");

                const color = d3.scaleOrdinal()
                    .domain(data.map(d => d.name))
                    .range(["#10B981", "#3B82F6", "#F59E0B", "#8B5CF6", "#F43F5E", "#06B6D4"]);

                const pie = d3.pie()
                    .value(d => d.value)
                    .sort(null);
                    
                const data_ready = pie(data);

                const arc = d3.arc()
                    .innerRadius(radius * 0.55)
                    .outerRadius(radius * 0.95);

                svg.selectAll('slices')
                    .data(data_ready)
                    .join('path')
                    .attr('fill', d => color(d.data.name))
                    .attr("stroke", "${if (isDark) "#141B2D" else "#FFFFFF"}")
                    .style("stroke-width", "1.5px")
                    .transition()
                    .duration(800)
                    .attrTween('d', function(d) {
                        var i = d3.interpolate({startAngle: 0, endAngle: 0}, d);
                        return function(t) { return arc(i(t)); };
                    });

                // total values static label
                const totalVal = d3.sum(data, d => d.value);

                svg.append("text")
                    .attr("text-anchor", "middle")
                    .attr("dy", "-0.1em")
                    .style("font-size", "8px")
                    .style("font-weight", "600")
                    .style("fill", "${if (isDark) "#94A3B8" else "#6B7280"}")
                    .text("D3 VALUE");

                svg.append("text")
                    .attr("text-anchor", "middle")
                    .attr("dy", "1.0em")
                    .style("font-size", "9px")
                    .style("font-weight", "800")
                    .style("fill", "#10B981")
                    .text("$currencySymbol" + Math.round(totalVal));
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = android.webkit.WebViewClient()
                setBackgroundColor(android.graphics.Color.parseColor(if (isDark) "#141B2D" else "#FFFFFF"))
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://local.d3", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

@Composable
fun RechartsTrendWebView(
    trendDataJson: String,
    budgetBarJson: String,
    monthToMonthDataJson: String,
    currencySymbol: String,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body {
                    background-color: ${if (isDark) "#141B2D" else "#FFFFFF"};
                    color: ${if (isDark) "#F8FAFC" else "#1F2937"};
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                    margin: 0;
                    padding: 8px 12px 16px 12px;
                    box-sizing: border-box;
                    width: 100vw;
                    height: 100vh;
                    overflow-x: hidden;
                    overflow-y: auto;
                }
                .chart-section {
                    margin-bottom: 24px;
                    width: 100%;
                }
                h3 {
                    font-size: 11px;
                    font-weight: 800;
                    color: ${if (isDark) "#94A3B8" else "#4B5563"};
                    letter-spacing: 1px;
                    margin-top: 0;
                    margin-bottom: 12px;
                    text-transform: uppercase;
                }
                .canvas-holder {
                    position: relative;
                    width: 100%;
                    height: 155px;
                }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
        </head>
        <body>
            <div class="chart-section">
                <h3>📈 CUMULATIVE MONTH SPENDING TREND</h3>
                <div class="canvas-holder">
                    <canvas id="trendChart"></canvas>
                </div>
            </div>
            
            <div class="chart-section">
                <h3>📊 CATEGORIES VS BUDGET LIMITS</h3>
                <div class="canvas-holder" style="height: 165px;">
                    <canvas id="budgetChart"></canvas>
                </div>
            </div>

            <div class="chart-section">
                <h3>📅 MONTH-TO-MONTH SPENDING TREND</h3>
                <div class="canvas-holder" style="height: 165px;">
                    <canvas id="monthlyTrendChart"></canvas>
                </div>
            </div>

            <div class="chart-section" style="margin-bottom: 0;">
                <h3>🍰 CATEGORY EXPENSE BREAKDOWN</h3>
                <div class="canvas-holder" style="height: 180px;">
                    <canvas id="categoryPieChart"></canvas>
                </div>
            </div>
 
            <script>
                // --- TREND LINE DATA ---
                const trendRaw = [ $trendDataJson ];
                const trendLabels = trendRaw.map(d => "Day " + d.day);
                const trendValues = trendRaw.map(d => d.Spent);
 
                // --- BUDGETS BAR DATA ---
                const budgetRaw = [ $budgetBarJson ];
                const budgetLabels = budgetRaw.map(d => d.category);
                const budgetLimits = budgetRaw.map(d => d.Limit);
                const budgetSpents = budgetRaw.map(d => d.Spent);

                // --- MONTH-TO-MONTH TREND DATA ---
                const monthlyRaw = [ $monthToMonthDataJson ];
                const monthlyLabels = monthlyRaw.map(d => d.month);
                const monthlyValues = monthlyRaw.map(d => d.Spent);
 
                // Set up Line Chart
                const trendCtx = document.getElementById('trendChart').getContext('2d');
                
                // Create gradient
                const trendGrad = trendCtx.createLinearGradient(0, 0, 0, 150);
                trendGrad.addColorStop(0, '${if (isDark) "rgba(16, 185, 129, 0.4)" else "rgba(5, 150, 105, 0.3)"}');
                trendGrad.addColorStop(1, 'rgba(16, 185, 129, 0.0)');
 
                new Chart(trendCtx, {
                    type: 'line',
                    data: {
                        labels: trendLabels,
                        datasets: [{
                            label: 'Total Spent',
                            data: trendValues,
                            borderColor: '${if (isDark) "#10B981" else "#059669"}',
                            borderWidth: 2,
                            backgroundColor: trendGrad,
                            fill: true,
                            tension: 0.4,
                            pointRadius: trendValues.length > 15 ? 0 : 3,
                            pointBackgroundColor: '${if (isDark) "#10B981" else "#059669"}'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            x: {
                                grid: { color: '${if (isDark) "#1E293B" else "#E2E8F0"}' },
                                ticks: { color: '${if (isDark) "#94A3B8" else "#4B5563"}', font: { size: 9 } }
                            },
                            y: {
                                grid: { color: '${if (isDark) "#1E293B" else "#E2E8F0"}' },
                                ticks: {
                                    color: '${if (isDark) "#94A3B8" else "#4B5563"}',
                                    font: { size: 9 },
                                    callback: function(value) { return '$currencySymbol' + value; }
                                }
                            }
                        }
                    }
                });
 
                // Set up Budget Bar Chart
                const budgetCtx = document.getElementById('budgetChart').getContext('2d');
                
                const backgroundColors = budgetSpents.map((spent, idx) => {
                    const limit = budgetLimits[idx];
                    return spent > limit ? '#F43F5E' : '${if (isDark) "#10B981" else "#059669"}';
                });
 
                new Chart(budgetCtx, {
                    type: 'bar',
                    data: {
                        labels: budgetLabels,
                        datasets: [
                            {
                                label: 'Budget Limit',
                                data: budgetLimits,
                                backgroundColor: '${if (isDark) "#3B82F6" else "#3B82F6"}',
                                borderRadius: 4,
                                barPercentage: 0.6,
                                categoryPercentage: 0.8
                            },
                            {
                                label: 'Actual Spent',
                                data: budgetSpents,
                                backgroundColor: backgroundColors,
                                borderRadius: 4,
                                barPercentage: 0.6,
                                categoryPercentage: 0.8
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                labels: { color: '${if (isDark) "#F8FAFC" else "#1F2937"}', font: { size: 10 } }
                            }
                        },
                        scales: {
                            x: {
                                grid: { color: '${if (isDark) "#1E293B" else "#E2E8F0"}' },
                                ticks: { color: '${if (isDark) "#94A3B8" else "#4B5563"}', font: { size: 9 } }
                            },
                            y: {
                                grid: { color: '${if (isDark) "#1E293B" else "#E2E8F0"}' },
                                ticks: {
                                    color: '${if (isDark) "#94A3B8" else "#4B5563"}',
                                    font: { size: 9 },
                                    callback: function(value) { return '$currencySymbol' + value; }
                                }
                            }
                        }
                    }
                });

                // Set up Month-to-Month Spending Trend Chart
                const monthlyCtx = document.getElementById('monthlyTrendChart').getContext('2d');
                new Chart(monthlyCtx, {
                    type: 'bar',
                    data: {
                        labels: monthlyLabels,
                        datasets: [{
                            label: 'Monthly Spending',
                            data: monthlyValues,
                            backgroundColor: '${if (isDark) "#FBBF24" else "#D97706"}',
                            borderRadius: 4,
                            barPercentage: 0.5
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            x: {
                                grid: { color: '${if (isDark) "#1E293B" else "#E2E8F0"}' },
                                ticks: { color: '${if (isDark) "#94A3B8" else "#4B5563"}', font: { size: 9 } }
                            },
                            y: {
                                grid: { color: '${if (isDark) "#1E293B" else "#E2E8F0"}' },
                                ticks: {
                                    color: '${if (isDark) "#94A3B8" else "#4B5563"}',
                                    font: { size: 9 },
                                    callback: function(value) { return '$currencySymbol' + value; }
                                }
                            }
                        }
                    }
                });
 
                // Set up Breakdown pie chart of only non-zero spent categories
                const pieCtx = document.getElementById('categoryPieChart').getContext('2d');
                const nonZeroBudgets = budgetRaw.filter(d => d.Spent > 0);
                const pieLabels = nonZeroBudgets.length > 0 ? nonZeroBudgets.map(d => d.category) : ["No Expenses"];
                const pieSpents = nonZeroBudgets.length > 0 ? nonZeroBudgets.map(d => d.Spent) : [0.01];
                const piePalette = ['#10B981', '#3B82F6', '#FBBF24', '#8B5CF6', '#EC4899', '#EF4444', '#14B8A6', '#F97316', '#22C55E', '#A855F7'];
 
                new Chart(pieCtx, {
                    type: 'doughnut',
                    data: {
                        labels: pieLabels,
                        datasets: [{
                            data: pieSpents,
                            backgroundColor: nonZeroBudgets.length > 0 ? piePalette : ['rgba(120, 120, 120, 0.15)'],
                            borderColor: '${if (isDark) "#141B2D" else "#FFFFFF"}',
                            borderWidth: 1.5
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                position: 'right',
                                labels: {
                                    color: '${if (isDark) "#94A3B8" else "#4B5563"}',
                                    font: { size: 9 }
                                }
                            }
                        }
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()
 
    android.view.View.OnClickListener { } // stub
 
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = android.webkit.WebViewClient()
                setBackgroundColor(android.graphics.Color.parseColor(if (isDark) "#141B2D" else "#FFFFFF"))
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://local.recharts", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddExpenseDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    val expenseCats = categories.filter { !it.isIncome }

    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(expenseCats.firstOrNull()?.id ?: 0L) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var merchantText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(config.currency) }
    var notesText by remember { mutableStateOf("") }
    var isTaxDeductible by remember { mutableStateOf(false) }

    var expandedCat by remember { mutableStateOf(false) }
    var expandedAcc by remember { mutableStateOf(false) }
    var expandedCurr by remember { mutableStateOf(false) }

    val currencyList = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "INR", "SGD", "BDT")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Add Expense", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Amount row & Currency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("quick_add_amount_input")
                    )

                    // Currency Dropdown Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedCurr = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("quick_add_currency_selector")
                        ) {
                            Text(selectedCurrency, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = expandedCurr,
                            onDismissRequest = { expandedCurr = false }
                        ) {
                            currencyList.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCurrency = curr
                                        expandedCurr = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentCatName = expenseCats.find { it.id == selectedCategoryId }?.name ?: "Select Expense Category..."
                    Column {
                        Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { expandedCat = true },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_category_selector")
                        ) {
                            Text(currentCatName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        expenseCats.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                // Account selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentAccName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Source Account..."
                    Column {
                        Text("Source Account", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { expandedAcc = true },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_account_selector")
                        ) {
                            Text(currentAccName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedAcc,
                        onDismissRequest = { expandedAcc = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${config.currencySymbol}${acc.balance.toInt()})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    expandedAcc = false
                                }
                            )
                        }
                    }
                }

                // Merchant input
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Store Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_merchant_input")
                )

                // Optional Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_notes_input")
                )

                // Is Tax Deductible
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tax Deductible", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Include in tax-saving deductions queries", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = isTaxDeductible,
                        onCheckedChange = { isTaxDeductible = it },
                        modifier = Modifier.testTag("quick_add_tax_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        return@Button
                    }
                    if (selectedCategoryId == 0L || selectedAccountId == 0L) {
                        return@Button
                    }

                    val baseAmount = viewModel.convertCurrency(amt, selectedCurrency, config.currency)
                    val customCategoryTax = viewModel.categoryTaxRates.value[selectedCategoryId] ?: config.taxRateDefault
                    val taxRateToUse = if (isTaxDeductible) customCategoryTax else 0.0

                    // Include original currency in notes for CSV analysis audit trails
                    val notesToUse = notesText.ifEmpty { "Form logged ($selectedCurrency $amt)" }

                    viewModel.addTransaction(
                        amount = baseAmount,
                        type = "EXPENSE",
                        categoryId = selectedCategoryId,
                        accountId = selectedAccountId,
                        merchant = merchantText.ifEmpty { "Standard Retail" },
                        isTaxDeductible = isTaxDeductible,
                        taxRate = taxRateToUse,
                        notes = notesToUse,
                        isRecurring = false
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("quick_add_save_btn")
            ) {
                Text("Quick Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val rawTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    val yearsList = remember(rawTransactions) {
        if (rawTransactions.isEmpty()) {
            listOf(Calendar.getInstance().get(Calendar.YEAR))
        } else {
            rawTransactions.map { tx ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = tx.timestamp
                cal.get(Calendar.YEAR)
            }.distinct().sortedDescending()
        }
    }

    var selectedYear by remember(yearsList) { mutableStateOf(yearsList.first()) }

    val annualTransactions = remember(rawTransactions, selectedYear) {
        rawTransactions.filter { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.timestamp
            cal.get(Calendar.YEAR) == selectedYear
        }
    }

    val totalIncome = remember(annualTransactions) {
        annualTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(annualTransactions) {
        annualTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val netSavings = remember(totalIncome, totalExpense) {
        totalIncome - totalExpense
    }

    val monthlySpend = remember(annualTransactions) {
        val monthsGroup = DoubleArray(12) { 0.0 }
        annualTransactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.timestamp
            val m = cal.get(Calendar.MONTH) // 0 to 11
            if (m in 0..11) {
                monthsGroup[m] += tx.amount
            }
        }
        monthsGroup
    }

    val categoryTotals = remember(annualTransactions, categories) {
        annualTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val catName = categories.find { it.id == catId }?.name ?: "Unassigned"
                val spent = txs.sumOf { it.amount }
                catName to spent
            }.sortedByDescending { it.second }
    }

    val (peakMonthName, peakMonthAmount) = remember(monthlySpend) {
        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        var maxVal = 0.0
        var maxIndex = 0
        monthlySpend.forEachIndexed { idx, valAmount ->
            if (valAmount > maxVal) {
                maxVal = valAmount
                maxIndex = idx
            }
        }
        if (maxVal > 0.0) {
            monthNames[maxIndex] to maxVal
        } else {
            "None" to 0.0
        }
    }

    val singleLargestTransaction = remember(annualTransactions) {
        annualTransactions.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.95f).testTag("yearly_report_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yearly Fiscal Report",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Year selectors
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(yearsList) { y ->
                        val isSelected = selectedYear == y
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYear = y },
                            label = { Text("Year $y", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("report_year_chip_$y")
                        )
                    }
                }

                if (annualTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No financial records found for Year $selectedYear.\nSwitch or create transactions to view metrics.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Long term trends summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "LONG-TERM TRENDS SUMMARY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Income
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Total Income", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(viewModel.formatCurrency(totalIncome), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FintechGreen)
                                    }
                                }
                                // Expense
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Total Expense", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(viewModel.formatCurrency(totalExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRose)
                                    }
                                }
                                // Savings
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val saveColor = if (netSavings >= 0) FintechGreen else ExpenseRose
                                        Text("Net Savings", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(viewModel.formatCurrency(netSavings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = saveColor)
                                    }
                                }
                            }
                        }
                    }

                    // Spend Trend Column Graph
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MONTHLY SPENDING CURVE ($selectedYear)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        AnnualTrendChart(
                            monthlySpend = monthlySpend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("yearly_trend_chart_canvas")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            months.forEach { m ->
                                Text(
                                    text = m,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Annual Statistics insights list
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "ANNUAL STATISTICAL INSIGHTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Average Spend / Month", fontSize = 11.sp)
                                Text(viewModel.formatCurrency(totalExpense / 12), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Peak Expense Month", fontSize = 11.sp)
                                Text("$peakMonthName (${viewModel.formatCurrency(peakMonthAmount)})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ExpenseRose)
                            }
                            if (singleLargestTransaction != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Single Largest Purchase", fontSize = 11.sp)
                                    Text(
                                        "${singleLargestTransaction.merchant} (${viewModel.formatCurrency(singleLargestTransaction.amount)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Category spending breakdown scrolling column
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ANNUAL CATEGORY BREAKDOWN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )

                        // Wrap inside a limited box so it is visually tight but scrollable if many categories exist
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val maxCatSpend = categoryTotals.firstOrNull()?.second ?: 1.0
                            categoryTotals.take(4).forEach { (catName, spent) ->
                                val pctTotal = if (totalExpense > 0) (spent / totalExpense * 100).toInt() else 0
                                val scalePct = if (maxCatSpend > 0) spent / maxCatSpend else 0.0

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(catName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(
                                            text = "${viewModel.formatCurrency(spent)} ($pctTotal%)",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = scalePct.toFloat(),
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("report_dismiss_button")
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun AnnualTrendChart(
    monthlySpend: DoubleArray,
    modifier: Modifier = Modifier
) {
    val maxSpend = remember(monthlySpend) {
        val maxVal = monthlySpend.maxOrNull() ?: 0.0
        if (maxVal == 0.0) 1.0 else maxVal
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 10.dp.toPx()
        val paddingBottom = 10.dp.toPx()
        val fillWidth = width - paddingLeft
        val fillHeight = height - paddingBottom
        val colWidth = fillWidth / 12

        // Draw horizontal trend grid lines
        val lineCount = 3
        for (i in 0..lineCount) {
            val y = i * (fillHeight / lineCount)
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.08f),
                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw vertical columns representation
        monthlySpend.forEachIndexed { idx, value ->
            val colHeight = (value / maxSpend) * fillHeight
            val x = paddingLeft + (idx * colWidth) + (colWidth * 0.15f)
            val y = fillHeight - colHeight

            drawRect(
                color = primaryColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, y.toFloat()),
                size = androidx.compose.ui.geometry.Size((colWidth * 0.7f), colHeight.toFloat()),
                alpha = if (value > 0) 1f else 0.12f
            )
        }
    }
}
