package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // mandatory modern full-bleed edge-to-edge support

        setContent {
            MyApplicationTheme {
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

data class NavigationTabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTagId: String
)
