package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen
import java.text.SimpleDateFormat
import java.util.*

data class MonthlyTrendData(
    val monthKey: String, // e.g. "2026-03"
    val displayLabel: String, // e.g. "Mar 26"
    val income: Double,
    val expense: Double,
    val netSavings: Double,
    val savingsRate: Double // %
)

@Composable
fun SavingsTrendScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()
    var selectedRangeMonths by remember { mutableIntStateOf(6) } // 3, 6, 12 months
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Aggregate monthly trend data
    val monthlyData = remember(allTransactions, selectedRangeMonths) {
        val cal = Calendar.getInstance()
        val monthMap = mutableMapOf<String, Pair<Double, Double>>() // monthKey -> (income, expense)

        // Initialize last N months
        val formatKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val formatLabel = SimpleDateFormat("MMM yy", Locale.US)
        val monthKeys = mutableListOf<Pair<String, String>>()

        for (i in (selectedRangeMonths - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val key = formatKey.format(c.time)
            val label = formatLabel.format(c.time)
            monthKeys.add(Pair(key, label))
            monthMap[key] = Pair(0.0, 0.0)
        }

        // Aggregate transactions
        allTransactions.forEach { tx ->
            val dateStr = formatKey.format(Date(tx.timestamp))
            if (monthMap.containsKey(dateStr)) {
                val current = monthMap[dateStr] ?: Pair(0.0, 0.0)
                if (tx.type == "INCOME") {
                    monthMap[dateStr] = Pair(current.first + tx.amount, current.second)
                } else if (tx.type == "EXPENSE") {
                    monthMap[dateStr] = Pair(current.first, current.second + tx.amount)
                }
            }
        }

        monthKeys.map { (key, label) ->
            val (inc, exp) = monthMap[key] ?: Pair(0.0, 0.0)
            val net = inc - exp
            val rate = if (inc > 0) ((net / inc) * 100.0).coerceIn(-100.0, 100.0) else 0.0
            MonthlyTrendData(
                monthKey = key,
                displayLabel = label,
                income = inc,
                expense = exp,
                netSavings = net,
                savingsRate = rate
            )
        }
    }

    val totalIncome = remember(monthlyData) { monthlyData.sumOf { it.income } }
    val totalExpense = remember(monthlyData) { monthlyData.sumOf { it.expense } }
    val totalNetSavings = remember(monthlyData) { monthlyData.sumOf { it.netSavings } }
    val avgSavingsRate = remember(monthlyData) {
        if (monthlyData.isNotEmpty()) monthlyData.map { it.savingsRate }.average() else 0.0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "SAVINGS & TREND ANALYSIS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Multi-Month Income vs Expense",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Timeframe pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(3, 6, 12).forEach { range ->
                                    FilterChip(
                                        selected = selectedRangeMonths == range,
                                        onClick = {
                                            selectedRangeMonths = range
                                            selectedPointIndex = null
                                        },
                                        label = { Text("${range}M", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.testTag("trend_range_${range}m_btn")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // High Level KPI Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Net Savings Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (totalNetSavings >= 0) FintechGreen.copy(alpha = 0.12f) else ExpenseRose.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "NET SAVINGS (${selectedRangeMonths}M)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalNetSavings >= 0) FintechGreen else ExpenseRose
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${config.currency} ${String.format(Locale.US, "%,.2f", totalNetSavings)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Average Savings Rate Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "AVG SAVINGS RATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", avgSavingsRate)}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Interactive Canvas Trend Line Chart
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("savings_trend_chart_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Savings Trajectory",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Chart Legend
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(FintechGreen, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Income", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(ExpenseRose, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expenses", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Multi-Line Canvas Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            IncomeVsExpenseTrendChart(
                                dataList = monthlyData,
                                selectedIndex = selectedPointIndex,
                                onPointSelect = { selectedPointIndex = it }
                            )
                        }

                        // Selected Point Tooltip Breakdown
                        val selectedPoint = selectedPointIndex?.let { idx -> monthlyData.getOrNull(idx) }
                        if (selectedPoint != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = selectedPoint.displayLabel,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Income: ${config.currency} ${String.format(Locale.US, "%,.2f", selectedPoint.income)}",
                                            fontSize = 11.sp,
                                            color = FintechGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Expenses: ${config.currency} ${String.format(Locale.US, "%,.2f", selectedPoint.expense)}",
                                            fontSize = 11.sp,
                                            color = ExpenseRose,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Net: ${config.currency} ${String.format(Locale.US, "%,.2f", selectedPoint.netSavings)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (selectedPoint.netSavings >= 0) FintechGreen else ExpenseRose
                                        )
                                        Text(
                                            text = "Savings Rate: ${String.format(Locale.US, "%.1f", selectedPoint.savingsRate)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "💡 Tap any data point on the trend chart above to inspect month details.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Month-by-Month Detailed Breakdown Table List
            item {
                Text(
                    text = "Monthly Progress Breakdown",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(monthlyData) { month ->
                MonthlyProgressItemCard(
                    data = month,
                    currencySymbol = config.currency,
                    onClick = {
                        val idx = monthlyData.indexOf(month)
                        selectedPointIndex = if (selectedPointIndex == idx) null else idx
                    }
                )
            }
        }
    }
}

@Composable
fun IncomeVsExpenseTrendChart(
    dataList: List<MonthlyTrendData>,
    selectedIndex: Int?,
    onPointSelect: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(dataList) {
                detectTapGestures { offset ->
                    if (dataList.isEmpty()) return@detectTapGestures
                    val stepX = size.width / (dataList.size - 1).coerceAtLeast(1)
                    val clickedIdx = (offset.x / stepX).toInt().coerceIn(0, dataList.size - 1)
                    onPointSelect(clickedIdx)
                }
            }
    ) {
        if (dataList.isEmpty()) return@Canvas

        val maxVal = (dataList.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(100.0) * 1.15
        val width = size.width
        val height = size.height - 30.dp.toPx() // Bottom padding for labels

        // Draw horizontal grid lines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = height * (i / gridCount.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val stepX = if (dataList.size > 1) width / (dataList.size - 1) else width / 2f

        val incomePoints = mutableListOf<Offset>()
        val expensePoints = mutableListOf<Offset>()

        dataList.forEachIndexed { idx, item ->
            val x = if (dataList.size > 1) idx * stepX else width / 2f
            val yInc = height - ((item.income / maxVal) * height).toFloat()
            val yExp = height - ((item.expense / maxVal) * height).toFloat()
            incomePoints.add(Offset(x, yInc))
            expensePoints.add(Offset(x, yExp))
        }

        // Draw Income path with gradient fill
        val incomePath = Path().apply {
            if (incomePoints.isNotEmpty()) {
                moveTo(incomePoints[0].x, incomePoints[0].y)
                for (i in 1 until incomePoints.size) {
                    val p0 = incomePoints[i - 1]
                    val p1 = incomePoints[i]
                    cubicTo(
                        (p0.x + p1.x) / 2, p0.y,
                        (p0.x + p1.x) / 2, p1.y,
                        p1.x, p1.y
                    )
                }
            }
        }

        val incomeFillPath = Path().apply {
            addPath(incomePath)
            if (incomePoints.isNotEmpty()) {
                lineTo(incomePoints.last().x, height)
                lineTo(incomePoints.first().x, height)
                close()
            }
        }

        drawPath(
            path = incomeFillPath,
            brush = Brush.verticalGradient(
                colors = listOf(FintechGreen.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = incomePath,
            color = FintechGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Expense path with gradient fill
        val expensePath = Path().apply {
            if (expensePoints.isNotEmpty()) {
                moveTo(expensePoints[0].x, expensePoints[0].y)
                for (i in 1 until expensePoints.size) {
                    val p0 = expensePoints[i - 1]
                    val p1 = expensePoints[i]
                    cubicTo(
                        (p0.x + p1.x) / 2, p0.y,
                        (p0.x + p1.x) / 2, p1.y,
                        p1.x, p1.y
                    )
                }
            }
        }

        drawPath(
            path = expensePath,
            color = ExpenseRose,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw data points & highlight selected
        dataList.forEachIndexed { idx, _ ->
            val pInc = incomePoints[idx]
            val pExp = expensePoints[idx]
            val isSelected = selectedIndex == idx

            // Income point
            drawCircle(
                color = FintechGreen,
                radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                center = pInc
            )
            // Expense point
            drawCircle(
                color = ExpenseRose,
                radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                center = pExp
            )

            if (isSelected) {
                // Vertical indicator line
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = Offset(pInc.x, 0f),
                    end = Offset(pInc.x, height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }
        }
    }
}

@Composable
fun MonthlyProgressItemCard(
    data: MonthlyTrendData,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isPositive = data.netSavings >= 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("monthly_progress_item_${data.monthKey}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isPositive) FintechGreen.copy(alpha = 0.15f) else ExpenseRose.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isPositive) FintechGreen else ExpenseRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = data.displayLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "In: $currencySymbol ${String.format(Locale.US, "%,.0f", data.income)} | Out: $currencySymbol ${String.format(Locale.US, "%,.0f", data.expense)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}$currencySymbol ${String.format(Locale.US, "%,.2f", data.netSavings)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (isPositive) FintechGreen else ExpenseRose
                )
                Text(
                    text = "Saved ${String.format(Locale.US, "%.0f", data.savingsRate)}%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
