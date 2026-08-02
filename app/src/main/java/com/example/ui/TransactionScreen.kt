package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import android.graphics.Bitmap
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen
import com.example.ui.theme.AccentGold
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    val aiInsightsLoading by viewModel.aiInsightsLoading.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var activeTabMode by remember { mutableStateOf("LOCAL") }

    var showAddSheet by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Search and Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIdFilter by remember { mutableStateOf<Long?>(null) }
    var minAmountQuery by remember { mutableStateOf("") }
    var maxAmountQuery by remember { mutableStateOf("") }
    var selectedCurrencyFilter by remember { mutableStateOf<String?>(null) }
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }
    
    // date limit ranges (offsets in millis)
    var dateRangePreset by remember { mutableStateOf("ALL") } // ALL, 7DAYS, 30DAYS
    val resolvedStartDate = remember(dateRangePreset) {
        when (dateRangePreset) {
            "7DAYS" -> System.currentTimeMillis() - (7L * 24 * 3600 * 1000)
            "30DAYS" -> System.currentTimeMillis() - (30L * 24 * 3600 * 1000)
            else -> null
        }
    }

    var showAdvancedFilters by remember { mutableStateOf(false) }

    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    val allTagsList = remember(transactions) {
        transactions.flatMap { tx ->
            tx.tags.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
        }.distinct().sorted()
    }

    // Compute the robust offline in-memory filtered transactions
    val displayedTransactions = remember(
        transactions, searchQuery, selectedCategoryIdFilter, minAmountQuery, maxAmountQuery, 
        resolvedStartDate, categories, selectedCurrencyFilter, customStartDate, customEndDate, config,
        selectedTagFilter
    ) {
        transactions.filter { tx ->
            // Search text matching
            val matchesSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                tx.merchant.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.tags.contains(searchQuery, ignoreCase = true) ||
                (categories.find { it.id == tx.categoryId }?.name?.contains(searchQuery, ignoreCase = true) == true)
            }

            // Category matching
            val matchesCategory = if (selectedCategoryIdFilter == null) {
                true
            } else {
                tx.categoryId == selectedCategoryIdFilter
            }

            // Amount minimum matching
            val minAmt = minAmountQuery.toDoubleOrNull()
            val matchesMin = minAmt == null || tx.amount >= minAmt

            // Amount maximum matching
            val maxAmt = maxAmountQuery.toDoubleOrNull()
            val matchesMax = maxAmt == null || tx.amount <= maxAmt

            // Date preset matching
            val matchesDate = resolvedStartDate == null || tx.timestamp >= resolvedStartDate

            // Custom exact date range matching YYYY-MM-DD
            val matchesCustomDate = try {
                if (customStartDate.isEmpty() && customEndDate.isEmpty()) {
                    true
                } else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val startMillis = if (customStartDate.isNotEmpty()) sdf.parse(customStartDate)?.time else null
                    val endMillis = if (customEndDate.isNotEmpty()) {
                        val parsed = sdf.parse(customEndDate)
                        parsed?.let { it.time + 24 * 3600 * 1000 - 1 }
                    } else null
                    
                    val matchesStart = startMillis == null || tx.timestamp >= startMillis
                    val matchesEnd = endMillis == null || tx.timestamp <= endMillis
                    matchesStart && matchesEnd
                }
            } catch (e: Exception) {
                true
            }

            // Currency matching
            val matchesCurrency = if (selectedCurrencyFilter == null) {
                true
            } else {
                val noteStr = tx.notes.lowercase()
                noteStr.contains(selectedCurrencyFilter!!.lowercase()) || 
                (selectedCurrencyFilter!! == config.currency && !noteStr.contains("usd") && !noteStr.contains("eur") && !noteStr.contains("gbp") && !noteStr.contains("jpy") && !noteStr.contains("cad") && !noteStr.contains("aud") && !noteStr.contains("inr") && !noteStr.contains("sgd") && !noteStr.contains("bdt"))
            }

            // Custom tag matching
            val matchesTag = if (selectedTagFilter == null) {
                true
            } else {
                tx.tags.split(",")
                    .map { it.trim().lowercase() }
                    .contains(selectedTagFilter!!.lowercase())
            }

            matchesSearch && matchesCategory && matchesMin && matchesMax && matchesDate && matchesCustomDate && matchesCurrency && matchesTag
        }
    }

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

            if (aiInsightsLoading) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("ai_insights_loading_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .testTag("ai_insights_loading_spinner")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI-Engine Analyzing Spend & Fiscal Trends...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // --- SEARCH AND FILTER INTERACTIVE BAR ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Main Search TextField & Advanced toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search merchant or description...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search icon",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("search_input_field")
                        )

                        IconButton(
                            onClick = { showAdvancedFilters = !showAdvancedFilters },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (showAdvancedFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("toggle_filters_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Advanced Filters Toggle",
                                tint = if (showAdvancedFilters) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.exportReportToCsv(context) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("ledger_export_csv_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV Backup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("ledger_import_csv_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import CSV Backup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.exportReportToPdf(context) },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .testTag("ledger_export_pdf_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF Statement",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Collapsible Advanced Filter Suite
                    if (showAdvancedFilters) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                        
                        // Amount Range Filter Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = minAmountQuery,
                                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) minAmountQuery = it },
                                label = { Text("Min Value", fontSize = 9.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("filter_min_amount")
                            )

                            OutlinedTextField(
                                value = maxAmountQuery,
                                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) maxAmountQuery = it },
                                label = { Text("Max Value", fontSize = 9.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("filter_max_amount")
                            )
                        }

                        // Date Range Preset Filter Chips
                        Column {
                            Text(
                                "Date limit metrics:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("ALL" to "All Dates", "7DAYS" to "Last 7 days", "30DAYS" to "Last 30 days").forEach { (valStr, labelVal) ->
                                    val isSelected = dateRangePreset == valStr
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { dateRangePreset = valStr },
                                        label = { Text(labelVal, fontSize = 10.sp) },
                                        modifier = Modifier.testTag("chip_date_$valStr")
                                    )
                                }
                            }
                        }

                        // Custom Date Range Inputs
                        Column {
                            Text(
                                "Custom Date Range (YYYY-MM-DD):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customStartDate,
                                    onValueChange = { customStartDate = it },
                                    placeholder = { Text("Start e.g. 2026-06-01", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("filter_custom_start_date")
                                )
                                OutlinedTextField(
                                    value = customEndDate,
                                    onValueChange = { customEndDate = it },
                                    placeholder = { Text("End e.g. 2026-06-30", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("filter_custom_end_date")
                                )
                            }
                        }

                        // Category Filter Dropdown / Tap selections
                        Column {
                            Text(
                                "Filter by specific category:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isAllSelected = selectedCategoryIdFilter == null
                                    FilterChip(
                                        selected = isAllSelected,
                                        onClick = { selectedCategoryIdFilter = null },
                                        label = { Text("All Categories", fontSize = 10.sp) },
                                        modifier = Modifier.testTag("chip_category_all")
                                    )
                                }
                                items(categories) { cat ->
                                    val isSelected = selectedCategoryIdFilter == cat.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategoryIdFilter = cat.id },
                                        label = { Text(cat.name, fontSize = 10.sp) },
                                        modifier = Modifier.testTag("chip_category_${cat.id}")
                                    )
                                }
                            }
                        }

                        // Currency Filter Tag selector
                        Column {
                            Text(
                                "Filter by currency tag:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    val isAllSelected = selectedCurrencyFilter == null
                                    FilterChip(
                                        selected = isAllSelected,
                                        onClick = { selectedCurrencyFilter = null },
                                        label = { Text("All Currencies", fontSize = 10.sp) },
                                        modifier = Modifier.testTag("chip_currency_all")
                                    )
                                }
                                val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "INR", "SGD", "BDT")
                                items(currencies) { curr ->
                                    val isSelected = selectedCurrencyFilter == curr
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCurrencyFilter = curr },
                                        label = { Text(curr, fontSize = 10.sp) },
                                        modifier = Modifier.testTag("chip_currency_$curr")
                                    )
                                }
                            }
                        }

                        // Custom Tag Filter Row
                        if (allTagsList.isNotEmpty()) {
                            Column {
                                Text(
                                    "Filter by custom tag:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        val isAllSelected = selectedTagFilter == null
                                        FilterChip(
                                            selected = isAllSelected,
                                            onClick = { selectedTagFilter = null },
                                            label = { Text("All Tags", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("chip_tag_all")
                                        )
                                    }
                                    items(allTagsList) { tag ->
                                        val isSelected = selectedTagFilter == tag
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedTagFilter = tag },
                                            label = { Text("#$tag", fontSize = 10.sp) },
                                            modifier = Modifier.testTag("chip_tag_$tag")
                                        )
                                    }
                                }
                            }
                        }

                        // Reset Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategoryIdFilter = null
                                    minAmountQuery = ""
                                    maxAmountQuery = ""
                                    selectedCurrencyFilter = null
                                    selectedTagFilter = null
                                    customStartDate = ""
                                    customEndDate = ""
                                    dateRangePreset = "ALL"
                                },
                                modifier = Modifier.height(32.dp).testTag("reset_filters_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Filters", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset filters", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // --- CLOUD VS LOCAL SELECTOR TABS ---
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cloudResults by viewModel.cloudSearchResult.collectAsState()
                    val cloudLoading by viewModel.cloudSearchLoading.collectAsState()
                    
                    Button(
                        onClick = { activeTabMode = "LOCAL" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTabMode == "LOCAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (activeTabMode == "LOCAL") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("local_tab_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Local Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { activeTabMode = "CLOUD" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTabMode == "CLOUD") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (activeTabMode == "CLOUD") MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("cloud_tab_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (cloudLoading) "Loading..." else "Cloud Vault (${cloudResults.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- HISTORIC ENTRIES LIST ---
            if (activeTabMode == "LOCAL") {
                if (displayedTransactions.isEmpty()) {
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
                                text = if (searchQuery.isNotEmpty() || selectedCategoryIdFilter != null || minAmountQuery.isNotEmpty() || maxAmountQuery.isNotEmpty() || dateRangePreset != "ALL") {
                                    "NO RESULTS MATCH THOSE FILTERS"
                                } else {
                                    "NO TRANSACTIONS IN $monthTitle"
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try clearing active query variables or add new entries.",
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
                        items(displayedTransactions, key = { it.id }) { tx ->
                            TransactionItemRow(
                                transaction = tx,
                                account = accounts.find { it.id == tx.accountId },
                                toAccount = if (tx.toAccountId != -1L) accounts.find { it.id == tx.toAccountId } else null,
                                category = categories.find { it.id == tx.categoryId },
                                formatter = { viewModel.formatCurrency(it) },
                                onDelete = { viewModel.deleteTransaction(tx) },
                                onUpdate = { viewModel.updateTransaction(it) }
                            )
                        }
                    }
                }
            } else {
                // Cloud Search UI Mode
                val cloudResults by viewModel.cloudSearchResult.collectAsState()
                val cloudLoading by viewModel.cloudSearchLoading.collectAsState()
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "CLOUD QUERY PARAMETERS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.5.sp
                            )
                            if (cloudLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            var cloudCatFilter by remember { mutableStateOf("") }
                            
                            OutlinedTextField(
                                value = cloudCatFilter,
                                onValueChange = { cloudCatFilter = it },
                                placeholder = { Text("Filter Category Name (e.g. food)", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("cloud_filter_category")
                            )
                            
                            Button(
                                onClick = {
                                    viewModel.performCloudSearch(
                                        startDate = if (customStartDate.isNotEmpty()) customStartDate else null,
                                        endDate = if (customEndDate.isNotEmpty()) customEndDate else null,
                                        categoryName = cloudCatFilter,
                                        keyword = searchQuery
                                    ) { errMsg ->
                                        if (errMsg != null) {
                                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Cloud query found ${cloudResults.size} matches!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("run_cloud_search_btn")
                            ) {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Matching keywords: '$searchQuery' ${if (customStartDate.isNotEmpty()) "from $customStartDate" else ""} ${if (customEndDate.isNotEmpty()) "to $customEndDate" else ""}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                if (cloudLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else if (cloudResults.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Cloud Search results matching criteria found.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "Try broadening your query variables or tapping 'Search API' to poll the Cloud Vault.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center
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
                        items(cloudResults) { cloudTx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_search_result_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cloudTx.merchant.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = cloudTx.categoryName.uppercase() + " • " + cloudTx.accountName.uppercase(),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (cloudTx.notes.isNotEmpty()) {
                                            Text(cloudTx.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }
                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                        Text(sdf.format(Date(cloudTx.timestamp)), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                    Text(
                                        text = (if (cloudTx.type == "Expense") "-" else "+") + viewModel.formatCurrency(cloudTx.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (cloudTx.type == "Expense") ExpenseRose else FintechGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- RAPID MANUAL ENTRY MODAL SHEET / OVERLAY ---
        if (showAddSheet) {
            RapidEntryScreen(
                viewModel = viewModel,
                onDismiss = { showAddSheet = false }
            )
        }

        if (showImportDialog) {
            CsvImportDialog(
                viewModel = viewModel,
                onDismissRequest = { showImportDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var csvText by remember { mutableStateOf("") }
    
    // Setup file content picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val content = inputStream?.bufferedReader().use { r -> r?.readText() } ?: ""
                csvText = content
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read empty or invalid file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("csv_import_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("CSV Migration Center", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Transfer transactions from other systems seamlessly. Columns mapping:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                // Show mapping column helpers
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Date, Merchant, Amount, Type, Category, Notes, Tags\ne.g. 2026-06-25, Starbucks, 4.50, EXPENSE, Food, Coffee, breakfast",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch("text/*") },
                        modifier = Modifier.weight(1f).testTag("csv_select_file_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Browse File...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            csvText = """Date,Merchant,Amount,Type,Category,Notes,Tags
2026-06-20,Organic Farm,42.50,EXPENSE,Groceries,Weekly greens,grocery,diet
2026-06-21,Salary Employer,1200.00,INCOME,Salary,Monthly payout,job,recurring
2026-06-22,Gas Station,35.00,EXPENSE,Fuel,Highway fill-up,commute,transit"""
                        },
                        modifier = Modifier.weight(1f).testTag("csv_load_demo_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Load Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("CSV Text Input", fontSize = 12.sp) },
                    placeholder = { Text("Paste spreadsheet lines here...", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("csv_textarea_input"),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    maxLines = 15
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (csvText.isNotBlank()) {
                        viewModel.importCsvData(csvText, context)
                        onDismissRequest()
                    }
                },
                enabled = csvText.isNotBlank(),
                modifier = Modifier.testTag("csv_import_submit_btn")
            ) {
                Text("Process Import", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("csv_import_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
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
    onDelete: () -> Unit,
    onUpdate: (TransactionEntity) -> Unit
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

    val context = LocalContext.current
    var showReceiptViewer by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val savedPath = saveReceiptImage(
                    context, 
                    bitmap, 
                    transaction.merchant.ifEmpty { "Linked Receipt" }, 
                    transaction.amount.toString()
                )
                if (savedPath.isNotEmpty()) {
                    onUpdate(transaction.copy(receiptPath = savedPath))
                    Toast.makeText(context, "Receipt attached successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save receipt image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission needed to attach receipt.", Toast.LENGTH_SHORT).show()
        }
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
                if (transaction.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        transaction.tags.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                        .testTag("tx_tag_badge_${transaction.id}_$tag")
                                ) {
                                    Text(
                                        text = "#$tag",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // RECEIPT ATTACHMENT ACTION BUTTONS
                    if (transaction.receiptPath.isNotEmpty()) {
                        IconButton(
                            onClick = { showReceiptViewer = true },
                            modifier = Modifier.size(28.dp).testTag("view_receipt_btn_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "View Receipt",
                                tint = FintechGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, android.Manifest.permission.CAMERA
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    cameraLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.size(28.dp).testTag("attach_receipt_btn_${transaction.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Attach Receipt",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDelete()
                        },
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

    // RECEIPT VIEWER DIALOG POPUP
    if (showReceiptViewer && transaction.receiptPath.isNotEmpty()) {
        val file = remember(transaction.receiptPath) { java.io.File(transaction.receiptPath) }
        AlertDialog(
            onDismissRequest = { showReceiptViewer = false },
            title = {
                Text(
                    text = "LINKED RECEIPT PROOF",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = "Receipt Image proof",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Image file not found.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Merchant: ${transaction.merchant.ifEmpty { "Unassigned" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Amount: ${formatter(transaction.amount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showReceiptViewer = false },
                    modifier = Modifier.testTag("close_receipt_viewer_dialog_btn")
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            // Unlink / Delete receipt path
                            onUpdate(transaction.copy(receiptPath = ""))
                            showReceiptViewer = false
                            Toast.makeText(context, "Receipt unlinked from transaction.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRose),
                        modifier = Modifier.testTag("unlink_receipt_dialog_btn")
                    ) {
                        Text("UNLINK", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            // Re-capture
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                            showReceiptViewer = false
                        },
                        modifier = Modifier.testTag("recapture_receipt_dialog_btn")
                    ) {
                        Text("RE-CAPTURE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}
