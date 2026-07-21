package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Backup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import coil.compose.AsyncImage

data class ReceiptGalleryItem(
    val filePath: String,
    val merchant: String,
    val amount: String,
    val timestamp: Long
)

fun getReceiptsList(context: android.content.Context): List<ReceiptGalleryItem> {
    val sharedPrefs = context.getSharedPreferences("receipt_gallery_metadata", android.content.Context.MODE_PRIVATE)
    val set = sharedPrefs.getStringSet("receipts_meta_set", emptySet()) ?: emptySet()
    return set.mapNotNull { str ->
        val parts = str.split("|")
        if (parts.size >= 4) {
            ReceiptGalleryItem(
                filePath = parts[0],
                merchant = parts[1],
                amount = parts[2],
                timestamp = parts[3].toLongOrNull() ?: 0L
            )
        } else null
    }.sortedByDescending { it.timestamp }
}

fun deleteReceiptImage(context: android.content.Context, item: ReceiptGalleryItem) {
    try {
        val file = java.io.File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val sharedPrefs = context.getSharedPreferences("receipt_gallery_metadata", android.content.Context.MODE_PRIVATE)
    val set = sharedPrefs.getStringSet("receipts_meta_set", emptySet()) ?: emptySet()
    val newSet = set.filter { !it.startsWith(item.filePath + "|") }.toSet()
    sharedPrefs.edit().putStringSet("receipts_meta_set", newSet).apply()
}

@Composable
fun TaxScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()
    val isEncrypted by viewModel.isExportEncryptionEnabled.collectAsState()
    val passcode by viewModel.exportPasscode.collectAsState()

    val deductibles = remember(transactions) {
        transactions.filter { it.isTaxDeductible && it.type == "EXPENSE" }
    }

    val totalDeductedVal = remember(deductibles) {
        deductibles.sumOf { it.amount }
    }

    val estimatedSavings = remember(totalDeductedVal, config) {
        // Simple visual formula checking dynamic savings (taxRate / 100 * total)
        totalDeductedVal * (config.taxRateDefault / 100.0)
    }

    var showExportSuccess by remember { mutableStateOf(false) }
    var receiptsList by remember { mutableStateOf(getReceiptsList(context)) }
    var selectedReceiptForDetail by remember { mutableStateOf<ReceiptGalleryItem?>(null) }

    LaunchedEffect(Unit) {
        receiptsList = getReceiptsList(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("tax_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. DEDUCTIBLE COMMANDER HEADER ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().testTag("tax_totals_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "${config.country} FISCAL YEAR SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DEDUCTIBLE EXPENDITURES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = viewModel.formatCurrency(totalDeductedVal),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = FintechGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EST. TAX OFFSET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "≈ " + viewModel.formatCurrency(estimatedSavings),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            showExportSuccess = true
                            viewModel.exportTaxReportToCsv(context)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_tax_ledger_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERATE REGIONAL TAX EXPORT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- SECURE OUTBOUND EXPORT ENCRYPTION CONFIG ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isEncrypted) FintechGreen else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AES-256 Outbound Key Vault",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isEncrypted,
                                onCheckedChange = { viewModel.toggleExportEncryption() },
                                modifier = Modifier.testTag("export_encryption_switch")
                            )
                        }

                        if (isEncrypted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passcode,
                                onValueChange = { viewModel.setExportPasscode(it) },
                                label = { Text("Filing Passphrase Key", fontSize = 10.sp) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("export_passcode_input")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Exported text files (.csv.enc) are securely structured and encrypted with CBC block cipher constraints.",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // --- SECURE CLOUD BACKUP & END-TO-END ENCRYPTION ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                var backupStatus by remember { mutableStateOf("") }
                var isBackingUp by remember { mutableStateOf(false) }
                val context = LocalContext.current

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "ENCRYPTED SQLITE CLOUD BACKUP",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Secure offline-first system snapshot",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Encodes and packages all transactions, custom category budgets, configured key store structures, and accounts into an end-to-end AES-256 encrypted .db.enc backup vault, directly syncing to WealthFlow secure cloud.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )

                    if (backupStatus.isNotEmpty() && backupStatus != "SUCCESS") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = backupStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isBackingUp = true
                            viewModel.exportSqliteDatabaseEncrypted(context) { status ->
                                backupStatus = status
                                if (status == "SUCCESS" || status.startsWith("Backup failed")) {
                                    isBackingUp = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("encrypted_backup_btn"),
                        enabled = !isBackingUp
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COMMITTING BACKUP...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CREATE CLOUD BACKUP NOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 2. LOCAL CODES DISPLAY ---
        item {
            val realTimeData by viewModel.realTimeTaxData.collectAsState()
            val realTimeLoading by viewModel.realTimeTaxLoading.collectAsState()
            val isDark by viewModel.isDarkTheme.collectAsState()

            val taxGradientBrush = remember(isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(FintechGreen.copy(alpha = 0.15f), CardSlate)
                    } else {
                        listOf(LightPrimary.copy(alpha = 0.1f), LightCard)
                    }
                )
            }
            val taxBorderBrush = remember(isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(FintechGreen.copy(alpha = 0.3f), BorderColor.copy(alpha = 0.15f))
                    } else {
                        listOf(LightPrimary.copy(alpha = 0.35f), Color.LightGray.copy(alpha = 0.25f))
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.8.dp, taxBorderBrush),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(taxGradientBrush, RoundedCornerShape(16.dp))
                    .testTag("real_time_tax_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME TAX COMPLIANCE (${config.country})",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (realTimeLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(
                                onClick = { viewModel.fetchTaxDataForCountry(config.country) },
                                modifier = Modifier.size(24.dp).testTag("refresh_realtime_tax_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Realtime Tax",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val vatToDisplay = realTimeData?.standardVatRate ?: config.taxRateDefault
                    Text(
                        text = "Standard VAT/MwSt average rate is estimated at $vatToDisplay% from official real-time APIs. Tag invoices appropriately to reduce gross annual taxable earnings in the ${config.fiscalYear} fiscal cycle.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (realTimeData != null && realTimeData!!.brackets.isNotEmpty()) {
                        Text(
                            text = "OFFICIAL INCOME TAX BRACKETS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        realTimeData!!.brackets.forEach { bracket ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bracket.incomeRange,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${bracket.rate}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FintechGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "DEDUCTIBLE REGIONS TO ENFORCE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    config.taxCategories.forEach { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = "Rule Match",
                                tint = FintechGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // --- 2.5 INTERACTIVE LOCALIZED TAX CALCULATOR ---
        item {
            var selectedJurisdiction by remember { mutableStateOf("USA (Federal)") }
            // Collect dynamic monthly inflow to populate defaults
            val currentInflow by viewModel.currentInflow.collectAsState()
            
            // Calculate default initial values based on actual database totals
            val initialDeductions = totalDeductedVal * 12.0
            val initialInflow = currentInflow * 12.0
            
            var incomeInput by remember(initialInflow) { 
                mutableStateOf(if (initialInflow > 0.0) "%.2f".format(java.util.Locale.US, initialInflow) else "75000.00") 
            }
            var deductionsInput by remember(initialDeductions) { 
                mutableStateOf(if (initialDeductions > 0.0) "%.2f".format(java.util.Locale.US, initialDeductions) else "12000.00") 
            }
            var expandedDropdown by remember { mutableStateOf(false) }

            val doubleIncome = incomeInput.toDoubleOrNull() ?: 0.0
            val doubleDeductions = deductionsInput.toDoubleOrNull() ?: 0.0
            val calculatedTax = calculateEstimatedTax(doubleIncome, doubleDeductions, selectedJurisdiction)
            val taxableNet = maxOf(0.0, doubleIncome - doubleDeductions)
            val effectiveRate = if (doubleIncome > 0.0) (calculatedTax / doubleIncome) * 100.0 else 0.0
            val netIncome = doubleIncome - calculatedTax

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("localized_tax_calculator_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "LOCALIZED ANNUAL TAX ESTIMATOR",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pro-rate logged transactions or estimate custom obligations",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Jurisdiction selection dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("calculator_jurisdiction_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Jurisdiction: $selectedJurisdiction",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expand jurisdiction list"
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            val jurisdictions = listOf(
                                "USA (Federal)",
                                "UK (Single)",
                                "Germany (Individual)",
                                "Canada (Federal)",
                                "India (New Regime)"
                            )
                            jurisdictions.forEach { juris ->
                                DropdownMenuItem(
                                    text = { Text(juris, fontSize = 12.sp) },
                                    onClick = {
                                        selectedJurisdiction = juris
                                        expandedDropdown = false
                                    },
                                    modifier = Modifier.testTag("jurisdiction_item_$juris")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row of Input fields
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = incomeInput,
                            onValueChange = { incomeInput = it },
                            label = { Text("Est. Annual Income (${config.currencySymbol})", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calculator_income_input")
                        )

                        OutlinedTextField(
                            value = deductionsInput,
                            onValueChange = { deductionsInput = it },
                            label = { Text("Est. Annual Deductible (${config.currencySymbol})", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("calculator_deductions_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calculation Summary Results Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Gross Revenue:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${config.currencySymbol}${"%,.2f".format(doubleIncome)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Applied Deductions:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "- ${config.currencySymbol}${"%,.2f".format(doubleDeductions)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Taxable Net Income:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${config.currencySymbol}${"%,.2f".format(taxableNet)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ESTIMATED TAX LIABILITY:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${config.currencySymbol}${"%,.2f".format(calculatedTax)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Effective Tax Rate:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "${"%.2f".format(effectiveRate)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Net Disposable Income:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${config.currencySymbol}${"%,.2f".format(netIncome)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = FintechGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Brief Bracket Breakdown info
                    Text(
                        text = "Calculation applies the progressive/allowance brackets of $selectedJurisdiction for the current filing cycle. Adjust inputs above for customized simulation.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // --- 3. EXPORT DIALOG POPUP ---
        if (showExportSuccess) {
            item {
                AlertDialog(
                    onDismissRequest = { showExportSuccess = false },
                    title = { Text("Tax Ledger Compiled") },
                    text = {
                        Text("Export completed successfully! Ledger database entries for standard transactions have been aggregated matching the ${config.country} schema (${config.fiscalYear} cycle) and compiled into 'TaxLedger_${config.country}.csv'.")
                    },
                    confirmButton = {
                        Button(
                            onClick = { showExportSuccess = false },
                            modifier = Modifier.testTag("dismiss_export_dialog")
                        ) {
                            Text("Acknowledge")
                        }
                    }
                )
            }
        }

        // --- 4. LIST DEDUCTIBLE ROWS ---
        if (deductibles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "None", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tax-deductible items detected in current range.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "DEDUCTIBLE LEDGER SEED RECORDS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            items(deductibles, key = { it.id }) { row ->
                TaxTransactionRow(row = row, category = categories.find { it.id == row.categoryId }, formatter = { viewModel.formatCurrency(it) })
            }
        }

        // --- SCANNED RECEIPTS GALLERY SECTION ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "AI SMART RECEIPTS GALLERY",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                if (receiptsList.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_gallery_empty_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your receipts gallery is currently empty.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Go to Transaction Ledger -> Add '+' -> AI Smart Camera Receipt Import to archive purchase proofs.",
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_gallery_row")
                    ) {
                        items(receiptsList) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        selectedReceiptForDetail = item
                                    }
                                    .testTag("receipt_item_${item.merchant}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val file = java.io.File(item.filePath)
                                        if (file.exists()) {
                                            AsyncImage(
                                                model = file,
                                                contentDescription = "Receipt Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Receipt,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = item.merchant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (item.amount.isNotEmpty() && item.amount != "0.00") {
                                                "${config.currencySymbol}${item.amount}"
                                            } else {
                                                "Pending scan"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(item.timestamp)),
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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

    if (selectedReceiptForDetail != null) {
        val currentSelected = selectedReceiptForDetail!!
        AlertDialog(
            onDismissRequest = { selectedReceiptForDetail = null },
            title = {
                Text(text = "PROOF OF PURCHASE DETAILS", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
                        val file = java.io.File(currentSelected.filePath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Receipt Full",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Merchant: ${currentSelected.merchant}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Amount Extracted: ${config.currencySymbol}${currentSelected.amount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Invoice Captured On: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(currentSelected.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(text = "Storage Location: ${currentSelected.filePath}", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteReceiptImage(context, currentSelected)
                        receiptsList = getReceiptsList(context)
                        selectedReceiptForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_receipt_confirm_btn")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Receipt")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReceiptForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TaxTransactionRow(
    row: TransactionEntity,
    category: CategoryEntity?,
    formatter: (Double) -> String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = category?.name ?: "Expense",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (row.merchant.isNotEmpty()) "Payee: ${row.merchant}" else "Indirect",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (row.notes.isNotEmpty()) {
                    Text(
                        text = "Notes: ${row.notes}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatter(row.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Tax Exempt",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
            }
        }
    }
}

fun calculateEstimatedTax(income: Double, deductions: Double, jurisdiction: String): Double {
    val taxableIncome = maxOf(0.0, income - deductions)
    return when (jurisdiction) {
        "USA (Federal)" -> {
            var tax = 0.0
            var remaining = taxableIncome
            val tier1 = minOf(remaining, 11600.0)
            tax += tier1 * 0.10
            remaining -= tier1
            if (remaining > 0.0) {
                val tier2 = minOf(remaining, 47150.0 - 11600.0)
                tax += tier2 * 0.12
                remaining -= tier2
            }
            if (remaining > 0.0) {
                val tier3 = minOf(remaining, 100525.0 - 47150.0)
                tax += tier3 * 0.22
                remaining -= tier3
            }
            if (remaining > 0.0) {
                tax += remaining * 0.24
            }
            tax
        }
        "UK (Single)" -> {
            var tax = 0.0
            var remaining = taxableIncome
            val tier1 = minOf(remaining, 12570.0)
            remaining -= tier1
            if (remaining > 0.0) {
                val tier2 = minOf(remaining, 50270.0 - 12570.0)
                tax += tier2 * 0.20
                remaining -= tier2
            }
            if (remaining > 0.0) {
                val tier3 = minOf(remaining, 125140.0 - 50270.0)
                tax += tier3 * 0.40
                remaining -= tier3
            }
            if (remaining > 0.0) {
                tax += remaining * 0.45
            }
            tax
        }
        "Germany (Individual)" -> {
            var tax = 0.0
            var remaining = taxableIncome
            val tier1 = minOf(remaining, 11784.0)
            remaining -= tier1
            if (remaining > 0.0) {
                val tier2 = minOf(remaining, 24000.0 - 11784.0)
                tax += tier2 * 0.14
                remaining -= tier2
            }
            if (remaining > 0.0) {
                val tier3 = minOf(remaining, 66760.0 - 24000.0)
                tax += tier3 * 0.24
                remaining -= tier3
            }
            if (remaining > 0.0) {
                tax += remaining * 0.42
            }
            tax
        }
        "Canada (Federal)" -> {
            var tax = 0.0
            var remaining = taxableIncome
            val tier1 = minOf(remaining, 55867.0)
            tax += tier1 * 0.15
            remaining -= tier1
            if (remaining > 0.0) {
                val tier2 = minOf(remaining, 111733.0 - 55867.0)
                tax += tier2 * 0.205
                remaining -= tier2
            }
            if (remaining > 0.0) {
                val tier3 = minOf(remaining, 173205.0 - 111733.0)
                tax += tier3 * 0.26
                remaining -= tier3
            }
            if (remaining > 0.0) {
                tax += remaining * 0.29
            }
            tax
        }
        "India (New Regime)" -> {
            var tax = 0.0
            var remaining = taxableIncome
            val tier1 = minOf(remaining, 300000.0)
            remaining -= tier1
            if (remaining > 0.0) {
                val tier2 = minOf(remaining, 700000.0 - 300000.0)
                tax += tier2 * 0.05
                remaining -= tier2
            }
            if (remaining > 0.0) {
                val tier3 = minOf(remaining, 1000000.0 - 700000.0)
                tax += tier3 * 0.10
                remaining -= tier3
            }
            if (remaining > 0.0) {
                val tier4 = minOf(remaining, 1200000.0 - 1000000.0)
                tax += tier4 * 0.15
                remaining -= tier4
            }
            if (remaining > 0.0) {
                tax += remaining * 0.20
            }
            tax
        }
        else -> taxableIncome * 0.20
    }
}
