package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.ExpenseRose
import com.example.ui.theme.FintechGreen
import com.example.ui.theme.AccentGold
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke

fun saveReceiptImage(context: android.content.Context, bitmap: Bitmap, merchant: String, amount: String): String {
    val dir = java.io.File(context.filesDir, "receipt_images")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val timestamp = System.currentTimeMillis()
    val file = java.io.File(dir, "receipt_${timestamp}.png")
    try {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        val sharedPrefs = context.getSharedPreferences("receipt_gallery_metadata", android.content.Context.MODE_PRIVATE)
        val metadataList = sharedPrefs.getStringSet("receipts_meta_set", null)?.toMutableSet() ?: mutableSetOf()
        val metaString = "${file.absolutePath}|$merchant|$amount|$timestamp"
        metadataList.add(metaString)
        sharedPrefs.edit().putStringSet("receipts_meta_set", metadataList).apply()
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return ""
    }
}

@Composable
fun RapidEntryScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    var amountStr by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf("EXPENSE") } // EXPENSE, INCOME, TRANSFER
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var selectedAccountId by remember { mutableStateOf(0L) }
    var toAccountId by remember { mutableStateOf(-1L) }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isTaxDeductible by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceInterval by remember { mutableStateOf("MONTHLY") }

    // Advanced Form Mode toggle + states
    var isFormMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var formAmount by remember { mutableStateOf("") }
    var formCurrency by remember { mutableStateOf(config.currency) }
    var formCurrencySymbol by remember { mutableStateOf(config.currencySymbol) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedTaxCategory by remember { mutableStateOf("") }

    LaunchedEffect(config) {
        formCurrency = config.currency
        formCurrencySymbol = config.currencySymbol
        val taxCats = config.taxCategories
        if (selectedTaxCategory.isEmpty() || !taxCats.contains(selectedTaxCategory)) {
            selectedTaxCategory = taxCats.firstOrNull() ?: ""
        }
    }

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

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("rapid_entry_full_screen"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RAPID LEDGER ENTRY",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("rapid_entry_close_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Screen")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction Type Selector (Large segments, easy lower grip)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val types = listOf("EXPENSE", "INCOME", "TRANSFER")
                types.forEach { type ->
                    val isSelected = transactionType == type
                    val containerColor = if (isSelected) {
                        when (type) {
                            "EXPENSE" -> ExpenseRose
                            "INCOME" -> FintechGreen
                            else -> MaterialTheme.colorScheme.primary
                        }
                    } else Color.Transparent

                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(containerColor)
                            .clickable { transactionType = type }
                            .testTag("type_btn_$type"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Selector: Instant Pad vs Detailed Form
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                SegmentedButton(
                    selected = !isFormMode,
                    onClick = { isFormMode = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("toggle_quick_mode")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Instant Pad", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                SegmentedButton(
                    selected = isFormMode,
                    onClick = { isFormMode = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("toggle_form_mode")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Detailed Form", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isFormMode) {
                DetailedFormLayout(
                    viewModel = viewModel,
                    accounts = accounts,
                    categories = categories,
                    config = config,
                    transactionType = transactionType,
                    selectedDate = selectedDate,
                    onDateChange = { selectedDate = it },
                    isTaxDeductible = isTaxDeductible,
                    onTaxChange = { isTaxDeductible = it },
                    isRecurring = isRecurring,
                    onRecurringChange = { isRecurring = it },
                    recurrenceInterval = recurrenceInterval,
                    onRecurrenceIntervalChange = { recurrenceInterval = it },
                    selectedTaxCategory = selectedTaxCategory,
                    onTaxCategoryChange = { selectedTaxCategory = it },
                    formAmount = formAmount,
                    onFormAmountChange = { formAmount = it },
                    formCurrency = formCurrency,
                    onFormCurrencyChange = { formCurrency = it },
                    formCurrencySymbol = formCurrencySymbol,
                    onFormCurrencySymbolChange = { formCurrencySymbol = it },
                    merchant = merchant,
                    onMerchantChange = { merchant = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    selectedAccountId = selectedAccountId,
                    onAccountIdChange = { selectedAccountId = it },
                    toAccountId = toAccountId,
                    onToAccountIdChange = { toAccountId = it },
                    selectedCategoryId = selectedCategoryId,
                    onCategoryIdChange = { selectedCategoryId = it },
                    onDismiss = onDismiss
                )
            } else {
                // READOUT SCREEN DISPLAY
                Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "AMOUNT (${config.currency})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = config.currencySymbol,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = amountStr.ifEmpty { "0" },
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("rapid_entry_amount_text")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SOURCE WALLET SELECTOR ROW
            Text(
                text = "SOURCE WALLET / ACCOUNT",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts) { acc ->
                    val isSelected = selectedAccountId == acc.id
                    val chipColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                    Card(
                        modifier = Modifier
                            .height(48.dp)
                            .clickable { selectedAccountId = acc.id }
                            .testTag("source_wallet_${acc.name.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(containerColor = chipColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${acc.name} (${config.currencySymbol}${acc.balance.toInt()})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CATEGORY OR DESTINATION ROW
            if (transactionType == "TRANSFER") {
                Text(
                    text = "DESTINATION BANK / WALLET",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filteredAccounts = accounts.filter { it.id != selectedAccountId }
                    items(filteredAccounts) { acc ->
                        val isSelected = toAccountId == acc.id
                        val chipColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Card(
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { toAccountId = acc.id }
                                .testTag("dest_wallet_${acc.name.replace(" ", "_")}"),
                            colors = CardDefaults.cardColors(containerColor = chipColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = acc.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "CATEGORY",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filteredCats = categories.filter { it.isIncome == (transactionType == "INCOME") }
                    items(filteredCats) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val chipColor = if (isSelected) {
                            if (transactionType == "INCOME") FintechGreen else ExpenseRose
                        } else MaterialTheme.colorScheme.surfaceVariant
                        
                        val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Card(
                            modifier = Modifier
                                .height(48.dp)
                                .clickable { selectedCategoryId = cat.id }
                                .testTag("category_chip_${cat.name.replace(" ", "_")}"),
                            colors = CardDefaults.cardColors(containerColor = chipColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val icon = when (cat.iconName) {
                                    "payments" -> Icons.Default.Payments
                                    "laptop_mac" -> Icons.Default.LaptopMac
                                    "storefront" -> Icons.Default.Storefront
                                    "restaurant" -> Icons.Default.Restaurant
                                    "directions_car" -> Icons.Default.DirectionsCar
                                    "home" -> Icons.Default.Home
                                    "movie" -> Icons.Default.Movie
                                    "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
                                    "trending_up" -> Icons.AutoMirrored.Filled.TrendingUp
                                    else -> Icons.Default.ShoppingBag
                                }
                                Icon(imageVector = icon, contentDescription = cat.name, tint = textColor, modifier = Modifier.size(16.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TAX DEDUCTIBLE SWITCH (FOR COMPLIANCE)
            if (transactionType == "EXPENSE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Tax", tint = AccentGold, modifier = Modifier.size(18.dp))
                        Text("Tax Deductible Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isTaxDeductible,
                        onCheckedChange = { isTaxDeductible = it },
                        modifier = Modifier.testTag("rapid_entry_tax_switch")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- ONE-HANDED DIGITAL KEYPAD ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val padRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "⌫")
                )

                padRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        if (digit == "⌫") {
                                            if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1)
                                        } else if (digit == ".") {
                                            if (!amountStr.contains(".")) {
                                                amountStr = if (amountStr.isEmpty()) "0." else "$amountStr."
                                            }
                                        } else {
                                            // limit depth to 10 chars
                                            if (amountStr.length < 10) {
                                                amountStr = if (amountStr == "0") digit else amountStr + digit
                                            }
                                        }
                                    }
                                    .testTag("keypad_btn_$digit"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PRIMARY BIG BUTTON FOR THUMB
            Button(
                onClick = {
                    val amtVal = amountStr.toDoubleOrNull() ?: 0.0
                    if (amtVal > 0) {
                        viewModel.addTransaction(
                            amount = amtVal,
                            type = transactionType,
                            categoryId = if (transactionType == "TRANSFER") 0L else selectedCategoryId,
                            accountId = selectedAccountId,
                            toAccountId = toAccountId,
                            merchant = merchant.ifEmpty { "Manual Log" },
                            isTaxDeductible = isTaxDeductible,
                            notes = notes.ifEmpty { "One-handed rapid logged" }
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("rapid_entry_submit_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transactionType == "EXPENSE") ExpenseRose else if (transactionType == "INCOME") FintechGreen else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "RECORD TRANSACTION (≤2 SECS)",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    fontSize = 14.sp
                )
            }
            } // end of else block
        }
    }
}

@Composable
fun DetailedFormLayout(
    viewModel: MainViewModel,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    config: CountryConfig,
    transactionType: String,
    selectedDate: Long,
    onDateChange: (Long) -> Unit,
    isTaxDeductible: Boolean,
    onTaxChange: (Boolean) -> Unit,
    isRecurring: Boolean,
    onRecurringChange: (Boolean) -> Unit,
    recurrenceInterval: String,
    onRecurrenceIntervalChange: (String) -> Unit,
    selectedTaxCategory: String,
    onTaxCategoryChange: (String) -> Unit,
    formAmount: String,
    onFormAmountChange: (String) -> Unit,
    formCurrency: String,
    onFormCurrencyChange: (String) -> Unit,
    formCurrencySymbol: String,
    onFormCurrencySymbolChange: (String) -> Unit,
    merchant: String,
    onMerchantChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    selectedAccountId: Long,
    onAccountIdChange: (Long) -> Unit,
    toAccountId: Long,
    onToAccountIdChange: (Long) -> Unit,
    selectedCategoryId: Long,
    onCategoryIdChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showVoiceDialog by remember { mutableStateOf(false) }
    var tagsString by remember { mutableStateOf("") }
    
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenTextSet = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextSet?.firstOrNull() ?: ""
            if (spokenText.isNotEmpty()) {
                val doubleAmountPattern = """(\d+(?:\.\d{1,2})?)""".toRegex()
                val matchResults = doubleAmountPattern.findAll(spokenText.lowercase(Locale.US))
                var foundAmount = ""
                for (match in matchResults) {
                    val candidate = match.value
                    if ((candidate.toDoubleOrNull() ?: 0.0) > 0.0) {
                        foundAmount = candidate
                        break
                    }
                }
                
                var foundMerchant = ""
                val merchantPatterns = listOf("at ([a-zA-Z0-9\\s]+)", "from ([a-zA-Z0-9\\s]+)", "for ([a-zA-Z0-9\\s]+)")
                for (patternStr in merchantPatterns) {
                    val regex = patternStr.toRegex()
                    val match = regex.find(spokenText)
                    if (match != null && match.groupValues.size > 1) {
                        val candidate = match.groupValues[1].split("on ", "for ", "tomorrow ", "today ", "yesterday ", "the ").first().trim()
                        if (candidate.isNotEmpty() && !candidate.equals("spent", ignoreCase = true)) {
                            foundMerchant = candidate
                            break
                        }
                    }
                }
                
                var foundCategoryId = selectedCategoryId
                categories.forEach { cat ->
                    val catNameLower = cat.name.lowercase(Locale.US)
                    if (spokenText.lowercase(Locale.US).contains(catNameLower) || catNameLower.split("&", " ").any { it.length > 3 && spokenText.lowercase(Locale.US).contains(it) }) {
                        foundCategoryId = cat.id
                    }
                }
                
                if (foundAmount.isNotEmpty()) {
                    onFormAmountChange(foundAmount)
                }
                if (foundMerchant.isNotEmpty()) {
                    onMerchantChange(foundMerchant.replaceFirstChar { it.uppercase() })
                }
                onNotesChange("Narrated: \"$spokenText\"")
                onCategoryIdChange(foundCategoryId)
                Toast.makeText(context, "Voice Applied: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI RECEIPT SCANNING HEADER CARD
        var isScanningReceipt by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        
        // Take picture from camera preview
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            if (bitmap != null) {
                isScanningReceipt = true
                scope.launch {
                    try {
                        val categoriesJson = categories.joinToString(prefix = "[", postfix = "]") { "{\"id\": ${it.id}, \"name\": \"${it.name.replace("\"", "\\\"")}\"}" }
                        val recentTxs = viewModel.allTransactions.value.take(20)
                        val historyJson = recentTxs.joinToString(prefix = "[", postfix = "]") { "{\"merchant\": \"${it.merchant.replace("\"", "\\\"")}\", \"categoryId\": ${it.categoryId}}" }
                        
                        val responseJson = com.example.data.api.GeminiApiClient.analyzeReceiptWithCategory(bitmap, categoriesJson, historyJson)
                        // Parse JSON: format is {"merchant": "Starbucks", "date": "2026-06-19", "amount": 12.50, "categoryId": 3}
                        if (responseJson.isNotEmpty() && !responseJson.startsWith("Exception:")) {
                            val cleanJson = responseJson.trim()
                                .replace("```json", "")
                                .replace("```", "")
                                .trim()
                            
                            val moshi = com.squareup.moshi.Moshi.Builder().build()
                            val adapter = moshi.adapter(Map::class.java)
                            val parsedMap = adapter.fromJson(cleanJson)
                            if (parsedMap != null) {
                                val extractedMerchant = parsedMap["merchant"]?.toString() ?: ""
                                val extractedDateStr = parsedMap["date"]?.toString() ?: ""
                                val extractedAmountStr = parsedMap["amount"]?.toString() ?: ""
                                val extractedCategoryId = (parsedMap["categoryId"]?.toString()?.toDoubleOrNull()?.toLong()) ?: 0L
                                
                                if (extractedMerchant.isNotEmpty()) {
                                    onMerchantChange(extractedMerchant)
                                }
                                if (extractedAmountStr.isNotEmpty()) {
                                    onFormAmountChange(extractedAmountStr)
                                }
                                
                                var parsedTimestamp = System.currentTimeMillis()
                                if (extractedDateStr.isNotEmpty()) {
                                    try {
                                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        val parsedDate = sdf.parse(extractedDateStr)
                                        if (parsedDate != null) {
                                            parsedTimestamp = parsedDate.time
                                            onDateChange(parsedTimestamp)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                var savedPath = ""
                                try {
                                    savedPath = saveReceiptImage(context, bitmap, extractedMerchant.ifEmpty { "Scanned Receipt" }, extractedAmountStr.ifEmpty { "0.00" })
                                } catch (err: Exception) {
                                    err.printStackTrace()
                                }

                                // Save directly to local Room SQLite as a pending transaction
                                try {
                                    val finalAmount = extractedAmountStr.toDoubleOrNull() ?: 0.0
                                    val finalCatId = if (extractedCategoryId != 0L && categories.any { it.id == extractedCategoryId }) {
                                        extractedCategoryId
                                    } else {
                                        viewModel.suggestCategoryForMerchant(extractedMerchant)
                                    }
                                    val finalAccId = if (selectedAccountId != 0L) selectedAccountId else (accounts.firstOrNull()?.id ?: 1L)
                                    
                                    onCategoryIdChange(finalCatId)
                                    
                                    viewModel.addTransaction(
                                        amount = finalAmount,
                                        type = "EXPENSE",
                                        categoryId = finalCatId,
                                        accountId = finalAccId,
                                        merchant = extractedMerchant.ifEmpty { "Scanned Merchant" },
                                        notes = "[Pending Review] - AI Camera Receipt Scan",
                                        customTimestamp = parsedTimestamp,
                                        receiptPath = savedPath
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                Toast.makeText(context, "Receipt Scanned, Pending Transaction Created & Saved to Gallery!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            var savedPath = ""
                            try {
                                savedPath = saveReceiptImage(context, bitmap, "Scanned Vendor", "0.00")
                            } catch (err: Exception) {
                                err.printStackTrace()
                            }
                            // Still add a transaction so they can link/view it
                            try {
                                val finalCatId = selectedCategoryId.let { if (it != 0L) it else (categories.firstOrNull()?.id ?: 1L) }
                                val finalAccId = selectedAccountId.let { if (it != 0L) it else (accounts.firstOrNull()?.id ?: 1L) }
                                viewModel.addTransaction(
                                    amount = 0.0,
                                    type = "EXPENSE",
                                    categoryId = finalCatId,
                                    accountId = finalAccId,
                                    merchant = "Scanned Vendor",
                                    notes = "[OCR Failed] - Tap to edit and update details",
                                    receiptPath = savedPath
                                )
                            } catch (e: Exception) {}
                            Toast.makeText(context, "Receipt Saved (OCR processing failed), Blank Transaction Created.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error reading receipt: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isScanningReceipt = false
                    }
                }
            }
        }
        
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(context, "Camera permission needed to scan receipt.", Toast.LENGTH_SHORT).show()
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        cameraLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }
                .testTag("scan_receipt_btn")
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isScanningReceipt) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Column {
                        Text("Extracting Receipts via Gemini API...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Reading merchant name, dates & total charges", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Camera Scan",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("AI Smart Camera Receipt Import", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Auto-populate vendor name, amount & invoice date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // --- AI VOICE NARRATION CARD & DIALOG ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showVoiceDialog = true
                }
                .testTag("voice_narrate_btn")
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice dictaphone",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text("AI Speak & Log Narrator", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text("Narrate expenses instantly (e.g. \"Spent 12 at Starbucks\")", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        if (showVoiceDialog) {
            var inputVoiceSimulationText by remember { mutableStateOf("") }
            val voicePresets = listOf(
                "Spent 14.50 at BlueBottle for cappuccino",
                "Dinner at McDonalds for 18.00",
                "Spent 120 on Groceries today",
                "Paid 35 dollars for Uber ride"
            )
            AlertDialog(
                onDismissRequest = { showVoiceDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("AI Voice Dictation Narrator", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Speak or enter your expense narration in natural language. Our system parses values, merchant names, and matches categories automatically.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Narrate your transaction")
                                }
                                try {
                                    voiceLauncher.launch(intent)
                                    showVoiceDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Voice recognizer not supported on this device. Use simulation/preset templates below.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_voice_recording_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START VOICE RECORDING", fontWeight = FontWeight.Bold)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            Text("OR DICTATE RAW TEXT", fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                        
                        OutlinedTextField(
                            value = inputVoiceSimulationText,
                            onValueChange = { inputVoiceSimulationText = it },
                            label = { Text("Enter Spoken Narration Text") },
                            placeholder = { Text("Spent 12.50 at Starbucks today...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("voice_simulation_input")
                        )
                        
                        Text("TAP QUICK SIMULATION PRESET:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            voicePresets.forEach { preset ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputVoiceSimulationText = preset
                                        }
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputVoiceSimulationText.trim().isNotEmpty()) {
                                val spokenText = inputVoiceSimulationText.trim()
                                val doubleAmountPattern = """(\d+(?:\.\d{1,2})?)""".toRegex()
                                val matchResults = doubleAmountPattern.findAll(spokenText.lowercase(Locale.US))
                                var foundAmount = ""
                                for (match in matchResults) {
                                    val candidate = match.value
                                    if ((candidate.toDoubleOrNull() ?: 0.0) > 0.0) {
                                        foundAmount = candidate
                                        break
                                    }
                                }
                                
                                var foundMerchant = ""
                                val merchantPatterns = listOf("at ([a-zA-Z0-9\\s]+)", "from ([a-zA-Z0-9\\s]+)", "for ([a-zA-Z0-9\\s]+)")
                                for (patternStr in merchantPatterns) {
                                    val regex = patternStr.toRegex()
                                    val match = regex.find(spokenText)
                                    if (match != null && match.groupValues.size > 1) {
                                        val candidate = match.groupValues[1].split("on ", "for ", "tomorrow ", "today ", "yesterday ", "the ").first().trim()
                                        if (candidate.isNotEmpty() && !candidate.equals("spent", ignoreCase = true)) {
                                            foundMerchant = candidate
                                            break
                                        }
                                    }
                                }
                                
                                var foundCategoryId = selectedCategoryId
                                categories.forEach { cat ->
                                    val catNameLower = cat.name.lowercase(Locale.US)
                                    if (spokenText.lowercase(Locale.US).contains(catNameLower) || catNameLower.split("&", " ").any { it.length > 3 && spokenText.lowercase(Locale.US).contains(it) }) {
                                        foundCategoryId = cat.id
                                    }
                                }
                                
                                if (foundAmount.isNotEmpty()) {
                                    onFormAmountChange(foundAmount)
                                }
                                if (foundMerchant.isNotEmpty()) {
                                    onMerchantChange(foundMerchant.replaceFirstChar { it.uppercase() })
                                }
                                onNotesChange("Narrated: \"$spokenText\"")
                                onCategoryIdChange(foundCategoryId)
                                Toast.makeText(context, "Narrative applied successfully!", Toast.LENGTH_SHORT).show()
                                showVoiceDialog = false
                            } else {
                                Toast.makeText(context, "Please enter some narration or start recording.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("apply_voice_simulation_btn")
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showVoiceDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ---- AMOUNT & CURRENCY ROW ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = formAmount,
                onValueChange = { onFormAmountChange(it) },
                label = { Text("Amount ($formCurrencySymbol)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("form_entry_amount"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )

            Column {
                var expandedCurr by remember { mutableStateOf(false) }
                Text("Currency", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { expandedCurr = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(52.dp).testTag("form_currency_selector")
                ) {
                    Text("$formCurrency ($formCurrencySymbol)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = expandedCurr, onDismissRequest = { expandedCurr = false }) {
                    val currList = listOf(
                        "USD" to "$", "EUR" to "€", "INR" to "₹", "BDT" to "৳", "GBP" to "£", "JPY" to "¥", "CAD" to "CA$", "AUD" to "A$"
                    )
                    currList.forEach { (code, symbol) ->
                        DropdownMenuItem(
                            text = { Text("$code ($symbol)") },
                            onClick = {
                                onFormCurrencyChange(code)
                                onFormCurrencySymbolChange(symbol)
                                expandedCurr = false
                            }
                        )
                    }
                }
            }
        }

        // --- DYNAMIC MULTI-CURRENCY CONVERSION PREVIEW — FETCH STABLE RATES ---
        val doubleAmount = formAmount.toDoubleOrNull() ?: 0.0
        val portfolioEquivalent = viewModel.convertCurrency(doubleAmount, formCurrency, config.currency)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (formCurrency.uppercase() != config.currency.uppercase() && doubleAmount > 0.0) {
                    Text(
                        text = "Portfolio Est: ${config.currencySymbol}${"%,.2f".format(portfolioEquivalent)} (${config.currency})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FintechGreen,
                        modifier = Modifier.testTag("currency_conversion_text")
                    )
                    Text(
                        text = "Rates pegged live based to sovereign USD base",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                } else {
                    Text(
                        text = "Portfolio Currency: ${config.currency} (${config.currencySymbol})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "1.00 USD @ fallback sovereign indexes",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            TextButton(
                onClick = {
                    viewModel.triggerExchangeRatesFetch(
                        onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                        onFailure = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                    )
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp).testTag("fetch_live_rates_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(11.dp))
                    Text("FETCH LIVE RATES", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ---- SOURCE & DESTINATION ACCOUNTS ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Source Account", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                var expandedAcc by remember { mutableStateOf(false) }
                val currentAccName = accounts.find { it.id == selectedAccountId }?.name ?: "Cash"
                Button(
                    onClick = { expandedAcc = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(currentAccName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
                DropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text("${acc.name} (${config.currencySymbol}${acc.balance.toInt()})") },
                            onClick = {
                                onAccountIdChange(acc.id)
                                expandedAcc = false
                            }
                        )
                    }
                }
            }

            if (transactionType == "TRANSFER") {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Destination Account", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    var expandedTo by remember { mutableStateOf(false) }
                    val currentToName = accounts.find { it.id == toAccountId }?.name ?: "Select..."
                    Button(
                        onClick = { expandedTo = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentToName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    onToAccountIdChange(acc.id)
                                    expandedTo = false
                                }
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    var expandedCat by remember { mutableStateOf(false) }
                    val currentCatName = categories.find { it.id == selectedCategoryId }?.name ?: "Select..."
                    Button(
                        onClick = { expandedCat = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(currentCatName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        val filteredCats = categories.filter { it.isIncome == (transactionType == "INCOME") }
                        filteredCats.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    onCategoryIdChange(cat.id)
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // ---- COMPLIANCE RULES & TAX CONFIGS ----
        if (transactionType == "EXPENSE") {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Tax status",
                                tint = AccentGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Tax Deductible Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = isTaxDeductible,
                            onCheckedChange = { onTaxChange(it) },
                            modifier = Modifier.testTag("form_tax_switch").scale(0.85f)
                        )
                    }

                    if (isTaxDeductible) {
                        Text(
                            "Country Tax category rule mapping",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        var expandedTaxCat by remember { mutableStateOf(false) }
                        val taxCats = config.taxCategories

                        Button(
                            onClick = { expandedTaxCat = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedTaxCategory.ifEmpty { "Select Tax Code..." },
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(expanded = expandedTaxCat, onDismissRequest = { expandedTaxCat = false }) {
                            taxCats.forEach { tc ->
                                DropdownMenuItem(
                                    text = { Text(tc) },
                                    onClick = {
                                        onTaxCategoryChange(tc)
                                        expandedTaxCat = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- DATE PICKER CONFIG ----
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = selectedDate
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                onDateChange(calendar.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Transaction Date", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    val sdf = SimpleDateFormat("dd MMMM yyyy (EEEE)", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(selectedDate)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Pick Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ---- RECURRING SUBSCRIPTION CONFIG ----
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Recurring Payment",
                            tint = FintechGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text("Recurring Subscription", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Auto-populate in local storage ledger", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { onRecurringChange(it) },
                        modifier = Modifier.testTag("form_recurring_switch").scale(0.85f)
                    )
                }

                if (isRecurring) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Recurrence settings interval:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DAILY", "WEEKLY", "MONTHLY").forEach { interval ->
                            val isSelected = recurrenceInterval == interval
                            FilterChip(
                                selected = isSelected,
                                onClick = { onRecurrenceIntervalChange(interval) },
                                label = { Text(interval, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("recur_interval_$interval")
                            )
                        }
                    }
                }
            }
        }

        // ---- MERCHANT & NOTES TEXT FIELDS ----
        OutlinedTextField(
            value = merchant,
            onValueChange = { onMerchantChange(it) },
            label = { Text("Merchant / Counterparty") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("form_merchant_field")
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { onNotesChange(it) },
            label = { Text("Internal Notes") },
            singleLine = false,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().testTag("form_notes_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tagsString,
            onValueChange = { tagsString = it },
            label = { Text("Custom tags (comma separated)") },
            placeholder = { Text("e.g. coffee, travel, subscription", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("form_tags_field")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ---- RECORD SUBMIT BUTTON ----
        Button(
            onClick = {
                val amt = formAmount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    val baseAmount = viewModel.convertCurrency(amt, formCurrency, config.currency)
                    val realTimeVat = viewModel.realTimeTaxData.value?.standardVatRate ?: config.taxRateDefault
                    val customCategoryTax = viewModel.categoryTaxRates.value[selectedCategoryId] ?: realTimeVat
                    val taxRateToUse = if (isTaxDeductible) customCategoryTax else 0.0

                    viewModel.addTransaction(
                        amount = baseAmount,
                        type = transactionType,
                        categoryId = if (transactionType == "TRANSFER") 0L else selectedCategoryId,
                        accountId = selectedAccountId,
                        toAccountId = toAccountId,
                        merchant = merchant.ifEmpty { if (transactionType == "TRANSFER") "Transfer" else "Retail Ledger" },
                        isTaxDeductible = isTaxDeductible,
                        taxRate = taxRateToUse,
                        notes = notes.ifEmpty { "Form logged ($formCurrency $amt with dynamic mapping: $selectedTaxCategory)" },
                        isRecurring = isRecurring,
                        recurrenceInterval = recurrenceInterval,
                        customTimestamp = selectedDate,
                        tags = tagsString
                    )
                    onDismiss()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("form_submit_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (transactionType == "EXPENSE") ExpenseRose else if (transactionType == "INCOME") FintechGreen else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "RECORD TRANSACTION (FORM)",
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                fontSize = 14.sp
            )
        }
    }
}
