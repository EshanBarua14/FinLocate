package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.activeBudgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()
    val inflow by viewModel.currentInflow.collectAsState()
    val outflow by viewModel.currentOutflow.collectAsState()
    val predictiveInsights by viewModel.predictiveInsights.collectAsState()
    var showAllInsights by remember { mutableStateOf(false) }
    var showTemplateSelector by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }

    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var selectedCategoryIdForNew by remember { mutableStateOf(0L) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var isStreakUnlocked by remember { mutableStateOf(false) }
    var isSavingsUnlocked by remember { mutableStateOf(false) }
    var isTemplateUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(budgets, outflow) {
        val prefs = context.getSharedPreferences("financial_milestones_prefs", android.content.Context.MODE_PRIVATE)
        isTemplateUnlocked = prefs.getBoolean("template_pioneer_unlocked", false)
        
        val budgetSum = budgets.sumOf { it.amount }
        val remaining = budgetSum - outflow
        if (remaining > 500.0 && budgetSum > 0) {
            prefs.edit().putBoolean("savings_champion_unlocked", true).apply()
            isSavingsUnlocked = true
        } else {
            isSavingsUnlocked = prefs.getBoolean("savings_champion_unlocked", false)
        }
        
        isStreakUnlocked = prefs.getBoolean("streak_3_months_unlocked", false)
    }

    val monthTitle = remember(selectedMonth) {
        try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
            parser.parse(selectedMonth)?.let { formatter.format(it) } ?: selectedMonth
        } catch (e: Exception) {
            selectedMonth
        }
    }

    val totalBudgetedLimit = remember(budgets) { budgets.sumOf { it.amount } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Add Budget")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- HEADER MONTH SELECTOR ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.decrementMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev")
                    }
                    Text(
                        text = "BUDGETS: $monthTitle".uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.incrementMonth() }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                }
            }

            // --- CORE BUDGETING DASHBOARD COMPONENT ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("budget_dashboard_component")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "BUDGET OVERVIEW & ANALYTICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Monthly Inflow (Income)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${config.currencySymbol}${inflow.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FintechGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Spending Limits Set", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${config.currencySymbol}${totalBudgetedLimit.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    val safeOutflow = outflow.coerceAtLeast(0.0)
                    val progressFraction = if (totalBudgetedLimit > 0) (safeOutflow / totalBudgetedLimit).toFloat().coerceIn(0f, 1f) else 0f
                    val progressColor = if (progressFraction > 0.85f) ExpenseRose else MaterialTheme.colorScheme.primary

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Consumed Expenses Tracker", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(progressFraction * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = progressColor)
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actual Expenses logged: ${config.currencySymbol}${safeOutflow.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        val remaining = totalBudgetedLimit - safeOutflow
                        Text(
                            text = if (remaining >= 0) "Remains: ${config.currencySymbol}${remaining.toInt()}" else "Over: ${config.currencySymbol}${-remaining.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining >= 0) FintechGreen else ExpenseRose
                        )
                    }
                }
            }

            // --- BUDGET TEMPLATES CARD ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("budget_templates_card")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BUDGET TEMPLATES",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Save or pre-fill budget limits from standard presets",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showTemplateSelector = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).weight(1f).testTag("load_template_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showSaveTemplateDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).weight(1f).testTag("save_template_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Current", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- 2.2. INTERACTIVE REVENUE VS EXPENSE COMPARITIVE CHART ---
            val safeOutflow = outflow.coerceAtLeast(0.0)
            InteractiveComparativeChart(
                income = inflow,
                outflow = safeOutflow,
                currencySymbol = config.currencySymbol,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("budget_interactive_chart_block")
            )

            // --- MODEL-BASED PREDICTIVE SPENDING GAUGE ---
            if (predictiveInsights.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Predictive Insights",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "AI SPENDING PREDICTIONS & ADJUSTMENTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            TextButton(
                                onClick = { showAllInsights = !showAllInsights },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (showAllInsights) "Collapse" else "View All (${predictiveInsights.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Based on historical transactions, our prediction engine forecasts category trend adjustments to secure targets.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        val displayedInsights = if (showAllInsights) predictiveInsights else listOf(predictiveInsights.first())
                        displayedInsights.forEach { insight ->
                            val trendColor = when (insight.trendType) {
                                "REDUCE" -> FintechGreen
                                "INCREASE" -> ExpenseRose
                                else -> MaterialTheme.colorScheme.primary
                            }
                            val badgeText = when (insight.trendType) {
                                "REDUCE" -> "Saves Money (REDUCE)"
                                "INCREASE" -> "Overrun Predicted (INCREASE)"
                                else -> "ALIGNED"
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = insight.categoryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(trendColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = trendColor
                                            )
                                        }
                                    }

                                    Text(
                                        text = insight.recommendation,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        lineHeight = 15.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Future Forecast: ${viewModel.formatCurrency(insight.predictedSpend)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                        if (insight.currentLimit != insight.suggestedLimit) {
                                            Button(
                                                onClick = {
                                                    viewModel.updateBudgetLimit(insight.categoryId, insight.suggestedLimit, false, 0.0)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = trendColor.copy(alpha = 0.15f),
                                                    contentColor = trendColor
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Apply Limit: ${viewModel.formatCurrency(insight.suggestedLimit)}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- FINANCIAL MILESTONES & ACHIEVEMENTS PANEL ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("financial_milestones_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🎖️", fontSize = 16.sp)
                            Text(
                                text = "FINANCIAL MILESTONES & BADGES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val count = (if (isStreakUnlocked) 1 else 0) + (if (isSavingsUnlocked) 1 else 0) + (if (isTemplateUnlocked) 1 else 0)
                            Text(
                                text = "$count / 3 UNLOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Achieve savings target limits and maintain budget health ratios to claim digital badges and boost financial wealth indicators.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Badges Grid Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge 1: Fiscal Saint
                        Card(
                            modifier = Modifier.weight(1f).testTag("badge_fiscal_saint"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isStreakUnlocked) FintechGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isStreakUnlocked) FintechGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isStreakUnlocked) "🛡️" else "🔒",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Fiscal Saint",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isStreakUnlocked) FintechGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "3-Mo Under Budget",
                                    fontSize = 8.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Badge 2: Savings Champion
                        Card(
                            modifier = Modifier.weight(1f).testTag("badge_savings_champion"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSavingsUnlocked) AccentGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSavingsUnlocked) AccentGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isSavingsUnlocked) "🏆" else "🔒",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Savings Hero",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSavingsUnlocked) AccentGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Save >${config.currencySymbol}500 Limits",
                                    fontSize = 8.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Badge 3: Template Pioneer
                        Card(
                            modifier = Modifier.weight(1f).testTag("badge_template_pioneer"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isTemplateUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isTemplateUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isTemplateUnlocked) "⚡" else "🔒",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Preset Pilot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isTemplateUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Load Budget Template",
                                    fontSize = 8.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Interactive actions simulation triggers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val prefs = context.getSharedPreferences("financial_milestones_prefs", android.content.Context.MODE_PRIVATE)
                                val current = prefs.getBoolean("streak_3_months_unlocked", false)
                                prefs.edit().putBoolean("streak_3_months_unlocked", !current).apply()
                                isStreakUnlocked = !current
                                val text = if (isStreakUnlocked) "Fiscal Saint Unlocked! 🛡️" else "Fiscal Saint locked."
                                android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f).testTag("simulate_streak_btn")
                        ) {
                            Text(text = if (isStreakUnlocked) "Reset Saint" else "Earn Streak 🛡️", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                val prefs = context.getSharedPreferences("financial_milestones_prefs", android.content.Context.MODE_PRIVATE)
                                val current = prefs.getBoolean("savings_champion_unlocked", false)
                                prefs.edit().putBoolean("savings_champion_unlocked", !current).apply()
                                isSavingsUnlocked = !current
                                val text = if (isSavingsUnlocked) "Savings Hero Unlocked! 🏆" else "Savings Hero locked."
                                android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).weight(1f).testTag("simulate_savings_btn")
                        ) {
                            Text(text = if (isSavingsUnlocked) "Reset Hero" else "Earn Savings 🏆", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val prefs = context.getSharedPreferences("financial_milestones_prefs", android.content.Context.MODE_PRIVATE)
                                prefs.edit().clear().apply()
                                isStreakUnlocked = false
                                isSavingsUnlocked = false
                                isTemplateUnlocked = false
                                android.widget.Toast.makeText(context, "Milestones & Achievements Reset", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("reset_milestones_btn")
                        ) {
                            Text("Reset", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // AI RECOMMENDER SHOUTOUT BAR
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Recommender",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI ADAPTIVE RECOMMENDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Enable 'Adopt AI suggestions' to let Gemini adjust safe rollover weights under the ${config.country} fiscal year cycle.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // --- BUDGET LIST ITEMS ---
            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "NO BUDGETS CONFIGURED FOR $monthTitle",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the budget pencil button to establish limits for Food, Commutes, Utilities or Leisure.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        val cat = categories.find { it.id == budget.categoryId }
                        val pct = if (budget.amount > 0) (budget.spent / budget.amount).toFloat() else 0f

                        BudgetCardItem(
                            budget = budget,
                            category = cat,
                            pct = pct,
                            formatter = { viewModel.formatCurrency(it) },
                            onEdit = { editingBudget = budget }
                        )
                    }
                }
            }
        }

        // --- UPDATE LIMIT DIALOG ---
        if (editingBudget != null) {
            val targetCategoryName = categories.find { it.id == editingBudget!!.categoryId }?.name ?: "Category"
            var limitInput by remember { mutableStateOf(editingBudget!!.amount.toInt().toString()) }
            var savingsGoalInput by remember { mutableStateOf(editingBudget!!.savingsGoal.toInt().toString()) }
            var isAdaptiveState by remember { mutableStateOf(editingBudget!!.isAdaptive) }

            AlertDialog(
                onDismissRequest = { editingBudget = null },
                title = { Text("Update $targetCategoryName Limit") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = limitInput,
                            onValueChange = { limitInput = it },
                            label = { Text("Max Limit (${config.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_budget_input")
                        )

                        OutlinedTextField(
                            value = savingsGoalInput,
                            onValueChange = { savingsGoalInput = it },
                            label = { Text("Savings Goal (${config.currencySymbol}) - Optional") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_budget_savings_goal")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Adopt AI Suggestions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Let Gemini dynamically shift limits", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = isAdaptiveState,
                                onCheckedChange = { isAdaptiveState = it },
                                modifier = Modifier.testTag("edit_budget_ai_switch")
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val lim = limitInput.toDoubleOrNull() ?: 0.0
                            val targetGoal = savingsGoalInput.toDoubleOrNull() ?: 0.0
                            if (lim >= 0) {
                                viewModel.updateBudgetLimit(editingBudget!!.categoryId, lim, isAdaptiveState, targetGoal)
                            }
                            editingBudget = null
                        },
                        modifier = Modifier.testTag("save_budget_btn")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingBudget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- CREATE BUDGET DIALOG ---
        if (showCreateDialog) {
            val expenseCats = categories.filter { !it.isIncome }
            if (selectedCategoryIdForNew == 0L && expenseCats.isNotEmpty()) {
                selectedCategoryIdForNew = expenseCats.first().id
            }

            var limitInput by remember { mutableStateOf("5000") }
            var savingsGoalInput by remember { mutableStateOf("0") }
            var isAdaptiveState by remember { mutableStateOf(true) }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Configure Category Budget") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        var expandedCat by remember { mutableStateOf(false) }
                        val currentCatName = expenseCats.find { it.id == selectedCategoryIdForNew }?.name ?: "Select Expense Category..."
                        Button(
                            onClick = { expandedCat = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentCatName)
                        }
                        DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                            expenseCats.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategoryIdForNew = cat.id
                                        expandedCat = false
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = limitInput,
                            onValueChange = { limitInput = it },
                            label = { Text("Budget Limit (${config.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_budget_input")
                        )

                        OutlinedTextField(
                            value = savingsGoalInput,
                            onValueChange = { savingsGoalInput = it },
                            label = { Text("Savings Goal (${config.currencySymbol}) - Optional") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_budget_savings_goal")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Adopt AI Suggestions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Let Gemini auto-tune limits", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = isAdaptiveState,
                                onCheckedChange = { isAdaptiveState = it },
                                modifier = Modifier.testTag("new_budget_ai_switch")
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val lim = limitInput.toDoubleOrNull() ?: 0.0
                            val targetGoal = savingsGoalInput.toDoubleOrNull() ?: 0.0
                            if (selectedCategoryIdForNew != 0L && lim >= 0) {
                                viewModel.updateBudgetLimit(selectedCategoryIdForNew, lim, isAdaptiveState, targetGoal)
                            }
                            showCreateDialog = false
                        },
                        modifier = Modifier.testTag("submit_budget_btn")
                    ) {
                        Text("Establish Limit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showTemplateSelector) {
            val templates = viewModel.getSavedBudgetTemplates()
            AlertDialog(
                onDismissRequest = { showTemplateSelector = false },
                title = { Text("Load Budget Template", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Selecting a template will replace existing budget allocations for $monthTitle with the template preset values.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        templates.forEach { temp ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadBudgetTemplate(temp)
                                        showTemplateSelector = false
                                        val prefs = context.getSharedPreferences("financial_milestones_prefs", android.content.Context.MODE_PRIVATE)
                                        prefs.edit().putBoolean("template_pioneer_unlocked", true).apply()
                                        isTemplateUnlocked = true
                                    }
                                    .testTag("template_item_$temp")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(temp, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTemplateSelector = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSaveTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showSaveTemplateDialog = false },
                title = { Text("Save Current Budget Limits", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Save your current $monthTitle budget configuration as a custom named template.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newTemplateName,
                            onValueChange = { newTemplateName = it },
                            label = { Text("Template Name") },
                            placeholder = { Text("Summer trip, Living plan...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_template_name_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTemplateName.trim().isNotEmpty()) {
                                viewModel.saveCurrentBudgetAsTemplate(newTemplateName.trim())
                                newTemplateName = ""
                                showSaveTemplateDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_save_template_btn")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveTemplateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BudgetCardItem(
    budget: BudgetEntity,
    category: CategoryEntity?,
    pct: Float,
    formatter: (Double) -> String,
    onEdit: () -> Unit
) {
    val barColor = when {
        pct >= 1.0f -> ExpenseRose
        pct >= 0.85f -> AccentGold
        else -> FintechGreen
    }

    val warningLabel = when {
        pct >= 1.0f -> "CRITICAL OVERRUN (⚠️)"
        pct >= 0.85f -> "CLOSE TO LIMIT (85%+)"
        else -> "HEALTHY DENSITY"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = category?.name ?: "Category Limit",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = warningLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                        if (budget.isAdaptive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("AI TUNED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_budget_btn_${budget.categoryId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit limits",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar Visual Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(kotlin.math.min(pct, 1.0f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(barColor)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CONSUMED", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(text = formatter(budget.spent), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "LIMIT ALLOTTED", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(text = formatter(budget.amount), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            if (budget.savingsGoal > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))
                
                val actualSavings = (budget.amount - budget.spent).coerceAtLeast(0.0)
                val savingsPct = if (budget.savingsGoal > 0.0) (actualSavings / budget.savingsGoal).toFloat() else 0f
                val goalAchieved = actualSavings >= budget.savingsGoal
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SAVINGS GOAL PROGRESS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (goalAchieved) "GOAL COMPLETED! 🎉" else "Saved ${formatter(actualSavings)} of ${formatter(budget.savingsGoal)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (goalAchieved) FintechGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = "${(savingsPct * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (goalAchieved) FintechGreen else MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Savings goal progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(kotlin.math.min(savingsPct, 1.0f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (goalAchieved) FintechGreen else FintechGreen.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveComparativeChart(
    income: Double,
    outflow: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var selectedBar by remember { mutableStateOf<String?>(null) } // "INCOME" or "OUTFLOW"
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "INFLOW VS SPENT COMPARISON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            val maxVal = maxOf(income, outflow, 100.0)
            val incomeRatio = (income / maxVal).toFloat().coerceIn(0.01f, 1f)
            val outflowRatio = (outflow / maxVal).toFloat().coerceIn(0.01f, 1f)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Income Bar Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .clickable { selectedBar = "INCOME" }
                        .testTag("chart_bar_income")
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight(incomeRatio)
                            .background(
                                color = if (selectedBar == "INCOME") FintechGreen else FintechGreen.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .border(
                                width = if (selectedBar == "INCOME") 2.dp else 0.dp,
                                color = if (selectedBar == "INCOME") MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Inflow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                
                // Outflow Bar Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .clickable { selectedBar = "OUTFLOW" }
                        .testTag("chart_bar_outflow")
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .fillMaxHeight(outflowRatio)
                            .background(
                                color = if (selectedBar == "OUTFLOW") ExpenseRose else ExpenseRose.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .border(
                                width = if (selectedBar == "OUTFLOW") 2.dp else 0.dp,
                                color = if (selectedBar == "OUTFLOW") MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Outflow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Interactive Display Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (selectedBar) {
                    "INCOME" -> {
                        Text(
                            text = "TOTAL REVENUE: $currencySymbol${"%,.2f".format(income)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FintechGreen
                        )
                    }
                    "OUTFLOW" -> {
                        Text(
                            text = "TOTAL SPENT: $currencySymbol${"%,.2f".format(outflow)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRose
                        )
                    }
                    else -> {
                        Text(
                            text = "Tap on either chart bar for real-time portfolio metrics",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
