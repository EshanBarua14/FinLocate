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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.InsightEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import kotlin.math.min

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                    var chartModeD3 by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATEGORY SPENDING DISTRIBUTION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "D3 Engine",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Switch(
                                checked = chartModeD3,
                                onCheckedChange = { chartModeD3 = it },
                                modifier = Modifier.testTag("d3_chart_mode_switch")
                            )
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
                                    if (chartModeD3) {
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
