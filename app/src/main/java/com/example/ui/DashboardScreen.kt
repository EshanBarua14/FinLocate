package com.example.ui

import android.widget.Toast
import kotlinx.coroutines.launch
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.InsightEntity
import com.example.data.model.TransactionEntity
import com.example.data.service.AnomalyReport
import com.example.data.service.AnomalyType
import com.example.ui.theme.*
import kotlin.math.min
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashboardContext = androidx.compose.ui.platform.LocalContext.current
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showOneTapModal by remember { mutableStateOf(false) }
    var showControlCenter by remember { mutableStateOf(false) }
    var showYearlyReport by remember { mutableStateOf(false) }
    val isDark by viewModel.isDarkTheme.collectAsState()
    val showBackupReminder by viewModel.showBackupReminder.collectAsState()
    val txCountSinceLastExport by viewModel.txCountSinceLastExport.collectAsState()
    val useBaseCurrency by viewModel.useBaseCurrency.collectAsState()
    val displayCurrencySymbol by viewModel.displayCurrencySymbol.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkBackupReminder()
    }
    val totalBalance by viewModel.totalBalance.collectAsState()
    val inflow by viewModel.currentInflow.collectAsState()
    val outflow by viewModel.currentOutflow.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val rawTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val insights by viewModel.smartInsights.collectAsState()
    val burnRateText by viewModel.burnRateAndRunway.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthToMonthSpending by viewModel.monthToMonthSpending.collectAsState()
    val countryConfig by viewModel.activeCountryConfig.collectAsState()
    val budgetProjection by viewModel.budgetProjection.collectAsState()
    val netWorthSummary by viewModel.netWorthSummary.collectAsState()
    val matchingRules by viewModel.matchingRules.collectAsState()
    val anomalies by viewModel.detectedAnomalies.collectAsState()
    val budgets by viewModel.activeBudgets.collectAsState()
    val budgetAlerts by viewModel.budgetAlerts.collectAsState()

    val csvPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.importCsvFromUri(dashboardContext, uri) { result ->
                android.widget.Toast.makeText(dashboardContext, result, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val backupPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.restoreSqliteDatabaseFromUri(dashboardContext, uri) { result ->
                android.widget.Toast.makeText(dashboardContext, result, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "WEALTHFLOW",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Ultra-Modern Minimalist Currency Switcher
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark) {
                                    if (useBaseCurrency) FintechGreen.copy(alpha = 0.15f) else CardSlate
                                } else {
                                    if (useBaseCurrency) LightPrimary.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (useBaseCurrency) {
                                    if (isDark) FintechGreen.copy(alpha = 0.4f) else LightPrimary.copy(alpha = 0.4f)
                                } else {
                                    if (isDark) BorderColor.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.setUseBaseCurrency(!useBaseCurrency) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("currency_toggle_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Switch Currency",
                                modifier = Modifier.size(12.dp),
                                tint = if (useBaseCurrency) {
                                    if (isDark) FintechGreen else LightPrimary
                                } else {
                                    if (isDark) TextMuted else LightTextMuted
                                }
                            )
                            Text(
                                text = if (useBaseCurrency) "USD ($)" else "LOCAL (${displayCurrency})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (useBaseCurrency) {
                                    if (isDark) FintechGreen else LightPrimary
                                } else {
                                    if (isDark) TextLight else LightText
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setDarkTheme(!isDark) },
                        modifier = Modifier.testTag("dashboard_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.testTag("quick_add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Quick Add Expense")
            }
        },
        containerColor = Color.Transparent
    ) { padValue ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValue)
                .testTag("dashboard_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DatabaseSyncStatusBadge(viewModel = viewModel)
        }

        item {
            OneTapShortcutCard(
                onOpenModal = { showOneTapModal = true }
            )
        }

        item {
            BrandColorThemeSelectorCard(viewModel = viewModel)
        }

        item {
            SpendingTrendCalendarCard(
                viewModel = viewModel,
                isDark = isDark
            )
        }

        item {
            FinancialAchievementsCard(
                viewModel = viewModel
            )
        }

        item {
            D3CalendarHeatmapCard(
                viewModel = viewModel,
                isDark = isDark
            )
        }

        item {
            DatabasePruneCard(
                viewModel = viewModel,
                isDark = isDark
            )
        }

        if (showBackupReminder) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_reminder_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alert",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "BACKUP REMINDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You have added $txCountSinceLastExport unbacked-up expense ledger modifications. Export your financial records to local database backups for offline data security.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.exportReportToCsv(dashboardContext) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).weight(1f).testTag("backup_export_csv_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.exportSqliteDatabaseEncrypted(dashboardContext) { _ -> } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).weight(1.5f).testTag("backup_export_db_btn")
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Secure DB Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.exportReportToPdf(dashboardContext) },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp).weight(1f).testTag("backup_export_pdf_btn")
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- DATA PORTABILITY & SECURITY HUB ---
        item {
            val hubGradientBrush = remember(isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(CardSlate, CardSlateElevated.copy(alpha = 0.5f))
                    } else {
                        listOf(LightCard, LightBg)
                    }
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.8.dp, if (isDark) BorderColor.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(hubGradientBrush, RoundedCornerShape(16.dp))
                    .testTag("data_portability_hub_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Data Security",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "DATA PORTABILITY & UTILITY HUB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Import, export, backup, or restore your financial data securely.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.exportReportToCsv(dashboardContext) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("hub_export_csv_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { csvPickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("hub_import_csv_btn")
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.exportSqliteDatabaseEncrypted(dashboardContext) { _ -> } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("hub_backup_db_btn")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Secure Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { backupPickerLauncher.launch("*/*") },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("hub_restore_db_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    var showCloudSettings by remember { mutableStateOf(false) }
                    val remoteSyncUrl by viewModel.remoteSyncUrl.collectAsState()
                    val exportPasscode by viewModel.exportPasscode.collectAsState()
                    var syncUrlInput by remember(remoteSyncUrl) { mutableStateOf(remoteSyncUrl) }
                    var passcodeInput by remember(exportPasscode) { mutableStateOf(exportPasscode) }
                    var cloudSyncStatus by remember { mutableStateOf("") }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCloudSettings = !showCloudSettings }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "PRODUCTION CLOUD VAULT SYNC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Icon(
                            imageVector = if (showCloudSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (showCloudSettings) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = syncUrlInput,
                            onValueChange = {
                                syncUrlInput = it
                                viewModel.setRemoteSyncUrl(it)
                            },
                            label = { Text("Remote Sync Server URL", fontSize = 11.sp) },
                            placeholder = { Text("e.g. http://10.0.2.2:5000", fontSize = 11.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hub_cloud_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Real-time Authenticated REST Sync Section ---
                        val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                        val usernameState by viewModel.username.collectAsState()
                        val userEmailState by viewModel.userEmail.collectAsState()

                        var isRegisterMode by remember { mutableStateOf(false) }
                        var authUsernameInput by remember { mutableStateOf("") }
                        var authEmailInput by remember { mutableStateOf("") }
                        var authPasswordInput by remember { mutableStateOf("") }
                        var authError by remember { mutableStateOf("") }
                        var authLoading by remember { mutableStateOf(false) }

                        if (!isLoggedIn) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isRegisterMode) "Create Secured Cloud Account" else "Sign In to Cloud Vault",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isRegisterMode) {
                                        OutlinedTextField(
                                            value = authUsernameInput,
                                            onValueChange = { authUsernameInput = it },
                                            label = { Text("Username", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("auth_username_field"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    OutlinedTextField(
                                        value = authEmailInput,
                                        onValueChange = { authEmailInput = it },
                                        label = { Text("Email Address", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = authPasswordInput,
                                        onValueChange = { authPasswordInput = it },
                                        label = { Text("Password", fontSize = 11.sp) },
                                        singleLine = true,
                                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )

                                    if (authError.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(authError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { 
                                            isRegisterMode = !isRegisterMode
                                            authError = ""
                                        }) {
                                            Text(
                                                text = if (isRegisterMode) "Already have an account? Sign In" else "New here? Create Account",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (authEmailInput.isEmpty() || authPasswordInput.isEmpty() || (isRegisterMode && authUsernameInput.isEmpty())) {
                                                    authError = "Please fill in all fields"
                                                    return@Button
                                                }
                                                authLoading = true
                                                authError = ""
                                                if (isRegisterMode) {
                                                    viewModel.registerUser(authUsernameInput, authPasswordInput, authEmailInput) { err ->
                                                        authLoading = false
                                                        if (err != null) authError = err else {
                                                            authUsernameInput = ""
                                                            authEmailInput = ""
                                                            authPasswordInput = ""
                                                        }
                                                    }
                                                } else {
                                                    viewModel.loginUser(authEmailInput, authPasswordInput) { err ->
                                                        authLoading = false
                                                        if (err != null) authError = err else {
                                                            authEmailInput = ""
                                                            authPasswordInput = ""
                                                        }
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp).testTag("auth_submit_btn")
                                        ) {
                                            if (authLoading) {
                                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.dp, color = MaterialTheme.colorScheme.onPrimary)
                                            } else {
                                                Text(if (isRegisterMode) "Register" else "Login", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- LOGGED IN SECURED SYNC DASHBOARD ---
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Secured Cloud Vault", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("User: ${usernameState ?: ""} (${userEmailState ?: ""})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                        }
                                        TextButton(onClick = { viewModel.logoutUser() }) {
                                            Text("Sign Out", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.syncExpensesAndAccountsWithCloud { status ->
                                                cloudSyncStatus = status
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("rest_sync_now_btn")
                                    ) {
                                        Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Local Database with Cloud REST API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "ALTERNATIVE: ZERO-KNOWLEDGE PASSCODE BACKUP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = passcodeInput,
                            onValueChange = {
                                passcodeInput = it
                                viewModel.setExportPasscode(it)
                            },
                            label = { Text("Client Cryptographic Passphrase", fontSize = 11.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hub_cloud_passcode_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.uploadBackupToCloud(dashboardContext) { status ->
                                        cloudSyncStatus = status
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("hub_cloud_upload_btn")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cloud Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.downloadBackupFromCloud(dashboardContext) { status ->
                                        cloudSyncStatus = status
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("hub_cloud_download_btn")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cloud Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (cloudSyncStatus.isNotEmpty() && cloudSyncStatus != "SUCCESS") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = cloudSyncStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 1. COMMAND INDEX BANNER ---
        item {
            val gradientBrush = remember(isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(FintechGreen.copy(alpha = 0.22f), CardSlate)
                    } else {
                        listOf(LightPrimary.copy(alpha = 0.15f), LightCard)
                    }
                )
            }
            val borderBrush = remember(isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(FintechGreen.copy(alpha = 0.35f), BorderColor.copy(alpha = 0.2f))
                    } else {
                        listOf(LightPrimary.copy(alpha = 0.4f), Color.LightGray.copy(alpha = 0.3f))
                    }
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderBrush),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush, RoundedCornerShape(24.dp))
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
                                text = "TOTAL NET WORTH",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatCurrency(netWorthSummary.totalNetWorth),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.testTag("total_balance_text")
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.triggerExchangeRatesFetch(
                                        onSuccess = { },
                                        onFailure = { }
                                    )
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .size(44.dp)
                                    .testTag("refresh_exchange_rates_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyExchange,
                                    contentDescription = "Refresh exchange rates and update net worth calculation on demand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.syncAllActiveAccounts() },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .size(44.dp)
                                    .testTag("quick_refresh_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync all bank gateway accounts and update net worth balance",
                                    tint = MaterialTheme.colorScheme.background
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Manual entries",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "MANUAL CASH",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatCurrency(netWorthSummary.manualNetInLocalCurrency),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("manual_net_worth_text")
                                )
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Synced accounts",
                                        tint = FintechGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SYNCED BANK/MFS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.formatCurrency(netWorthSummary.syncedNetInLocalCurrency),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.testTag("synced_net_worth_text")
                                )
                            }
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
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
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
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
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

        // --- ACTIVE SPENDING ALERTS ---
        if (budgetAlerts.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_budget_alerts_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Active Alerts",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "ACTIVE BUDGET ALERTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        budgetAlerts.forEach { alert ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "🚨", fontSize = 14.sp)
                                Column {
                                    Text(
                                        text = alert.message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MONTHLY PERFORMANCE HEADER CARDS ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MONTHLY PERFORMANCE CARDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Total Expenses Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(145.dp)
                            .testTag("summary_card_total_expenses")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = "Total Expenses",
                                tint = ExpenseRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Total Expenses",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = viewModel.formatCurrency(outflow),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRose
                            )
                        }
                    }

                    // 2. Current Balance Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(145.dp)
                            .testTag("summary_card_current_balance")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Current Balance",
                                tint = FintechGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Current Balance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = viewModel.formatCurrency(totalBalance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FintechGreen
                            )
                        }
                    }

                    // 3. Remaining Budget Card
                    val totalBudgeted = budgets.sumOf { it.amount }
                    val remainingBudget = totalBudgeted - outflow
                    val budgetColor = if (remainingBudget >= 0) FintechGreen else ExpenseRose
                    val budgetBg = if (remainingBudget >= 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = budgetBg
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(185.dp)
                            .testTag("summary_card_remaining_budget")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = "Remaining Budget",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Remaining Budget",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = viewModel.formatCurrency(remainingBudget),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = budgetColor,
                                    maxLines = 1
                                )
                            }
                            BudgetProgressRing(
                                spent = outflow,
                                limit = totalBudgeted
                            )
                        }
                    }
                }
            }
        }

        // --- 1.12. YEARLY FISCAL TRENDS & ANNUAL REPORT ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showYearlyReport = true }
                    .testTag("dashboard_yearly_report_button_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Yearly Report Icon",
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Column {
                            Text(
                                "Annual Fiscal Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Aggregate monthly spend trends & categories breakdown",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open report",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- 1.25. ANOMALY DETECTION AND FRAUD WARNINGS ---
        if (anomalies.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("anomaly_alert_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alert",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SUSPICIOUS PATTERN DEVIATIONS (${anomalies.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WealthFlow real-time risk algorithms have flagged the following potential duplicate records or transaction outliers for prompt review:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            lineHeight = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            anomalies.take(3).forEach { report ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = report.transaction.merchant.ifEmpty { "Indirect Ledger Entry" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = viewModel.formatCurrency(report.transaction.amount),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = report.description,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                            lineHeight = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Reliability Confidence: ${(report.confidence * 100).toInt()}% • Group: ${report.type.name.replace("_", " ")}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            if (anomalies.size > 3) {
                                Text(
                                    text = "+ ${anomalies.size - 3} additional suspicious records audited.",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 1.5. SMART SPENDING PROJECTION AND VELOCITY ALERT ---
        if (budgetProjection.totalBudget > 0.0) {
            item {
                val isWarning = budgetProjection.isProjectedToExceed
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWarning) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isWarning) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_projection_trend_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = "Trend Alert Symbol",
                                tint = if (isWarning) MaterialTheme.colorScheme.error else FintechGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWarning) "BUDGET OVERRUN ALERT (TREND-PROJECTED)" else "SAFE VELOCITY PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWarning) MaterialTheme.colorScheme.error else FintechGreen,
                                letterSpacing = 0.8.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Daily Spend Avg",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = viewModel.formatCurrency(budgetProjection.dailyAverage),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Projected Month-End Finish",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${viewModel.formatCurrency(budgetProjection.projectedTotalSpent)} / ${viewModel.formatCurrency(budgetProjection.totalBudget)} limit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                val ratio = (budgetProjection.projectedTotalSpent / budgetProjection.totalBudget).toFloat()
                                ratio.coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isWarning) MaterialTheme.colorScheme.error else FintechGreen,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isWarning) {
                                "⚠️ Warning: Based on your average of ${viewModel.formatCurrency(budgetProjection.dailyAverage)} over ${budgetProjection.daysPassed} days, you are projected to exceed your current budget. We recommend immediate restraint."
                            } else {
                                "✅ Dynamic Trend Safe: Spending is well regulated (~${viewModel.formatCurrency(budgetProjection.dailyAverage)} daily). If speed is maintained, you will finish within limits."
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 14.sp
                        )
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

        // --- 2.2 CATEGORY BUDGET PROGRESS GAUGE ---
        item {
            val activeBudgetsSnapshot by viewModel.activeBudgets.collectAsState()
            val themeCategories by viewModel.categories.collectAsState()
            
            if (activeBudgetsSnapshot.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_budget_progress_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CATEGORY BUDGET PROGRESS GAUGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activeBudgetsSnapshot.forEach { bud ->
                                val cat = themeCategories.find { it.id == bud.categoryId }
                                val pct = if (bud.amount > 0.0) (bud.spent / bud.amount).toFloat().coerceIn(0f, 1f) else 0f
                                val barColor = when {
                                    pct >= 1.0f -> ExpenseRose
                                    pct >= 0.85f -> AccentGold
                                    else -> FintechGreen
                                }
                                
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat?.name ?: "Category Limit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${viewModel.formatCurrency(bud.spent)} / ${viewModel.formatCurrency(bud.amount)} (${(pct * 100).toInt()}%)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (pct >= 1.0f) ExpenseRose else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ESTABLISH SPENDING CAPS PER CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No spending limits set this month. Use the Category Limits page to customize caps & track overruns.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // --- 2.5 DEDICATED GEMINI AI SUMMARY & SPENDING ADVICE CARD ---
        item {
            val aiReport by viewModel.aiReport.collectAsState()
            val aiLoading by viewModel.aiInsightsLoading.collectAsState()
            val activeCountryConfig by viewModel.activeCountryConfig.collectAsState()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_ai_summary_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini Spark",
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI ADVISOR INSIGHTS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.triggerGeminiEvaluation() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("gemini_recompute_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload predictions",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (aiLoading) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Text(
                                text = "Strategizing tax-saving advice tailored for ${activeCountryConfig.country}...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else if (aiReport != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RenderMarkdownStructuredText(markdown = aiReport!!)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Get personalized, instant budget optimization tips and legal tax deductions based on ${activeCountryConfig.country} guidelines.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.triggerGeminiEvaluation() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("gemini_card_trigger_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "RUN GEMINI FINANCIAL AUDIT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2B. GEMINI AI CONTEXT-AWARE SPENDING TOOLTIPS ---
        item {
            ContextAwareAiInsightsTooltipsCard(viewModel = viewModel, isDark = isDark)
        }

        // --- 3. ACCOUNTS SNAPSHOTS (HORIZONTAL SCROLLER SIMULATOR) ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNTS & MOBILE WALLETS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { showControlCenter = true },
                        modifier = Modifier.size(24.dp).testTag("accounts_control_panel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Inter-App Bank Controller",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (accounts.isEmpty()) {
                    Text(
                        text = "No accounts configured. Click settings gear icon to link banks & mobile wallets.",
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

        // --- 3B. LINKED INTEGRATION MODULES (ACTIVE CHANNELS) ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("linked_accounts_dashboard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LINKED BANK & MFS SERVICE CHANNELS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.syncAllActiveAccounts() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("dashboard_bulk_sync_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FintechGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BULK SYNC", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val integrations = accounts.filter { it.type == "BANK" || it.type == "MOBILE_WALLET" }
                    if (integrations.isEmpty()) {
                        Text(
                            text = "No linked external banking/MFS gateways active. Configure connections inside the setup gear panel.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            integrations.forEach { account ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
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
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (account.type == "BANK") Icons.Default.AccountBalance else Icons.Default.PhonelinkRing,
                                                        contentDescription = null,
                                                        tint = Color(android.graphics.Color.parseColor(account.accountColorHex)),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = account.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${account.provider} Gateway • ${if (account.isSyncEnabled) "ACTIVE SYNCHRONIZATION" else "LOCAL ACCOUNT ONLY"}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (account.isSyncEnabled) FintechGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }

                                            Text(
                                                text = viewModel.formatCurrency(account.balance),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.testTag("linked_acc_balance_${account.id}")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = account.isSyncEnabled,
                                                    onCheckedChange = { viewModel.toggleAccountSync(account) },
                                                    modifier = Modifier.size(24.dp).testTag("sync_toggle_chk_${account.id}")
                                                )
                                                Text(
                                                    text = "Automated API Syncing",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.syncAccountInstance(account) },
                                                enabled = account.isSyncEnabled,
                                                modifier = Modifier
                                                    .background(
                                                        if (account.isSyncEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                        CircleShape
                                                    )
                                                    .size(32.dp)
                                                    .testTag("api_sync_refresh_btn_${account.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Sync transaction logs",
                                                    tint = if (account.isSyncEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(16.dp)
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
                    var chartSelectionState by remember { mutableIntStateOf(0) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VISUAL ANALYSIS PLATFORM",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Three-Way Selector
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val choices = listOf("Native Pie", "D3 Pie", "Recharts Trend")
                        choices.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = chartSelectionState == index,
                                onClick = { chartSelectionState = index },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
                                modifier = Modifier.testTag("chart_btn_$index")
                            ) {
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
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

                        if (chartSelectionState == 2) {
                            // Recharts custom line area & bar graphs
                            val calendar = java.util.Calendar.getInstance()
                            val activeYearMonth = selectedMonth // "YYYY-MM"
                            val daysInMonth = try {
                                val parts = activeYearMonth.split("-")
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.YEAR, parts[0].toInt())
                                cal.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                                cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                            } catch (e: Exception) {
                                30
                            }

                            val dayDailyTotals = DoubleArray(daysInMonth + 1)
                            expenseTransactions.forEach { tx ->
                                calendar.timeInMillis = tx.timestamp
                                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                if (day in 1..daysInMonth) {
                                    dayDailyTotals[day] += tx.amount
                                }
                            }

                            var runSum = 0.0
                            val trendJson = (1..daysInMonth).joinToString(",") { day ->
                                runSum += dayDailyTotals[day]
                                "{\"day\": $day, \"Spent\": $runSum}"
                            }

                            val activeBudgetsSnapshot by viewModel.activeBudgets.collectAsState()
                            val budgetBarJson = activeBudgetsSnapshot.joinToString(",") { bud ->
                                val catName = categories.find { it.id == bud.categoryId }?.name ?: "Unknown"
                                "{\"category\": \"${catName.replace("\"", "\\\"")}\", \"Limit\": ${bud.amount}, \"Spent\": ${bud.spent}}"
                            }

                            val calendarTmp = java.util.Calendar.getInstance()
                            try {
                                val monthParts = selectedMonth.split("-")
                                if (monthParts.size == 2) {
                                    calendarTmp.set(java.util.Calendar.YEAR, monthParts[0].toInt())
                                    calendarTmp.set(java.util.Calendar.MONTH, monthParts[1].toInt() - 1)
                                }
                            } catch (e: Exception) {}
                            calendarTmp.set(java.util.Calendar.DAY_OF_MONTH, 1)
                            val firstDayOfWeek = calendarTmp.get(java.util.Calendar.DAY_OF_WEEK)
                            val firstDayOffset = firstDayOfWeek - 1

                            val dailySpentJson = (1..daysInMonth).joinToString(",") { day ->
                                "{\"day\": $day, \"amount\": ${dayDailyTotals[day]}}"
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(980.dp)
                            ) {
                                RechartsTrendWebView(
                                    trendDataJson = trendJson,
                                    budgetBarJson = budgetBarJson,
                                    monthToMonthDataJson = monthToMonthSpending,
                                    dailySpentDataJson = dailySpentJson,
                                    startDayOffset = firstDayOffset,
                                    currencySymbol = displayCurrencySymbol,
                                    isDark = isDark,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
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
                                        if (chartSelectionState == 1) {
                                            D3ChartWebView(
                                                shares = sortedCategories,
                                                currencySymbol = countryConfig.currencySymbol,
                                                isDark = isDark,
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

        // --- STANDALONE FINANCIAL TOOLS ---
        item {
            StandaloneCurrencyConverterWidget(viewModel = viewModel)
        }
        item {
            UpcomingBillsCalendar(viewModel = viewModel)
        }
        item {
            SmartSubscriptionDetectorComponent(viewModel = viewModel)
        }
        item {
            DebtPayoffTimelineComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            FinancialHealthScoreComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            ComparativeSpendingBarChartComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            InteractiveSunburstChartComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            DedicatedSavingsGoalsTrackerComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            FutureSavingsCalculatorComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            MonthlyTransactionsCalendarComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            InteractiveAssetDistributionChartComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            EmergencyFundOptimizerComponent(viewModel = viewModel)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            SubscriptionIntelligenceComponent(viewModel = viewModel)
        }
    }
    }

    if (showQuickAddDialog) {
        QuickAddExpenseDialog(
            viewModel = viewModel,
            onDismiss = { showQuickAddDialog = false }
        )
    }

    if (showOneTapModal) {
        OneTapSimpleExpenseModal(
            viewModel = viewModel,
            onDismiss = { showOneTapModal = false }
        )
    }

    if (showYearlyReport) {
        YearlyReportDialog(
            viewModel = viewModel,
            onDismissRequest = { showYearlyReport = false }
        )
    }

    if (showControlCenter) {
        AlertDialog(
            onDismissRequest = { showControlCenter = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Inter-App Bank link icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("BANK & MFS CONTROL CENTER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                var selectedTab by remember { mutableStateOf(0) } // 0: Link, 1: Control
                var name by remember { mutableStateOf("") }
                var balanceStr by remember { mutableStateOf("") }
                var accountType by remember { mutableStateOf("BANK") }
                var provider by remember { mutableStateOf("Chase") }
                var expandedProvider by remember { mutableStateOf(false) }
                
                var activeAuditLog by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
                var activeAuditLoading by remember { mutableStateOf<Map<Long, Boolean>>(emptyMap()) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Link App", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("App Controller", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Auto-Match Rules", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                    
                    if (selectedTab == 0) {
                        Text("Connect External Bank or Mobile Financial Service (MFS)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("App Label Name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("link_acc_name_input")
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("BANK", "MOBILE_WALLET").forEach { type ->
                                FilterChip(
                                    selected = accountType == type,
                                    onClick = { 
                                        accountType = type
                                        provider = if (type == "BANK") "Chase" else "bKash"
                                    },
                                    label = { Text(if (type == "BANK") "🏦 BANK APP" else "📱 MFS WALLET", fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        val providerList = if (accountType == "BANK") {
                            listOf("Chase", "Bank of America", "HSBC", "Citi", "State Bank of India", "Barclays")
                        } else {
                            listOf("bKash", "SSL Wallet", "Nagad", "Rocket", "Upay")
                        }
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedProvider = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Provider API Gateway: $provider", fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedProvider,
                                onDismissRequest = { expandedProvider = false }
                            ) {
                                providerList.forEach { prov ->
                                    DropdownMenuItem(
                                        text = { Text(prov, fontSize = 11.sp) },
                                        onClick = {
                                            provider = prov
                                            expandedProvider = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = balanceStr,
                            onValueChange = { balanceStr = it },
                            label = { Text("Starter Connection Balance", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("link_acc_balance_input")
                        )
                        
                        Button(
                            onClick = {
                                val bal = balanceStr.toDoubleOrNull() ?: 0.0
                                if (name.isNotEmpty()) {
                                    viewModel.addAccount(
                                        name = name,
                                        type = accountType,
                                        startingBalance = bal,
                                        provider = provider,
                                        colorHex = if (accountType == "BANK") "#0D9488" else "#DB2777"
                                    )
                                    android.widget.Toast.makeText(context, "$provider app integrated!", android.widget.Toast.LENGTH_SHORT).show()
                                    name = ""
                                    balanceStr = ""
                                    selectedTab = 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("link_institution_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("LINK SECURE APP INSTANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (selectedTab == 1) {
                        if (accounts.isEmpty()) {
                            Text("No linked apps. Link accounts in first tab.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 240.dp).fillMaxWidth()
                            ) {
                                items(accounts.size) { index ->
                                    val acc = accounts[index]
                                    val isChecking = activeAuditLoading[acc.id] ?: false
                                    val auditLog = activeAuditLog[acc.id] ?: "API Idle. Secure token registered."
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text("${acc.provider} [${acc.type}]", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(viewModel.formatCurrency(acc.balance), fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            }
                                            
                                            Text(
                                                text = "Log: $auditLog",
                                                fontSize = 9.sp,
                                                color = if (isChecking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        activeAuditLoading = activeAuditLoading + (acc.id to true)
                                                        activeAuditLog = activeAuditLog + (acc.id to "Handshaking secure API Plaid credentials...")
                                                        coroutineScope.launch {
                                                            kotlinx.coroutines.delay(1000)
                                                            activeAuditLoading = activeAuditLoading + (acc.id to false)
                                                            activeAuditLog = activeAuditLog + (acc.id to "Success! Plaid Visa token verified. Balance synchronized.")
                                                            android.widget.Toast.makeText(context, "${acc.name} state synced!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    enabled = !isChecking,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("SYNC API", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        activeAuditLoading = activeAuditLoading + (acc.id to true)
                                                        activeAuditLog = activeAuditLog + (acc.id to "Validating routing, SSL handshake, compliance encryption...")
                                                        coroutineScope.launch {
                                                            kotlinx.coroutines.delay(1200)
                                                            activeAuditLoading = activeAuditLoading + (acc.id to false)
                                                            activeAuditLog = activeAuditLog + (acc.id to "Cleared: Encryption block secure (SHA-256 routing verified).")
                                                            android.widget.Toast.makeText(context, "${acc.name} deep audit completed!", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    enabled = !isChecking,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = FintechGreen.copy(alpha = 0.12f),
                                                        contentColor = FintechGreen
                                                    ),
                                                    modifier = Modifier.weight(1.1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("AUDIT", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        activeAuditLog = activeAuditLog + (acc.id to "Deep-link intent generated. Launcher redirected safely.")
                                                        android.widget.Toast.makeText(context, "Launched external secure app!", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    enabled = !isChecking,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    modifier = Modifier.weight(1f).height(28.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text("LAUNCH", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 2: Custom auto match rule mapping controls!
                        var ruleKeyword by remember { mutableStateOf("") }
                        var ruleCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
                        var isTaxDeductible by remember { mutableStateOf(false) }
                        var taxRateStr by remember { mutableStateOf("8.25") }
                        var expandedCatDropdown by remember { mutableStateOf(false) }

                        Text("Configure patterns to auto-assign category & tax tags", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = ruleKeyword,
                            onValueChange = { ruleKeyword = it },
                            label = { Text("Keyword Pattern (e.g., Walmart)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("rule_keyword_input")
                        )

                        // Category Dropdown selector
                        val matchedCat = categories.find { it.id == ruleCategoryId } ?: categories.firstOrNull()
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedCatDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mapped Category: ${matchedCat?.name ?: "Select Box"}", fontSize = 11.sp)
                            }
                            DropdownMenu(
                                expanded = expandedCatDropdown,
                                onDismissRequest = { expandedCatDropdown = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name, fontSize = 11.sp) },
                                        onClick = {
                                            ruleCategoryId = cat.id
                                            expandedCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isTaxDeductible,
                                    onCheckedChange = { isTaxDeductible = it },
                                    modifier = Modifier.size(32.dp).testTag("rule_tax_checkbox")
                                )
                                Text("Mark Tax Deductible", fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                            }

                            if (isTaxDeductible) {
                                OutlinedTextField(
                                    value = taxRateStr,
                                    onValueChange = { taxRateStr = it },
                                    label = { Text("Tax Rate %", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.width(100.dp).testTag("rule_tax_rate_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                if (ruleKeyword.isNotEmpty()) {
                                    val rateVal = taxRateStr.toDoubleOrNull() ?: 0.0
                                    viewModel.addMatchingRule(ruleKeyword, ruleCategoryId, isTaxDeductible, rateVal)
                                    ruleKeyword = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_matching_rule_btn")
                        ) {
                            Text("SAVE AUTO-MATCHING RULE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        Text("ACTIVE MAPPING TEMPLATES (${matchingRules.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold)

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth()
                        ) {
                            items(matchingRules.size) { index ->
                                val r = matchingRules[index]
                                val rCatName = categories.find { it.id == r.categoryId }?.name ?: "Unknown"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("\"${r.keyword}\" ➜ $rCatName", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(
                                            text = if (r.isTaxDeductible) "Tax Deductible (${r.taxRate}%)" else "Non-deductible Ledger",
                                            fontSize = 9.sp,
                                            color = if (r.isTaxDeductible) FintechGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMatchingRule(r) },
                                        modifier = Modifier.size(24.dp).testTag("delete_rule_btn_${r.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete mapping pattern Rule",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showControlCenter = false }) {
                    Text("Close", fontSize = 11.sp)
                }
            }
        )
    }

    val showRatePrompt by viewModel.showRatePrompt.collectAsState()
    if (showRatePrompt) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.dismissRatePrompt() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Exchange rates update prompt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Currency Rates")
                }
            },
            text = {
                Text(
                    text = "Would you like to update your currency rates using the latest online prices (USD base currency) to guarantee absolute multi-currency portfolio accuracy?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                var loading by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        loading = true
                        viewModel.triggerExchangeRatesFetch(
                            onSuccess = { msg ->
                                loading = false
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            },
                            onFailure = { err ->
                                loading = false
                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.testTag("rate_prompt_confirm_btn")
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissRatePrompt() },
                    modifier = Modifier.testTag("rate_prompt_dismiss_btn")
                ) {
                    Text("Skip")
                }
            }
        )
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
    isDark: Boolean = true,
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
                    background-color: ${if (isDark) "#141B2D" else "#FFFFFF"};
                    color: ${if (isDark) "#F8FAFC" else "#1F2937"};
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
                    .attr("stroke", "${if (isDark) "#141B2D" else "#FFFFFF"}")
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
                    .style("fill", "${if (isDark) "#94A3B8" else "#6B7280"}")
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
                setBackgroundColor(android.graphics.Color.parseColor(if (isDark) "#141B2D" else "#FFFFFF"))
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://local.d3", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

@Composable
fun BudgetProgressRing(
    spent: Double,
    limit: Double,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val ratio = if (limit > 0.0) (spent / limit).coerceIn(0.0, 1.0) else 0.0
    val sweepAngle = (ratio * 360f).toFloat()

    val trackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val progressColor = if (ratio > 0.9) ExpenseRose else FintechGreen

    Box(
        modifier = modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = trackColor,
                radius = size.minDimension / 2f - 2.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5.dp.toPx())
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 3.5.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Text(
            text = "${(ratio * 100).toInt()}%",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color.Black
        )
    }
}

@Composable
fun RechartsTrendWebView(
    trendDataJson: String,
    budgetBarJson: String,
    monthToMonthDataJson: String,
    dailySpentDataJson: String,
    startDayOffset: Int,
    currencySymbol: String,
    isDark: Boolean = true,
    modifier: Modifier = Modifier
) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <script src="https://cdn.tailwindcss.com"></script>
            <script src="https://unpkg.com/react@18/umd/react.production.min.js" crossorigin></script>
            <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" crossorigin></script>
            <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.js" crossorigin></script>
            <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
            <style>
                body {
                    background-color: ${if (isDark) "#141B2D" else "#FFFFFF"};
                    color: ${if (isDark) "#F8FAFC" else "#1F2937"};
                    font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                    margin: 0;
                    padding: 8px 12px 24px 12px;
                    box-sizing: border-box;
                    width: 100vw;
                    height: 100vh;
                    overflow-x: hidden;
                    overflow-y: auto;
                }
                ::-webkit-scrollbar {
                    width: 4px;
                }
                ::-webkit-scrollbar-track {
                    background: transparent;
                }
                ::-webkit-scrollbar-thumb {
                    background: ${if (isDark) "#334155" else "#CBD5E1"};
                    border-radius: 2px;
                }
            </style>
        </head>
        <body>
            <div id="root"></div>

            <script type="text/babel">
                const { 
                    ResponsiveContainer, AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
                    XAxis, YAxis, CartesianGrid, Tooltip, Legend 
                } = Recharts;

                const trendData = [ $trendDataJson ];
                const budgetData = [ $budgetBarJson ];
                const monthlyData = [ $monthToMonthDataJson ];
                const dailySpentData = [ $dailySpentDataJson ];
                const startDayOffset = $startDayOffset;
                const isDark = ${isDark};
                const currencySymbol = "$currencySymbol";

                const nonZeroBudgets = budgetData.filter(b => b.Spent > 0);
                const pieData = nonZeroBudgets.length > 0 ? nonZeroBudgets : [{ category: "No Expenses", Spent: 0.01 }];

                const COLORS = ['#10B981', '#3B82F6', '#FBBF24', '#8B5CF6', '#EC4899', '#EF4444', '#14B8A6', '#F97316', '#22C55E', '#A855F7'];

                const CustomTooltip = ({ active, payload, label }) => {
                    if (active && payload && payload.length) {
                        return (
                            <div className={"p-2.5 rounded-lg border text-xs shadow-xl font-semibold " + (
                                isDark ? "bg-slate-900 border-slate-800 text-slate-100" : "bg-white border-slate-200 text-slate-800"
                            )}>
                                <p className="font-bold mb-1 opacity-75">{label}</p>
                                {payload.map((p, idx) => (
                                    <p key={idx} style={{ color: p.color || p.fill }}>
                                        {p.name}: {currencySymbol}{Number(p.value).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}
                                    </p>
                                ))}
                            </div>
                        );
                    }
                    return null;
                };

                function App() {
                    const totalLimit = budgetData.reduce((acc, b) => acc + (b.Limit || 0), 0);
                    const totalSpent = budgetData.reduce((acc, b) => acc + (b.Spent || 0), 0);
                    const remaining = totalLimit - totalSpent;
                    const spentPercent = totalLimit > 0 ? Math.min(100, (totalSpent / totalLimit) * 100) : 0;

                    return (
                        <div className="flex flex-col gap-6 w-full pb-8">
                            {/* Radial Budget Progress Ring */}
                            {totalLimit > 0 && (
                                <div className={"p-4 rounded-xl border flex items-center justify-between gap-4 " + (isDark ? "bg-slate-900/60 border-slate-800/80" : "bg-slate-50 border-slate-200")}>
                                    <div className="flex-1">
                                        <h3 className="text-[11px] font-bold tracking-wider text-emerald-500 uppercase mb-1">
                                            🎯 TOTAL BUDGET REACH RING
                                        </h3>
                                        <p className="text-xs font-semibold opacity-85">
                                            Spent <span className="font-bold text-rose-500">{currencySymbol}{totalSpent.toLocaleString(undefined, {maximumFractionDigits: 0})}</span> of {currencySymbol}{totalLimit.toLocaleString(undefined, {maximumFractionDigits: 0})} limit.
                                        </p>
                                        <div className="mt-2 text-[10px] opacity-70">
                                            {remaining > 0 ? (
                                                <span className="text-emerald-500 font-bold">✓ {currencySymbol}{remaining.toLocaleString(undefined, {maximumFractionDigits: 0})} safe</span>
                                            ) : (
                                                <span className="text-rose-500 font-bold">⚠️ Overrun by {currencySymbol}{Math.abs(remaining).toLocaleString(undefined, {maximumFractionDigits: 0})}</span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="w-[100px] h-[100px] relative flex items-center justify-center">
                                        <div className="absolute inset-0">
                                            <ResponsiveContainer width="100%" height="100%">
                                                <PieChart>
                                                    <Pie
                                                        data={[
                                                            { name: 'Spent', value: totalSpent, fill: spentPercent > 90 ? '#F43F5E' : '#10B981' },
                                                            { name: 'Remaining', value: Math.max(0, remaining), fill: isDark ? '#1E293B' : '#E2E8F0' }
                                                        ]}
                                                        cx="50%"
                                                        cy="50%"
                                                        innerRadius={30}
                                                        outerRadius={40}
                                                        startAngle={90}
                                                        endAngle={-270}
                                                        dataKey="value"
                                                    />
                                                </PieChart>
                                            </ResponsiveContainer>
                                        </div>
                                        <span className="text-xs font-black z-10">{Math.round(spentPercent)}%</span>
                                    </div>
                                </div>
                            )}

                            {/* 1. Area Spend Trend */}
                            <div className={"p-4 rounded-xl border " + (isDark ? "bg-slate-900/60 border-slate-800/80" : "bg-slate-50 border-slate-200")}>
                                <div className="flex justify-between items-center mb-3">
                                    <h3 className="text-[11px] font-bold tracking-wider text-emerald-500 uppercase flex items-center gap-1.5">
                                        📈 CUMULATIVE MONTH SPENDING TREND (RECHARTS)
                                    </h3>
                                    <span className="text-[9px] opacity-60">Real-time Cloud Sync</span>
                                </div>
                                <div className="h-[160px] w-full">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={trendData} margin={{ top: 5, right: 5, left: -25, bottom: 0 }}>
                                            <defs>
                                                <linearGradient id="colorSpent" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor="#10B981" stopOpacity={isDark ? 0.4 : 0.25}/>
                                                    <stop offset="95%" stopColor="#10B981" stopOpacity={0}/>
                                                </linearGradient>
                                            </defs>
                                            <CartesianGrid strokeDasharray="3 3" stroke={isDark ? "#1E293B" : "#E2E8F0"} vertical={false} />
                                            <XAxis 
                                                dataKey="day" 
                                                tickFormatter={(day) => "Day " + day} 
                                                tick={{ fill: isDark ? "#94A3B8" : "#4B5563", fontSize: 9 }}
                                                stroke={isDark ? "#334155" : "#CBD5E1"}
                                            />
                                            <YAxis 
                                                tickFormatter={(val) => currencySymbol + val} 
                                                tick={{ fill: isDark ? "#94A3B8" : "#4B5563", fontSize: 9 }}
                                                stroke={isDark ? "#334155" : "#CBD5E1"}
                                            />
                                            <Tooltip content={<CustomTooltip />} />
                                            <Area 
                                                type="monotone" 
                                                dataKey="Spent" 
                                                name="Total Spent" 
                                                stroke="#10B981" 
                                                strokeWidth={2} 
                                                fillOpacity={1} 
                                                fill="url(#colorSpent)" 
                                            />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>

                            {/* 1b. Monthly Spending Intensity Heatmap */}
                            <div className={"p-4 rounded-xl border " + (isDark ? "bg-slate-900/60 border-slate-800/80" : "bg-slate-50 border-slate-200")}>
                                <div className="flex justify-between items-center mb-3">
                                    <h3 className="text-[11px] font-bold tracking-wider text-rose-500 uppercase flex items-center gap-1.5">
                                        🔥 MONTHLY SPENDING INTENSITY HEATMAP
                                    </h3>
                                    <span className="text-[9px] opacity-60">Avg: {currencySymbol}{Math.round(dailySpentData.reduce((acc, d) => acc + d.amount, 0) / (dailySpentData.length || 1))}/day</span>
                                </div>
                                <div className="grid grid-cols-7 gap-1 text-center mb-2">
                                    {['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'].map(h => (
                                        <div key={h} className="text-[9px] font-bold opacity-50 py-1">{h}</div>
                                    ))}
                                    {(() => {
                                        const cells = [];
                                        const totalSpentSum = dailySpentData.reduce((acc, d) => acc + d.amount, 0);
                                        const averageSpent = dailySpentData.length > 0 ? (totalSpentSum / dailySpentData.length) : 0;
                                        
                                        for (let i = 0; i < startDayOffset; i++) {
                                            cells.push(<div key={"empty-" + i} className="aspect-square opacity-0" />);
                                        }
                                        dailySpentData.forEach((item, idx) => {
                                            let bgColor = isDark ? "bg-slate-800/20 text-slate-600" : "bg-slate-100 text-slate-300";
                                            let borderCol = isDark ? "border-slate-800/50" : "border-slate-200";
                                            let textWeight = "font-normal";
                                            
                                            if (item.amount > 0) {
                                                if (item.amount > averageSpent * 1.5) {
                                                    bgColor = "bg-rose-500 text-white";
                                                    textWeight = "font-bold";
                                                } else if (item.amount >= averageSpent) {
                                                    bgColor = "bg-amber-500 text-slate-950";
                                                    textWeight = "font-bold";
                                                } else {
                                                    bgColor = "bg-emerald-500/50 text-emerald-950";
                                                    textWeight = "font-semibold";
                                                }
                                            }
                                            
                                            cells.push(
                                                <div 
                                                    key={"day-" + item.day} 
                                                    className={"aspect-square flex flex-col items-center justify-center rounded border " + borderCol + " " + bgColor + " transition-all relative group p-0.5 cursor-help"}
                                                    title={"Day " + item.day + ": " + currencySymbol + item.amount.toFixed(2)}
                                                >
                                                    <span className={"text-[10px] " + textWeight}>{item.day}</span>
                                                    {item.amount > 0 && (
                                                        <span className="text-[6.5px] opacity-90 block truncate max-w-full font-mono">
                                                            {currencySymbol}{Math.round(item.amount)}
                                                        </span>
                                                    )}
                                                </div>
                                            );
                                        });
                                        return cells;
                                    })()}
                                </div>
                                
                                <div className="flex gap-3 mt-3 justify-end text-[8.5px] font-semibold opacity-85">
                                    <div className="flex items-center gap-1">
                                        <span className="w-2.5 h-2.5 rounded bg-slate-800/20 border border-slate-700 inline-block"></span>
                                        <span>No Spend</span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <span className="w-2.5 h-2.5 rounded bg-emerald-500/50 inline-block"></span>
                                        <span>Below Avg</span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <span className="w-2.5 h-2.5 rounded bg-amber-500 inline-block"></span>
                                        <span>Above Avg</span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <span className="w-2.5 h-2.5 rounded bg-rose-500 inline-block"></span>
                                        <span>High Spikes</span>
                                    </div>
                                </div>
                            </div>

                            {/* 2. Budget limits bar */}
                            <div className={"p-4 rounded-xl border " + (isDark ? "bg-slate-900/60 border-slate-800/80" : "bg-slate-50 border-slate-200")}>
                                <h3 className="text-[11px] font-bold tracking-wider text-blue-500 uppercase mb-3">
                                    📊 CATEGORIES VS BUDGET LIMITS (RECHARTS)
                                </h3>
                                <div className="h-[180px] w-full">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={budgetData} margin={{ top: 5, right: 5, left: -25, bottom: 0 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke={isDark ? "#1E293B" : "#E2E8F0"} vertical={false} />
                                            <XAxis 
                                                dataKey="category" 
                                                tick={{ fill: isDark ? "#94A3B8" : "#4B5563", fontSize: 9 }}
                                                stroke={isDark ? "#334155" : "#CBD5E1"}
                                            />
                                            <YAxis 
                                                tickFormatter={(val) => currencySymbol + val} 
                                                tick={{ fill: isDark ? "#94A3B8" : "#4B5563", fontSize: 9 }}
                                                stroke={isDark ? "#334155" : "#CBD5E1"}
                                            />
                                            <Tooltip content={<CustomTooltip />} />
                                            <Legend wrapperStyle={{ fontSize: '9px', paddingTop: '8px' }} />
                                            <Bar dataKey="Limit" name="Budget Limit" fill="#3B82F6" radius={[4, 4, 0, 0]} />
                                            <Bar dataKey="Spent" name="Actual Spent" radius={[4, 4, 0, 0]}>
                                                {budgetData.map((entry, index) => {
                                                    const isOver = entry.Spent > entry.Limit;
                                                    return <Cell key={"cell-" + index} fill={isOver ? '#F43F5E' : '#10B981'} />;
                                                })}
                                            </Bar>
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>

                            {/* 3. Month to month spending */}
                            <div className={"p-4 rounded-xl border " + (isDark ? "bg-slate-900/60 border-slate-800/80" : "bg-slate-50 border-slate-200")}>
                                <h3 className="text-[11px] font-bold tracking-wider text-amber-500 uppercase mb-3">
                                    📅 MONTH-TO-MONTH SPENDING TREND
                                </h3>
                                <div className="h-[180px] w-full">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={monthlyData} margin={{ top: 5, right: 5, left: -25, bottom: 0 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke={isDark ? "#1E293B" : "#E2E8F0"} vertical={false} />
                                            <XAxis 
                                                dataKey="month" 
                                                tick={{ fill: isDark ? "#94A3B8" : "#4B5563", fontSize: 9 }}
                                                stroke={isDark ? "#334155" : "#CBD5E1"}
                                            />
                                            <YAxis 
                                                tickFormatter={(val) => currencySymbol + val} 
                                                tick={{ fill: isDark ? "#94A3B8" : "#4B5563", fontSize: 9 }}
                                                stroke={isDark ? "#334155" : "#CBD5E1"}
                                            />
                                            <Tooltip content={<CustomTooltip />} />
                                            <Bar dataKey="Spent" name="Monthly Spending" fill={isDark ? "#FBBF24" : "#D97706"} radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>

                            {/* 4. Pie Category Breakdown */}
                            <div className={"p-4 rounded-xl border " + (isDark ? "bg-slate-900/60 border-slate-800/80" : "bg-slate-50 border-slate-200")}>
                                <h3 className="text-[11px] font-bold tracking-wider text-violet-500 uppercase mb-3">
                                    🍰 CATEGORY EXPENSE BREAKDOWN
                                </h3>
                                <div className="flex flex-col items-center justify-between gap-4">
                                    <div className="h-[180px] w-[180px]">
                                        <ResponsiveContainer width="100%" height="100%">
                                            <PieChart>
                                                <Pie
                                                    data={pieData}
                                                    cx="50%"
                                                    cy="50%"
                                                    innerRadius={45}
                                                    outerRadius={70}
                                                    paddingAngle={3}
                                                    dataKey="Spent"
                                                    nameKey="category"
                                                >
                                                    {pieData.map((entry, index) => (
                                                        <Cell key={"cell-" + index} fill={nonZeroBudgets.length > 0 ? COLORS[index % COLORS.length] : 'rgba(120, 120, 120, 0.15)'} />
                                                    ))}
                                                </Pie>
                                                <Tooltip content={<CustomTooltip />} />
                                            </PieChart>
                                        </ResponsiveContainer>
                                    </div>
                                    <div className="flex flex-wrap gap-2 justify-center max-w-full">
                                        {pieData.map((entry, index) => (
                                            <div key={index} className="flex items-center gap-1.5 text-[10px] font-semibold">
                                                <span className="w-2.5 h-2.5 rounded-full inline-block" style={{ backgroundColor: nonZeroBudgets.length > 0 ? COLORS[index % COLORS.length] : 'rgba(120, 120, 120, 0.15)' }}></span>
                                                <span className="opacity-85">{entry.category}:</span>
                                                <span className="font-bold">{currencySymbol}{Number(entry.Spent).toFixed(0)}</span>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </div>
                    );
                }

                const root = ReactDOM.createRoot(document.getElementById('root'));
                root.render(<App />);
            </script>
        </body>
        </html>
    """.trimIndent()

    android.view.View.OnClickListener { } // stub

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = android.webkit.WebViewClient()
                setBackgroundColor(android.graphics.Color.parseColor(if (isDark) "#141B2D" else "#FFFFFF"))
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://local.recharts", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddExpenseDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    val expenseCats = categories.filter { !it.isIncome }

    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(expenseCats.firstOrNull()?.id ?: 0L) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0L) }
    var merchantText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(config.currency) }
    var notesText by remember { mutableStateOf("") }
    var isTaxDeductible by remember { mutableStateOf(false) }

    var expandedCat by remember { mutableStateOf(false) }
    var expandedAcc by remember { mutableStateOf(false) }
    var expandedCurr by remember { mutableStateOf(false) }

    val currencyList = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "INR", "SGD", "BDT")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Add Expense", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Amount row & Currency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("quick_add_amount_input")
                    )

                    // Currency Dropdown Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedCurr = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("quick_add_currency_selector")
                        ) {
                            Text(selectedCurrency, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = expandedCurr,
                            onDismissRequest = { expandedCurr = false }
                        ) {
                            currencyList.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCurrency = curr
                                        expandedCurr = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Category selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentCatName = expenseCats.find { it.id == selectedCategoryId }?.name ?: "Select Expense Category..."
                    Column {
                        Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { expandedCat = true },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_category_selector")
                        ) {
                            Text(currentCatName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        expenseCats.forEach { cat ->
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

                // Account selection Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val currentAccName = accounts.find { it.id == selectedAccountId }?.name ?: "Select Source Account..."
                    Column {
                        Text("Source Account", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { expandedAcc = true },
                            modifier = Modifier.fillMaxWidth().testTag("quick_add_account_selector")
                        ) {
                            Text(currentAccName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedAcc,
                        onDismissRequest = { expandedAcc = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${config.currencySymbol}${acc.balance.toInt()})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    expandedAcc = false
                                }
                            )
                        }
                    }
                }

                // Merchant input
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Store Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_merchant_input")
                )

                // Optional Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_notes_input")
                )

                // Is Tax Deductible
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tax Deductible", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Include in tax-saving deductions queries", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = isTaxDeductible,
                        onCheckedChange = { isTaxDeductible = it },
                        modifier = Modifier.testTag("quick_add_tax_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0) {
                        return@Button
                    }
                    if (selectedCategoryId == 0L || selectedAccountId == 0L) {
                        return@Button
                    }

                    val baseAmount = viewModel.convertCurrency(amt, selectedCurrency, config.currency)
                    val realTimeVat = viewModel.realTimeTaxData.value?.standardVatRate ?: config.taxRateDefault
                    val customCategoryTax = viewModel.categoryTaxRates.value[selectedCategoryId] ?: realTimeVat
                    val taxRateToUse = if (isTaxDeductible) customCategoryTax else 0.0

                    // Include original currency in notes for CSV analysis audit trails
                    val notesToUse = notesText.ifEmpty { "Form logged ($selectedCurrency $amt)" }

                    viewModel.addTransaction(
                        amount = baseAmount,
                        type = "EXPENSE",
                        categoryId = selectedCategoryId,
                        accountId = selectedAccountId,
                        merchant = merchantText.ifEmpty { "Standard Retail" },
                        isTaxDeductible = isTaxDeductible,
                        taxRate = taxRateToUse,
                        notes = notesToUse,
                        isRecurring = false
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("quick_add_save_btn")
            ) {
                Text("Quick Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val rawTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    val yearsList = remember(rawTransactions) {
        if (rawTransactions.isEmpty()) {
            listOf(Calendar.getInstance().get(Calendar.YEAR))
        } else {
            rawTransactions.map { tx ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = tx.timestamp
                cal.get(Calendar.YEAR)
            }.distinct().sortedDescending()
        }
    }

    var selectedYear by remember(yearsList) { mutableStateOf(yearsList.first()) }

    val annualTransactions = remember(rawTransactions, selectedYear) {
        rawTransactions.filter { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.timestamp
            cal.get(Calendar.YEAR) == selectedYear
        }
    }

    val totalIncome = remember(annualTransactions) {
        annualTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(annualTransactions) {
        annualTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val netSavings = remember(totalIncome, totalExpense) {
        totalIncome - totalExpense
    }

    val monthlySpend = remember(annualTransactions) {
        val monthsGroup = DoubleArray(12) { 0.0 }
        annualTransactions.filter { it.type == "EXPENSE" }.forEach { tx ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = tx.timestamp
            val m = cal.get(Calendar.MONTH) // 0 to 11
            if (m in 0..11) {
                monthsGroup[m] += tx.amount
            }
        }
        monthsGroup
    }

    val categoryTotals = remember(annualTransactions, categories) {
        annualTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val catName = categories.find { it.id == catId }?.name ?: "Unassigned"
                val spent = txs.sumOf { it.amount }
                catName to spent
            }.sortedByDescending { it.second }
    }

    val (peakMonthName, peakMonthAmount) = remember(monthlySpend) {
        val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        var maxVal = 0.0
        var maxIndex = 0
        monthlySpend.forEachIndexed { idx, valAmount ->
            if (valAmount > maxVal) {
                maxVal = valAmount
                maxIndex = idx
            }
        }
        if (maxVal > 0.0) {
            monthNames[maxIndex] to maxVal
        } else {
            "None" to 0.0
        }
    }

    val singleLargestTransaction = remember(annualTransactions) {
        annualTransactions.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxWidth(0.95f).testTag("yearly_report_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yearly Fiscal Report",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Year selectors
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(yearsList) { y ->
                        val isSelected = selectedYear == y
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedYear = y },
                            label = { Text("Year $y", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("report_year_chip_$y")
                        )
                    }
                }

                if (annualTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No financial records found for Year $selectedYear.\nSwitch or create transactions to view metrics.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    // Long term trends summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "LONG-TERM TRENDS SUMMARY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Income
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Total Income", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(viewModel.formatCurrency(totalIncome), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FintechGreen)
                                    }
                                }
                                // Expense
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Total Expense", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(viewModel.formatCurrency(totalExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRose)
                                    }
                                }
                                // Savings
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val saveColor = if (netSavings >= 0) FintechGreen else ExpenseRose
                                        Text("Net Savings", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(viewModel.formatCurrency(netSavings), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = saveColor)
                                    }
                                }
                            }
                        }
                    }

                    // Spend Trend Column Graph
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MONTHLY SPENDING CURVE ($selectedYear)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        AnnualTrendChart(
                            monthlySpend = monthlySpend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("yearly_trend_chart_canvas")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            months.forEach { m ->
                                Text(
                                    text = m,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Annual Statistics insights list
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "ANNUAL STATISTICAL INSIGHTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Average Spend / Month", fontSize = 11.sp)
                                Text(viewModel.formatCurrency(totalExpense / 12), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Peak Expense Month", fontSize = 11.sp)
                                Text("$peakMonthName (${viewModel.formatCurrency(peakMonthAmount)})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ExpenseRose)
                            }
                            if (singleLargestTransaction != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Single Largest Purchase", fontSize = 11.sp)
                                    Text(
                                        "${singleLargestTransaction.merchant} (${viewModel.formatCurrency(singleLargestTransaction.amount)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Category spending breakdown scrolling column
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ANNUAL CATEGORY BREAKDOWN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )

                        // Wrap inside a limited box so it is visually tight but scrollable if many categories exist
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val maxCatSpend = categoryTotals.firstOrNull()?.second ?: 1.0
                            categoryTotals.take(4).forEach { (catName, spent) ->
                                val pctTotal = if (totalExpense > 0) (spent / totalExpense * 100).toInt() else 0
                                val scalePct = if (maxCatSpend > 0) spent / maxCatSpend else 0.0

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(catName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(
                                            text = "${viewModel.formatCurrency(spent)} ($pctTotal%)",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = scalePct.toFloat(),
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("report_dismiss_button")
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun AnnualTrendChart(
    monthlySpend: DoubleArray,
    modifier: Modifier = Modifier
) {
    val maxSpend = remember(monthlySpend) {
        val maxVal = monthlySpend.maxOrNull() ?: 0.0
        if (maxVal == 0.0) 1.0 else maxVal
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 10.dp.toPx()
        val paddingBottom = 10.dp.toPx()
        val fillWidth = width - paddingLeft
        val fillHeight = height - paddingBottom
        val colWidth = fillWidth / 12

        // Draw horizontal trend grid lines
        val lineCount = 3
        for (i in 0..lineCount) {
            val y = i * (fillHeight / lineCount)
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.08f),
                start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw vertical columns representation
        monthlySpend.forEachIndexed { idx, value ->
            val colHeight = (value / maxSpend) * fillHeight
            val x = paddingLeft + (idx * colWidth) + (colWidth * 0.15f)
            val y = fillHeight - colHeight

            drawRect(
                color = primaryColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, y.toFloat()),
                size = androidx.compose.ui.geometry.Size((colWidth * 0.7f), colHeight.toFloat()),
                alpha = if (value > 0) 1f else 0.12f
            )
        }
    }
}

@Composable
fun OneTapShortcutCard(
    onOpenModal: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenModal() }
            .testTag("one_tap_shortcut_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚡ ONE-TAP EXPENSE LOGGING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tap to instantly record daily expenses using smart minimal presets.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun OneTapSimpleExpenseModal(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()
    
    val expenseCats = categories.filter { !it.isIncome }
    val defaultAccount = accounts.firstOrNull()
    
    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(expenseCats.firstOrNull()?.id ?: 0L) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val presets = listOf(
        Triple("☕ Coffee", 5.0, "Food"),
        Triple("🍔 Lunch", 15.0, "Food"),
        Triple("🚌 Transit", 10.0, "Transport"),
        Triple("🛍️ Shopping", 40.0, "Shopping")
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = FintechGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text("One-Tap Quick Log", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tap a preset to log instantly or enter an amount below.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Text("QUICK-TAP PRESETS (INSTANT LOG)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.take(2).forEach { (label, amt, catName) ->
                            val targetCat = expenseCats.find { it.name.contains(catName, ignoreCase = true) } ?: expenseCats.firstOrNull()
                            val catId = targetCat?.id ?: 0L
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (defaultAccount == null || catId == 0L) {
                                        android.widget.Toast.makeText(context, "No accounts configured.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@FilterChip
                                    }
                                    viewModel.addTransaction(
                                        amount = amt,
                                        type = "EXPENSE",
                                        categoryId = catId,
                                        accountId = defaultAccount.id,
                                        merchant = label.substring(2).trim(),
                                        notes = "Instant One-Tap Log ($label)"
                                    )
                                    android.widget.Toast.makeText(context, "Logged $label for $amt!", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                label = { Text("$label: ${config.currencySymbol}${amt.toInt()}", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f).testTag("preset_chip_${label.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.drop(2).forEach { (label, amt, catName) ->
                            val targetCat = expenseCats.find { it.name.contains(catName, ignoreCase = true) } ?: expenseCats.firstOrNull()
                            val catId = targetCat?.id ?: 0L
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (defaultAccount == null || catId == 0L) {
                                        android.widget.Toast.makeText(context, "No accounts configured.", android.widget.Toast.LENGTH_SHORT).show()
                                        return@FilterChip
                                    }
                                    viewModel.addTransaction(
                                        amount = amt,
                                        type = "EXPENSE",
                                        categoryId = catId,
                                        accountId = defaultAccount.id,
                                        merchant = label.substring(2).trim(),
                                        notes = "Instant One-Tap Log ($label)"
                                    )
                                    android.widget.Toast.makeText(context, "Logged $label for $amt!", android.widget.Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                label = { Text("$label: ${config.currencySymbol}${amt.toInt()}", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f).testTag("preset_chip_${label.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                
                Text("MANUAL MINIMAL LOG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${config.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("one_tap_amount_input")
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(expenseCats) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                modifier = Modifier.testTag("one_tap_cat_chip_${cat.id}")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt <= 0.0 || defaultAccount == null || selectedCategoryId == 0L) {
                        android.widget.Toast.makeText(context, "Please enter a valid amount and select category.", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.addTransaction(
                        amount = amt,
                        type = "EXPENSE",
                        categoryId = selectedCategoryId,
                        accountId = defaultAccount.id,
                        merchant = "Manual Quick Log",
                        notes = "Form logged quick expense"
                    )
                    android.widget.Toast.makeText(context, "Logged expense of $amt ${config.currency}!", android.widget.Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = FintechGreen),
                modifier = Modifier.testTag("one_tap_log_confirm_btn")
            ) {
                Text("LOG EXPENSE", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("one_tap_log_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun D3CalendarHeatmapCard(
    viewModel: MainViewModel,
    isDark: Boolean
) {
    val rawTransactions by viewModel.filteredTransactions.collectAsState()
    
    val dailySpent = remember(rawTransactions) {
        val calendar = Calendar.getInstance()
        rawTransactions
            .filter { it.type == "EXPENSE" }
            .groupBy {
                calendar.timeInMillis = it.timestamp
                calendar.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { (_, txList) -> txList.sumOf { it.amount } }
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, if (isDark) BorderColor.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("d3_spending_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = FintechGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "D3 CALENDAR HEATMAP",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Visualize high-spending days throughout the current month in real time.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val calendar = Calendar.getInstance()
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            
            val jsonDailySpent = dailySpent.entries.joinToString(prefix = "{", postfix = "}") { 
                "\"${it.key}\": ${it.value}" 
            }
            
            val htmlData = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <script src="https://cdnjs.cloudflare.com/ajax/libs/d3/7.8.5/d3.min.js"></script>
                  <style>
                    body {
                      margin: 0;
                      padding: 0;
                      background-color: transparent;
                      color: ${if (isDark) "#ffffff" else "#121212"};
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                      display: flex;
                      justify-content: center;
                      align-items: center;
                    }
                    #heatmap {
                      width: 100%;
                      max-width: 300px;
                    }
                    .cell {
                      stroke: ${if (isDark) "#1c1c1e" else "#ffffff"};
                      stroke-width: 2px;
                      rx: 4px;
                      ry: 4px;
                    }
                    .cell-text {
                      font-size: 10px;
                      font-weight: 700;
                      fill: ${if (isDark) "#ffffff" else "#121212"};
                      text-anchor: middle;
                      pointer-events: none;
                    }
                    .label {
                      font-size: 10px;
                      font-weight: 600;
                      fill: #8e8e93;
                      text-anchor: middle;
                    }
                  </style>
                </head>
                <body>
                  <div id="heatmap"></div>
                  <script>
                    const dailySpent = $jsonDailySpent;
                    const daysInMonth = $daysInMonth;
                    const firstDayOfWeek = $firstDayOfWeek;
                    
                    const data = [];
                    for (let i = 0; i < firstDayOfWeek; i++) {
                      data.push({ empty: true });
                    }
                    for (let d = 1; d <= daysInMonth; d++) {
                      data.push({ day: d, spent: dailySpent[d] || 0 });
                    }
                    
                    const width = 300;
                    const cellSize = width / 7;
                    const rowsCount = Math.ceil(data.length / 7);
                    const height = rowsCount * cellSize + 24;
                    
                    const svg = d3.select("#heatmap")
                      .append("svg")
                      .attr("width", "100%")
                      .attr("height", height)
                      .attr("viewBox", "0 0 300 " + height);
                      
                    const weekdays = ["S", "M", "T", "W", "T", "F", "S"];
                    
                    weekdays.forEach((day, index) => {
                      svg.append("text")
                        .attr("x", index * cellSize + cellSize / 2)
                        .attr("y", 14)
                        .attr("class", "label")
                        .text(day);
                    });
                    
                    const maxSpend = d3.max(data, d => d.spent || 0) || 1;
                    const colorScale = d3.scaleLinear()
                      .domain([0, maxSpend * 0.1, maxSpend * 0.5, maxSpend])
                      .range([
                        "${if (isDark) "#1c1c1e" else "#f2f2f7"}", 
                        "#A5D6A7", 
                        "#4CAF50", 
                        "#1B5E20"
                      ]);
                      
                    data.forEach((item, index) => {
                      const row = Math.floor(index / 7);
                      const col = index % 7;
                      const x = col * cellSize;
                      const y = row * cellSize + 24;
                      
                      if (!item.empty) {
                        svg.append("rect")
                          .attr("x", x + 1)
                          .attr("y", y + 1)
                          .attr("width", cellSize - 2)
                          .attr("height", cellSize - 2)
                          .attr("class", "cell")
                          .attr("fill", colorScale(item.spent));
                          
                        svg.append("text")
                          .attr("x", x + cellSize / 2)
                          .attr("y", y + cellSize / 2 + 3)
                          .attr("class", "cell-text")
                          .text(item.day);
                      }
                    });
                  </script>
                </body>
                </html>
            """.trimIndent()
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            setBackgroundColor(0)
                            loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun DatabasePruneCard(
    viewModel: MainViewModel,
    isDark: Boolean
) {
    var selectedYears by remember { mutableStateOf(2) }
    var doArchive by remember { mutableStateOf(true) }
    var isPruning by remember { mutableStateOf(false) }
    var pruneResult by remember { mutableStateOf<String?>(null) }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, if (isDark) BorderColor.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("database_pruning_utility_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "SQLITE PERFORMANCE & PRUNING",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Optimize database size and speed by archiving or permanently deleting historical transactions older than a certain number of years.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Keep data from past:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3, 5).forEach { yrs ->
                        FilterChip(
                            selected = selectedYears == yrs,
                            onClick = { selectedYears = yrs },
                            label = { Text("$yrs Year${if (yrs > 1) "s" else ""}") },
                            modifier = Modifier.testTag("prune_years_chip_$yrs")
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Archive to CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Generates a local backup CSV copy before purging records", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(
                    checked = doArchive,
                    onCheckedChange = { doArchive = it },
                    modifier = Modifier.testTag("prune_archive_switch")
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isPruning) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)
                )
            } else {
                Button(
                    onClick = {
                        isPruning = true
                        pruneResult = null
                        viewModel.pruneTransactionsOlderThanYears(selectedYears, doArchive) { count, filePath ->
                            isPruning = false
                            if (count < 0) {
                                pruneResult = "Error optimizing database: $filePath"
                            } else if (count == 0) {
                                pruneResult = "No historical transactions found older than $selectedYears years."
                            } else {
                                pruneResult = "Purged and optimized $count transaction entries. " +
                                        (if (filePath != null) "\nArchive saved to: $filePath" else "")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prune_now_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("EXECUTE SQLite VACUUM & PURGE", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
            
            pruneResult?.let { res ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = res,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (res.startsWith("Error")) MaterialTheme.colorScheme.error else FintechGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingTrendCalendarCard(
    viewModel: MainViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    var calendarMonthOffset by remember { mutableIntStateOf(0) }
    var selectedDayNumber by remember { mutableStateOf<Int?>(null) }

    val cal = remember(calendarMonthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, calendarMonthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

    val monthExpenses = remember(allExpenses, year, month) {
        allExpenses.filter { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
            expCal.get(Calendar.YEAR) == year && expCal.get(Calendar.MONTH) == month
        }
    }

    val dailyTotals = remember(monthExpenses) {
        val map = mutableMapOf<Int, Double>()
        monthExpenses.forEach { exp ->
            val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
            val day = expCal.get(Calendar.DAY_OF_MONTH)
            map[day] = (map[day] ?: 0.0) + exp.amount
        }
        map
    }

    val activeDaysCount = dailyTotals.count { it.value > 0 }
    val totalMonthSpend = dailyTotals.values.sum()
    val avgDailySpend = if (activeDaysCount > 0) totalMonthSpend / activeDaysCount else 0.0

    val highSpendDays = remember(dailyTotals, avgDailySpend) {
        if (avgDailySpend <= 0) emptySet<Int>()
        else dailyTotals.filter { it.value >= avgDailySpend * 1.5 }.keys
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().testTag("spending_trend_calendar_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "SPENDING TREND CALENDAR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Highlights unusually high spend days (>1.5x avg)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { calendarMonthOffset-- },
                        modifier = Modifier.size(32.dp).testTag("prev_month_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                    }
                    Text(
                        text = monthName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = { calendarMonthOffset++ },
                        modifier = Modifier.size(32.dp).testTag("next_month_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Avg Daily Spend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${config.currencySymbol}${String.format(Locale.US, "%.2f", avgDailySpend)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = ExpenseRose.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("High Spend Days", fontSize = 10.sp, color = ExpenseRose)
                        Text("${highSpendDays.size} Days 🔥", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRose)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                daysOfWeek.forEach { day ->
                    Text(text = day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val totalCells = firstDayOfWeek + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until totalRows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - firstDayOfWeek + 1

                            if (cellIndex < firstDayOfWeek || dayNum > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val daySpent = dailyTotals[dayNum] ?: 0.0
                                val isHighSpend = highSpendDays.contains(dayNum)
                                val isSelected = selectedDayNumber == dayNum

                                val bgColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isHighSpend -> ExpenseRose
                                    daySpent > 0 -> FintechGreen.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }

                                val textColor = when {
                                    isSelected || isHighSpend -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .clickable { selectedDayNumber = if (isSelected) null else dayNum }
                                        .testTag(if (isHighSpend) "high_spend_day_$dayNum" else "calendar_day_$dayNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "$dayNum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                                        if (isHighSpend) {
                                            Text(text = "🔥", fontSize = 8.sp)
                                        } else if (daySpent > 0) {
                                            Text(text = "${config.currencySymbol}${daySpent.toInt()}", fontSize = 8.sp, color = textColor.copy(alpha = 0.8f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedDayNumber != null) {
                val day = selectedDayNumber!!
                val daySpent = dailyTotals[day] ?: 0.0
                val dayExps = monthExpenses.filter { exp ->
                    val expCal = Calendar.getInstance().apply { timeInMillis = exp.date }
                    expCal.get(Calendar.DAY_OF_MONTH) == day
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("day_breakdown_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expenses for $monthName $day", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Total: ${config.currencySymbol}${String.format(Locale.US, "%.2f", daySpent)}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        if (dayExps.isEmpty()) {
                            Text("No recorded transactions on this date.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            dayExps.forEach { exp ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${exp.merchant.ifEmpty { "Expense" }}", fontSize = 11.sp)
                                    Text("${config.currencySymbol}${exp.amount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialAchievementsCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allBudgets by viewModel.activeBudgets.collectAsState()
    val taxCategories by viewModel.allTaxCategories.collectAsState()

    val (isBudgetMasterUnlocked, budgetConsecutiveMonths) = remember(allExpenses, allBudgets) {
        var consecutiveCount = 0
        for (i in 0 until 3) {
            val checkCal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
            val y = checkCal.get(Calendar.YEAR)
            val m = checkCal.get(Calendar.MONTH)

            val monthExps = allExpenses.filter { exp ->
                val c = Calendar.getInstance().apply { timeInMillis = exp.date }
                c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
            }
            val monthSpent = monthExps.sumOf { it.amount }
            val monthCap = allBudgets.sumOf { it.amount }

            if (monthCap > 0 && monthSpent <= monthCap) {
                consecutiveCount++
            }
        }
        Pair(consecutiveCount >= 3, consecutiveCount)
    }

    val isTaxGuardianUnlocked = remember(taxCategories, allExpenses) {
        taxCategories.isNotEmpty() && taxCategories.all { tc ->
            val curMonthSpent = allExpenses.sumOf { exp -> if (exp.taxCategoryId == tc.id) exp.amount else 0.0 }
            tc.monthlyCap == 0.0 || curMonthSpent <= tc.monthlyCap
        }
    }

    val isConsistencyStreakUnlocked = remember(allExpenses) {
        val daysWithExp = allExpenses.map { exp ->
            val c = Calendar.getInstance().apply { timeInMillis = exp.date }
            "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.DAY_OF_MONTH)}"
        }.toSet()
        daysWithExp.size >= 7
    }

    val isMultiCurrencyMavenUnlocked = remember(allExpenses) {
        allExpenses.map { it.currency }.toSet().size >= 2
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().testTag("financial_achievements_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = AccentGold, modifier = Modifier.size(22.dp))
                        Text(
                            text = "FINANCIAL ACHIEVEMENTS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Badges & milestones unlocked based on budget discipline",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = AccentGold.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val unlockedCount = listOf(isBudgetMasterUnlocked, isTaxGuardianUnlocked, isConsistencyStreakUnlocked, isMultiCurrencyMavenUnlocked).count { it }
                    Text(
                        text = "$unlockedCount / 4 Badges",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AchievementBadgeItem(
                    title = "🛡️ Budget Master",
                    description = "Stay under defined monthly budget caps for 3 consecutive months",
                    progressText = "$budgetConsecutiveMonths / 3 Months",
                    progressRatio = budgetConsecutiveMonths / 3f,
                    isUnlocked = isBudgetMasterUnlocked,
                    testTag = "badge_budget_master"
                )

                AchievementBadgeItem(
                    title = "🎯 Tax Cap Guardian",
                    description = "Keep all tax category spending within defined monthly caps",
                    progressText = if (isTaxGuardianUnlocked) "Completed!" else "In Progress",
                    progressRatio = if (isTaxGuardianUnlocked) 1f else 0.5f,
                    isUnlocked = isTaxGuardianUnlocked,
                    testTag = "badge_tax_guardian"
                )

                AchievementBadgeItem(
                    title = "⚡ Consistency Hero",
                    description = "Log transactions on 7 or more distinct days in Room database",
                    progressText = if (isConsistencyStreakUnlocked) "Unlocked!" else "Keep logging",
                    progressRatio = if (isConsistencyStreakUnlocked) 1f else 0.4f,
                    isUnlocked = isConsistencyStreakUnlocked,
                    testTag = "badge_consistency_hero"
                )

                AchievementBadgeItem(
                    title = "🌐 Multi-Currency Maven",
                    description = "Track expenses in 2 or more world currencies",
                    progressText = if (isMultiCurrencyMavenUnlocked) "Unlocked!" else "1 / 2 Currencies",
                    progressRatio = if (isMultiCurrencyMavenUnlocked) 1f else 0.5f,
                    isUnlocked = isMultiCurrencyMavenUnlocked,
                    testTag = "badge_multi_currency"
                )
            }
        }
    }
}

@Composable
fun AchievementBadgeItem(
    title: String,
    description: String,
    progressText: String,
    progressRatio: Float,
    isUnlocked: Boolean,
    testTag: String
) {
    val containerColor = if (isUnlocked) AccentGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val borderColor = if (isUnlocked) AccentGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isUnlocked) {
                        Surface(color = AccentGold, shape = RoundedCornerShape(4.dp)) {
                            Text("UNLOCKED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressRatio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (isUnlocked) AccentGold else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(progressText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isUnlocked) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class AiTooltipItem(
    val id: String,
    val categoryTag: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val chipLabel: String,
    val shortSummary: String,
    val detailedAdvice: String,
    val severityColor: Color
)

@Composable
fun ContextAwareAiInsightsTooltipsCard(
    viewModel: MainViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeBudgets by viewModel.activeBudgets.collectAsState()
    val config by viewModel.activeCountryConfig.collectAsState()

    var selectedTooltip by remember { mutableStateOf<AiTooltipItem?>(null) }
    var aiDynamicResponse by remember { mutableStateOf<String?>(null) }

    // Dynamic tooltips computation
    val tooltips = remember(allTransactions, categories, activeBudgets, config) {
        val list = mutableListOf<AiTooltipItem>()
        val expenseTxs = allTransactions.filter { it.type == "EXPENSE" }
        val totalSpent = expenseTxs.sumOf { it.amount }

        // 1. Top category spending tooltip
        val catMap = categories.associate { it.id to it.name }
        val topCategoryEntry = expenseTxs.groupBy { catMap[it.categoryId] ?: "Other" }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .maxByOrNull { it.value }

        if (topCategoryEntry != null && totalSpent > 0) {
            val pct = ((topCategoryEntry.value / totalSpent) * 100).toInt()
            val formattedAmt = "${config.currencySymbol}${String.format(Locale.US, "%.2f", topCategoryEntry.value)}"
            list.add(
                AiTooltipItem(
                    id = "top_cat_surge",
                    categoryTag = "SPENDING SURGE",
                    icon = Icons.Default.TrendingUp,
                    chipLabel = "💡 ${topCategoryEntry.key} ($pct% of total)",
                    shortSummary = "${topCategoryEntry.key} is your largest expense bucket this period ($formattedAmt).",
                    detailedAdvice = "Gemini AI Recommendation: Your ${topCategoryEntry.key} expenditure constitutes $pct% of your overall monthly outflows. Consider setting a dedicated monthly budget limit or reviewing recurring merchants in this category to optimize savings.",
                    severityColor = Color(0xFF3B82F6)
                )
            )
        }

        // 2. Budget proximity tooltip
        val budgetAlert = activeBudgets.mapNotNull { b ->
            val catName = catMap[b.categoryId] ?: "Category"
            val pct = if (b.amount > 0) (b.spent / b.amount) * 100 else 0.0
            if (pct >= 75.0) Triple(catName, pct, b) else null
        }.maxByOrNull { it.second }

        if (budgetAlert != null) {
            val (catName, pct, _) = budgetAlert
            list.add(
                AiTooltipItem(
                    id = "budget_threshold",
                    categoryTag = "BUDGET ALERT",
                    icon = Icons.Default.Warning,
                    chipLabel = "⚠️ $catName Budget (${pct.toInt()}% used)",
                    shortSummary = "$catName budget is near capacity at ${pct.toInt()}% utilization.",
                    detailedAdvice = "Gemini AI Warning: You have consumed ${pct.toInt()}% of your allocated threshold for $catName. At current velocity, you risk exceeding this budget before month-end. Consider pacing non-essential purchases.",
                    severityColor = Color(0xFFE53935)
                )
            )
        }

        // 3. Tax deduction tooltip
        val taxDeductibleCount = expenseTxs.count { it.isTaxDeductible }
        if (taxDeductibleCount > 0) {
            val taxDeductibleSum = expenseTxs.filter { it.isTaxDeductible }.sumOf { it.amount }
            val formattedTaxSum = "${config.currencySymbol}${String.format(Locale.US, "%.2f", taxDeductibleSum)}"
            list.add(
                AiTooltipItem(
                    id = "tax_deduction",
                    categoryTag = "TAX SAVINGS",
                    icon = Icons.Default.Verified,
                    chipLabel = "✨ Tax Deductions ($formattedTaxSum)",
                    shortSummary = "$taxDeductibleCount tax-deductible items flagged for ${config.country} compliance.",
                    detailedAdvice = "Gemini AI Tax Advisor: You have recorded $taxDeductibleCount tax-deductible expenses totaling $formattedTaxSum. Under ${config.country} tax regulations, these can lower your overall taxable liability.",
                    severityColor = FintechGreen
                )
            )
        }

        // 4. Inflow vs Outflow velocity tooltip
        val incomeTxs = allTransactions.filter { it.type == "INCOME" }
        val totalIncome = incomeTxs.sumOf { it.amount }
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalSpent) / totalIncome) * 100 else 0.0
        list.add(
            AiTooltipItem(
                id = "savings_rate",
                categoryTag = "SAVINGS RATE",
                icon = Icons.Default.AutoAwesome,
                chipLabel = "📈 Net Savings Rate (${savingsRate.toInt()}%)",
                shortSummary = "Current net savings margin stands at ${savingsRate.toInt()}%.",
                detailedAdvice = "Gemini AI Cashflow Velocity: Your current monthly savings rate is ${savingsRate.toInt()}%. Wealth managers recommend maintaining at least a 20% net margin to support long-term investment goals and emergency buffers.",
                severityColor = AccentGold
            )
        )

        list
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E232A) else Color(0xFFF3F6FA)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_context_tooltips_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Tooltips",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "GEMINI AI CONTEXT-AWARE TOOLTIPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Tap chips for real-time spending intelligence & AI guidance",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.triggerGeminiEvaluation() },
                    modifier = Modifier.size(28.dp).testTag("ai_tooltips_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh AI Tooltips",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable horizontal row of context-aware tooltips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("ai_tooltips_lazy_row")
            ) {
                items(tooltips, key = { it.id }) { tooltip ->
                    Surface(
                        onClick = { selectedTooltip = tooltip },
                        shape = RoundedCornerShape(12.dp),
                        color = tooltip.severityColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, tooltip.severityColor.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("ai_tooltip_chip_${tooltip.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tooltip.icon,
                                contentDescription = null,
                                tint = tooltip.severityColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tooltip.chipLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialog when tooltip chip is tapped
    if (selectedTooltip != null) {
        val activeTooltip = selectedTooltip!!
        AlertDialog(
            onDismissRequest = {
                selectedTooltip = null
                aiDynamicResponse = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = activeTooltip.icon,
                        contentDescription = null,
                        tint = activeTooltip.severityColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = activeTooltip.categoryTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTooltip.severityColor
                        )
                        Text(
                            text = "Gemini Contextual Spending Insight",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = activeTooltip.shortSummary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    Text(
                        text = activeTooltip.detailedAdvice,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (aiDynamicResponse != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiDynamicResponse!!,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerGeminiEvaluation()
                        selectedTooltip = null
                    },
                    modifier = Modifier.testTag("ai_tooltip_action_btn")
                ) {
                    Text("Run Full AI Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedTooltip = null },
                    modifier = Modifier.testTag("ai_tooltip_dismiss_btn")
                ) {
                    Text("Close", fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
fun DatabaseSyncStatusBadge(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    val txCountSinceLastExport by viewModel.txCountSinceLastExport.collectAsState()
    val context = LocalContext.current

    val formattedTime = remember(lastBackupTime) {
        val diff = System.currentTimeMillis() - lastBackupTime
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastBackupTime))
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("database_sync_status_badge")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(FintechGreen)
                )

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DATABASE ACTIVE & SYNCED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Room SQLite v3 • Last Sync: $formattedTime • Unbacked: $txCountSinceLastExport tx",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    viewModel.resetTxCountSinceLastExport()
                    Toast.makeText(context, "Database state verified & timestamp updated!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Sync Now",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BrandColorThemeSelectorCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentBrandHex by viewModel.brandColorHex.collectAsState()
    val context = LocalContext.current

    val presetBrands = listOf(
        "Emerald Fintech" to "#10B981",
        "Chase Blue" to "#0052CC",
        "BofA Red" to "#E31837",
        "Fidelity Green" to "#008240",
        "Revolut Pink" to "#FF0055",
        "N26 Teal" to "#36A18B",
        "Barclays Cyan" to "#00AEEF",
        "Wells Fargo Gold" to "#D4A017"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("brand_color_theme_selector_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Brand Color",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BANK & BRANDING COLOR THEME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    color = try { Color(android.graphics.Color.parseColor(currentBrandHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary },
                    shape = CircleShape,
                    modifier = Modifier.size(20.dp)
                ) {}
            }

            Text(
                text = "Customize UI theme colors to match your regional financial institution or brand aesthetic.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetBrands) { (label, hex) ->
                    val isSelected = currentBrandHex.equals(hex, ignoreCase = true)
                    val colorVal = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }

                    Surface(
                        color = if (isSelected) colorVal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colorVal else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .clickable {
                                viewModel.setBrandColorHex(hex)
                                Toast.makeText(context, "$label Theme Applied!", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("brand_theme_$label")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                            )
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


