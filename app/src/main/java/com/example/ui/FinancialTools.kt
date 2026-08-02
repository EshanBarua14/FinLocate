package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.UserDebtEntity
import com.example.data.model.SavingsGoalEntity
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. STANDALONE CURRENCY CONVERTER WIDGET
// ==========================================
@Composable
fun StandaloneCurrencyConverterWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val exchangeRates by viewModel.exchangeRates.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    var calcAmount by remember { mutableStateOf("100") }
    var calcFromCur by remember { mutableStateOf("USD") }
    var calcToCur by remember { mutableStateOf("EUR") }
    var showFromMenu by remember { mutableStateOf(false) }
    var showToMenu by remember { mutableStateOf(false) }

    val currenciesList = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "INR", "SGD", "BDT")

    val doubleAmount = calcAmount.toDoubleOrNull() ?: 100.0
    val convertedEstimate = viewModel.convertCurrency(doubleAmount, calcFromCur, calcToCur)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("standalone_currency_calc_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "OFFLINE CURRENCY CONVERTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Using local cached exchange rates",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Conversion input and selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = calcAmount,
                    onValueChange = { calcAmount = it },
                    label = { Text("Amount", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("standalone_conversion_amount"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )

                // FROM Selector
                Box {
                    Button(
                        onClick = { showFromMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .width(85.dp)
                            .height(56.dp)
                            .testTag("standalone_conversion_from_btn")
                    ) {
                        Text(calcFromCur, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showFromMenu,
                        onDismissRequest = { showFromMenu = false }
                    ) {
                        currenciesList.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    calcFromCur = cur
                                    showFromMenu = false
                                }
                            )
                        }
                    }
                }

                // Swap Button
                IconButton(
                    onClick = {
                        val temp = calcFromCur
                        calcFromCur = calcToCur
                        calcToCur = temp
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .testTag("standalone_conversion_swap_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap currencies",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // TO Selector
                Box {
                    Button(
                        onClick = { showToMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .width(85.dp)
                            .height(56.dp)
                            .testTag("standalone_conversion_to_btn")
                    ) {
                        Text(calcToCur, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = showToMenu,
                        onDismissRequest = { showToMenu = false }
                    ) {
                        currenciesList.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text(cur, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    calcToCur = cur
                                    showToMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conversion display output
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONVERTED ESTIMATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f %s", convertedEstimate, calcToCur),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("standalone_conversion_result")
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val fromRate = exchangeRates[calcFromCur] ?: 1.0
                    val toRate = exchangeRates[calcToCur] ?: 1.0
                    val rateRelation = if (fromRate > 0.0) toRate / fromRate else 0.0
                    Text(
                        text = "1 $calcFromCur = ${String.format(Locale.US, "%.4f", rateRelation)} $calcToCur",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            var isSyncingRates by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = {
                    isSyncingRates = true
                    viewModel.syncExchangeRatesNow(context) { success, count ->
                        isSyncingRates = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_rates_workmanager_btn"),
                shape = RoundedCornerShape(10.dp),
                enabled = !isSyncingRates
            ) {
                if (isSyncingRates) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching Exchange Rates via WorkManager...", fontSize = 11.sp)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Live Rates via WorkManager API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ==========================================
// 2. UPCOMING MONTHLY BILLS CALENDAR
// ==========================================
@Composable
fun UpcomingBillsCalendar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeRecurring by viewModel.allRecurring.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    // Default to July 2026 based on model additional metadata
    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JULY)
        })
    }

    var selectedDay by remember { mutableStateOf<Int?>(7) } // Default select today (July 7, 2026)

    val currentYear = selectedCalendar.get(Calendar.YEAR)
    val currentMonthIdx = selectedCalendar.get(Calendar.MONTH) // 0-indexed

    val monthName = remember(currentMonthIdx) {
        SimpleDateFormat("MMMM", Locale.US).format(selectedCalendar.time).uppercase()
    }

    // Filter recurring transactions that are bills/expenses and active
    val bills = remember(activeRecurring) {
        activeRecurring.filter { it.type == "EXPENSE" && it.isActive }
    }

    // Standard days calculation
    val daysInMonth = remember(currentMonthIdx, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonthIdx)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val startDayOfWeek = remember(currentMonthIdx, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonthIdx)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
    }

    // Map which day numbers have bills due in this month
    val billsOnDays = remember(bills, currentMonthIdx, currentYear) {
        val daysWithBills = mutableMapOf<Int, MutableList<RecurringTransactionEntity>>()
        
        for (day in 1..daysInMonth) {
            val calCheck = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonthIdx)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dayOfWeek = calCheck.get(Calendar.DAY_OF_WEEK)

            for (bill in bills) {
                val calBill = Calendar.getInstance().apply { timeInMillis = bill.nextExecutionTimestamp }
                val billDay = calBill.get(Calendar.DAY_OF_MONTH)
                val billMonth = calBill.get(Calendar.MONTH)
                val billYear = calBill.get(Calendar.YEAR)

                var isDue = false
                when (bill.recurrenceInterval.uppercase()) {
                    "DAILY" -> isDue = true
                    "WEEKLY" -> {
                        // Check if day of week matches nextExecutionTimestamp's day of week
                        isDue = (dayOfWeek == calBill.get(Calendar.DAY_OF_WEEK))
                    }
                    "MONTHLY" -> {
                        // Match day of month
                        isDue = (day == billDay)
                    }
                    else -> {
                        // ONE-SHOT / NONE
                        isDue = (day == billDay && currentMonthIdx == billMonth && currentYear == billYear)
                    }
                }

                if (isDue) {
                    daysWithBills.getOrPut(day) { mutableListOf() }.add(bill)
                }
            }
        }
        daysWithBills
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("bills_calendar_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UPCOMING BILLS CALENDAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                            selectedDay = if (selectedCalendar.get(Calendar.YEAR) == 2026 && selectedCalendar.get(Calendar.MONTH) == Calendar.JULY) 7 else 1
                        },
                        modifier = Modifier.size(32.dp).testTag("calendar_prev_month")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = "$monthName $currentYear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp).testTag("calendar_month_year_text")
                    )
                    IconButton(
                        onClick = {
                            selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                            selectedDay = if (selectedCalendar.get(Calendar.YEAR) == 2026 && selectedCalendar.get(Calendar.MONTH) == Calendar.JULY) 7 else 1
                        },
                        modifier = Modifier.size(32.dp).testTag("calendar_next_month")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar grid header days of the week (S, M, T, W, T, F, S)
            val weekDays = listOf("SU", "MO", "TU", "WE", "TH", "FR", "SA")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar days grid
            val totalCells = startDayOfWeek - 1 + daysInMonth
            val rows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0..6) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - (startDayOfWeek - 2)
                            val isValidDay = dayNumber in 1..daysInMonth

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            !isValidDay -> Color.Transparent
                                            selectedDay == dayNumber -> MaterialTheme.colorScheme.primary
                                            dayNumber == 7 && currentMonthIdx == Calendar.JULY && currentYear == 2026 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable(enabled = isValidDay) {
                                        selectedDay = dayNumber
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isValidDay) {
                                    val isSelected = (selectedDay == dayNumber)
                                    val dayBills = billsOnDays[dayNumber] ?: emptyList()
                                    val hasBills = dayBills.isNotEmpty()

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected || dayNumber == 7) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                dayNumber == 7 && currentMonthIdx == Calendar.JULY && currentYear == 2026 -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (hasBills) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else ExpenseRose,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(10.dp))

            // Details on selected day
            val activeDayNum = selectedDay
            if (activeDayNum != null) {
                val dayBills = billsOnDays[activeDayNum] ?: emptyList()
                
                Text(
                    text = "BILLS DUE ON $monthName $activeDayNum".uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (dayBills.isEmpty()) {
                    Text(
                        text = "No bills scheduled for this date.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayBills.forEach { bill ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = ExpenseRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = bill.merchant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Due: ${bill.recurrenceInterval}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Text(
                                    text = "-${activeConfig.currencySymbol}${bill.amount.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = ExpenseRose,
                                    modifier = Modifier.testTag("calendar_bill_amount_${bill.id}")
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Select a day to inspect scheduled bills",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


// ==========================================
// 3. DEBT PORTFOLIO & PAYOFF TIMELINE COMPONENT
// ==========================================
@Composable
private fun DebtReductionVisualizer(
    principal: Double,
    baseMilestones: List<AmortizationMilestone>,
    extraMilestones: List<AmortizationMilestone>,
    currencySymbol: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = "DEBT REDUCTION PROGRESS PATH",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        val maxMonths = maxOf(
            baseMilestones.lastOrNull()?.monthNumber ?: 1,
            extraMilestones.lastOrNull()?.monthNumber ?: 1
        ).toFloat()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Draw background grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = height * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw curve helper
                val drawCurve = { milestones: List<AmortizationMilestone>, color: Color, strokeW: Float ->
                    if (milestones.isNotEmpty()) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                        }

                        milestones.forEach { milestone ->
                            val x = (milestone.monthNumber.toFloat() / maxMonths) * width
                            val y = (1f - (milestone.remainingBalance.toFloat() / principal.toFloat())) * height
                            path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = strokeW,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                            )
                        )

                        // End point dot
                        milestones.lastOrNull()?.let { last ->
                            val endX = (last.monthNumber.toFloat() / maxMonths) * width
                            val endY = height
                            drawCircle(
                                color = color,
                                radius = 6f,
                                center = androidx.compose.ui.geometry.Offset(endX, endY)
                            )
                        }
                    }
                }

                // Draw standard payoff curve (ExpenseRose)
                drawCurve(baseMilestones, ExpenseRose, 3f)

                // Draw extra payoff curve (FintechGreen)
                drawCurve(extraMilestones, FintechGreen, 5f)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Legends
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(ExpenseRose, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                val baseMonths = baseMilestones.lastOrNull()?.monthNumber ?: 0
                Text("Standard: $baseMonths mos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(FintechGreen, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                val extraMonths = extraMilestones.lastOrNull()?.monthNumber ?: 0
                Text("With Extra: $extraMonths mos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DebtPayoffTimelineComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val debts by viewModel.allDebts.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    var extraPaymentInput by remember { mutableStateOf("100") }
    val extraPayment = extraPaymentInput.toDoubleOrNull() ?: 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_portfolio_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEBT PORTFOLIO & PAYOFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("add_debt_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Debt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (debts.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CreditCardOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO ACTIVE DEBTS REGISTERED",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add student loans, car financing, or card debts to track payoffs.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // List of debts
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    debts.forEach { debt ->
                        val standardMonthly = calculateStandardMonthlyPayment(debt.amount, debt.interestRate, debt.termMonths)
                        
                        // Simulations
                        val baseSim = simulateDebtPayoff(debt.amount, debt.interestRate, debt.termMonths, debt.monthlyPayment, 0.0)
                        val extraSim = simulateDebtPayoff(debt.amount, debt.interestRate, debt.termMonths, debt.monthlyPayment, extraPayment)

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
                                    Column {
                                        Text(
                                            text = debt.name.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Int. Rate: ${debt.interestRate}% | Term: ${debt.termMonths} mos",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${config.currencySymbol}${debt.amount.toInt()}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.testTag("debt_balance_${debt.id}")
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteDebt(debt) },
                                            modifier = Modifier.size(24.dp).testTag("delete_debt_btn_${debt.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Clear debt",
                                                tint = ExpenseRose.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                // Timeline and savings comparisons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Standard Payment", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text("${config.currencySymbol}${standardMonthly.toInt()}/mo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Payoff Est.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(
                                            text = if (baseSim.monthsToPayoff < 0) "Never" else "${baseSim.monthsToPayoff} mos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (baseSim.monthsToPayoff < 0) ExpenseRose else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Interactive accelerator simulation results
                                if (extraPayment > 0.0 && baseSim.monthsToPayoff > 0 && extraSim.monthsToPayoff > 0) {
                                    val monthsSaved = baseSim.monthsToPayoff - extraSim.monthsToPayoff
                                    val interestSaved = baseSim.totalInterestPaid - extraSim.totalInterestPaid

                                    if (monthsSaved > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(FintechGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.OfflineBolt,
                                                        contentDescription = null,
                                                        tint = FintechGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Extra +${config.currencySymbol}${extraPayment.toInt()}/mo saves:",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FintechGreen
                                                    )
                                                }
                                                Text(
                                                    text = "$monthsSaved months & ${config.currencySymbol}${interestSaved.toInt()} in interest!",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = FintechGreen,
                                                    modifier = Modifier.testTag("savings_simulation_result")
                                                )
                                            }
                                        }
                                    }
                                }

                                if (baseSim.monthsToPayoff > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DebtReductionVisualizer(
                                        principal = debt.amount,
                                        baseMilestones = baseSim.monthlyMilestones,
                                        extraMilestones = extraSim.monthlyMilestones,
                                        currencySymbol = config.currencySymbol
                                    )
                                }
                            }
                        }
                    }

                    // Extra Payment Simulator controls
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "SIMULATE ACCELERATED DEBT ACCELERATOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Add Extra monthly payment:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = extraPaymentInput,
                                onValueChange = { extraPaymentInput = it },
                                leadingIcon = { Text(config.currencySymbol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .width(95.dp)
                                    .height(46.dp)
                                    .testTag("debt_extra_payment_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // Debt Strategy Calculator (Snowball vs Avalanche)
                    var selectedStrategy by remember { mutableStateOf(DebtStrategy.SNOWBALL) }
                    val strategyResult = calculateDebtStrategyPayoff(debts, extraPayment, selectedStrategy)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PAYOFF STRATEGY CALCULATOR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )

                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    color = if (selectedStrategy == DebtStrategy.SNOWBALL) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clickable { selectedStrategy = DebtStrategy.SNOWBALL }
                                        .testTag("snowball_strategy_btn")
                                ) {
                                    Text(
                                        text = "Snowball",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedStrategy == DebtStrategy.SNOWBALL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    color = if (selectedStrategy == DebtStrategy.AVALANCHE) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .clickable { selectedStrategy = DebtStrategy.AVALANCHE }
                                        .testTag("avalanche_strategy_btn")
                                ) {
                                    Text(
                                        text = "Avalanche",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedStrategy == DebtStrategy.AVALANCHE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (selectedStrategy == DebtStrategy.SNOWBALL)
                                "Snowball: Pays off smallest balances first for quick momentum."
                            else
                                "Avalanche: Pays off highest interest rates first to minimize total interest paid.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Debt-Free Timeline", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("${strategyResult.totalMonths} months", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Est. Interest Paid", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("${config.currencySymbol}${strategyResult.totalInterestPaid.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("10000") }
        var rateStr by remember { mutableStateOf("6.0") }
        var termStr by remember { mutableStateOf("36") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Register Loan / Debt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Debt Name (e.g. Car Loan)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_debt_name_input")
                    )

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Loan Principal Amount (${config.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_debt_amount_input")
                    )

                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Annual Interest Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_debt_rate_input")
                    )

                    OutlinedTextField(
                        value = termStr,
                        onValueChange = { termStr = it },
                        label = { Text("Repayment Term (months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_debt_term_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        val rate = rateStr.toDoubleOrNull() ?: 0.0
                        val term = termStr.toIntOrNull() ?: 0
                        if (name.isNotEmpty() && amt > 0.0 && term > 0) {
                            viewModel.addDebt(name, amt, rate, term, 0.0)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_debt_btn")
                ) {
                    Text("Add Debt")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


enum class DebtStrategy { SNOWBALL, AVALANCHE }

data class DebtStrategyResult(
    val strategy: DebtStrategy,
    val totalMonths: Int,
    val totalInterestPaid: Double,
    val payoffOrder: List<Pair<String, Int>>
)

fun calculateDebtStrategyPayoff(
    debts: List<UserDebtEntity>,
    extraMonthly: Double,
    strategy: DebtStrategy
): DebtStrategyResult {
    if (debts.isEmpty()) return DebtStrategyResult(strategy, 0, 0.0, emptyList())

    class ActiveDebt(val name: String, var balance: Double, val rate: Double, val minPayment: Double)

    val activeDebts = debts.map { d ->
        val minPay = if (d.monthlyPayment > 0) d.monthlyPayment else calculateStandardMonthlyPayment(d.amount, d.interestRate, d.termMonths)
        ActiveDebt(d.name, d.amount, d.interestRate / 100.0 / 12.0, maxOf(10.0, minPay))
    }.toMutableList()

    var month = 0
    var totalInterest = 0.0
    val payoffOrder = mutableListOf<Pair<String, Int>>()

    while (activeDebts.any { it.balance > 0.01 } && month < 360) {
        month++
        val sortedActive = activeDebts.filter { it.balance > 0.01 }.sortedWith(
            if (strategy == DebtStrategy.SNOWBALL) {
                compareBy { it.balance }
            } else {
                compareByDescending { it.rate }
            }
        )

        var availableExtra = extraMonthly

        for (d in sortedActive) {
            val interest = d.balance * d.rate
            totalInterest += interest
            d.balance += interest

            val actualPayment = minOf(d.balance, d.minPayment)
            d.balance -= actualPayment

            if (d.balance <= 0.01 && !payoffOrder.any { it.first == d.name }) {
                payoffOrder.add(d.name to month)
            }
        }

        for (d in sortedActive) {
            if (d.balance > 0.01 && availableExtra > 0) {
                val extraApplied = minOf(d.balance, availableExtra)
                d.balance -= extraApplied
                availableExtra -= extraApplied

                if (d.balance <= 0.01 && !payoffOrder.any { it.first == d.name }) {
                    payoffOrder.add(d.name to month)
                }
            }
        }
    }

    return DebtStrategyResult(strategy, month, totalInterest, payoffOrder)
}

// Math Helpers for Debt calculations
private fun calculateStandardMonthlyPayment(principal: Double, annualRate: Double, termMonths: Int): Double {
    if (termMonths <= 0) return 0.0
    if (annualRate <= 0.0) return principal / termMonths
    val monthlyRate = (annualRate / 100.0) / 12.0
    val pow = Math.pow(1.0 + monthlyRate, termMonths.toDouble())
    return principal * (monthlyRate * pow) / (pow - 1.0)
}

private data class PayoffSimulationResult(
    val monthsToPayoff: Int,
    val totalInterestPaid: Double,
    val monthlyMilestones: List<AmortizationMilestone>
)

private data class AmortizationMilestone(
    val monthNumber: Int,
    val remainingBalance: Double,
    val cumulativeInterest: Double
)

private fun simulateDebtPayoff(
    principal: Double,
    annualRate: Double,
    termMonths: Int,
    customMonthlyPayment: Double,
    extraMonthlyPayment: Double
): PayoffSimulationResult {
    if (termMonths <= 0) return PayoffSimulationResult(0, 0.0, emptyList())
    val monthlyRate = (annualRate / 100.0) / 12.0
    val basePayment = if (customMonthlyPayment > 0.0) customMonthlyPayment else calculateStandardMonthlyPayment(principal, annualRate, termMonths)
    val actualMonthlyPayment = basePayment + extraMonthlyPayment
    
    var remainingBalance = principal
    var totalInterest = 0.0
    var monthCount = 0
    val milestones = mutableListOf<AmortizationMilestone>()
    
    // Cover the edge case where actual monthly payment cannot even cover monthly interest
    if (remainingBalance * monthlyRate >= actualMonthlyPayment && monthlyRate > 0.0) {
        return PayoffSimulationResult(-1, 999999.0, emptyList())
    }

    while (remainingBalance > 0.01 && monthCount < 600) {
        monthCount++
        val interestForMonth = remainingBalance * monthlyRate
        val principalForMonth = (actualMonthlyPayment - interestForMonth).coerceAtMost(remainingBalance)
        
        if (principalForMonth <= 0.0 && interestForMonth > 0.0) {
            return PayoffSimulationResult(-1, 999999.0, emptyList())
        }
        
        remainingBalance -= principalForMonth
        totalInterest += interestForMonth
        
        if (monthCount % 12 == 0 || remainingBalance <= 0.01) {
            milestones.add(
                AmortizationMilestone(
                    monthNumber = monthCount,
                    remainingBalance = remainingBalance.coerceAtLeast(0.0),
                    cumulativeInterest = totalInterest
                )
            )
        }
    }
    
    return PayoffSimulationResult(monthCount, totalInterest, milestones)
}

// =========================================================================
// 4. HIGH-FIDELITY FINANCIAL HEALTH INDEX ASSESSMENT SCORECARD
// =========================================================================
@Composable
fun FinancialHealthScoreComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val debts by viewModel.allDebts.collectAsState()
    val budgets by viewModel.activeBudgets.collectAsState()
    val inflow by viewModel.currentInflow.collectAsState()
    val outflow by viewModel.currentOutflow.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    val totalMonthlyDebt = debts.sumOf { it.monthlyPayment }
    val dti = if (inflow > 0) (totalMonthlyDebt / inflow) * 100 else 0.0
    val savingsRate = if (inflow > 0) ((inflow - outflow) / inflow) * 100 else 0.0

    val totalLimit = budgets.sumOf { it.amount }
    val totalOverrun = budgets.sumOf { maxOf(0.0, it.spent - it.amount) }
    val adherenceScore = if (totalLimit > 0) (1.0 - (totalOverrun / totalLimit).coerceIn(0.0, 1.0)) * 100 else 100.0

    val dtiScore = (1.0 - (dti / 60.0)).coerceIn(0.0, 1.0) * 100
    val savingsScore = if (savingsRate <= 0.0) 0.0 else if (savingsRate >= 30.0) 100.0 else (savingsRate / 30.0) * 100
    val weightedScore = (dtiScore * 0.3) + (savingsScore * 0.4) + (adherenceScore * 0.3)
    val finalScore = weightedScore.toInt()

    val (scoreColor, scoreText, scoreDesc) = when {
        finalScore >= 80 -> Triple(FintechGreen, "EXCELLENT", "Your financial position is secure and resilient. Outstanding savings habit and debt-to-income balance!")
        finalScore >= 60 -> Triple(AccentGold, "GOOD", "Decent financial balance, but there is room to improve. Consider pruning minor subscriptions to boost your savings rate.")
        finalScore >= 40 -> Triple(Color(0xFFFFA726), "FAIR", "Moderate risk detected. Keep budget limits updated and allocate extra flows to pending debts.")
        else -> Triple(ExpenseRose, "NEEDS ATTENTION", "High burn rate and debt load warning. Please establish explicit budget caps to avoid overdraft trends.")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_health_score_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FINANCIAL HEALTH INDEX",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real-time metrics assessment scorecard",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main score circle and stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Circle Score Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    val secondaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = secondaryColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        drawArc(
                            color = scoreColor,
                            startAngle = -90f,
                            sweepAngle = (finalScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$finalScore",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "HEALTH",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Score Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(scoreColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = scoreText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = scoreColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = scoreDesc,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics grids
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Debt-to-Income Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("DEBT RATIO (DTI)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.1f%%", dti), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (dti <= 30) FintechGreen else ExpenseRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (dti <= 30) "Healthy DTI" else "High Debt Load", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                // Savings Rate Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("SAVINGS RATE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.1f%%", savingsRate), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (savingsRate >= 20) FintechGreen else ExpenseRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if (savingsRate >= 20) "Target met!" else "Save 20%+", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                // Budget Adherence Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("BUDGET COMPLIANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(String.format(Locale.US, "%.0f%%", adherenceScore), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (adherenceScore >= 80) FintechGreen else ExpenseRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Adherence index", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// =========================================================================
// 5. NATIVE COMPARATIVE MONTH-OVER-MONTH SPENDING BAR CHART
// =========================================================================
@Composable
fun ComparativeSpendingBarChartComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentOutflow by viewModel.currentOutflow.collectAsState()
    val previousOutflow by viewModel.previousMonthOutflow.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    val currencyFormatter = { value: Double ->
        viewModel.formatCurrency(value)
    }

    val maxVal = maxOf(10.0, maxOf(currentOutflow, previousOutflow))
    val currentHeightFraction = (currentOutflow / maxVal).toFloat()
    val previousHeightFraction = (previousOutflow / maxVal).toFloat()

    val diffAmount = currentOutflow - previousOutflow
    val isIncrease = diffAmount > 0
    val diffPct = if (previousOutflow > 0) (Math.abs(diffAmount) / previousOutflow) * 100 else 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("comparative_spending_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "MONTH-OVER-MONTH OUTFLOWS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Comparative budget behavior index",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two bars side-by-side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Previous Month Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currencyFormatter(previousOutflow),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight(previousHeightFraction.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PREVIOUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Current Month Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currencyFormatter(currentOutflow),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight(currentHeightFraction.coerceIn(0.05f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "CURRENT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Summary Text info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isIncrease) ExpenseRose else FintechGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isIncrease) {
                        String.format(Locale.US, "Your monthly spending increased by %.1f%% (+%s) compared to last month. Consider review.", diffPct, currencyFormatter(diffAmount))
                    } else if (diffAmount < 0) {
                        String.format(Locale.US, "Fantastic! You saved %.1f%% (%s) more than last month. Keep up the high savings discipline!", diffPct, currencyFormatter(Math.abs(diffAmount)))
                    } else {
                        "Your spending is perfectly aligned and flat compared to previous month."
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// =========================================================================
// 6. NATIVE DOUBLE-RING INTERACTIVE SUNBURST BREAKDOWN CHART
// =========================================================================
@Composable
fun InteractiveSunburstChartComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    val categorySpending = categories.map { cat ->
        val amt = transactions.filter { it.categoryId == cat.id && it.type == "EXPENSE" }.sumOf { it.amount }
        cat to amt
    }.filter { it.second > 0.0 }.sortedByDescending { it.second }

    val grandTotal = categorySpending.sumOf { it.second }
    var selectedSliceIndex by remember { mutableStateOf(-1) }

    val colorPalette = listOf(
        MaterialTheme.colorScheme.primary,
        FintechGreen,
        AccentGold,
        ExpenseRose,
        MaterialTheme.colorScheme.secondary,
        Color(0xFF8E24AA), // Purple
        Color(0xFF00ACC1), // Cyan
        Color(0xFFF4511E), // Orange-Red
        Color(0xFF3949AB)  // Indigo
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("sunburst_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "EXPENSE STRUCTURE BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Dynamic Category allocations chart",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categorySpending.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded expenses for this month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Canvas
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(130.dp)
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            categorySpending.forEachIndexed { index, pair ->
                                val sweepAngle = ((pair.second / grandTotal) * 360f).toFloat()
                                val isSelected = index == selectedSliceIndex
                                val strokeWidthValue = if (isSelected) 14.dp.toPx() else 8.dp.toPx()
                                drawArc(
                                    color = colorPalette[index % colorPalette.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidthValue)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Text in center of Donut
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (selectedSliceIndex >= 0 && selectedSliceIndex < categorySpending.size) {
                                val item = categorySpending[selectedSliceIndex]
                                Text(
                                    text = item.first.name.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = viewModel.formatCurrency(item.second),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "TOTAL EXPENSES",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = viewModel.formatCurrency(grandTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Legend list
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categorySpending.take(4).forEachIndexed { index, pair ->
                            val pct = (pair.second / grandTotal) * 100
                            val isSelected = index == selectedSliceIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        selectedSliceIndex = if (isSelected) -1 else index
                                    }
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colorPalette[index % colorPalette.size])
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = pair.first.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", pct),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 7. DEDICATED SAVINGS GOALS TARGET TRACKER & DISCIPLINE LEDGER
// =========================================================================
@Composable
fun DedicatedSavingsGoalsTrackerComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allGoals.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var allocateTargetGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dedicated_savings_goals_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SAVINGS GOAL TRACKER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Define targets and allocate custom savings",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                
                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("add_savings_goal_icon_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Goal",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No dedicated savings goals established yet.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("Create your first goal 🎯", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    goals.forEach { goal ->
                        val progressFraction = if (goal.targetAmount > 0.0) (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        val isCompleted = goal.savedAmount >= goal.targetAmount

                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        val dateStr = sdf.format(Date(goal.targetDate))
                        val daysRemaining = maxOf(0L, (goal.targetDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Title and delete button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = goal.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Target Date: $dateStr ($daysRemaining days remaining)",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { allocateTargetGoal = goal },
                                            modifier = Modifier.size(24.dp).testTag("allocate_goal_${goal.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CurrencyExchange,
                                                contentDescription = "Allocate Funds",
                                                tint = FintechGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteSavingsGoal(goal) },
                                            modifier = Modifier.size(24.dp).testTag("delete_goal_${goal.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Goal",
                                                tint = ExpenseRose,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Progress row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${viewModel.formatCurrency(goal.savedAmount)} saved",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) FintechGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Goal: ${viewModel.formatCurrency(goal.targetAmount)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Progress indicator
                                LinearProgressIndicator(
                                    progress = progressFraction,
                                    color = if (isCompleted) FintechGreen else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                                
                                 if (isCompleted) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TARGET ACCOMPLISHED! 🎉",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = FintechGreen
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val remainingAmt = maxOf(0.0, goal.targetAmount - goal.savedAmount)
                                    val monthsLeft = maxOf(1.0, daysRemaining / 30.0)
                                    val monthlyNeeded = remainingAmt / monthsLeft
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Required Monthly Savings:",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${viewModel.formatCurrency(monthlyNeeded)} / mo",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
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

    // Add Goal Dialog
    if (showAddDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetAmt by remember { mutableStateOf("") }
        var targetDays by remember { mutableStateOf("90") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("NEW SAVINGS TARGET", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Goal Name (e.g. Vacation Fund)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_goal_name_field")
                    )
                    OutlinedTextField(
                        value = targetAmt,
                        onValueChange = { targetAmt = it },
                        label = { Text("Target Amount (${activeConfig.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_goal_amount_field")
                    )
                    OutlinedTextField(
                        value = targetDays,
                        onValueChange = { targetDays = it },
                        label = { Text("Term (Days to achieve)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_goal_days_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = targetAmt.toDoubleOrNull() ?: 0.0
                        val days = targetDays.toLongOrNull() ?: 30L
                        if (goalName.isNotEmpty() && amount > 0) {
                            val targetTimestamp = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                            viewModel.addSavingsGoal(goalName, amount, targetTimestamp, 0.0)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("add_goal_confirm_btn")
                ) {
                    Text("CREATE GOAL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Allocate Funds Dialog
    if (allocateTargetGoal != null) {
        var allocateAmt by remember { mutableStateOf("") }
        val targetGoal = allocateTargetGoal!!

        AlertDialog(
            onDismissRequest = { allocateTargetGoal = null },
            title = { Text("ALLOCATE FUNDS", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Transfer money from portfolio balances to your savings target '${targetGoal.name}'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = allocateAmt,
                        onValueChange = { allocateAmt = it },
                        label = { Text("Amount to Allocate (${activeConfig.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("allocate_goal_amt_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = allocateAmt.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.allocateToSavingsGoal(targetGoal, amount)
                            allocateTargetGoal = null
                        }
                    },
                    modifier = Modifier.testTag("allocate_goal_confirm_btn")
                ) {
                    Text("CONFIRM TRANSFER")
                }
            },
            dismissButton = {
                TextButton(onClick = { allocateTargetGoal = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// =========================================================================
// 8. FUTURE SAVINGS POWER CALCULATOR (ADJUSTED FOR JURISDICTION INFLATION)
// =========================================================================
@Composable
fun FutureSavingsCalculatorComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeConfig by viewModel.activeCountryConfig.collectAsState()
    val inflow by viewModel.currentInflow.collectAsState()
    val outflow by viewModel.currentOutflow.collectAsState()

    // Default surplus calculation (Inflow - Outflow) coerced to >= 0
    val defaultSurplus = maxOf(0.0, inflow - outflow)

    val countryDefaultInflation = remember(activeConfig) {
        when (activeConfig.country.uppercase(Locale.US)) {
            "BANGLADESH" -> 8.5
            "USA" -> 2.8
            "INDIA" -> 5.1
            "GERMANY" -> 2.1
            else -> 3.0
        }
    }

    var savingsAmountStr by remember(defaultSurplus) { mutableStateOf(String.format(Locale.US, "%.0f", if (defaultSurplus > 0) defaultSurplus else 500.0)) }
    var nominalRate by remember { mutableStateOf(7.0f) }
    var inflationRate by remember(countryDefaultInflation) { mutableStateOf(countryDefaultInflation.toFloat()) }
    var projectionYears by remember { mutableStateOf(10f) }

    val savingsP = savingsAmountStr.toDoubleOrNull() ?: 0.0
    val totalMonths = (projectionYears.toInt() * 12)

    val monthlyNominalRate = (nominalRate / 100.0) / 12.0
    val fvNominal = if (monthlyNominalRate == 0.0) {
        savingsP * totalMonths
    } else {
        savingsP * ((Math.pow(1.0 + monthlyNominalRate, totalMonths.toDouble()) - 1.0) / monthlyNominalRate)
    }

    // Fisher equation for real rate: (1 + nominal) / (1 + inflation) - 1
    val realAnnualRate = (1.0 + nominalRate / 100.0) / (1.0 + inflationRate / 100.0) - 1.0
    val monthlyRealRate = realAnnualRate / 12.0
    val fvReal = if (monthlyRealRate == 0.0) {
        savingsP * totalMonths
    } else {
        savingsP * ((Math.pow(1.0 + monthlyRealRate, totalMonths.toDouble()) - 1.0) / monthlyRealRate)
    }

    val inflationTax = maxOf(0.0, fvNominal - fvReal)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("future_savings_calculator_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "FUTURE SAVINGS POWER PROJECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Inflation-adjusted real purchasing power modeler",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = savingsAmountStr,
                    onValueChange = { savingsAmountStr = it },
                    label = { Text("Monthly Savings (${activeConfig.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("inflation_calculator_savings_input")
                )

                Button(
                    onClick = {
                        inflationRate = countryDefaultInflation.toFloat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1f)
                ) {
                    Text(
                        text = "Reset ${activeConfig.country}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Projection Years Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Projection Period", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("${projectionYears.toInt()} Years", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = projectionYears,
                    onValueChange = { projectionYears = it },
                    valueRange = 1f..40f,
                    steps = 39,
                    modifier = Modifier.testTag("inflation_calculator_years_slider")
                )
            }

            // Nominal Return Rate Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Expected Nominal APY", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text(String.format(Locale.US, "%.1f%%", nominalRate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = nominalRate,
                    onValueChange = { nominalRate = it },
                    valueRange = 0f..20f,
                    steps = 40,
                    modifier = Modifier.testTag("inflation_calculator_rate_slider")
                )
            }

            // Inflation Rate Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Inflation Rate (${activeConfig.country})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Info, contentDescription = "Jurisdiction default", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(String.format(Locale.US, "%.1f%%", inflationRate), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (inflationRate > 5) ExpenseRose else MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = inflationRate,
                    onValueChange = { inflationRate = it },
                    valueRange = 0f..15f,
                    steps = 30,
                    modifier = Modifier.testTag("inflation_calculator_inflation_slider")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Results Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PROJECTION RESULTS AT YEAR ${projectionYears.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Nominal Value Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Nominal Saved Balance:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(viewModel.formatCurrency(fvNominal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Real Value Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Real Purchasing Power (Adjusted):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(viewModel.formatCurrency(fvReal), fontSize = 14.sp, fontWeight = FontWeight.Black, color = FintechGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Inflation Tax Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = ExpenseRose)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Purchasing Power Lost to Inflation:", fontSize = 11.sp, color = ExpenseRose, fontWeight = FontWeight.Bold)
                        }
                        Text("-${viewModel.formatCurrency(inflationTax)}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ExpenseRose)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 With an inflation rate of ${String.format(Locale.US, "%.1f%%", inflationRate)}, your money will lose significant purchasing power over ${projectionYears.toInt()} years. Target nominal returns above inflation to grow real wealth.",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 9. MONTHLY TRANSACTIONS CALENDAR VISUALIZATION (LEDGER INSPECTION)
// =========================================================================
@Composable
fun MonthlyTransactionsCalendarComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance())
    }

    var selectedDay by remember { mutableStateOf<Int?>(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) }

    val currentYear = selectedCalendar.get(Calendar.YEAR)
    val currentMonthIdx = selectedCalendar.get(Calendar.MONTH) // 0-indexed

    val monthName = remember(currentMonthIdx, currentYear) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(selectedCalendar.time).uppercase()
    }

    val daysInMonth = remember(currentMonthIdx, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonthIdx)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val startDayOfWeek = remember(currentMonthIdx, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentYear)
        cal.set(Calendar.MONTH, currentMonthIdx)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
    }

    // Filter transactions belonging strictly to the selected year & month
    val currentMonthTransactions = remember(allTransactions, currentMonthIdx, currentYear) {
        allTransactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonthIdx
        }
    }

    // Group transactions by day of month
    val dailyTransactionsMap = remember(currentMonthTransactions) {
        val map = mutableMapOf<Int, MutableList<com.example.data.model.TransactionEntity>>()
        for (tx in currentMonthTransactions) {
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val day = txCal.get(Calendar.DAY_OF_MONTH)
            map.getOrPut(day) { mutableListOf() }.add(tx)
        }
        map
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_transactions_calendar_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MONTHLY TRANSACTIONS LEDGER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap any date to inspect daily itemized details",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.primary)
                    }

                    Text(
                        text = monthName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            selectedCalendar = (selectedCalendar.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Columns (Days of week headings)
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")
                weekDays.forEach { dayHead ->
                    Text(
                        text = dayHead,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Grid Layout (Using nested Rows/Columns to prevent infinite height issues inside LazyColumns)
            val totalCells = daysInMonth + startDayOfWeek - 1
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIdx = row * 7 + col
                            val dayNumber = cellIdx - startDayOfWeek + 2

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.95f)
                                    .padding(2.dp)
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = selectedDay == dayNumber
                                    val dayTxs = dailyTransactionsMap[dayNumber] ?: emptyList()

                                    val netAmt = dayTxs.sumOf { tx ->
                                        if (tx.type == "INCOME") tx.amount else -tx.amount
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        ),
                                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                selectedDay = dayNumber
                                            }
                                            .testTag("calendar_day_cell_$dayNumber")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize().padding(2.dp)
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )

                                            // Mini Net indicators or dots
                                            if (dayTxs.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(1.dp))
                                                if (isSelected) {
                                                    // Simple dot on selected
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary))
                                                } else {
                                                    // Mini Net Amount
                                                    Text(
                                                        text = if (netAmt >= 0) {
                                                            "+" + String.format(Locale.US, "%.0f", netAmt)
                                                        } else {
                                                            "-" + String.format(Locale.US, "%.0f", Math.abs(netAmt))
                                                        },
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = if (netAmt >= 0) FintechGreen else ExpenseRose,
                                                        maxLines = 1
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

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Daily Detailed summary panel
            val currentSelectedDay = selectedDay
            if (currentSelectedDay != null) {
                val dayTxs = dailyTransactionsMap[currentSelectedDay] ?: emptyList()
                val dayIn = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val dayOut = dayTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETAILS FOR DATE: $currentSelectedDay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (dayIn > 0) {
                                Text(
                                    text = "IN: +${viewModel.formatCurrency(dayIn)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FintechGreen
                                )
                            }
                            if (dayOut > 0) {
                                Text(
                                    text = "OUT: -${viewModel.formatCurrency(dayOut)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRose
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (dayTxs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ledger entries recorded on this day.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayTxs.forEach { tx ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (tx.merchant.isNotEmpty()) tx.merchant else "Self/Transfer",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (tx.notes.isNotEmpty()) {
                                                Text(
                                                    text = tx.notes,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (tx.type == "INCOME") "+" + viewModel.formatCurrency(tx.amount) else "-" + viewModel.formatCurrency(tx.amount),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (tx.type == "INCOME") FintechGreen else ExpenseRose
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

// =========================================================================
// 10. INTERACTIVE ASSETS DISTRIBUTION DONUT CHART WIDGET
// =========================================================================
@Composable
fun InteractiveAssetDistributionChartComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    // 1. Calculate live assets from actual account database entities
    val dbCash = accounts.filter { it.type == "CASH" || it.type == "MOBILE_WALLET" }.sumOf { it.balance }
    val dbSavings = accounts.filter { it.type == "BANK" && !it.name.lowercase(Locale.US).contains("invest") }.sumOf { it.balance }
    val dbInvestments = accounts.filter {
        val nameL = it.name.lowercase(Locale.US)
        nameL.contains("invest") || nameL.contains("stock") || nameL.contains("brokerage") || nameL.contains("crypto") || nameL.contains("ira")
    }.sumOf { it.balance }

    // User customization option (lets them customize/simulate asset allocation weights easily!)
    var isSimulationMode by remember { mutableStateOf(false) }
    var customCashStr by remember { mutableStateOf("15000") }
    var customSavingsStr by remember { mutableStateOf("25000") }
    var customInvestStr by remember { mutableStateOf("45000") }

    val liveTotal = dbCash + dbSavings + dbInvestments
    val hasLiveBalances = liveTotal > 0.0

    // Auto-disable simulation mode if we have real accounts but let the user toggle it back on for playground!
    val activeCash = if (isSimulationMode || !hasLiveBalances) (customCashStr.toDoubleOrNull() ?: 0.0) else dbCash
    val activeSavings = if (isSimulationMode || !hasLiveBalances) (customSavingsStr.toDoubleOrNull() ?: 0.0) else dbSavings
    val activeInvestments = if (isSimulationMode || !hasLiveBalances) (customInvestStr.toDoubleOrNull() ?: 0.0) else dbInvestments

    val grandTotal = activeCash + activeSavings + activeInvestments
    var selectedSliceIndex by remember { mutableStateOf(-1) } // -1 means total

    val primaryColor = MaterialTheme.colorScheme.primary
    val assetList = remember(activeCash, activeSavings, activeInvestments, grandTotal, primaryColor) {
        listOf(
            Triple("CASH & WALLETS", activeCash, FintechGreen),
            Triple("BANK SAVINGS", activeSavings, primaryColor),
            Triple("INVESTMENTS", activeInvestments, AccentGold)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("assets_donut_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "NET ASSET DISTRIBUTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Native interactive high-fidelity donut visualization",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Simulation mode toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Simulation", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isSimulationMode || !hasLiveBalances,
                        onCheckedChange = { isSimulationMode = it },
                        modifier = Modifier
                            .testTag("asset_donut_simulation_toggle"),
                        enabled = hasLiveBalances // only disabled if they have no other options
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Donut Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Donut Canvas (Replaces web-based D3.js seamlessly)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(135.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        assetList.forEachIndexed { index, triple ->
                            val amt = triple.second
                            val color = triple.third
                            val sweepAngle = if (grandTotal > 0.0) ((amt / grandTotal) * 360f).toFloat() else 0f
                            val isSelected = selectedSliceIndex == index
                            val strokeWidthValue = if (isSelected) 15.dp.toPx() else 9.dp.toPx()

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidthValue)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center Interactive Labels
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selectedSliceIndex in 0..2) {
                            val activeItem = assetList[selectedSliceIndex]
                            val pct = if (grandTotal > 0.0) (activeItem.second / grandTotal) * 100 else 0.0
                            Text(
                                text = activeItem.first,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = activeItem.third,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = viewModel.formatCurrency(activeItem.second),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f%%", pct),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        } else {
                            Text(
                                text = "TOTAL ASSETS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = viewModel.formatCurrency(grandTotal),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Interactive legend panel
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    assetList.forEachIndexed { index, triple ->
                        val isSelected = selectedSliceIndex == index
                        val pct = if (grandTotal > 0.0) (triple.second / grandTotal) * 100 else 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedSliceIndex = if (isSelected) -1 else index
                                }
                                .background(if (isSelected) triple.third.copy(alpha = 0.08f) else Color.Transparent)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(triple.third)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = triple.first,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = viewModel.formatCurrency(triple.second),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.0f%%", pct),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Input Fields if Simulator Mode active
            if (isSimulationMode || !hasLiveBalances) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCashStr,
                        onValueChange = { customCashStr = it },
                        label = { Text("Cash (${activeConfig.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("donut_simulate_cash_field")
                    )
                    OutlinedTextField(
                        value = customSavingsStr,
                        onValueChange = { customSavingsStr = it },
                        label = { Text("Savings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("donut_simulate_savings_field")
                    )
                    OutlinedTextField(
                        value = customInvestStr,
                        onValueChange = { customInvestStr = it },
                        label = { Text("Investments") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("donut_simulate_invest_field")
                    )
                }
            }
        }
    }
}

// =========================================================================
// 11. EMERGENCY FUND OPTIMIZER & RUNWAY ASSESSOR
// =========================================================================
@Composable
fun EmergencyFundOptimizerComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeRecurring by viewModel.allRecurring.collectAsState()
    val activeConfig by viewModel.activeCountryConfig.collectAsState()

    // 1. Calculate current cash reserves (CASH + MOBILE_WALLET + BANK)
    val currentCashReserves = remember(accounts) {
        accounts.filter { it.type == "CASH" || it.type == "MOBILE_WALLET" || it.type == "BANK" }.sumOf { it.balance }
    }

    // 2. Fetch baseline fixed monthly expenses (default to total active recurring expenses, else fallback)
    val recurringExpenseTotal = remember(activeRecurring) {
        activeRecurring.filter { it.type == "EXPENSE" && it.isActive }.sumOf { it.amount }
    }
    val defaultFixedExpenses = if (recurringExpenseTotal > 0) recurringExpenseTotal else 2000.0

    var fixedExpensesStr by remember(defaultFixedExpenses) { mutableStateOf(String.format(Locale.US, "%.0f", defaultFixedExpenses)) }
    var selectedTargetMultiplier by remember { mutableStateOf(6) } // Default standard 6 months cushion

    val monthlyFixedExpenses = fixedExpensesStr.toDoubleOrNull() ?: 1.0
    val survivalMonths = currentCashReserves / monthlyFixedExpenses

    val recommendedTargetAmount = monthlyFixedExpenses * selectedTargetMultiplier
    val coverageFraction = if (recommendedTargetAmount > 0) (currentCashReserves / recommendedTargetAmount).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("emergency_fund_optimizer_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "EMERGENCY BUFFER OPTIMIZER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Audit safety cushions and runway survival reserves",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fixedExpensesStr,
                    onValueChange = { fixedExpensesStr = it },
                    label = { Text("Monthly Fixed Expenses (${activeConfig.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("emergency_fixed_expenses_field")
                )

                // Quick selector for safety multipliers
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Safety Cushion", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(3, 6, 12).forEach { multiplier ->
                            val isSelected = selectedTargetMultiplier == multiplier
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable { selectedTargetMultiplier = multiplier }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${multiplier}M",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live status assessments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENT RESERVES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(viewModel.formatCurrency(currentCashReserves), fontSize = 15.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("SURVIVAL RUNWAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        text = if (survivalMonths >= 100) "99+ Months" else String.format(Locale.US, "%.1f Months", survivalMonths),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (survivalMonths >= selectedTargetMultiplier) FintechGreen else if (survivalMonths >= 1.5) AccentGold else ExpenseRose
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target and Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Progress to recommended ${selectedTargetMultiplier}-month cushion",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${(coverageFraction * 100).toInt()}% Met (${viewModel.formatCurrency(recommendedTargetAmount)})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (coverageFraction >= 1.0f) FintechGreen else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = coverageFraction,
                color = if (coverageFraction >= 1.0f) FintechGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("emergency_fund_progress_bar")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Text Recommendation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = (if (coverageFraction >= 1.0f) FintechGreen else if (coverageFraction >= 0.5f) AccentGold else ExpenseRose).copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = when {
                        coverageFraction >= 1.0f -> "✅ EXCELLENT SECURITY! Your reserves of ${viewModel.formatCurrency(currentCashReserves)} fully cover the recommended safety cap. You have a resilient cushion."
                        coverageFraction >= 0.5f -> "⚠️ MODERATE COVERAGE. You are halfway to your ideal 6-month safety net of ${viewModel.formatCurrency(recommendedTargetAmount)}. Try setting aside regular contributions."
                        else -> "🚨 CRITICAL BUFFER GAP! Your current runway lasts only ${String.format(Locale.US, "%.1f", survivalMonths)} months. Prioritize building an emergency cap to avoid overdraft cycles."
                    },
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (coverageFraction >= 1.0f) FintechGreen else if (coverageFraction >= 0.5f) AccentGold else ExpenseRose
                )
            }
        }
    }
}

// =========================================================================
// 12. SUBSCRIPTION INTELLIGENCE (DETECTION, PRICE ALERTS & DORMANCY)
// =========================================================================
@Composable
fun SubscriptionIntelligenceComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val activeRecurring by viewModel.allRecurring.collectAsState()

    // Heuristic: Auto-detect subscriptions by scanning expense ledger for repeats or subscription keys
    val detectedSubscriptions = remember(allTransactions, activeRecurring) {
        val subsList = mutableListOf<DetectedSubscription>()

        // 1. Group transaction expenses by merchant name
        val expenseTxs = allTransactions.filter { it.type == "EXPENSE" && it.merchant.isNotEmpty() }
        val merchantGroups = expenseTxs.groupBy { it.merchant.lowercase(Locale.US).trim() }

        for ((merchantLower, txs) in merchantGroups) {
            val sortedTxs = txs.sortedBy { it.timestamp }
            val count = sortedTxs.size

            // Keywords matching or repeat payments
            val isKnownSub = merchantLower.contains("netflix") || merchantLower.contains("spotify") ||
                             merchantLower.contains("youtube") || merchantLower.contains("disney") ||
                             merchantLower.contains("amazon prime") || merchantLower.contains("apple") ||
                             merchantLower.contains("cloud") || merchantLower.contains("adobe") ||
                             merchantLower.contains("gym") || merchantLower.contains("zoom") ||
                             merchantLower.contains("canva") || merchantLower.contains("github") ||
                             merchantLower.contains("chatgpt") || merchantLower.contains("openai") ||
                             merchantLower.contains("subscription") || merchantLower.contains("membership")

            val isRepeating = count >= 2

            if (isKnownSub || isRepeating) {
                // Check for price increases in consecutive payments
                var hasPriceHike = false
                var previousPrice = 0.0
                var currentPrice = 0.0
                var hikePct = 0.0

                if (count >= 2) {
                    val prevTx = sortedTxs[count - 2]
                    val latestTx = sortedTxs[count - 1]
                    if (latestTx.amount > prevTx.amount) {
                        hasPriceHike = true
                        previousPrice = prevTx.amount
                        currentPrice = latestTx.amount
                        hikePct = ((latestTx.amount - prevTx.amount) / prevTx.amount) * 100
                    } else {
                        currentPrice = latestTx.amount
                    }
                } else if (count == 1) {
                    currentPrice = sortedTxs[0].amount
                }

                // Check for dormancy: recurring item is active in database schedule, but no transaction has been registered in the past 45 days
                val isDormantInScheduler = activeRecurring.any { rec ->
                    rec.merchant.lowercase(Locale.US).contains(merchantLower) &&
                    rec.isActive &&
                    (System.currentTimeMillis() - rec.lastExecutionTimestamp > 45 * 24 * 60 * 60 * 1000L)
                }

                val lastPaidTimestamp = sortedTxs.last().timestamp
                val daysSinceLastPaid = (System.currentTimeMillis() - lastPaidTimestamp) / (24 * 60 * 60 * 1000)
                val isUnusedDormant = daysSinceLastPaid > 45

                subsList.add(
                    DetectedSubscription(
                        merchantName = sortedTxs[0].merchant,
                        latestAmount = currentPrice,
                        previousAmount = previousPrice,
                        hasPriceIncrease = hasPriceHike,
                        priceIncreasePercent = hikePct,
                        isDormant = isDormantInScheduler || isUnusedDormant,
                        daysSinceLastCharge = daysSinceLastPaid,
                        frequency = "Monthly"
                    )
                )
            }
        }
        subsList.distinctBy { it.merchantName.lowercase(Locale.US).trim() }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("subscription_intelligence_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SUBSCRIPTION INTELLIGENCE & ALERTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Price hike detector and dormant payment audits",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (detectedSubscriptions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active subscription patterns detected inside ledger.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detectedSubscriptions.forEach { sub ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (sub.hasPriceIncrease || sub.isDormant) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                }
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = sub.merchantName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Estimated frequency: ${sub.frequency}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = viewModel.formatCurrency(sub.latestAmount),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "last charged ${sub.daysSinceLastCharge}d ago",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                // Interactive price hike and dormancy tags
                                if (sub.hasPriceIncrease || sub.isDormant) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (sub.hasPriceIncrease) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ExpenseRose.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = String.format(Locale.US, "🚨 PRICE INCREASED! (+%.1f%%)", sub.priceIncreasePercent),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = ExpenseRose
                                                )
                                            }
                                        }

                                        if (sub.isDormant) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AccentGold.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "⚠️ DORMANT / UNUSED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = AccentGold
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
    }
}

data class DetectedSubscription(
    val merchantName: String,
    val latestAmount: Double,
    val previousAmount: Double,
    val hasPriceIncrease: Boolean,
    val priceIncreasePercent: Double,
    val isDormant: Boolean,
    val daysSinceLastCharge: Long,
    val frequency: String
)

@Composable
fun SmartSubscriptionDetectorComponent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val suggestions by viewModel.recurringSuggestions.collectAsState()

    if (suggestions.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("subscription_detector_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SUGGESTED RECURRING EXPENSES & SUBSCRIPTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Analysis engine detected ${suggestions.size} un-tracked recurring pattern(s)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            suggestions.forEach { sug ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sug.merchant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${sug.sampleNotes} • ~${viewModel.formatCurrency(sug.estimatedAmount)} / ${sug.frequency.lowercase()}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.convertSuggestionToRecurring(sug) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("track_recurring_${sug.merchant.lowercase().replace(" ", "_")}")
                        ) {
                            Text("Track Recurring", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


