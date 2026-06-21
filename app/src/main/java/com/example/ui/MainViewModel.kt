package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.data.repository.CountryConfigProviderService
import com.example.data.service.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NetWorthSummary(
    val manualNetInLocalCurrency: Double = 0.0,
    val syncedNetInLocalCurrency: Double = 0.0,
    val totalNetWorth: Double = 0.0
)

data class BudgetProjectionAnalysis(
    val dailyAverage: Double = 0.0,
    val projectedTotalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    val isProjectedToExceed: Boolean = false,
    val daysPassed: Int = 1,
    val totalDays: Int = 30,
    val currentSpent: Double = 0.0
)

data class PredictiveInsight(
    val categoryId: Long,
    val categoryName: String,
    val predictedSpend: Double,
    val currentLimit: Double,
    val suggestedLimit: Double,
    val recommendation: String,
    val trendType: String // "REDUCE", "INCREASE", "STABLE"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(db)
    private val countryConfigProvider = CountryConfigProviderService(application)

    // --- SharedPreferences for local settings ---
    private val prefs = application.getSharedPreferences("wealthflow_prefs", Application.MODE_PRIVATE)
    private val _applyLocalTax = MutableStateFlow(prefs.getBoolean("apply_local_tax", true))
    val applyLocalTax: StateFlow<Boolean> = _applyLocalTax.asStateFlow()

    fun setApplyLocalTax(value: Boolean) {
        _applyLocalTax.value = value
        prefs.edit().putBoolean("apply_local_tax", value).apply()
    }

    // --- Global Theme Toggle ---
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(value: Boolean) {
        _isDarkTheme.value = value
        prefs.edit().putBoolean("is_dark_theme", value).apply()
    }

    // --- Interactive Notifications Flow ---
    private val _notifications = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val notifications = _notifications.asSharedFlow()

    fun postNotification(msg: String) {
        _notifications.tryEmit(msg)
    }

    // --- Exchange Rate Fetch Prompt Engine ---
    private val _showRatePrompt = MutableStateFlow(false)
    val showRatePrompt: StateFlow<Boolean> = _showRatePrompt.asStateFlow()

    fun dismissRatePrompt() {
        prefs.edit().putLong("last_rate_prompt", System.currentTimeMillis()).apply()
        _showRatePrompt.value = false
    }

    fun checkRateUpdatePrompt() {
        val lastPrompt = prefs.getLong("last_rate_prompt", 0L)
        val now = System.currentTimeMillis()
        // If never prompted, or if 1 hour has elapsed (3600000ms), show prompt
        if (now - lastPrompt > 3600000L) {
            _showRatePrompt.value = true
        }
    }

    fun triggerExchangeRatesFetch(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val updated = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val conn = java.net.URL("https://open.er-api.com/v6/latest/USD").openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val regex = "\"([A-Z]{3})\":\\s*([0-9\\.]+)".toRegex()
                        val matches = regex.findAll(text)
                        val newRates = matches.associate {
                            it.groupValues[1] to (it.groupValues[2].toDoubleOrNull() ?: 1.0)
                        }
                        if (newRates.isNotEmpty() && newRates.containsKey("USD")) {
                            val merged = newRates.toMutableMap()
                            _userCustomExchangeRates.value.forEach { (k, v) ->
                                merged[k] = v
                            }
                            _exchangeRates.value = merged
                            true
                        } else false
                    } else false
                }
                if (updated) {
                    prefs.edit().putLong("last_rate_prompt", System.currentTimeMillis()).apply()
                    _showRatePrompt.value = false
                    onSuccess("Rates updated successfully online!")
                } else {
                    onFailure("Could not process online rates. Using offline fallback.")
                }
            } catch (e: Exception) {
                onFailure("Network error: ${e.localizedMessage ?: "timeout"}")
            }
        }
    }

    // --- Exchange Rate Engine ---
    private val _userCustomExchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val userCustomExchangeRates: StateFlow<Map<String, Double>> = _userCustomExchangeRates.asStateFlow()

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(
        mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "JPY" to 157.5,
            "CAD" to 1.37,
            "AUD" to 1.51,
            "INR" to 83.5,
            "SGD" to 1.34,
            "BDT" to 117.5
        )
    )
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    fun updateExchangeRate(currency: String, rate: Double) {
        val currentCustom = _userCustomExchangeRates.value.toMutableMap()
        currentCustom[currency.uppercase()] = rate
        _userCustomExchangeRates.value = currentCustom
        prefs.edit().putString("custom_exchange_rates", currentCustom.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()

        val currentAll = _exchangeRates.value.toMutableMap()
        currentAll[currency.uppercase()] = rate
        _exchangeRates.value = currentAll
    }

    // --- Category-Specific Tax Rates Panel ---
    private val _categoryTaxRates = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val categoryTaxRates: StateFlow<Map<Long, Double>> = _categoryTaxRates.asStateFlow()

    fun setTaxRateForCategory(categoryId: Long, rate: Double) {
        val current = _categoryTaxRates.value.toMutableMap()
        current[categoryId] = rate
        _categoryTaxRates.value = current
        prefs.edit().putString("cat_tax_rates", current.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
    }

    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.uppercase() == toCurrency.uppercase()) return amount
        val rates = exchangeRates.value
        val fromRateInUsd = rates[fromCurrency.uppercase()] ?: return amount
        val toRateInUsd = rates[toCurrency.uppercase()] ?: return amount
        val amountInUsd = amount / fromRateInUsd
        return amountInUsd * toRateInUsd
    }

    // --- Country State ---
    val activeCountrySetting: StateFlow<CountrySettingEntity> = repository.activeCountrySetting
        .map { it ?: CountrySettingEntity(selectedCountry = "USA") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CountrySettingEntity(selectedCountry = "USA"))

    val activeCountryConfig: StateFlow<CountryConfig> = activeCountrySetting
        .map { countryConfigProvider.loadConfigForCountry(it.selectedCountry) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CountryConfig.USA)

    // --- Core Flows ---
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isExportEncryptionEnabled = MutableStateFlow(false)
    val exportPasscode = MutableStateFlow("SecurePass2026")

    fun toggleExportEncryption() {
        isExportEncryptionEnabled.value = !isExportEncryptionEnabled.value
    }

    fun setExportPasscode(code: String) {
        exportPasscode.value = code
    }

    val detectedAnomalies: StateFlow<List<AnomalyReport>> = combine(allTransactions, accounts) { txs, accs ->
        AnomalyDetectionService.analyzeTransactions(txs, accs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartInsights: StateFlow<List<InsightEntity>> = repository.allInsights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Monthly Filtering ---
    private val _selectedMonth = MutableStateFlow("") // format YYYY-MM
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    init {
        // Set to current month by default
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        _selectedMonth.value = sdf.format(Date())

        // Load saved custom values from Shared Preferences
        val savedCustomRatesStr = prefs.getString("custom_exchange_rates", "") ?: ""
        if (savedCustomRatesStr.isNotEmpty()) {
            try {
                val map = savedCustomRatesStr.split(",").associate {
                    val parts = it.split(":")
                    parts[0] to parts[1].toDouble()
                }
                _userCustomExchangeRates.value = map
                // Apply on top of seeded exchange rates
                val currentAll = _exchangeRates.value.toMutableMap()
                map.forEach { (k, v) -> currentAll[k] = v }
                _exchangeRates.value = currentAll
            } catch (e: Exception) {
                // ignore
            }
        }

        val savedTaxRatesStr = prefs.getString("cat_tax_rates", "") ?: ""
        if (savedTaxRatesStr.isNotEmpty()) {
            try {
                val map = savedTaxRatesStr.split(",").associate {
                    val parts = it.split(":")
                    parts[0].toLong() to parts[1].toDouble()
                }
                _categoryTaxRates.value = map
            } catch (e: Exception) {
                // ignore
            }
        }

        // Ensure database seeds on first run
        viewModelScope.launch {
            val setting = db.countrySettingDao().getCountrySettingStatic()
            if (setting == null) {
                // First initialization - Seed USA
                repository.setCountrySetting("USA", forceReSeed = true)
            }
        }

        // Real-time currency background fetch from reliable api open.er-api.com
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val conn = java.net.URL("https://open.er-api.com/v6/latest/USD").openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val regex = "\"([A-Z]{3})\":\\s*([0-9\\.]+)".toRegex()
                        val matches = regex.findAll(text)
                        val newRates = matches.associate {
                            it.groupValues[1] to (it.groupValues[2].toDoubleOrNull() ?: 1.0)
                        }
                        if (newRates.isNotEmpty() && newRates.containsKey("USD")) {
                            val merged = newRates.toMutableMap()
                            _userCustomExchangeRates.value.forEach { (k, v) ->
                                merged[k] = v
                            }
                            _exchangeRates.value = merged
                        }
                    }
                }
            } catch (e: Exception) {
                // Fall back gracefully to stable pre-seeded rates offline
            }
        }
    }

    // Transactions filtered for the active month (including automated subscription projections for future months)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        selectedMonth
    ) { txs, month ->
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        txs.flatMap { tx ->
            val txMonth = sdf.format(Date(tx.timestamp))
            if (txMonth == month) {
                listOf(tx)
            } else if (tx.isRecurring && month > txMonth) {
                // Future projection: construct virtual projected duplicate in chosen target month
                listOf(
                    tx.copy(
                        id = tx.id * 100_000 + month.replace("-", "").toLongOrNull().hashCode(), // unique virtual ID to avoid recycler conflicts
                        timestamp = parseMonthToTimestamp(month, tx.timestamp),
                        notes = "[Recurring Subscription] ${tx.notes}"
                    )
                )
            } else {
                emptyList()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun parseMonthToTimestamp(targetYearMonth: String, originalTimestamp: Long): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val targetDate = sdf.parse(targetYearMonth) ?: return originalTimestamp
            
            val origCal = Calendar.getInstance()
            origCal.timeInMillis = originalTimestamp
            
            val targetCal = Calendar.getInstance()
            targetCal.time = targetDate
            targetCal.set(Calendar.DAY_OF_MONTH, origCal.get(Calendar.DAY_OF_MONTH).coerceAtMost(targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)))
            targetCal.set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
            targetCal.set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
            targetCal.timeInMillis
        } catch (e: Exception) {
            originalTimestamp
        }
    }

    // Budgets for the active month
    val activeBudgets: StateFlow<List<BudgetEntity>> = selectedMonth
        .flatMapLatest { month -> repository.getBudgetsForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State Calculations ---
    val totalBalance: StateFlow<Double> = combine(accounts, activeCountryConfig, exchangeRates) { accList, country, rates ->
        accList.sumOf { acc ->
            convertCurrency(acc.balance, acc.currency, country.currency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netWorthSummary: StateFlow<NetWorthSummary> = combine(accounts, activeCountryConfig, exchangeRates) { accList, country, rates ->
        var manualSum = 0.0
        var syncedSum = 0.0
        accList.forEach { acc ->
            val converted = convertCurrency(acc.balance, acc.currency, country.currency)
            val isManual = acc.type == "CASH" || !acc.isSyncEnabled
            if (isManual) {
                manualSum += converted
            } else {
                syncedSum += converted
            }
        }
        NetWorthSummary(
            manualNetInLocalCurrency = manualSum,
            syncedNetInLocalCurrency = syncedSum,
            totalNetWorth = manualSum + syncedSum
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthSummary())

    val currentInflow: StateFlow<Double> = combine(filteredTransactions, accounts, activeCountryConfig, exchangeRates) { txList, accList, country, rates ->
        txList.filter { it.type == "INCOME" }.sumOf { tx ->
            val acc = accList.find { it.id == tx.accountId }
            val txCurrency = acc?.currency ?: "USD"
            convertCurrency(tx.amount, txCurrency, country.currency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentOutflow: StateFlow<Double> = combine(filteredTransactions, accounts, activeCountryConfig, exchangeRates) { txList, accList, country, rates ->
        txList.filter { it.type == "EXPENSE" }.sumOf { tx ->
            val acc = accList.find { it.id == tx.accountId }
            val txCurrency = acc?.currency ?: "USD"
            convertCurrency(tx.amount, txCurrency, country.currency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val burnRateAndRunway: StateFlow<String> = combine(
        totalBalance,
        currentInflow,
        currentOutflow
    ) { balance, inflow, outflow ->
        val netDeficit = outflow - inflow
        if (netDeficit <= 0) {
            "Runway: Infinite (Surplus Flow) 🚀"
        } else {
            val monthsRemaining = balance / netDeficit
            if (balance <= 0) {
                "Runway: Exhausted (Critical Balance) ⚠️"
            } else {
                String.format(Locale.US, "Runway: %.1f Months Remaining (Burn: %s/mo)", monthsRemaining, formatCurrency(netDeficit))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Calculating...")

    // Smart trend projection to warn if user exceeds monthly limit based on daily spending average
    val budgetProjection: StateFlow<BudgetProjectionAnalysis> = combine(
        filteredTransactions,
        activeBudgets,
        selectedMonth
    ) { txList, budgetList, monthStr ->
        val totalBudgetAmount = budgetList.sumOf { it.amount }
        val currentSpentAmount = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        
        if (totalBudgetAmount <= 0.0) {
            BudgetProjectionAnalysis(
                dailyAverage = 0.0,
                projectedTotalSpent = 0.0,
                totalBudget = 0.0,
                isProjectedToExceed = false,
                daysPassed = 1,
                totalDays = 30,
                currentSpent = currentSpentAmount
            )
        } else {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val targetCal = Calendar.getInstance()
            var daysPassed = 1
            var totalDays = 30
            try {
                val targetDate = sdf.parse(monthStr)
                if (targetDate != null) {
                    targetCal.time = targetDate
                    totalDays = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    val targetYear = targetCal.get(Calendar.YEAR)
                    val targetMonth = targetCal.get(Calendar.MONTH)
                    
                    if (targetYear == currentYear && targetMonth == currentMonth) {
                        daysPassed = currentDay.coerceIn(1, totalDays)
                    } else if (targetYear < currentYear || (targetYear == currentYear && targetMonth < currentMonth)) {
                        daysPassed = totalDays
                    } else {
                        daysPassed = 1
                    }
                }
            } catch (e: Exception) {
                // ignore
            }

            val dailyAvg = currentSpentAmount / daysPassed
            val projectedTotal = dailyAvg * totalDays
            val isExceeding = projectedTotal > totalBudgetAmount && currentSpentAmount > 0.0

            BudgetProjectionAnalysis(
                dailyAverage = dailyAvg,
                projectedTotalSpent = projectedTotal,
                totalBudget = totalBudgetAmount,
                isProjectedToExceed = isExceeding,
                daysPassed = daysPassed,
                totalDays = totalDays,
                currentSpent = currentSpentAmount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetProjectionAnalysis())

    val predictiveInsights: StateFlow<List<PredictiveInsight>> = combine(
        allTransactions,
        categories,
        activeBudgets
    ) { txs, cats, budgets ->
        if (txs.isEmpty() || cats.isEmpty()) {
            return@combine emptyList()
        }
        
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        
        // Group transactions by month to see the historical span size
        val txsByMonth = txs.groupBy { sdf.format(Date(it.timestamp)) }
        val uniqueMonthsCount = txsByMonth.keys.size.coerceAtLeast(1)
        
        cats.filter { !it.isIncome }.mapNotNull { cat ->
            val catTxs = txs.filter { it.categoryId == cat.id && it.type == "EXPENSE" }
            if (catTxs.isEmpty()) return@mapNotNull null
            
            val totalSpent = catTxs.sumOf { it.amount }
            val averageMonthlySpend = totalSpent / uniqueMonthsCount
            
            val budget = budgets.find { it.categoryId == cat.id }
            val currentLimit = budget?.amount ?: 0.0
            
            val predictedSpend = averageMonthlySpend * 1.05 // add 5% seasonal volatility factor
            
            val (suggestedLimit, recommendation, trendType) = when {
                currentLimit <= 0.0 -> {
                    val suggested = (Math.ceil(predictedSpend / 10) * 10).coerceAtLeast(50.0)
                    Triple(
                        suggested,
                        "We noticed steady historical spending. Consider establishing a monthly budget limit of ${formatCurrency(suggested)} to safeguard balances.",
                        "INCREASE"
                    )
                }
                predictedSpend > currentLimit * 1.1 -> {
                    val suggested = (Math.ceil(predictedSpend / 50) * 50)
                    Triple(
                        suggested,
                        "Your historical outlays average ${formatCurrency(averageMonthlySpend)}, which model projects will overrun current ${formatCurrency(currentLimit)} limit. Consider increasing limit to ${formatCurrency(suggested)}.",
                        "INCREASE"
                    )
                }
                predictedSpend < currentLimit * 0.7 && currentLimit > 0.0 -> {
                    val suggested = (Math.ceil(predictedSpend / 50) * 50).coerceAtLeast(20.0)
                    val difference = currentLimit - suggested
                    Triple(
                        suggested,
                        "You consistently spend less than budgeted. Lowering this limit to ${formatCurrency(suggested)} frees up ${formatCurrency(difference)} which can be allocated to your savings goals!",
                        "REDUCE"
                    )
                }
                else -> {
                    Triple(
                        currentLimit,
                        "Perfect alignment! Historical spending of ${formatCurrency(averageMonthlySpend)} perfectly aligns with your set budget limit of ${formatCurrency(currentLimit)}.",
                        "STABLE"
                    )
                }
            }
            
            PredictiveInsight(
                categoryId = cat.id,
                categoryName = cat.name,
                predictedSpend = predictedSpend,
                currentLimit = currentLimit,
                suggestedLimit = suggestedLimit,
                recommendation = recommendation,
                trendType = trendType
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI Insight Generation Scope ---
    private val _aiInsightsLoading = MutableStateFlow(false)
    val aiInsightsLoading: StateFlow<Boolean> = _aiInsightsLoading.asStateFlow()

    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    fun triggerGeminiEvaluation() {
        viewModelScope.launch {
            _aiInsightsLoading.value = true
            _aiReport.value = null

            val currentCountryName = activeCountryConfig.value.country
            val currencySymbol = activeCountryConfig.value.currencySymbol
            val activeTxs = filteredTransactions.value
            val activeBuds = activeBudgets.value
            val activeCat = categories.value

            val totalIn = currentInflow.value
            val totalOut = currentOutflow.value
            val balance = totalBalance.value

            if (!GeminiApiClient.isApiKeyConfigured()) {
                // Return immediate elegant, country-specific mock fallback
                generateDynamicMockInsights(currentCountryName, currencySymbol, totalIn, totalOut, balance, activeBuds, activeCat)
                _aiInsightsLoading.value = false
                return@launch
            }

            // Construct full data context for Gemini
            val transactionSummary = activeTxs.joinToString("\n") { tx ->
                val catName = activeCat.find { it.id == tx.categoryId }?.name ?: "Transfer"
                "- ${catName}: ${currencySymbol}${tx.amount} (${tx.type}) at ${tx.merchant}. Tax Deductible: ${tx.isTaxDeductible}"
            }

            val budgetSummary = activeBuds.joinToString("\n") { bud ->
                val catName = activeCat.find { it.id == bud.categoryId }?.name ?: "Unknown"
                "- Budget for $catName: Limit ${currencySymbol}${bud.amount}, Spent ${currencySymbol}${bud.spent} (Adaptive: ${bud.isAdaptive})"
            }

            val prompt = """
                Optimize my finances under the regulations for target country: $currentCountryName.
                Current Accounts Balance: $currencySymbol$balance
                Monthly Inflow: $currencySymbol$totalIn
                Monthly Outflow: $currencySymbol$totalOut
                
                HISTORIC TRANSACTIONS FOR THIS MONTH:
                $transactionSummary
                
                BUDGET DESIGN LIMITS:
                $budgetSummary
                
                Please generate:
                1. Anomaly Detection: Flag any high burn-rate behaviors or budget overruns.
                2. Personalized Action Plan: Detail exactly 3 tactical saving tips customized for the payment habits, rules, and categories of $currentCountryName.
                3. Forecast: Predict upcoming expenses and outline a brief tax advice on deductible tags detected. Keep response concise, readable, markdown styled, and directly actionable. Do not print system details.
            """.trimIndent()

            val systemInstruction = "You are a senior elite Fintech Wealth Strategist advising users on personal finance. Write in a clear, highly tactical, professional financial advisor tone."

            val resultText = GeminiApiClient.getAiInsights(prompt, systemInstruction)
            
            _aiReport.value = resultText

            // Insert a new event log into local DB for standard user alerts tracking
            if (resultText.isNotEmpty() && resultText != "API_KEY_MISSING") {
                val briefTitle = "Smart AI Audit - $currentCountryName"
                val briefText = if (resultText.length > 180) resultText.substring(0, 177) + "..." else resultText
                repository.insertInsight(
                    InsightEntity(
                        title = briefTitle,
                        description = briefText,
                        category = "AI_PREDICTION",
                        severity = "SUCCESS"
                    )
                )
            }
            _aiInsightsLoading.value = false
        }
    }

    private suspend fun generateDynamicMockInsights(
        country: String,
        symbol: String,
        inflow: Double,
        outflow: Double,
        balance: Double,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>
    ) {
        val runwayText = if (outflow > inflow) {
            String.format(Locale.US, "%.1f Months remaining", balance / (outflow - inflow))
        } else {
            "Surplus health"
        }

        // Construct dynamic content based on real DB status
        val overspentBudgets = budgets.filter { it.spent > it.amount }
        val budgetComment = if (overspentBudgets.isNotEmpty()) {
            "Budget Alerts: Your budgets for " + overspentBudgets.joinToString(", ") { bud ->
                categories.find { it.id == bud.categoryId }?.name ?: ""
            } + " have been exceeded. We recommend adjusting adaptive boundaries."
        } else {
            "No active budget overruns. Good discipline observed."
        }

        val config = CountryConfig.find(country)

        val reportStr = """
            ### 🤖 AI Financial Evaluation (${country}) - Offline Mode
            
            *A personal finance evaluation generated via localized fallback algorithms. Activate terminal environment credentials to leverage standard Google Gemini live predictions.*
            
            #### 📊 1. Core Anomaly & Runway Audit
            *   **Net Inflow/Outflow Balance**: Inflow of ${symbol}${inflow} vs Outflow of ${symbol}${outflow}.
            *   **Liquid Health**: Cumulative buffer is ${symbol}${balance} (${runwayText}).
            *   **Compliance Standard**: $budgetComment
            
            #### 🏦 2. Specialized ${country} Wallet & Bank Insights
            *   We noticed your registered funds align to local bank channels (${config.standardBanks.joinToString(", ")}). Consider routing direct recurring bills through auto-debit templates to avoid late fee structures.
            *   **Mobile Wallet Savings**: Check for cashbacks or specific discount tiers on popular digital platforms (${config.wallets.joinToString(", ")}) configured for ${country}.
            
            #### 🧾 3. Tax Optimization Directive (${config.fiscalYear} Cycle)
            *   In ${country}, remember to check if your expenses are eligible for tax-deductions.
            *   Deductible Categories to prioritize in your region: **${config.taxCategories.joinToString(", ")}**.
            *   You have tax-deductible items flagged. Ensure these receipts are preserved for annual filing exports.
        """.trimIndent()

        _aiReport.value = reportStr

        // Insert new alert
        repository.insertInsight(
            InsightEntity(
                title = "Offline Financial Diagnostic - $country",
                description = "Analytics compiled based on $country configuration rules and bank templates.",
                category = "AI_PREDICTION",
                severity = "INFO"
            )
        )
    }

    // --- Basic Helpers ---
    fun selectMonth(yearMonth: String) {
        _selectedMonth.value = yearMonth
    }

    fun incrementMonth() {
        adjustMonth(1)
    }

    fun decrementMonth() {
        adjustMonth(-1)
    }

    private fun adjustMonth(delta: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(_selectedMonth.value)
            if (date != null) {
                val cal = Calendar.getInstance()
                cal.time = date
                cal.add(Calendar.MONTH, delta)
                _selectedMonth.value = sdf.format(cal.time)
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    fun switchCountry(countryName: String) {
        viewModelScope.launch {
            repository.setCountrySetting(countryName, forceReSeed = true)
        }
    }

    fun addTransaction(
        amount: Double,
        type: String,
        categoryId: Long,
        accountId: Long,
        toAccountId: Long = -1,
        merchant: String = "",
        isTaxDeductible: Boolean = false,
        taxRate: Double = 0.0,
        notes: String = "",
        isRecurring: Boolean = false,
        recurrenceInterval: String = "NONE",
        splitCount: Int = 1,
        customTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                toAccountId = toAccountId,
                timestamp = customTimestamp ?: System.currentTimeMillis(),
                merchant = merchant,
                isTaxDeductible = isTaxDeductible,
                taxRate = taxRate,
                notes = notes,
                isRecurring = isRecurring,
                recurrenceInterval = recurrenceInterval,
                splitCount = splitCount
            )
            repository.insertTransaction(tx)

            // Auto-populate subsequent entries in local storage if recurring
            if (isRecurring && recurrenceInterval != "NONE") {
                val baseTime = customTimestamp ?: System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                
                val countToGenerate = when (recurrenceInterval) {
                    "DAILY" -> 30  // Physical pre-population of 30 days
                    "WEEKLY" -> 12 // Physical pre-population of 12 weeks
                    "MONTHLY" -> 12 // Physical pre-population of 12 months
                    else -> 0
                }

                for (i in 1..countToGenerate) {
                    calendar.timeInMillis = baseTime
                    when (recurrenceInterval) {
                        "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, i)
                        "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, i)
                        "MONTHLY" -> calendar.add(Calendar.MONTH, i)
                    }
                    val recurringTx = tx.copy(
                        timestamp = calendar.timeInMillis,
                        notes = if (notes.isEmpty()) "Recurring $recurrenceInterval entry #$i" else "$notes (Recurring $recurrenceInterval #$i)"
                    )
                    repository.insertTransaction(recurringTx)
                }
            }

            // Trigger proactive budget alerts dynamically
            if (type == "EXPENSE") {
                val cat = categories.value.find { it.id == categoryId }
                val bud = activeBudgets.value.find { it.categoryId == categoryId }
                if (bud != null && cat != null) {
                    val updatedSpent = bud.spent + amount
                    if (updatedSpent > bud.amount * 0.90) {
                        val pct = (updatedSpent / bud.amount * 100).toInt()
                        val msg = if (updatedSpent > bud.amount) {
                            "🚨 Budget Exceeded! ${cat.name} is at $pct%! Spend is ${formatCurrency(updatedSpent)} of ${formatCurrency(bud.amount)}."
                        } else {
                            "⚠️ Budget Alert! ${cat.name} exceeds 90% limit ($pct%) at ${formatCurrency(updatedSpent)} of ${formatCurrency(bud.amount)}."
                        }
                        _notifications.tryEmit(msg)
                    }

                    if (updatedSpent > bud.amount) {
                        repository.insertInsight(
                            InsightEntity(
                                title = "Budget Exceeded - ${cat.name}",
                                description = "You spent ${formatCurrency(updatedSpent)} out of a set limit of ${formatCurrency(bud.amount)} for category ${cat.name}!",
                                category = "BUDGET_EXCEED",
                                severity = "ALERT"
                             )
                        )
                    } else if (updatedSpent > bud.amount * 0.85) {
                        repository.insertInsight(
                            InsightEntity(
                                title = "Budget Warning - ${cat.name}",
                                description = "You have consumed 85% of your set limit for ${cat.name}!",
                                category = "BUDGET_EXCEED",
                                severity = "WARNING"
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun updateBudgetLimit(categoryId: Long, newLimit: Double, isAdaptive: Boolean, savingsGoal: Double = 0.0) {
        viewModelScope.launch {
            val monthStr = selectedMonth.value
            val existing = repository.getBudgetsForMonth(monthStr).firstOrNull()?.find { it.categoryId == categoryId }
            if (existing != null) {
                repository.updateBudget(existing.copy(amount = newLimit, isAdaptive = isAdaptive, savingsGoal = savingsGoal, updatedAt = System.currentTimeMillis()))
            } else {
                val newBud = BudgetEntity(categoryId = categoryId, amount = newLimit, spent = 0.0, month = monthStr, isAdaptive = isAdaptive, savingsGoal = savingsGoal)
                repository.insertBudget(newBud)
            }
        }
    }

    fun addAccount(name: String, type: String, startingBalance: Double, provider: String, colorHex: String) {
        viewModelScope.launch {
            val newAcc = AccountEntity(
                name = name,
                type = type,
                balance = startingBalance,
                currency = activeCountryConfig.value.currency,
                provider = provider,
                accountColorHex = colorHex,
                isSyncEnabled = (type == "BANK" || type == "MOBILE_WALLET")
            )
            repository.insertAccount(newAcc)
        }
    }

    val matchingRules: StateFlow<List<MatchingRuleEntity>> = db.matchingRuleDao().getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val syncService = BankAndMfsSyncService()

    fun addMatchingRule(keyword: String, categoryId: Long, isTaxDeductible: Boolean, taxRate: Double) {
        viewModelScope.launch {
            val rule = MatchingRuleEntity(
                keyword = keyword,
                categoryId = categoryId,
                isTaxDeductible = isTaxDeductible,
                taxRate = taxRate
            )
            db.matchingRuleDao().insertRule(rule)
            postNotification("Rule added: '$keyword' mapping registered.")
        }
    }

    fun deleteMatchingRule(rule: MatchingRuleEntity) {
        viewModelScope.launch {
            db.matchingRuleDao().deleteRule(rule)
            postNotification("Rule deleted: '${rule.keyword}' removed.")
        }
    }

    fun toggleAccountSync(account: AccountEntity) {
        viewModelScope.launch {
            val updated = account.copy(isSyncEnabled = !account.isSyncEnabled)
            repository.updateAccount(updated)
            val stateText = if (updated.isSyncEnabled) "Synced (Active)" else "Local Manual (Unsynced)"
            postNotification("${updated.name} tracking toggled to $stateText.")
        }
    }

    fun syncAccountInstance(account: AccountEntity) {
        viewModelScope.launch {
            try {
                val count = syncService.executeSync(account, db)
                if (count > 0) {
                    postNotification("Synced $count transactions from ${account.provider} (${account.name})!")
                } else {
                    postNotification("${account.provider} is already up-to-date.")
                }
            } catch (e: Exception) {
                postNotification("Sync Failure: ${e.localizedMessage ?: "Unknown Gateway Block"}")
            }
        }
    }

    fun syncAllActiveAccounts() {
        viewModelScope.launch {
            var totalSyncedCount = 0
            val activeAccounts = repository.allAccounts.firstOrNull() ?: emptyList()
            activeAccounts.forEach { acc ->
                if (acc.isSyncEnabled && (acc.type == "BANK" || acc.type == "MOBILE_WALLET")) {
                    try {
                        totalSyncedCount += syncService.executeSync(acc, db)
                    } catch (e: Exception) {
                        // skip isolated sync error
                    }
                }
            }
            if (totalSyncedCount > 0) {
                postNotification("All accounts synced! $totalSyncedCount brand-new transactions integrated.")
            } else {
                postNotification("All accounts are fully up-to-date.")
            }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun clearNotificationFeedStr() {
        viewModelScope.launch {
            repository.clearInsights()
        }
    }

    fun dismissSingleInsight(insightId: Long) {
        viewModelScope.launch {
            repository.markInsightRead(insightId)
        }
    }

    // Dynamic Currency Formatter based on country setting
    fun formatCurrency(amount: Double): String {
        val config = activeCountryConfig.value
        return String.format(Locale.US, "%s%,.2f", config.currencySymbol, amount)
    }

    fun exportReportToCsv(context: android.content.Context) {
        val txs = filteredTransactions.value
        val cats = categories.value
        val buds = activeBudgets.value
        val currentMonthStr = selectedMonth.value
        val config = activeCountryConfig.value

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        
        val csvBuilder = StringBuilder()
        csvBuilder.append("WEALTHFLOW STATEMENT REPORT - ${config.country.uppercase()}\n")
        csvBuilder.append("Report Date: ${sdf.format(Date())}\n")
        csvBuilder.append("Active Month: $currentMonthStr\n\n")

        csvBuilder.append("--- TRANSACTIONS LEDGER ---\n")
        csvBuilder.append("Date,Type,Category,Merchant,Amount (${config.currency}),Tax Deductible,Recurring,Notes\n")
        
        txs.forEach { tx ->
            val dateStr = sdf.format(Date(tx.timestamp))
            val catName = cats.find { it.id == tx.categoryId }?.name ?: "Transfer"
            val cleanNotes = tx.notes.replace("\"", "\"\"")
            val cleanMerchant = tx.merchant.replace("\"", "\"\"")
            csvBuilder.append("$dateStr,${tx.type},\"$catName\",\"$cleanMerchant\",${tx.amount},${tx.isTaxDeductible},${tx.isRecurring},\"$cleanNotes\"\n")
        }

        csvBuilder.append("\n--- BUDGET TRACKER LIMITS ($currentMonthStr) ---\n")
        csvBuilder.append("Category,Budget Limit (${config.currency}),Spent (${config.currency}),Overruns\n")
        buds.forEach { bud ->
            val catName = cats.find { it.id == bud.categoryId }?.name ?: "Unknown"
            val overrun = if (bud.spent > bud.amount) bud.spent - bud.amount else 0.0
            csvBuilder.append("\"$catName\",${bud.amount},${bud.spent},$overrun\n")
        }

        val csvContent = csvBuilder.toString()
        val encryptEnabled = isExportEncryptionEnabled.value
        val passcode = exportPasscode.value
        
        val finalContent = if (encryptEnabled) {
            CsvEncryptionUtility.encrypt(csvContent, passcode)
        } else {
            csvContent
        }
        
        try {
            val fileExtension = if (encryptEnabled) ".csv.enc" else ".csv"
            val mimeType = if (encryptEnabled) "text/plain" else "text/csv"
            val fileName = "wealthflow_finance_report_${currentMonthStr.replace("-", "_")}$fileExtension"
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(finalContent)

            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow Finance Report - $currentMonthStr" + (if (encryptEnabled) " (AES-256 Encrypted)" else ""))
                putExtra(android.content.Intent.EXTRA_TEXT, finalContent) // fallback text
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share statements CSV"))
            
            if (encryptEnabled) {
                postNotification("Export report securely encrypted using industry-standard AES-256!")
            }
        } catch (e: Exception) {
            // Safe plain text sharing fallback if URI conversion hits any device constraints
            try {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow Finance Report - $currentMonthStr" + (if (encryptEnabled) " (AES-256 Encrypted)" else ""))
                    putExtra(android.content.Intent.EXTRA_TEXT, finalContent)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Report (Text Fallback)"))
            } catch (fallbackEx: Exception) {
                android.widget.Toast.makeText(context, "Export error: ${fallbackEx.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun exportTaxReportToCsv(context: android.content.Context) {
        val txs = allTransactions.value // load all transactions for annual assessment
        val cats = categories.value
        val config = activeCountryConfig.value
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val taxBuilder = StringBuilder()
        taxBuilder.append("REGIONAL ANNUAL TAX SUMMARY REPORT - ${config.country.uppercase()}\n")
        taxBuilder.append("Fiscal Year / Underneath Schedule: ${config.fiscalYear} cycle\n")
        taxBuilder.append("Submission Compliant Standard Format: Schedule-B/Form-1040 (US) / SEC-24/NBR (BD)\n")
        taxBuilder.append("Generated on Date: ${sdf.format(Date())}\n\n")

        taxBuilder.append("--- REVENUE SUMMARY & TAX RELIEF CALCULATIONS ---\n")
        taxBuilder.append("Invoiced Category,Aggregate Spent,Applicable Flat Rate %,Resolved Net Estimated Relief\n")
        
        val deductibles = txs.filter { it.isTaxDeductible && it.type == "EXPENSE" }
        val groupedByCat = deductibles.groupBy { it.categoryId }

        var totalDeductionVal = 0.0
        var totalTaxOffsetVal = 0.0

        groupedByCat.forEach { (catId, catTxs) ->
            val catName = cats.find { it.id == catId }?.name ?: "Eligible Deductions"
            val spentSum = catTxs.sumOf { it.amount }
            // Use average tax rate of transactions or default country taxRate
            val rate = catTxs.firstOrNull()?.taxRate ?: config.taxRateDefault
            val relief = spentSum * (rate / 100.0)
            taxBuilder.append("\"$catName\",$spentSum,${rate}%,$relief\n")
            totalDeductionVal += spentSum
            totalTaxOffsetVal += relief
        }

        taxBuilder.append("\n--- CONSOLIDATED TOTALS ---\n")
        taxBuilder.append("Gross Deductible Outlay,${totalDeductionVal}\n")
        taxBuilder.append("Estimated Sovereign Tax Credit Offset,${totalTaxOffsetVal}\n\n")

        taxBuilder.append("--- LEDGER TRANSACTION AUDIT TRIAL RECORDS ---\n")
        taxBuilder.append("Date,Merchant Payee,Category,Amount (${config.currency}),Tax Rate,Tax Saving,Notes\n")
        deductibles.forEach { tx ->
            val dateStr = sdf.format(Date(tx.timestamp))
            val catName = cats.find { it.id == tx.categoryId }?.name ?: "Tax-Deductible Segment"
            val saving = tx.amount * (tx.taxRate / 100.0)
            val cleanNotes = tx.notes.replace("\"", "\"\"")
            val cleanMerchant = tx.merchant.replace("\"", "\"\"")
            taxBuilder.append("$dateStr,\"$cleanMerchant\",\"$catName\",${tx.amount},${tx.taxRate}%,$saving,\"$cleanNotes\"\n")
        }

        val taxCsvContent = taxBuilder.toString()
        val encryptEnabled = isExportEncryptionEnabled.value
        val passcode = exportPasscode.value
        
        val finalContent = if (encryptEnabled) {
            CsvEncryptionUtility.encrypt(taxCsvContent, passcode)
        } else {
            taxCsvContent
        }

        try {
            val fileExtension = if (encryptEnabled) ".csv.enc" else ".csv"
            val mimeType = if (encryptEnabled) "text/plain" else "text/csv"
            val fileName = "TaxLedger_${config.country.replace(" ", "_")}$fileExtension"
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(finalContent)

            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Annual Tax Filing CSV - ${config.country} " + (if (encryptEnabled) "(AES)" else ""))
                putExtra(android.content.Intent.EXTRA_TEXT, finalContent)
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Annual Tax Report"))

            if (encryptEnabled) {
                postNotification("Annual Tax Report compiled containing regional IRS-SEC files (Securely Encrypted)!")
            } else {
                postNotification("Annual Tax Report compiled successfully!")
            }
        } catch (e: Exception) {
            try {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Annual Tax Filing CSV - ${config.country}")
                    putExtra(android.content.Intent.EXTRA_TEXT, finalContent)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Tax Report"))
            } catch (fallbackEx: Exception) {
                android.widget.Toast.makeText(context, "Export error: ${fallbackEx.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
