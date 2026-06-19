package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen
import com.example.ui.theme.AccentGold
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }

    // Human-readable active month name
    val monthTitle = remember(selectedMonth) {
        try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
            parser.parse(selectedMonth)?.let { formatter.format(it) } ?: selectedMonth
        } catch (e: Exception) {
            selectedMonth
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- MONTH SWITCHER BAR ---
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
                    IconButton(
                        onClick = { viewModel.decrementMonth() },
                        modifier = Modifier.testTag("prev_month_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                    }
                    Text(
                        text = monthTitle.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("month_label_text")
                    )
                    IconButton(
                        onClick = { viewModel.incrementMonth() },
                        modifier = Modifier.testTag("next_month_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            // --- HISTORIC ENTRIES LIST ---
            if (transactions.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "NO TRANSACTIONS IN $monthTitle",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '+' floating button to initiate rapid logging.",
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
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItemRow(
                            transaction = tx,
                            account = accounts.find { it.id == tx.accountId },
                            toAccount = if (tx.toAccountId != -1L) accounts.find { it.id == tx.toAccountId } else null,
                            category = categories.find { it.id == tx.categoryId },
                            formatter = { viewModel.formatCurrency(it) },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }

        // --- RAPID MANUAL ENTRY MODAL SHEET ---
        if (showAddSheet) {
            AddTransactionSheet(
                categories = categories,
                accounts = accounts,
                countryConfig = config,
                onDismiss = { showAddSheet = false },
                onAdd = { amount, type, categoryId, accountId, toAccountId, merchant, isDeductible, notes, split ->
                    viewModel.addTransaction(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        accountId = accountId,
                        toAccountId = toAccountId,
                        merchant = merchant,
                        isTaxDeductible = isDeductible,
                        notes = notes,
                        splitCount = split
                    )
                    showAddSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    countryConfig: CountryConfig,
    onDismiss: () -> Unit,
    onAdd: (Double, String, Long, Long, Long, String, Boolean, String, Int) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, TRANSFER
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var selectedAccountId by remember { mutableStateOf(0L) }
    var toAccountId by remember { mutableStateOf(-1L) }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isTaxDeductible by remember { mutableStateOf(false) }
    var splitCount by remember { mutableStateOf(1) }

    // Seed defaults based on list contents
    LaunchedEffect(categories, accounts, transactionType) {
        val filteredCats = categories.filter { it.isIncome == (transactionType == "INCOME") }
        if (selectedCategoryId == 0L || categories.none { it.id == selectedCategoryId && it.isIncome == (transactionType == "INCOME") }) {
            selectedCategoryId = filteredCats.firstOrNull()?.id ?: 0L
        }
        if (selectedAccountId == 0L && accounts.isNotEmpty()) {
            selectedAccountId = accounts.firstOrNull()?.id ?: 0L
        }
        if (toAccountId == -1L && transactionType == "TRANSFER" && accounts.size > 1) {
            toAccountId = accounts.getOrNull(1)?.id ?: -1L
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("add_transaction_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAPID ENTRY LOG",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Logging")
                }
            }

            // TYPE SELECTOR SEGMENTS
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf("EXPENSE", "INCOME", "TRANSFER")
                types.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = transactionType == type,
                        onClick = { transactionType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = types.size)
                    ) {
                        Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // AMOUNT TEXT FIELD
            OutlinedTextField(
                value = amountStr,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amountStr = it },
                label = { Text("Amount (${countryConfig.currencySymbol})", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )

            // ONE-HAND RAPID AMOUNT PRESETS (<1sec taps)
            val presetAmounts = remember(countryConfig.currency) {
                when (countryConfig.currency) {
                    "BDT" -> listOf(100.0, 500.0, 1000.0, 5000.0)
                    "INR" -> listOf(100.0, 500.0, 1000.0, 2000.0)
                    "EUR" -> listOf(5.0, 10.0, 20.0, 100.0)
                    else -> listOf(5.0, 10.0, 20.0, 50.0) // USD defaults
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetAmounts.forEach { amt ->
                    Button(
                        onClick = { amountStr = amt.toInt().toString() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("${countryConfig.currencySymbol}${amt.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // ACCOUNT / SOURCE SELECTOR
                Column(modifier = Modifier.weight(1f)) {
                    Text("Source Wallet/Bank", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    var expandedAcc by remember { mutableStateOf(false) }
                    val currentAccName = accounts.find { it.id == selectedAccountId }?.name ?: "Cash"
                    Button(
                        onClick = { expandedAcc = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentAccName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${countryConfig.currencySymbol}${acc.balance})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    expandedAcc = false
                                }
                            )
                        }
                    }
                }

                // TO ACCOUNT SELECTOR (ONLY FOR TRANSFERS)
                if (transactionType == "TRANSFER") {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Destination Bank", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        var expandedTo by remember { mutableStateOf(false) }
                        val currentToName = accounts.find { it.id == toAccountId }?.name ?: "Select..."
                        Button(
                            onClick = { expandedTo = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentToName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                            accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        toAccountId = acc.id
                                        expandedTo = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // CATEGORY SELECTOR (ONLY EX/IN)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        var expandedCat by remember { mutableStateOf(false) }
                        val currentCatName = categories.find { it.id == selectedCategoryId }?.name ?: "Select..."
                        Button(
                            onClick = { expandedCat = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentCatName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                            val activeGroup = categories.filter { it.isIncome == (transactionType == "INCOME") }
                            activeGroup.forEach { cat ->
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
                }
            }

            // SUBFEATURE FIELDS
            if (transactionType == "EXPENSE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tax Deductible Receipt", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Toggle tax tracking for ${countryConfig.country} fiscal summaries", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = isTaxDeductible,
                        onCheckedChange = { isTaxDeductible = it },
                        modifier = Modifier.testTag("tax_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Split Bill Calculation", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Divide expense across friends / colleagues", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (splitCount > 1) splitCount-- }) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrement")
                        }
                        Text("$splitCount Payees", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { splitCount++ }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increment")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Counterparty", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Description / Notes", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amtVal = amountStr.toDoubleOrNull() ?: 0.0
                    if (amtVal > 0) {
                        onAdd(
                            amtVal,
                            transactionType,
                            if (transactionType == "TRANSFER") 0L else selectedCategoryId,
                            selectedAccountId,
                            toAccountId,
                            merchant,
                            isTaxDeductible,
                            notes,
                            splitCount
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_transaction_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RECORD TRANSACTION (≤5s)", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    account: AccountEntity?,
    toAccount: AccountEntity?,
    category: CategoryEntity?,
    formatter: (Double) -> String,
    onDelete: () -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
        sdf.format(Date(transaction.timestamp))
    }

    val typeColor = when (transaction.type) {
        "INCOME" -> FintechGreen
        "EXPENSE" -> ExpenseRose
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    }

    val icon = when (transaction.type) {
        "INCOME" -> Icons.Default.ArrowUpward
        "EXPENSE" -> Icons.Default.ArrowDownward
        else -> Icons.Default.SwapHoriz
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.type,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val primaryText = if (transaction.type == "TRANSFER") {
                    "Transfer: ${account?.name ?: "Wallet"} ➔ ${toAccount?.name ?: "Checking"}"
                } else {
                    category?.name ?: "Unassigned"
                }

                Text(
                    text = primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.merchant.ifEmpty { "Direct Ledger" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (transaction.isTaxDeductible) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("TAX DEDUCT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                        }
                    }
                    if (transaction.splitCount > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text("SPLIT ÷${transaction.splitCount}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text(
                    text = "$dateStr • ${account?.name ?: "Cash"}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val dispAmount = if (transaction.splitCount > 1) {
                    "${formatter(transaction.amount)} (÷${transaction.splitCount} = ${formatter(transaction.amount / transaction.splitCount)})"
                } else {
                    formatter(transaction.amount)
                }

                Text(
                    text = if (transaction.type == "INCOME") "+ $dispAmount" else "- $dispAmount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_tx_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
