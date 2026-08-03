package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isUnlocked by mutableStateOf(false)

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("WealthFlow Safe Lock")
            .setSubtitle("Authenticate using your credentials to open data safe")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // mandatory modern full-bleed edge-to-edge support

        // Schedule background workers for recurring expenses, budget checks, exchange rates, and daily database archival
        try {
            com.example.data.worker.RecurringExpenseWorker.scheduleWorker(this)
            com.example.data.worker.BudgetCheckWorker.scheduleWorker(this)
            com.example.data.worker.DatabaseArchiveWorker.schedulePeriodic(this)
            com.example.data.worker.ExchangeRateWorker.schedulePeriodic(this)

            // Dynamic App Shortcut for long-press main app icon
            val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(this, "quick_add_expense")
                .setShortLabel("Add Expense")
                .setLongLabel("Quick Add Expense")
                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(this, R.drawable.ic_launcher_foreground))
                .setIntent(android.content.Intent(this, MainActivity::class.java).apply {
                    action = "com.example.ACTION_QUICK_ADD_EXPENSE"
                })
                .build()
            androidx.core.content.pm.ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val brandColorHex by viewModel.brandColorHex.collectAsState()
            val systemInDark = androidx.compose.foundation.isSystemInDarkTheme()

            androidx.compose.runtime.LaunchedEffect(systemInDark) {
                com.example.ui.theme.ThemeManager.updateSystemThemePreference(systemInDark)
            }

            MyApplicationTheme(
                darkTheme = com.example.ui.theme.ThemeManager.isDarkThemeState,
                dynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                customPrimaryHex = brandColorHex
            ) {
                val authPrefs = remember { getSharedPreferences("finance_tracker_auth_prefs", MODE_PRIVATE) }
                var isLoggedIn by remember { mutableStateOf(authPrefs.getBoolean("is_logged_in", false)) }

                if (!isLoggedIn) {
                    AuthScreen(
                        onAuthSuccess = {
                            isLoggedIn = true
                        }
                    )
                } else if (!isUnlocked) {
                    val sharedPrefs = remember { getSharedPreferences("wealthflow_security_prefs", MODE_PRIVATE) }
                    var savedPin by remember { mutableStateOf(sharedPrefs.getString("secure_pin", "") ?: "") }
                    var pinFlowState by remember { mutableStateOf(if (savedPin.isEmpty()) "CREATE" else "ENTER") }
                    var pinInput by remember { mutableStateOf("") }
                    var tempFirstPin by remember { mutableStateOf("") }
                    var errorMessage by remember { mutableStateOf("") }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .testTag("biometric_security_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (pinFlowState == "CREATE" || pinFlowState == "CONFIRM") Icons.Default.LockOpen else Icons.Default.Lock,
                                    contentDescription = "Safe Lock",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    text = if (pinFlowState == "CREATE") "SET PRIVACY PIN" else if (pinFlowState == "CONFIRM") "CONFIRM PRIVACY PIN" else "WEALTHFLOW SAFE LOCK",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (pinFlowState == "CREATE") {
                                        "Configure a local 4-digit security PIN to restrict startup access on this device."
                                    } else if (pinFlowState == "CONFIRM") {
                                        "Retype the 4-digit code to initialize encryption shield."
                                    } else {
                                        "Enter your security keys or tap fingerprint to initialize local ledger."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                if (errorMessage.isNotEmpty()) {
                                    Text(
                                        text = errorMessage,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Visual PIN slots representation dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0..3) {
                                        val isFilled = pinInput.length > i
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3x4 Custom Lockpad Keypad
                                val keys = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("FP", "0", "DEL")
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    keys.forEach { rowKeys ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            rowKeys.forEach { key ->
                                                if (key == "FP") {
                                                    IconButton(
                                                        onClick = { showBiometricPrompt() },
                                                        modifier = Modifier
                                                            .size(60.dp)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Fingerprint,
                                                            contentDescription = "Fingerprint sensor",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(28.dp)
                                                        )
                                                    }
                                                } else if (key == "DEL") {
                                                    IconButton(
                                                        onClick = {
                                                            if (pinInput.isNotEmpty()) {
                                                                pinInput = pinInput.dropLast(1)
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .size(60.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp))
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                            contentDescription = "Delete key",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            if (pinInput.length < 4) {
                                                                pinInput += key
                                                                errorMessage = ""
                                                            }
                                                            if (pinInput.length == 4) {
                                                                when (pinFlowState) {
                                                                    "CREATE" -> {
                                                                        tempFirstPin = pinInput
                                                                        pinInput = ""
                                                                        pinFlowState = "CONFIRM"
                                                                    }
                                                                    "CONFIRM" -> {
                                                                        if (pinInput == tempFirstPin) {
                                                                            sharedPrefs.edit().putString("secure_pin", pinInput).apply()
                                                                            savedPin = pinInput
                                                                            isUnlocked = true
                                                                        } else {
                                                                            errorMessage = "PINs do not match. Try creating again."
                                                                            pinFlowState = "CREATE"
                                                                            pinInput = ""
                                                                            tempFirstPin = ""
                                                                        }
                                                                    }
                                                                    "ENTER" -> {
                                                                        if (pinInput == savedPin) {
                                                                            isUnlocked = true
                                                                        } else {
                                                                            errorMessage = "Incorrect PIN code. Please retry."
                                                                            pinInput = ""
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        modifier = Modifier.size(60.dp),
                                                        shape = RoundedCornerShape(30.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        ),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text(
                                                            text = key,
                                                            fontSize = 20.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        errorMessage = "Biometrics bypassed. Enter your 4-digit PIN code."
                                    },
                                    modifier = Modifier.testTag("pin_code_fallback_btn")
                                ) {
                                    Text("🔒 Biometrics Unavailable? Enter Security PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    LaunchedEffect(Unit) {
                        if (pinFlowState == "ENTER") {
                            showBiometricPrompt()
                        }
                    }
                } else {
                    var activeTabIndex by remember { mutableStateOf(0) }
                    var showRapidEntrySheet by remember { mutableStateOf(false) }
                    var showDataManagementSheet by remember { mutableStateOf(false) }
                    val countryConfig by viewModel.activeCountryConfig.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

                    LaunchedEffect(Unit) {
                        viewModel.checkRateUpdatePrompt()
                        if (intent?.action == "com.example.ACTION_QUICK_ADD_EXPENSE") {
                            showRapidEntrySheet = true
                        }
                        
                        launch {
                            viewModel.notifications.collect { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        }

                        launch {
                            viewModel.uiEvents.collect { event ->
                                when (event) {
                                    is UiEvent.ExpenseSubmitted -> {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    }
                                    is UiEvent.SavingsGoalReached -> {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        try {
                                            kotlinx.coroutines.delay(150)
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            kotlinx.coroutines.delay(150)
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        } catch (e: Exception) {}
                                    }
                                    is UiEvent.BudgetAlertTriggered -> {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        try {
                                            kotlinx.coroutines.delay(200)
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    showRapidEntrySheet = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(18.dp),
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 12.dp),
                                modifier = Modifier.testTag("floating_rapid_entry_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Rapid Expense Entry",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        },
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier
                                    .testTag("app_bottom_nav_bar")
                                    .windowInsetsPadding(WindowInsets.navigationBars), // prevent cutoffs on gesture/3-button views
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 12.dp
                            ) {
                                val tabs = listOf(
                                    NavigationTabItem("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
                                    NavigationTabItem("Ledger", Icons.AutoMirrored.Filled.ListAlt, "tab_ledger"),
                                    NavigationTabItem("Budgets", Icons.Default.PieChart, "tab_budgets"),
                                    NavigationTabItem("Trends", Icons.Default.ShowChart, "tab_trends"),
                                    NavigationTabItem("Tax Board", Icons.AutoMirrored.Filled.ReceiptLong, "tab_tax"),
                                    NavigationTabItem("Regions", Icons.Default.Language, "tab_countries"),
                                    NavigationTabItem("AI Assist", Icons.Default.AutoAwesome, "tab_ai")
                                )

                                tabs.forEachIndexed { idx, tab ->
                                    NavigationBarItem(
                                        selected = activeTabIndex == idx,
                                        onClick = {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            activeTabIndex = idx
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.label,
                                                modifier = Modifier.size(if (activeTabIndex == idx) 22.dp else 20.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                fontSize = 9.sp,
                                                fontWeight = if (activeTabIndex == idx) FontWeight.ExtraBold else FontWeight.Medium,
                                                maxLines = 1
                                            )
                                        },
                                        modifier = Modifier.testTag(tab.testTagId),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        },
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            CenterAlignedTopAppBar(
                                title = {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                                            )
                                            Text(
                                                text = "WEALTHFLOW",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.titleMedium,
                                                letterSpacing = 2.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "  ${countryConfig.country.uppercase()} PORTFOLIO (${countryConfig.currency})  ",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.8.sp,
                                                modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp)
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { showDataManagementSheet = true },
                                        modifier = Modifier.testTag("action_data_management")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = "Data Management Settings",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.exportReportToCsv(this@MainActivity) },
                                        modifier = Modifier.testTag("action_export_csv")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export CSV Report",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setDarkTheme(!isDarkTheme) },
                                        modifier = Modifier.testTag("action_toggle_theme")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            authPrefs.edit().putBoolean("is_logged_in", false).apply()
                                            isLoggedIn = false
                                        },
                                        modifier = Modifier.testTag("action_logout")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Sign Out",
                                            tint = com.example.ui.theme.ExpenseRose
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.testTag("app_top_bar")
                            )
                        },
                        contentWindowInsets = WindowInsets.safeDrawing // manage top/bottom safe notch/system layouts perfectly
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            // Staggered transitions on tab change for sleek high-fidelity motion feeling
                            AnimatedContent(
                                targetState = activeTabIndex,
                                transitionSpec = {
                                    slideInHorizontally { width -> width / 4 } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -width / 4 } + fadeOut()
                                },
                                label = "tab_switch_animation"
                            ) { index ->
                                when (index) {
                                    0 -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToTransactions = { activeTabIndex = 1 }
                                    )
                                    1 -> TransactionScreen(
                                        viewModel = viewModel
                                    )
                                    2 -> BudgetScreen(
                                        viewModel = viewModel
                                    )
                                    3 -> SavingsTrendScreen(
                                        viewModel = viewModel
                                    )
                                    4 -> TaxScreen(
                                        viewModel = viewModel
                                    )
                                    5 -> CountryScreen(
                                        viewModel = viewModel
                                    )
                                    6 -> AiInsightsScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }

                            if (showRapidEntrySheet) {
                                @OptIn(ExperimentalMaterial3Api::class)
                                ModalBottomSheet(
                                    onDismissRequest = { showRapidEntrySheet = false },
                                    modifier = Modifier.testTag("rapid_entry_modal_sheet")
                                ) {
                                    RapidEntryScreen(
                                        viewModel = viewModel,
                                        onDismiss = { showRapidEntrySheet = false }
                                    )
                                }
                            }

                            if (showDataManagementSheet) {
                                @OptIn(ExperimentalMaterial3Api::class)
                                ModalBottomSheet(
                                    onDismissRequest = { showDataManagementSheet = false },
                                    modifier = Modifier.testTag("data_management_modal_sheet")
                                ) {
                                    DataManagementScreen(
                                        viewModel = viewModel
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

data class NavigationTabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTagId: String
)
