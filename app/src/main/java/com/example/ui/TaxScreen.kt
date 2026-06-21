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
import com.example.ui.theme.AccentGold
import com.example.ui.theme.FintechGreen

import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Lock

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

        // --- 2. LOCAL CODES DISPLAY ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COMPLIANCE & DEDUCTIONS RULES (${config.country})",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Standard Bracket VAT/MwSt average rate is estimated at ${config.taxRateDefault}%. Tag invoices appropriately to reduce gross annual taxable earnings in the ${config.fiscalYear} fiscal cycle.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
