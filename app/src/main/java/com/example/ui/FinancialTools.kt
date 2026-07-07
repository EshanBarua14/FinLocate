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
                                        imageVector = Icons.Default.ReceiptLong,
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
                        imageVector = Icons.Default.TrendingDown,
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
