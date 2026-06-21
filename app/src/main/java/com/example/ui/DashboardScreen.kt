package com.example.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
    var showQuickAddDialog by remember { mutableStateOf(false) }
    val totalBalance by viewModel.totalBalance.collectAsState()
    val inflow by viewModel.currentInflow.collectAsState()
    val outflow by viewModel.currentOutflow.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val rawTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val insights by viewModel.smartInsights.collectAsState()
    val burnRateText by viewModel.burnRateAndRunway.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val countryConfig by viewModel.activeCountryConfig.collectAsState()
    val budgetProjection by viewModel.budgetProjection.collectAsState()

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
        // --- 1. COMMAND INDEX BANNER ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
                                text = "NET LIQUID WORTH",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatCurrency(totalBalance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("total_balance_text")
                            )
                        }
                        IconButton(
                            onClick = { viewModel.triggerGeminiEvaluation() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .size(48.dp)
                                .testTag("quick_refresh_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Audit",
                                tint = MaterialTheme.colorScheme.background
                            )
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
                Text(
                    text = "ACCOUNTS & mobile wallets",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (accounts.isEmpty()) {
                    Text(
                        text = "No accounts configured. Click '+' to add customized banks.",
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
                                    .height(430.dp)
                            ) {
                                RechartsTrendWebView(
                                    trendDataJson = trendJson,
                                    budgetBarJson = budgetBarJson,
                                    currencySymbol = countryConfig.currencySymbol,
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
                    background-color: #0F172A; /* Match Slate 900 */
                    color: #F8FAFC;
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
                    .attr("stroke", "#0F172A")
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
                    .style("fill", "#94A3B8")
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
                setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
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
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                body {
                    background-color: #0F172A; /* Slate 900 */
                    color: #F8FAFC;
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
                    color: #94A3B8;
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
            
            <div class="chart-section" style="margin-bottom: 0;">
                <h3>📊 CATEGORIES VS BUDGET LIMITS</h3>
                <div class="canvas-holder" style="height: 165px;">
                    <canvas id="budgetChart"></canvas>
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

                // Set up Line Chart
                const trendCtx = document.getElementById('trendChart').getContext('2d');
                
                // Create gradient
                const trendGrad = trendCtx.createLinearGradient(0, 0, 0, 150);
                trendGrad.addColorStop(0, 'rgba(16, 185, 129, 0.4)');
                trendGrad.addColorStop(1, 'rgba(16, 185, 129, 0.0)');

                new Chart(trendCtx, {
                    type: 'line',
                    data: {
                        labels: trendLabels,
                        datasets: [{
                            label: 'Total Spent',
                            data: trendValues,
                            borderColor: '#10B981',
                            borderWidth: 2,
                            backgroundColor: trendGrad,
                            fill: true,
                            tension: 0.4,
                            pointRadius: trendValues.length > 15 ? 0 : 3,
                            pointBackgroundColor: '#10B981'
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
                                grid: { color: '#1E293B' },
                                ticks: { color: '#94A3B8', font: { size: 9 } }
                            },
                            y: {
                                grid: { color: '#1E293B' },
                                ticks: {
                                    color: '#94A3B8',
                                    font: { size: 9 },
                                    callback: function(value) { return '$currencySymbol' + value; }
                                }
                            }
                        }
                    }
                });

                // Set up Budget Bar Chart
                const budgetCtx = document.getElementById('budgetChart').getContext('2d');
                
                // Define modern colors: limit bars are always deep blue; spent bars are green or rose based on overrun
                const backgroundColors = budgetSpents.map((spent, idx) => {
                    const limit = budgetLimits[idx];
                    return spent > limit ? '#F43F5E' : '#10B981';
                });

                new Chart(budgetCtx, {
                    type: 'bar',
                    data: {
                        labels: budgetLabels,
                        datasets: [
                            {
                                label: 'Budget Limit',
                                data: budgetLimits,
                                backgroundColor: '#3B82F6',
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
                                labels: { color: '#F8FAFC', font: { size: 10 } }
                            }
                        },
                        scales: {
                            x: {
                                grid: { color: '#1E293B' },
                                ticks: { color: '#94A3B8', font: { size: 9 } }
                            },
                            y: {
                                grid: { color: '#1E293B' },
                                ticks: {
                                    color: '#94A3B8',
                                    font: { size: 9 },
                                    callback: function(value) { return '$currencySymbol' + value; }
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
                setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
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
