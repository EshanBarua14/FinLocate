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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // mandatory modern full-bleed edge-to-edge support

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                if (!isUnlocked) {
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
                                .padding(16.dp)
                                .testTag("biometric_security_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Safe Lock",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "WEALTHFLOW SECURITY",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "This finance application is locked for privacy-first user protection.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showBiometricPrompt() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("authenticate_biometric_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Fingerprint")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("UNLOCK PORTFOLIO", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                                
                                TextButton(
                                    onClick = { isUnlocked = true },
                                    modifier = Modifier
                                        .testTag("bypass_biometric_btn")
                                ) {
                                    Text("Bypass (Testing & Emulators)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                    LaunchedEffect(Unit) {
                        showBiometricPrompt()
                    }
                } else {
                    var activeTabIndex by remember { mutableStateOf(0) }
                    val countryConfig by viewModel.activeCountryConfig.collectAsState()

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier
                                    .testTag("app_bottom_nav_bar")
                                    .windowInsetsPadding(WindowInsets.navigationBars), // prevent cutoffs on gesture/3-button views
                                tonalElevation = 8.dp
                            ) {
                                val tabs = listOf(
                                    NavigationTabItem("Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
                                    NavigationTabItem("Ledger", Icons.Default.ListAlt, "tab_ledger"),
                                    NavigationTabItem("Budgets", Icons.Default.PieChart, "tab_budgets"),
                                    NavigationTabItem("Tax Board", Icons.Default.ReceiptLong, "tab_tax"),
                                    NavigationTabItem("Regions", Icons.Default.Language, "tab_countries"),
                                    NavigationTabItem("AI Assist", Icons.Default.AutoAwesome, "tab_ai")
                                )

                                tabs.forEachIndexed { idx, tab ->
                                    NavigationBarItem(
                                        selected = activeTabIndex == idx,
                                        onClick = { activeTabIndex = idx },
                                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                                        modifier = Modifier.testTag(tab.testTagId),
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.background,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary
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
                                        Text(
                                            text = "WEALTHFLOW",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleMedium,
                                            letterSpacing = 1.6.sp
                                        )
                                        Text(
                                            text = "${countryConfig.country.uppercase()} PORTFOLIO (${countryConfig.currency})",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { viewModel.exportReportToCsv(this@MainActivity) },
                                        modifier = Modifier.testTag("action_export_csv")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export CSV Report"
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setDarkTheme(!isDarkTheme) },
                                        modifier = Modifier.testTag("action_toggle_theme")
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = "Toggle Theme"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .testTag("app_top_bar")
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
                                    3 -> TaxScreen(
                                        viewModel = viewModel
                                    )
                                    4 -> CountryScreen(
                                        viewModel = viewModel
                                    )
                                    5 -> AiInsightsScreen(
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
