package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.data.repository.CountryConfigProviderService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

        // Ensure database seeds on first run
        viewModelScope.launch {
            val setting = db.countrySettingDao().getCountrySettingStatic()
            if (setting == null) {
                // First initialization - Seed USA
                repository.setCountrySetting("USA", forceReSeed = true)
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
    val totalBalance: StateFlow<Double> = accounts
        .map { accList -> accList.sumOf { it.balance } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentInflow: StateFlow<Double> = filteredTransactions
        .map { txList -> txList.filter { it.type == "INCOME" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentOutflow: StateFlow<Double> = filteredTransactions
        .map { txList -> txList.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
                splitCount = splitCount
            )
            repository.insertTransaction(tx)

            // Trigger proactive budget alerts dynamically
            if (type == "EXPENSE") {
                val cat = categories.value.find { it.id == categoryId }
                val bud = activeBudgets.value.find { it.categoryId == categoryId }
                if (bud != null && cat != null) {
                    val updatedSpent = bud.spent + amount
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

    fun updateBudgetLimit(categoryId: Long, newLimit: Double, isAdaptive: Boolean) {
        viewModelScope.launch {
            val monthStr = selectedMonth.value
            val existing = repository.getBudgetsForMonth(monthStr).firstOrNull()?.find { it.categoryId == categoryId }
            if (existing != null) {
                repository.updateBudget(existing.copy(amount = newLimit, isAdaptive = isAdaptive, updatedAt = System.currentTimeMillis()))
            } else {
                val newBud = BudgetEntity(categoryId = categoryId, amount = newLimit, spent = 0.0, month = monthStr, isAdaptive = isAdaptive)
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
                accountColorHex = colorHex
            )
            repository.insertAccount(newAcc)
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
        
        try {
            val fileName = "wealthflow_finance_report_${currentMonthStr.replace("-", "_")}.csv"
            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(csvContent)

            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow Finance Report - $currentMonthStr")
                putExtra(android.content.Intent.EXTRA_TEXT, csvContent) // fallback text
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share statements CSV"))
        } catch (e: Exception) {
            // Safe plain text sharing fallback if URI conversion hits any device constraints
            try {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow Finance Report - $currentMonthStr")
                    putExtra(android.content.Intent.EXTRA_TEXT, csvContent)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Report (Text Fallback)"))
            } catch (fallbackEx: Exception) {
                android.widget.Toast.makeText(context, "Export error: ${fallbackEx.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
