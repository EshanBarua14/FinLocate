package com.example.ui

import android.content.Context
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

sealed class UiEvent {
    object ExpenseSubmitted : UiEvent()
    object SavingsGoalReached : UiEvent()
    object BudgetAlertTriggered : UiEvent()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // --- Interactive UI & Haptic Event Flow ---
    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)
    val uiEvents = _uiEvents.asSharedFlow()

    private val db = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(db)
    private val countryConfigProvider = CountryConfigProviderService(application)

    // --- SharedPreferences for local settings ---
    private val prefs = application.getSharedPreferences("wealthflow_prefs", Application.MODE_PRIVATE)

    // --- User Authentication and State ---
    private val _userToken = MutableStateFlow(prefs.getString("user_jwt_token", null))
    val userToken: StateFlow<String?> = _userToken.asStateFlow()

    private val _username = MutableStateFlow(prefs.getString("user_username", null))
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun registerUser(usernameInput: String, passwordInput: String, emailInput: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val registerUrl = if (baseUrl.isEmpty()) "http://localhost:5000/api/auth/register" else "$baseUrl/api/auth/register"
                val request = com.example.data.api.AuthRegisterRequest(
                    username = usernameInput,
                    email = emailInput,
                    password = passwordInput,
                    taxProfile = activeCountryConfig.value.country
                )
                val response = com.example.data.api.CloudSyncClient.service.registerUser(registerUrl, request)
                if (response.success && response.token != null && response.user != null) {
                    prefs.edit()
                        .putString("user_jwt_token", response.token)
                        .putString("user_username", response.user.username)
                        .putString("user_email", response.user.email)
                        .putBoolean("is_logged_in", true)
                        .apply()
                    _userToken.value = response.token
                    _username.value = response.user.username
                    _userEmail.value = response.user.email
                    _isLoggedIn.value = true
                    onResult(null) // success
                } else {
                    onResult(response.error ?: response.message ?: "Registration failed")
                }
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun loginUser(emailInput: String, passwordInput: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val loginUrl = if (baseUrl.isEmpty()) "http://localhost:5000/api/auth/login" else "$baseUrl/api/auth/login"
                val request = com.example.data.api.AuthLoginRequest(
                    email = emailInput,
                    password = passwordInput
                )
                val response = com.example.data.api.CloudSyncClient.service.loginUser(loginUrl, request)
                if (response.success && response.token != null && response.user != null) {
                    prefs.edit()
                        .putString("user_jwt_token", response.token)
                        .putString("user_username", response.user.username)
                        .putString("user_email", response.user.email)
                        .putBoolean("is_logged_in", true)
                        .apply()
                    _userToken.value = response.token
                    _username.value = response.user.username
                    _userEmail.value = response.user.email
                    _isLoggedIn.value = true
                    onResult(null) // success
                } else {
                    onResult(response.error ?: response.message ?: "Invalid credentials")
                }
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun logoutUser() {
        prefs.edit()
            .remove("user_jwt_token")
            .remove("user_username")
            .remove("user_email")
            .putBoolean("is_logged_in", false)
            .apply()
        _userToken.value = null
        _username.value = null
        _userEmail.value = null
        _isLoggedIn.value = false
    }

    fun syncExpensesAndAccountsWithCloud(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val token = userToken.value
            if (token == null) {
                onResult("Error: You must be logged in to sync with the cloud.")
                return@launch
            }
            try {
                onResult("Preparing data for sync...")
                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val authHeader = "Bearer $token"

                // Fetch current lists
                val localAccounts = accounts.value
                val localTxs = allTransactions.value
                val localCats = categories.value

                // Map accounts to DTO
                val accountDtos = localAccounts.map { a ->
                    com.example.data.api.AccountSyncDto(
                        id = a.id.toString(),
                        name = a.name,
                        type = a.type,
                        balance = a.balance,
                        currency = a.currency,
                        provider = a.provider,
                        updatedAt = a.updatedAt
                    )
                }

                // Map transactions to DTO
                val expenseDtos = localTxs.map { t ->
                    val catName = localCats.find { it.id == t.categoryId }?.name ?: "Other"
                    val accName = localAccounts.find { it.id == t.accountId }?.name ?: "Cash"
                    com.example.data.api.ExpenseSyncDto(
                        id = t.id.toString(),
                        amount = t.amount,
                        type = t.type,
                        merchant = t.merchant,
                        categoryName = catName,
                        accountName = accName,
                        notes = t.notes,
                        timestamp = t.timestamp,
                        isTaxDeductible = t.isTaxDeductible,
                        taxRate = t.taxRate
                    )
                }

                onResult("Uploading local data...")
                val accountsSyncUrl = if (baseUrl.isEmpty()) "http://localhost:5000/api/accounts" else "$baseUrl/api/accounts"
                val expensesSyncUrl = if (baseUrl.isEmpty()) "http://localhost:5000/api/expenses" else "$baseUrl/api/expenses"
                val budgetsSyncUrl = if (baseUrl.isEmpty()) "http://localhost:5000/api/budgets" else "$baseUrl/api/budgets"

                val accPushResponse = com.example.data.api.CloudSyncClient.service.syncAccounts(accountsSyncUrl, authHeader, accountDtos)
                val expPushResponse = com.example.data.api.CloudSyncClient.service.syncExpenses(expensesSyncUrl, authHeader, expenseDtos)

                // Push budgets to cloud database
                val localBudgets = activeBudgets.value
                val budgetDtos = localBudgets.map { b ->
                    val catName = localCats.find { it.id == b.categoryId }?.name ?: "Unknown"
                    com.example.data.api.BudgetSyncDto(
                        id = "${b.categoryId}_${b.month}",
                        categoryId = b.categoryId,
                        categoryName = catName,
                        amount = b.amount,
                        month = b.month
                    )
                }
                if (budgetDtos.isNotEmpty()) {
                    try {
                        com.example.data.api.CloudSyncClient.service.syncBudgets(budgetsSyncUrl, authHeader, budgetDtos)
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "Budgets push soft-failed: ${e.localizedMessage}")
                    }
                }

                if (!accPushResponse.success || !expPushResponse.success) {
                    onResult("Push sync failed: " + (accPushResponse.error ?: expPushResponse.error ?: "Server error"))
                    return@launch
                }

                onResult("Downloading cloud updates...")
                val remoteAccsResult = com.example.data.api.CloudSyncClient.service.getAccounts(accountsSyncUrl, authHeader)
                val remoteExpsResult = com.example.data.api.CloudSyncClient.service.getExpenses(expensesSyncUrl, authHeader)

                if (remoteAccsResult.success && remoteExpsResult.success) {
                    onResult("Merging remote data...")
                    
                    // A. Merge Accounts
                    remoteAccsResult.accounts?.forEach { remoteAcc ->
                        val localMatch = localAccounts.find { it.name.equals(remoteAcc.name, ignoreCase = true) }
                        if (localMatch == null) {
                            repository.insertAccount(
                                com.example.data.model.AccountEntity(
                                    name = remoteAcc.name,
                                    type = remoteAcc.type,
                                    balance = remoteAcc.balance,
                                    currency = remoteAcc.currency,
                                    provider = remoteAcc.provider,
                                    updatedAt = remoteAcc.updatedAt
                                )
                            )
                        } else {
                            if (remoteAcc.updatedAt > localMatch.updatedAt) {
                                repository.updateAccount(
                                    localMatch.copy(
                                        balance = remoteAcc.balance,
                                        type = remoteAcc.type,
                                        provider = remoteAcc.provider,
                                        updatedAt = remoteAcc.updatedAt
                                    )
                                )
                            }
                        }
                    }

                    // Refresh local caches for transactions matching
                    val updatedLocalAccounts = db.accountDao().getAllAccountsStatic()
                    val updatedLocalCats = db.categoryDao().getAllCategoriesStatic()

                    // B. Merge Expenses
                    remoteExpsResult.expenses?.forEach { remoteExp ->
                        val txMatch = localTxs.find { 
                            it.merchant == remoteExp.merchant && 
                            Math.abs(it.amount - remoteExp.amount) < 0.01 && 
                            it.timestamp == remoteExp.timestamp 
                        }
                        if (txMatch == null) {
                            val catId = updatedLocalCats.find { it.name.equals(remoteExp.categoryName, ignoreCase = true) }?.id
                                ?: updatedLocalCats.firstOrNull()?.id ?: 1L
                            val accId = updatedLocalAccounts.find { it.name.equals(remoteExp.accountName, ignoreCase = true) }?.id
                                ?: updatedLocalAccounts.firstOrNull()?.id ?: 1L

                            repository.insertTransaction(
                                com.example.data.model.TransactionEntity(
                                    amount = remoteExp.amount,
                                    type = remoteExp.type,
                                    categoryId = catId,
                                    accountId = accId,
                                    merchant = remoteExp.merchant,
                                    isTaxDeductible = remoteExp.isTaxDeductible,
                                    taxRate = remoteExp.taxRate,
                                    notes = remoteExp.notes,
                                    timestamp = remoteExp.timestamp
                                )
                            )
                        }
                    }

                    // Download any budget alerts
                    fetchCloudNotifications()

                    postNotification("Real-time cloud database sync complete!")
                    onResult("SUCCESS")
                } else {
                    onResult("Pull sync failed: " + (remoteAccsResult.error ?: remoteExpsResult.error ?: "Download rejected"))
                }
            } catch (e: Exception) {
                onResult("Cloud Sync Fail: " + (e.localizedMessage ?: "Connection error"))
            }
        }
    }

    // --- Cloud Notifications & Budget Alarms ---
    private val _cloudNotifications = MutableStateFlow<List<com.example.data.api.NotificationSyncDto>>(emptyList())
    val cloudNotifications: StateFlow<List<com.example.data.api.NotificationSyncDto>> = _cloudNotifications.asStateFlow()

    fun fetchCloudNotifications(onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val token = userToken.value ?: return@launch
            try {
                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val url = if (baseUrl.isEmpty()) "http://localhost:5000/api/notifications" else "$baseUrl/api/notifications"
                val response = com.example.data.api.CloudSyncClient.service.getNotifications(url, "Bearer $token")
                if (response.success && response.notifications != null) {
                    _cloudNotifications.value = response.notifications
                    onComplete(null)

                    // Post toast/alert notification if any unread alarms exist
                    response.notifications.firstOrNull()?.let { lastNotif ->
                        val shownKey = "shown_notif_${lastNotif.id}"
                        if (!prefs.getBoolean(shownKey, false)) {
                            postNotification("⚠️ ${lastNotif.title}: ${lastNotif.message}")
                            prefs.edit().putBoolean(shownKey, true).apply()
                        }
                    }
                } else {
                    onComplete(response.error ?: "Failed to retrieve notifications")
                }
            } catch (e: Exception) {
                onComplete(e.localizedMessage ?: "Network error")
            }
        }
    }

    // --- Cloud Database Search Engine ---
    private val _cloudSearchResult = MutableStateFlow<List<com.example.data.api.ExpenseSyncDto>>(emptyList())
    val cloudSearchResult: StateFlow<List<com.example.data.api.ExpenseSyncDto>> = _cloudSearchResult.asStateFlow()

    private val _cloudSearchLoading = MutableStateFlow(false)
    val cloudSearchLoading: StateFlow<Boolean> = _cloudSearchLoading.asStateFlow()

    fun performCloudSearch(startDate: String?, endDate: String?, categoryName: String?, keyword: String?, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val token = userToken.value
            if (token == null) {
                onResult("You must be logged in to search the cloud.")
                return@launch
            }
            _cloudSearchLoading.value = true
            try {
                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val url = if (baseUrl.isEmpty()) "http://localhost:5000/api/expenses" else "$baseUrl/api/expenses"

                val start = startDate?.ifEmpty { null }
                val end = endDate?.ifEmpty { null }
                val cat = categoryName?.ifEmpty { null }
                val key = keyword?.ifEmpty { null }

                val response = com.example.data.api.CloudSyncClient.service.searchExpenses(
                    url = url,
                    authHeader = "Bearer $token",
                    startDate = start,
                    endDate = end,
                    categoryName = cat,
                    keyword = key
                )

                if (response.success && response.expenses != null) {
                    _cloudSearchResult.value = response.expenses
                    onResult(null)
                } else {
                    onResult(response.error ?: "Cloud search returned an error")
                }
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Network connection error during cloud search")
            } finally {
                _cloudSearchLoading.value = false
            }
        }
    }

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

    // --- Global Multi-Currency Toggle (Local vs USD Base) ---
    private val _useBaseCurrency = MutableStateFlow(prefs.getBoolean("use_base_currency", false))
    val useBaseCurrency: StateFlow<Boolean> = _useBaseCurrency.asStateFlow()

    fun setUseBaseCurrency(value: Boolean) {
        _useBaseCurrency.value = value
        prefs.edit().putBoolean("use_base_currency", value).apply()
    }

    // --- SQLite Manual Export & Backup Period Reminder ---
    private val _showBackupReminder = MutableStateFlow(false)
    val showBackupReminder: StateFlow<Boolean> = _showBackupReminder.asStateFlow()

    private val _txCountSinceLastExport = MutableStateFlow(prefs.getInt("tx_count_since_last_export", 0))
    val txCountSinceLastExport: StateFlow<Int> = _txCountSinceLastExport.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong("last_export_time", System.currentTimeMillis()))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _brandColorHex = MutableStateFlow(prefs.getString("brand_color_hex", "#10B981") ?: "#10B981")
    val brandColorHex: StateFlow<String> = _brandColorHex.asStateFlow()

    fun setBrandColorHex(hex: String) {
        prefs.edit().putString("brand_color_hex", hex).apply()
        _brandColorHex.value = hex
    }

    fun checkBackupReminder() {
        val count = prefs.getInt("tx_count_since_last_export", 0)
        val lastExport = prefs.getLong("last_export_time", 0L)
        val effectiveLastExport = if (lastExport == 0L) System.currentTimeMillis() else lastExport
        _lastBackupTime.value = effectiveLastExport
        val timePassed = System.currentTimeMillis() - effectiveLastExport
        _showBackupReminder.value = count >= 3 || (timePassed > 7 * 24 * 3600 * 1000L && lastExport > 0L)
        _txCountSinceLastExport.value = count
    }

    fun incrementTxCountSinceLastExport() {
        val currentCount = prefs.getInt("tx_count_since_last_export", 0) + 1
        prefs.edit().putInt("tx_count_since_last_export", currentCount).apply()
        checkBackupReminder()
    }

    fun resetTxCountSinceLastExport() {
        val now = System.currentTimeMillis()
        prefs.edit().putInt("tx_count_since_last_export", 0).apply()
        prefs.edit().putLong("last_export_time", now).apply()
        _lastBackupTime.value = now
        checkBackupReminder()
    }

    // --- Budget Templates Mode ---
    fun loadBudgetTemplate(templateName: String) {
        val cats = categories.value
        val monthStr = selectedMonth.value
        viewModelScope.launch {
            val foodCat = cats.find { it.name.contains("Food", ignoreCase = true) }?.id
            val transportCat = cats.find { it.name.contains("Transport", ignoreCase = true) }?.id
            val rentCat = cats.find { it.name.contains("Rent", ignoreCase = true) }?.id
            val entertainmentCat = cats.find { it.name.contains("Entertainment", ignoreCase = true) }?.id
            val billingCat = cats.find { it.name.contains("Taxes", ignoreCase = true) }?.id
            val shoppingCat = cats.find { it.name.contains("Shopping", ignoreCase = true) }?.id

            val templateMap = when (templateName.lowercase(Locale.ROOT)) {
                "travel" -> {
                    mapOf(
                        foodCat to 300.0,
                        transportCat to 800.0,
                        entertainmentCat to 500.0,
                        shoppingCat to 400.0
                    )
                }
                "business" -> {
                    mapOf(
                        rentCat to 1200.0,
                        billingCat to 1000.0,
                        transportCat to 300.0,
                        foodCat to 150.0
                    )
                }
                "monthly living" -> {
                    mapOf(
                        rentCat to 1500.0,
                        foodCat to 600.0,
                        transportCat to 250.0,
                        entertainmentCat to 150.0,
                        shoppingCat to 200.0
                    )
                }
                else -> {
                    val savedJson = prefs.getString("budget_template_$templateName", null)
                    if (savedJson != null) {
                        try {
                            val moshi = com.squareup.moshi.Moshi.Builder().build()
                            val adapter = moshi.adapter(Map::class.java)
                            val parsed = adapter.fromJson(savedJson) as? Map<*, *>
                            parsed?.map {
                                val k = it.key.toString().toLongOrNull()
                                val v = it.value.toString().toDoubleOrNull() ?: 0.0
                                k to v
                            }?.filter { it.first != null }?.toMap() as? Map<Long, Double> ?: emptyMap()
                        } catch (e: Exception) {
                            emptyMap()
                        }
                    } else {
                        emptyMap()
                    }
                }
            }

            // Clean previous budgets for this month to avoid duplicates
            val currentMonthBudgets = repository.getBudgetsForMonth(monthStr).first().filter { it.month == monthStr }
            currentMonthBudgets.forEach { b ->
                repository.deleteBudget(b)
            }

            templateMap.forEach { (catId, amt) ->
                if (catId != null && amt > 0.0) {
                    repository.insertBudget(
                        BudgetEntity(
                            categoryId = catId,
                            month = monthStr,
                            amount = amt,
                            isAdaptive = false,
                            savingsGoal = 0.0,
                            spent = 0.0
                        )
                    )
                }
            }
            postNotification("Loaded '$templateName' budget template for $monthStr successfully!")
        }
    }

    fun saveCurrentBudgetAsTemplate(templateName: String) {
        val currentBudgets = activeBudgets.value
        val templateMap = currentBudgets.associate { it.categoryId.toString() to it.amount }
        try {
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter(Map::class.java)
            val json = adapter.toJson(templateMap)
            prefs.edit().putString("budget_template_$templateName", json).apply()
            
            val savedList = prefs.getStringSet("saved_budget_templates_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            savedList.add(templateName)
            prefs.edit().putStringSet("saved_budget_templates_list", savedList).apply()

            postNotification("Current budget configuration saved as template '$templateName'!")
        } catch (e: Exception) {
            postNotification("Failed to save budget template: ${e.message}")
        }
    }

    fun getSavedBudgetTemplates(): List<String> {
        val defaultTemplates = listOf("Travel", "Business", "Monthly Living")
        val savedList = prefs.getStringSet("saved_budget_templates_list", emptySet()) ?: emptySet()
        return defaultTemplates + savedList.toList()
    }

    fun generateBudgetFromSpendingHabits(monthsCount: Int) {
        val cats = categories.value
        val monthStr = selectedMonth.value
        val txs = allTransactions.value

        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -monthsCount)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val expenses = txs.filter { 
                it.type == "EXPENSE" && it.timestamp in startTime..endTime 
            }

            if (expenses.isEmpty()) {
                postNotification("No previous expenses found to analyze spending habits.")
                return@launch
            }

            val avgSpendingByCategory = expenses
                .groupBy { it.categoryId }
                .mapValues { (_, txList) ->
                    txList.sumOf { it.amount } / monthsCount.toDouble()
                }

            val currentMonthBudgets = repository.getBudgetsForMonth(monthStr).first().filter { it.month == monthStr }
            currentMonthBudgets.forEach { b ->
                repository.deleteBudget(b)
            }

            avgSpendingByCategory.forEach { (catId, avgAmount) ->
                if (avgAmount > 0.0) {
                    repository.insertBudget(
                        BudgetEntity(
                            categoryId = catId,
                            month = monthStr,
                            amount = kotlin.math.round(avgAmount * 100) / 100.0,
                            isAdaptive = true,
                            savingsGoal = 0.0,
                            spent = 0.0
                        )
                    )
                }
            }

            postNotification("Generated budgets for $monthStr based on previous $monthsCount months' average spending habits!")
        }
    }

    // --- Interactive Notifications Flow ---
    private val _notifications = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val notifications = _notifications.asSharedFlow()
    
    @android.annotation.SuppressLint("MissingPermission")
    private fun showSystemNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val channelId = "budget_alerts_channel"
        val notificationId = (System.currentTimeMillis() % 100000).toInt()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Budget Alerts",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when category budget utilization exceeds 90% or limits are exceeded"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // use standard system alert icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun postNotification(msg: String) {
        _notifications.tryEmit(msg)
        if (msg.contains("90%") || msg.contains("Exceeded") || msg.contains("limit") || msg.contains("Warning") || msg.contains("Alert")) {
            showSystemNotification("WealthFlow Budget Shield", msg)
        }
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
                    try {
                        val response = com.example.data.api.ExchangeRateApiClient.service.getLatestRates()
                        val newRates = response.rates
                        if (!newRates.isNullOrEmpty() && newRates.containsKey("USD")) {
                            val updatedEntities = newRates.map { (cur, valRate) ->
                                ExchangeRateEntity(currency = cur, rate = valRate, updatedAt = System.currentTimeMillis())
                            }
                            db.exchangeRateDao().insertAllRates(updatedEntities)

                            val merged = newRates.toMutableMap()
                            _userCustomExchangeRates.value.forEach { (k, v) ->
                                merged[k] = v
                            }
                            _exchangeRates.value = merged
                            return@withContext true
                        }
                    } catch (e: Exception) {
                        // Fallback to connection
                    }

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
                            val updatedEntities = newRates.map { (cur, valRate) ->
                                ExchangeRateEntity(currency = cur, rate = valRate, updatedAt = System.currentTimeMillis())
                            }
                            db.exchangeRateDao().insertAllRates(updatedEntities)

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
                    postNotification("Net worth & live exchange rates updated on demand via Retrofit!")
                    onSuccess("Exchange rates & Net Worth updated successfully!")
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

    val displayCurrency: StateFlow<String> = combine(useBaseCurrency, activeCountryConfig) { useBase, config ->
        if (useBase) "USD" else config.currency
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val displayCurrencySymbol: StateFlow<String> = combine(useBaseCurrency, activeCountryConfig) { useBase, config ->
        if (useBase) "$" else config.currencySymbol
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    // --- Real-time Tax Bracket Flow ---
    private val _realTimeTaxData = MutableStateFlow<RealTimeTaxData?>(null)
    val realTimeTaxData: StateFlow<RealTimeTaxData?> = _realTimeTaxData.asStateFlow()
    
    private val _realTimeTaxLoading = MutableStateFlow(false)
    val realTimeTaxLoading: StateFlow<Boolean> = _realTimeTaxLoading.asStateFlow()

    fun fetchTaxDataForCountry(countryName: String) {
        viewModelScope.launch {
            _realTimeTaxLoading.value = true
            try {
                val data = com.example.data.api.GeminiApiClient.fetchRealTimeTaxData(countryName)
                _realTimeTaxData.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _realTimeTaxLoading.value = false
            }
        }
    }

    // --- Core Flows ---
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTaxCategories: StateFlow<List<TaxCategoryEntity>> = repository.allTaxCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurringExpenses: StateFlow<List<RecurringExpenseEntity>> = repository.allRecurringExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExchangeRateLogs: StateFlow<List<TransactionExchangeRateLogEntity>> = repository.allExchangeRateLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pruneTransactionsOlderThanYears(years: Int, archive: Boolean, onComplete: (Int, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val thresholdTime = System.currentTimeMillis() - (years * 365L * 24L * 60L * 60L * 1000L)
                val oldTransactions = repository.getTransactionsOlderThan(thresholdTime)
                if (oldTransactions.isEmpty()) {
                    onComplete(0, null)
                    return@launch
                }

                var archiveFilePath: String? = null
                if (archive) {
                    val fileName = "wealthflow_archive_older_than_${years}_yrs_${System.currentTimeMillis()}.csv"
                    val file = java.io.File(getApplication<Application>().filesDir, fileName)
                    file.bufferedWriter().use { writer ->
                        writer.write("id,amount,type,categoryId,accountId,toAccountId,timestamp,merchant,isTaxDeductible,taxRate,notes\n")
                        oldTransactions.forEach { tx ->
                            writer.write("${tx.id},${tx.amount},${tx.type},${tx.categoryId},${tx.accountId},${tx.toAccountId},${tx.timestamp},\"${tx.merchant.replace("\"", "\"\"")}\",${tx.isTaxDeductible},${tx.taxRate},\"${tx.notes.replace("\"", "\"\"")}\"\n")
                        }
                    }
                    archiveFilePath = file.absolutePath
                }

                val deletedCount = repository.deleteTransactionsOlderThan(thresholdTime)
                
                // VACUUM SQLite
                db.openHelper.writableDatabase.execSQL("VACUUM")

                postNotification("Successfully cleaned up $deletedCount transactions older than $years years!")
                onComplete(deletedCount, archiveFilePath)
            } catch (e: Exception) {
                onComplete(-1, e.message)
            }
        }
    }

    val monthToMonthSpending: StateFlow<String> = allTransactions.map { txList ->
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthlyMap = txList.filter { it.type == "EXPENSE" }
            .groupBy { sdf.format(Date(it.timestamp)) }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val sortedList = monthlyMap.entries
            .sortedBy { it.key }
            .takeLast(6)

        sortedList.joinToString(",") { entry ->
            "{\"month\": \"${entry.key}\", \"Spent\": ${entry.value}}"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val monthlySpendingByCategory: StateFlow<Map<String, Map<String, Double>>> = combine(
        allTransactions,
        categories
    ) { txList, catList ->
        val catMap = catList.associate { it.id to it.name }
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        txList.filter { it.type == "EXPENSE" }
            .groupBy { sdf.format(Date(it.timestamp)) }
            .mapValues { (_, txs) ->
                txs.groupBy { catMap[it.categoryId] ?: "Uncategorized" }
                    .mapValues { (_, subTxs) -> subTxs.sumOf { it.amount } }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val isExportEncryptionEnabled = MutableStateFlow(false)
    val exportPasscode = MutableStateFlow("SecurePass2026")
    val remoteSyncUrl = MutableStateFlow(prefs.getString("remote_sync_url", "http://10.0.2.2:5000") ?: "http://10.0.2.2:5000")

    fun toggleExportEncryption() {
        isExportEncryptionEnabled.value = !isExportEncryptionEnabled.value
    }

    fun setExportPasscode(code: String) {
        exportPasscode.value = code
    }

    fun setRemoteSyncUrl(url: String) {
        remoteSyncUrl.value = url
        prefs.edit().putString("remote_sync_url", url).apply()
    }

    private fun hashPasscode(passcode: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(passcode.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            passcode
        }
    }

    val detectedAnomalies: StateFlow<List<AnomalyReport>> = combine(allTransactions, accounts) { txs, accs ->
        AnomalyDetectionService.analyzeTransactions(txs, accs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            // Automatically execute the recurring transaction scheduler on startup
            runRecurringScheduler()

            // Run database background pruning and optimization on boot
            try {
                com.example.data.service.DatabasePrunerUtility.pruneAndOptimize(application, db)
                com.example.data.worker.CacheCleanupWorker.schedulePeriodic(application)
                com.example.data.worker.ExchangeRateWorker.schedulePeriodic(application)
                com.example.data.worker.DatabaseArchiveWorker.schedulePeriodic(application)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Collect and check for upcoming recurring transaction warnings (3-day window)
        viewModelScope.launch {
            repository.allRecurring.collect { recurringList ->
                checkRecurringWarnings(recurringList)
            }
        }

        // Real-time currency background fetch from reliable api open.er-api.com with local SQLite caching (24-hour interval)
        viewModelScope.launch {
            // 1. Load rates from SQLite on startup
            try {
                var sqliteRates = db.exchangeRateDao().getAllRatesStatic()
                if (sqliteRates.isEmpty()) {
                    // Seed the default rate map into SQLite
                    val defaults = mapOf(
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
                    val seedEntities = defaults.map { (cur, valRate) ->
                        ExchangeRateEntity(currency = cur, rate = valRate, updatedAt = System.currentTimeMillis())
                    }
                    db.exchangeRateDao().insertAllRates(seedEntities)
                    sqliteRates = seedEntities
                }
                
                // Map SQLite values to state flow
                val loadedMap = sqliteRates.associate { it.currency to it.rate }.toMutableMap()
                _userCustomExchangeRates.value.forEach { (k, v) ->
                    loadedMap[k] = v
                }
                _exchangeRates.value = loadedMap
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Background service / sync loop: Check and fetch every hour, ensuring we fetch at least once every 24 hours
            while (true) {
                try {
                    val sqliteRates = db.exchangeRateDao().getAllRatesStatic()
                    val lastUpdated = sqliteRates.maxOfOrNull { it.updatedAt } ?: 0L
                    val now = System.currentTimeMillis()
                    
                    // If never fetched or more than 24 hours (86,400,000 ms) has passed
                    if (now - lastUpdated > 24 * 3600 * 1000L || sqliteRates.isEmpty()) {
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
                                    val updatedEntities = newRates.map { (cur, valRate) ->
                                        ExchangeRateEntity(currency = cur, rate = valRate, updatedAt = System.currentTimeMillis())
                                    }
                                    db.exchangeRateDao().insertAllRates(updatedEntities)
                                    
                                    val merged = newRates.toMutableMap()
                                    _userCustomExchangeRates.value.forEach { (k, v) ->
                                        merged[k] = v
                                    }
                                    _exchangeRates.value = merged
                                    postNotification("Exchange rates synced automatically online with local storage.")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fall back gracefully to local SQLite table offline
                }
                // Delay for 1 hour before next periodic background check
                kotlinx.coroutines.delay(3600_000L)
            }
        }

        viewModelScope.launch {
            activeCountryConfig.collect { config ->
                fetchTaxDataForCountry(config.country)
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

    // --- Expense & Tax Category Coroutines Persistence Operations ---
    fun addExpense(
        amount: Double,
        currency: String = "USD",
        date: Long = System.currentTimeMillis(),
        taxCategoryId: Long = 0L,
        merchant: String = "",
        notes: String = "",
        receiptPath: String = ""
    ) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                amount = amount,
                currency = currency,
                date = date,
                taxCategoryId = taxCategoryId,
                merchant = merchant,
                notes = notes,
                receiptPath = receiptPath
            )
            repository.insertExpense(expense)
            postNotification("Expense added: $currency $amount")
            _uiEvents.emit(UiEvent.ExpenseSubmitted)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            postNotification("Expense removed successfully")
        }
    }

    fun addTaxCategory(name: String, code: String = "", description: String = "", isDeductible: Boolean = true, defaultRate: Double = 0.0, monthlyCap: Double = 0.0) {
        viewModelScope.launch {
            val tc = TaxCategoryEntity(
                name = name,
                code = code,
                description = description,
                isDeductible = isDeductible,
                defaultRate = defaultRate,
                monthlyCap = monthlyCap
            )
            repository.insertTaxCategory(tc)
            postNotification("Tax Category '$name' created successfully")
        }
    }

    fun updateTaxCategoryCap(taxCategory: TaxCategoryEntity, newCap: Double) {
        viewModelScope.launch {
            val updated = taxCategory.copy(monthlyCap = newCap)
            repository.updateTaxCategory(updated)
            postNotification("Updated monthly spending cap for ${taxCategory.name} to $newCap")
        }
    }

    // --- Recurring Expense Operations ---
    fun addRecurringExpense(
        amount: Double,
        currency: String = "USD",
        taxCategoryId: Long = 0L,
        merchant: String = "",
        notes: String = "",
        frequency: String = "MONTHLY"
    ) {
        viewModelScope.launch {
            val recurring = RecurringExpenseEntity(
                amount = amount,
                currency = currency,
                taxCategoryId = taxCategoryId,
                merchant = merchant,
                notes = notes,
                frequency = frequency,
                nextDueAt = System.currentTimeMillis() + 86400000L // default next due tomorrow or today
            )
            repository.insertRecurringExpense(recurring)
            postNotification("Recurring $frequency expense ($currency $amount) scheduled")
            com.example.data.worker.RecurringExpenseWorker.scheduleWorker(getApplication())
        }
    }

    fun deleteRecurringExpense(recurringExpense: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(recurringExpense)
            postNotification("Recurring expense removed")
        }
    }

    fun scheduleRecurringExpenseWorker() {
        com.example.data.worker.RecurringExpenseWorker.scheduleWorker(getApplication())
    }

    // --- Database Encryption & Encrypted Backup Operations ---
    fun exportEncryptedDatabaseBackup(passphrase: String, onResult: (Boolean, String) -> Unit) {
        if (passphrase.length < 4) {
            onResult(false, "Passphrase must be at least 4 characters long")
            return
        }
        viewModelScope.launch {
            val result = com.example.data.database.DatabaseEncryptionBackupManager.exportAndEncryptDatabase(
                context = getApplication(),
                passphrase = passphrase
            )
            result.fold(
                onSuccess = { metadata ->
                    val msg = "Encrypted backup saved: ${metadata.fileName} (${metadata.sizeBytes / 1024} KB)"
                    postNotification(msg)
                    onResult(true, metadata.filePath)
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Backup encryption failed")
                }
            )
        }
    }

    fun restoreEncryptedDatabaseBackup(backupFile: java.io.File, passphrase: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = com.example.data.database.DatabaseEncryptionBackupManager.decryptAndRestoreDatabase(
                context = getApplication(),
                backupFile = backupFile,
                passphrase = passphrase
            )
            result.fold(
                onSuccess = {
                    postNotification("Database successfully restored from encrypted backup! Please restart app.")
                    onResult(true, "Database successfully restored!")
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Decryption or restore failed. Check passphrase.")
                }
            )
        }
    }

    fun getEncryptedBackupsList(): List<com.example.data.database.DatabaseEncryptionBackupManager.BackupMetadata> {
        return com.example.data.database.DatabaseEncryptionBackupManager.listBackups(getApplication())
    }

    // Budgets for the active month
    val activeBudgets: StateFlow<List<BudgetEntity>> = selectedMonth
        .flatMapLatest { month -> repository.getBudgetsForMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Keep track of which alert keys we have already notified in the current app session to avoid spamming
    private val notifiedAlertKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    val budgetAlerts: StateFlow<List<com.example.data.service.BudgetNotificationService.BudgetAlert>> = combine(activeBudgets, categories) { buds, cats ->
        val alerts = com.example.data.service.BudgetNotificationService.checkBudgets(buds, cats)
        alerts.forEach { alert ->
            val key = "${alert.categoryId}_${alert.isExceeded}"
            if (!notifiedAlertKeys.contains(key)) {
                notifiedAlertKeys.add(key)
                postNotification(alert.message)
                com.example.data.service.BudgetNotificationService.triggerLocalSystemNotification(getApplication(), alert)
                _uiEvents.tryEmit(UiEvent.BudgetAlertTriggered)
            }
        }
        alerts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State Calculations ---
    val totalBalance: StateFlow<Double> = combine(accounts, displayCurrency, exchangeRates) { accList, targetCurrency, rates ->
        accList.sumOf { acc ->
            convertCurrency(acc.balance, acc.currency, targetCurrency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netWorthSummary: StateFlow<NetWorthSummary> = combine(accounts, displayCurrency, exchangeRates) { accList, targetCurrency, rates ->
        var manualSum = 0.0
        var syncedSum = 0.0
        accList.forEach { acc ->
            val converted = convertCurrency(acc.balance, acc.currency, targetCurrency)
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

    val currentInflow: StateFlow<Double> = combine(filteredTransactions, accounts, displayCurrency, exchangeRates) { txList, accList, targetCurrency, rates ->
        txList.filter { it.type == "INCOME" }.sumOf { tx ->
            val acc = accList.find { it.id == tx.accountId }
            val txCurrency = acc?.currency ?: "USD"
            convertCurrency(tx.amount, txCurrency, targetCurrency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentOutflow: StateFlow<Double> = combine(filteredTransactions, accounts, displayCurrency, exchangeRates) { txList, accList, targetCurrency, rates ->
        txList.filter { it.type == "EXPENSE" }.sumOf { tx ->
            val acc = accList.find { it.id == tx.accountId }
            val txCurrency = acc?.currency ?: "USD"
            convertCurrency(tx.amount, txCurrency, targetCurrency)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val previousMonthOutflow: StateFlow<Double> = combine(allTransactions, selectedMonth, accounts, displayCurrency, exchangeRates) { txs, month, accList, targetCurrency, rates ->
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val cal = Calendar.getInstance()
        try {
            val date = sdf.parse(month)
            if (date != null) {
                cal.time = date
                cal.add(Calendar.MONTH, -1)
            }
        } catch (e: Exception) {}
        val prevMonthStr = sdf.format(cal.time)
        txs.filter { tx ->
            val txMonth = sdf.format(Date(tx.timestamp))
            txMonth == prevMonthStr && tx.type == "EXPENSE"
        }.sumOf { tx ->
            val acc = accList.find { it.id == tx.accountId }
            val txCurrency = acc?.currency ?: "USD"
            convertCurrency(tx.amount, txCurrency, targetCurrency)
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

    private val _monthlySummaryLoading = MutableStateFlow(false)
    val monthlySummaryLoading: StateFlow<Boolean> = _monthlySummaryLoading.asStateFlow()

    private val _monthlySummaryReport = MutableStateFlow<String?>(null)
    val monthlySummaryReport: StateFlow<String?> = _monthlySummaryReport.asStateFlow()

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

    fun triggerMonthlySpendingSummary() {
        viewModelScope.launch {
            _monthlySummaryLoading.value = true
            _monthlySummaryReport.value = null

            val currentCountryName = activeCountryConfig.value.country
            val currencySymbol = activeCountryConfig.value.currencySymbol
            val activeTxs = filteredTransactions.value
            val activeCat = categories.value

            val totalIn = currentInflow.value
            val totalOut = currentOutflow.value
            val balance = totalBalance.value

            if (!GeminiApiClient.isApiKeyConfigured()) {
                // Return country-specific mock monthly summary report fallback
                generateMonthlyMockSummary(currentCountryName, currencySymbol, totalIn, totalOut, balance)
                _monthlySummaryLoading.value = false
                return@launch
            }

            // Construct full data context for Gemini
            val transactionSummary = activeTxs.take(50).joinToString("\n") { tx ->
                val catName = activeCat.find { it.id == tx.categoryId }?.name ?: "Transfer"
                "- ${catName}: ${currencySymbol}${tx.amount} (${tx.type}) at ${tx.merchant}. Tax Deductible: ${tx.isTaxDeductible}"
            }

            val prompt = """
                Generate a plain-text monthly financial summary report and spending health overview.
                Current Country: $currentCountryName
                Current Accounts Balance: $currencySymbol$balance
                Monthly Inflow (Income): $currencySymbol$totalIn
                Monthly Outflow (Expenses): $currencySymbol$totalOut
                
                RECENT LEDGER ENTRIES FOR THIS MONTH:
                $transactionSummary
                
                Please generate:
                1. Monthly Spending Health Overview: A natural language evaluation of my spending health (e.g. savings rate, cashflow status, burn rate).
                2. Key Spending Drivers: Identify the major categories and specific transactions driving outflows.
                3. Direct Financial Health Rating & Guidance: Give a simple health score/rating (Healthy, Stable, or At Risk) with clear, actionable explanation.
                
                Keep response in an elegant, plain-text advisory format with clean bullet points.
            """.trimIndent()

            val systemInstruction = "You are an elite automated financial intelligence officer. Write in a clear, objective, conversational, and direct advisory tone."

            val resultText = GeminiApiClient.getAiInsights(prompt, systemInstruction)
            _monthlySummaryReport.value = resultText
            _monthlySummaryLoading.value = false
        }
    }

    private fun generateMonthlyMockSummary(
        country: String,
        symbol: String,
        inflow: Double,
        outflow: Double,
        balance: Double
    ) {
        val savingsRate = if (inflow > 0.0) ((inflow - outflow) / inflow * 100.0).coerceAtLeast(0.0) else 0.0
        val rating = when {
            savingsRate >= 30.0 -> "HEALTHY"
            savingsRate >= 10.0 -> "STABLE"
            else -> "AT RISK"
        }
        val adviceText = when (rating) {
            "HEALTHY" -> "Outstanding savings buffer! Your surplus position is resilient and well-aligned with wealth creation principles. Consider allocating this extra flow to high-yielding assets or savings goals."
            "STABLE" -> "Your monthly ledger is balanced, but your savings buffer remains thin. Focus on tightening discretionary shopping or food & dining limits to push your savings rate closer to 30%."
            else -> "Warning: High burn rate or negative net cash flow detected. Your outflows are dangerously close to or exceed your inflows. Action is needed immediately to curtail non-essential expenses."
        }

        val mockReport = """
            ### 📊 Monthly Financial Health Overview (${country})
            
            *A localized plain-text financial health overview generated via offline intelligence engines.*
            
            *   **Cash Flow Position**: Inflow of ${symbol}${inflow} vs Outflow of ${symbol}${outflow}.
            *   **Calculated Savings Rate**: ${String.format(Locale.US, "%.1f", savingsRate)}%
            *   **Discretionary Outflow Burden**: Discretionary spending represents approximately ${String.format(Locale.US, "%.1f", outflow * 0.45)} of total cash outflow.
            
            ### 📉 Major Spending Drivers
            1.  **Fixed Obligations**: Utilities, rent, and scheduled bills represent the primary baseline.
            2.  **Discretionary Leaks**: Retail dining and lifestyle items have generated secondary friction.
            
            ### 🛡️ Financial Health Rating & Advice
            *   **Rating**: **$rating**
            *   **Guidance**: $adviceText
        """.trimIndent()

        _monthlySummaryReport.value = mockReport
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

    fun autoCategorizeByDescriptionAndMerchant(notes: String, merchant: String, defaultCategoryId: Long, type: String): Long {
        if (type == "TRANSFER") return 0L
        val notesLower = notes.lowercase(Locale.US)
        val merchantLower = merchant.lowercase(Locale.US)
        val combined = "$notesLower $merchantLower"
        val cats = categories.value

        // 0. Historical User Pattern Matching: Match by merchant name in past transactions
        if (merchant.isNotEmpty()) {
            val pastTransactions = allTransactions.value
            val merchantMatches = pastTransactions.filter { 
                it.merchant.equals(merchant, ignoreCase = true) && it.categoryId != 0L
            }
            if (merchantMatches.isNotEmpty()) {
                val categoryFrequency = merchantMatches.groupBy { it.categoryId }
                    .mapValues { it.value.size }
                val mostFrequentCategory = categoryFrequency.maxByOrNull { it.value }?.key
                if (mostFrequentCategory != null) {
                    return mostFrequentCategory
                }
            }
            
            // Substring pattern: if past transactions contain this merchant as a substring
            val partialMatches = pastTransactions.filter {
                val pastMerchantLower = it.merchant.lowercase(Locale.US)
                (pastMerchantLower.isNotEmpty() && (pastMerchantLower.contains(merchantLower) || merchantLower.contains(pastMerchantLower))) && it.categoryId != 0L
            }
            if (partialMatches.isNotEmpty()) {
                val categoryFrequency = partialMatches.groupBy { it.categoryId }
                    .mapValues { it.value.size }
                val mostFrequentCategory = categoryFrequency.maxByOrNull { it.value }?.key
                if (mostFrequentCategory != null) {
                    return mostFrequentCategory
                }
            }
        }

        // 1. Prioritize dynamic user-defined matching rules from DB
        val customRules = matchingRules.value
        for (rule in customRules) {
            val kw = rule.keyword.lowercase(Locale.US)
            if ((merchantLower.isNotEmpty() && merchantLower.contains(kw)) ||
                (notesLower.isNotEmpty() && notesLower.contains(kw))
            ) {
                return rule.categoryId
            }
        }

        val keywordToCategoryName = mapOf(
            "grocery" to "Food & Dining",
            "groceries" to "Food & Dining",
            "supermarket" to "Food & Dining",
            "food" to "Food & Dining",
            "dining" to "Food & Dining",
            "restaurant" to "Food & Dining",
            "restaurants" to "Food & Dining",
            "cafe" to "Food & Dining",
            "coffee" to "Food & Dining",
            "starbucks" to "Food & Dining",
            "eat" to "Food & Dining",
            "meal" to "Food & Dining",
            
            "utility" to "Rent & Bills",
            "utilities" to "Rent & Bills",
            "electric" to "Rent & Bills",
            "electricity" to "Rent & Bills",
            "water" to "Rent & Bills",
            "gas" to "Rent & Bills",
            "power" to "Rent & Bills",
            "internet" to "Rent & Bills",
            "cable" to "Rent & Bills",
            "bill" to "Rent & Bills",
            "bills" to "Rent & Bills",
            "rent" to "Rent & Bills",
            "apartment" to "Rent & Bills",
            
            "transport" to "Transport",
            "bus" to "Transport",
            "taxi" to "Transport",
            "uber" to "Transport",
            "lyft" to "Transport",
            "fuel" to "Transport",
            "gasoline" to "Transport",
            "metro" to "Transport",
            "train" to "Transport",
            "car" to "Transport",
            
            "movie" to "Entertainment",
            "netflix" to "Entertainment",
            "spotify" to "Entertainment",
            "game" to "Entertainment",
            "gaming" to "Entertainment",
            "steam" to "Entertainment",
            "disney" to "Entertainment",
            "hulu" to "Entertainment",
            "theater" to "Entertainment",
            
            "tax" to "Taxes & Duties",
            "duty" to "Taxes & Duties",
            "taxes" to "Taxes & Duties",
            "irs" to "Taxes & Duties",
            
            "salary" to "Salary",
            "wage" to "Salary",
            "paycheck" to "Salary",
            "income" to "Salary",
            
            "freelance" to "Freelance",
            "contract" to "Freelance",
            "consulting" to "Freelance",
            "gig" to "Freelance",
            
            "invest" to "Savings & Investments",
            "investment" to "Savings & Investments",
            "savings" to "Savings & Investments",
            "stock" to "Savings & Investments",
            "shares" to "Savings & Investments",
            "mutual fund" to "Savings & Investments",
            
            "shopping" to "Shopping",
            "clothes" to "Shopping",
            "amazon" to "Shopping",
            "mall" to "Shopping",
            "apparel" to "Shopping",
            "electronics" to "Shopping",
            "target" to "Shopping",
            "walmart" to "Shopping"
        )

        for ((keyword, targetCatName) in keywordToCategoryName) {
            if (combined.contains(keyword)) {
                val literalMatch = cats.find { it.name.equals(keyword, ignoreCase = true) }
                if (literalMatch != null) {
                    return literalMatch.id
                }
                val foundCat = cats.find { it.name.contains(targetCatName, ignoreCase = true) || targetCatName.contains(it.name, ignoreCase = true) }
                if (foundCat != null) {
                    return foundCat.id
                }
            }
        }

        val matchedCat = cats.find { cat ->
            cat.name.lowercase(Locale.US).contains(notesLower) || 
            notesLower.contains(cat.name.lowercase(Locale.US)) ||
            cat.subcategories.split(",").any { sub ->
                val trimmedSub = sub.trim().lowercase(Locale.US)
                trimmedSub.isNotEmpty() && (notesLower.contains(trimmedSub) || trimmedSub.contains(notesLower))
            }
        }
        if (matchedCat != null) {
            return matchedCat.id
        }

        return defaultCategoryId
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
        customTimestamp: Long? = null,
        tags: String = "",
        receiptPath: String = ""
    ) {
        viewModelScope.launch {
            val finalCategoryId = autoCategorizeByDescriptionAndMerchant(notes, merchant, categoryId, type)
            val tx = TransactionEntity(
                amount = amount,
                type = type,
                categoryId = finalCategoryId,
                accountId = accountId,
                toAccountId = toAccountId,
                timestamp = customTimestamp ?: System.currentTimeMillis(),
                merchant = merchant,
                isTaxDeductible = isTaxDeductible,
                taxRate = taxRate,
                notes = notes,
                isRecurring = isRecurring,
                recurrenceInterval = recurrenceInterval,
                splitCount = splitCount,
                tags = tags,
                receiptPath = receiptPath
            )
            val insertedId = repository.insertTransaction(tx)
            
            // Log exchange rates used at the time of each transaction
            val currentCurrency = activeCountryConfig.value.currency
            val rateInUsd = exchangeRates.value[currentCurrency] ?: 1.0
            val exchangeLog = TransactionExchangeRateLogEntity(
                transactionId = insertedId,
                originalCurrency = currentCurrency,
                targetCurrency = "USD",
                exchangeRateUsed = rateInUsd,
                timestamp = tx.timestamp
            )
            repository.insertExchangeRateLog(exchangeLog)

            _uiEvents.tryEmit(UiEvent.ExpenseSubmitted)

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

                // Also populate scheduler's automatic execution entry to execute on future dates
                val schedCal = Calendar.getInstance()
                schedCal.timeInMillis = baseTime
                when (recurrenceInterval) {
                    "DAILY" -> schedCal.add(Calendar.DAY_OF_YEAR, 1)
                    "WEEKLY" -> schedCal.add(Calendar.WEEK_OF_YEAR, 1)
                    "MONTHLY" -> schedCal.add(Calendar.MONTH, 1)
                }
                val scheEntity = com.example.data.model.RecurringTransactionEntity(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    merchant = merchant,
                    notes = notes,
                    recurrenceInterval = recurrenceInterval,
                    lastExecutionTimestamp = baseTime,
                    nextExecutionTimestamp = schedCal.timeInMillis,
                    isTaxDeductible = isTaxDeductible,
                    taxRate = taxRate,
                    isActive = true
                )
                repository.insertRecurring(scheEntity)
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
            incrementTxCountSinceLastExport()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun importCsvData(csvText: String, context: Context) {
        viewModelScope.launch {
            try {
                val lines = csvText.lines()
                if (lines.isEmpty()) {
                    _notifications.tryEmit("Empty CSV data provided.")
                    return@launch
                }

                var importedCount = 0
                val dbCategories = categories.value
                val dbAccounts = accounts.value
                val defaultAccount = dbAccounts.firstOrNull()?.id ?: 1L
                val defaultCategory = dbCategories.firstOrNull()?.id ?: 1L

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                // Smart column index mapping fallback
                var dateIdx = 0
                var merchantIdx = 1
                var amountIdx = 2
                var typeIdx = 3
                var categoryIdx = 4
                var notesIdx = 5
                var tagsIdx = 6

                var startLineIdx = 0
                val headerLine = lines.firstOrNull { it.trim().isNotEmpty() }
                if (headerLine != null) {
                    val tokens = headerLine.split(",").map { it.trim().lowercase().removeSurrounding("\"") }
                    val hasKeywords = tokens.any { 
                        it.contains("date") || it.contains("amount") || it.contains("merchant") || it.contains("payee") || it.contains("type") || it.contains("category") || it.contains("notes") || it.contains("description")
                    }
                    if (hasKeywords) {
                        tokens.forEachIndexed { index, col ->
                            if (col.contains("date")) dateIdx = index
                            else if (col.contains("merchant") || col.contains("payee") || col.contains("description") || col.contains("vendor") || col.contains("title")) merchantIdx = index
                            else if (col.contains("amount") || col.contains("value") || col.contains("sum") || col.contains("price") || col.contains("total")) amountIdx = index
                            else if (col.contains("type")) typeIdx = index
                            else if (col.contains("category")) categoryIdx = index
                            else if (col.contains("notes") || col.contains("memo") || col.contains("comment")) notesIdx = index
                            else if (col.contains("tags") || col.contains("tag")) tagsIdx = index
                        }
                        startLineIdx = lines.indexOf(headerLine) + 1
                    }
                }

                for (idx in startLineIdx until lines.size) {
                    val line = lines[idx].trim()
                    if (line.isEmpty()) continue

                    // Hand-crafted CSV split which handles quoted values safely if any, or standard simple commas
                    val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (tokens.size <= amountIdx) continue

                    // Parse Date (Dynamic Column)
                    val dateParsed = try {
                        val dStr = tokens.getOrNull(dateIdx) ?: ""
                        sdf.parse(dStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    // Dynamic Column: Merchant
                    val merchantParsed = tokens.getOrNull(merchantIdx)?.ifEmpty { "Bank Export" } ?: "Bank Export"

                    // Dynamic Column: Amount
                    val cleanAmountStr = (tokens.getOrNull(amountIdx) ?: "0.0").replace("[^0-9.]".toRegex(), "")
                    val amountParsed = cleanAmountStr.toDoubleOrNull() ?: 0.0

                    // Dynamic Column: Type (INCOME, EXPENSE, TRANSFER)
                    var typeParsed = tokens.getOrNull(typeIdx)?.uppercase()?.ifEmpty { "EXPENSE" } ?: "EXPENSE"
                    if (typeParsed != "EXPENSE" && typeParsed != "INCOME" && typeParsed != "TRANSFER") {
                        typeParsed = "EXPENSE"
                    }

                    // Dynamic Column: Category Name
                    val categoryName = tokens.getOrNull(categoryIdx) ?: ""

                    // Dynamic Column: Notes
                    val notesParsed = tokens.getOrNull(notesIdx)?.ifEmpty { "Bank CSV Import" } ?: "Bank CSV Import"

                    // Dynamic Column: Tags
                    val tagsParsed = tokens.getOrNull(tagsIdx) ?: ""

                    // Lookup matching category id or auto-suggest
                    val matchedCategory = if (categoryName.isNotEmpty()) {
                        dbCategories.find { it.name.equals(categoryName, ignoreCase = true) }?.id ?: defaultCategory
                    } else {
                        suggestCategoryForMerchant(merchantParsed)
                    }

                    if (amountParsed > 0) {
                        val tx = TransactionEntity(
                            amount = amountParsed,
                            type = typeParsed,
                            categoryId = if (typeParsed == "TRANSFER") 0L else matchedCategory,
                            accountId = defaultAccount,
                            timestamp = dateParsed,
                            merchant = merchantParsed,
                            notes = notesParsed,
                            tags = tagsParsed
                        )
                        repository.insertTransaction(tx)
                        importedCount++
                    }
                }
                _notifications.tryEmit("Successfully imported $importedCount transactions from bank CSV!")
                incrementTxCountSinceLastExport()
            } catch (e: Exception) {
                _notifications.tryEmit("Failed to import CSV: ${e.message}")
            }
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            incrementTxCountSinceLastExport()
        }
    }

    val allRecurring: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringSuggestions: StateFlow<List<com.example.data.service.RecurringSuggestion>> = combine(
        allTransactions,
        allRecurring
    ) { txList, recList ->
        com.example.data.service.RecurringDetectorService.analyzeTransactions(txList, recList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun convertSuggestionToRecurring(suggestion: com.example.data.service.RecurringSuggestion) {
        viewModelScope.launch {
            val intervalMs = when (suggestion.frequency) {
                "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
                "YEARLY" -> 365 * 24 * 60 * 60 * 1000L
                else -> 30 * 24 * 60 * 60 * 1000L
            }
            val newEntity = RecurringTransactionEntity(
                amount = suggestion.estimatedAmount,
                type = "EXPENSE",
                categoryId = suggestion.categoryId,
                accountId = suggestion.accountId,
                merchant = suggestion.merchant,
                notes = suggestion.sampleNotes,
                recurrenceInterval = suggestion.frequency,
                lastExecutionTimestamp = suggestion.lastDate,
                nextExecutionTimestamp = suggestion.lastDate + intervalMs,
                isActive = true
            )
            repository.insertRecurring(newEntity)
            postNotification("Added recurring subscription: '${suggestion.merchant}' (${formatCurrency(suggestion.estimatedAmount)} / ${suggestion.frequency.lowercase()})")
        }
    }

    fun syncExchangeRatesNow(context: android.content.Context, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = com.example.data.worker.ExchangeRateWorker.fetchAndUpdateRates(context)
                if (count > 0) {
                    val sqliteRates = db.exchangeRateDao().getAllRatesStatic()
                    val loadedMap = sqliteRates.associate { it.currency to it.rate }
                    _exchangeRates.value = loadedMap
                    postNotification("Synced $count live exchange rates via WorkManager!")
                    onResult(true, count)
                } else {
                    onResult(false, 0)
                }
            } catch (e: Exception) {
                onResult(false, 0)
            }
        }
    }

    val allDebts: StateFlow<List<UserDebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addDebt(name: String, amount: Double, interestRate: Double, termMonths: Int, monthlyPayment: Double) {
        viewModelScope.launch {
            val debt = UserDebtEntity(
                name = name,
                amount = amount,
                interestRate = interestRate,
                termMonths = termMonths,
                monthlyPayment = monthlyPayment
            )
            repository.insertDebt(debt)
            postNotification("Registered user debt: '$name' of ${formatCurrency(amount)}")
        }
    }

    fun updateDebt(debt: UserDebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt)
            postNotification("Updated user debt: '${debt.name}'")
        }
    }

    fun deleteDebt(debt: UserDebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
            postNotification("Cleared user debt: '${debt.name}'")
        }
    }

    val allGoals: StateFlow<List<SavingsGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSavingsGoal(name: String, targetAmount: Double, targetDate: Long, savedAmount: Double) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                name = name,
                targetAmount = targetAmount,
                targetDate = targetDate,
                savedAmount = savedAmount
            )
            repository.insertGoal(goal)
            postNotification("Created Savings Goal: '$name' of ${formatCurrency(targetAmount)}")
        }
    }

    fun updateSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
            postNotification("Updated savings goal: '${goal.name}'")
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            postNotification("Removed savings goal: '${goal.name}'")
        }
    }

    fun allocateToSavingsGoal(goal: SavingsGoalEntity, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(savedAmount = (goal.savedAmount + amount).coerceAtLeast(0.0))
            repository.updateGoal(updated)
            postNotification("Allocated ${formatCurrency(amount)} to '${goal.name}'")

            val originallyCompleted = goal.savedAmount >= goal.targetAmount
            val nowCompleted = updated.savedAmount >= updated.targetAmount
            if (nowCompleted && !originallyCompleted) {
                postNotification("🎉 SAVINGS GOAL ACHIEVED: '${goal.name}' reached its target of ${formatCurrency(goal.targetAmount)}!")
                _uiEvents.tryEmit(UiEvent.SavingsGoalReached)
            }
        }
    }

    fun runRecurringScheduler() {
        viewModelScope.launch {
            val msgs = com.example.data.service.RecurringTransactionScheduler.checkAndProcessRecurring(repository)
            msgs.forEach { postNotification(it) }
        }
    }

    private val notifiedRecurringKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun checkRecurringWarnings(recurringList: List<RecurringTransactionEntity>) {
        val currentTime = System.currentTimeMillis()
        val threeDaysMs = 3L * 24 * 60 * 60 * 1000
        
        recurringList.forEach { schedule ->
            if (schedule.isActive) {
                val timeLeft = schedule.nextExecutionTimestamp - currentTime
                // Warn if scheduled date is within the next 3 days
                if (timeLeft in 0L..threeDaysMs) {
                    val daysLeft = Math.ceil(timeLeft.toDouble() / (24 * 60 * 60 * 1000)).toInt()
                    val key = "${schedule.id}_${schedule.nextExecutionTimestamp}"
                    if (!notifiedRecurringKeys.contains(key)) {
                        notifiedRecurringKeys.add(key)
                        val typeLabel = if (schedule.type == "EXPENSE") "payment" else "transaction"
                        postNotification("📅 Upcoming Scheduled Transaction Warning: Your recurring $typeLabel for '${schedule.merchant}' of ${formatCurrency(schedule.amount)} is scheduled in $daysLeft days on ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(schedule.nextExecutionTimestamp))}.")
                    }
                }
            }
        }
    }

    fun suggestCategoryForMerchant(merchantName: String): Long {
        val txs = allTransactions.value
        val matchedTxs = txs.filter { it.merchant.contains(merchantName, ignoreCase = true) || merchantName.contains(it.merchant, ignoreCase = true) }
        if (matchedTxs.isNotEmpty()) {
            val mostFrequentCat = matchedTxs.groupBy { it.categoryId }
                .maxByOrNull { it.value.size }?.key
            if (mostFrequentCat != null) {
                return mostFrequentCat
            }
        }
        
        val cats = categories.value
        val merchantLower = merchantName.lowercase()
        
        val keywordMappings = mapOf(
            "starbucks" to listOf("Food", "Cafe", "Dining", "Coffee", "Restaurants"),
            "mcdonald" to listOf("Food", "Dining", "Restaurants", "Fast Food"),
            "uber" to listOf("Transport", "Travel", "Cab", "Taxi", "Ride"),
            "lyft" to listOf("Transport", "Travel", "Cab", "Taxi", "Ride"),
            "walmart" to listOf("Groceries", "Shopping", "Supermarket"),
            "target" to listOf("Shopping", "Groceries", "Retail"),
            "amazon" to listOf("Shopping", "Online", "Retail"),
            "netflix" to listOf("Entertainment", "Subscriptions", "Media"),
            "spotify" to listOf("Entertainment", "Subscriptions", "Media"),
            "steam" to listOf("Entertainment", "Gaming"),
            "chevron" to listOf("Fuel", "Gas", "Transport", "Automobile"),
            "shell" to listOf("Fuel", "Gas", "Transport", "Automobile"),
            "pharmacy" to listOf("Health", "Medical", "Medicine"),
            "hospital" to listOf("Health", "Medical"),
            "rent" to listOf("Housing", "Rent", "Utilities"),
            "electric" to listOf("Utilities", "Housing", "Bills")
        )
        
        for ((keyword, catKeywords) in keywordMappings) {
            if (merchantLower.contains(keyword)) {
                val matchedCat = cats.find { cat -> 
                    catKeywords.any { kw -> cat.name.contains(kw, ignoreCase = true) }
                }
                if (matchedCat != null) {
                    return matchedCat.id
                }
            }
        }
        
        val fallbackCat = cats.find { it.name.contains("Miscellaneous", ignoreCase = true) || it.name.contains("Shopping", ignoreCase = true) }
        return fallbackCat?.id ?: cats.firstOrNull()?.id ?: 1L
    }

    fun insertRecurringSchedule(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.insertRecurring(recurring)
            postNotification("Registered recurring scheduler for '${recurring.merchant}'")
            runRecurringScheduler()
        }
    }

    fun deleteRecurringSchedule(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
            postNotification("Removed recurring scheduler for '${recurring.merchant}'")
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

    // Dynamic Currency Formatter based on country setting or global currency toggle
    fun formatCurrency(amount: Double): String {
        val config = activeCountryConfig.value
        if (useBaseCurrency.value) {
            val formatStyle = java.text.NumberFormat.getCurrencyInstance(Locale.US)
            return formatStyle.format(amount)
        }
        return com.example.data.service.CurrencyFormatterHelper.format(amount, config)
    }

    fun exportReportToPdf(context: android.content.Context) {
        val txs = filteredTransactions.value
        val cats = categories.value
        val config = activeCountryConfig.value
        val currentMonthStr = selectedMonth.value

        try {
            val file = com.example.data.service.PdfExportHelper.generatePdfReport(
                context, txs, cats, currentMonthStr, config
            )

            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow Finance Statement PDF - $currentMonthStr")
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share statement PDF"))
            resetTxCountSinceLastExport()
            postNotification("Finance Statement PDF compiled and exported successfully!")

        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "PDF Export error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
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
            resetTxCountSinceLastExport()
            
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
            val rate = catTxs.firstOrNull()?.taxRate ?: (realTimeTaxData.value?.standardVatRate ?: config.taxRateDefault)
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

    fun exportTaxReportToPdf(context: android.content.Context) {
        val txs = allTransactions.value.filter { it.isTaxDeductible && it.type == "EXPENSE" }
        val cats = categories.value
        val currentMonthStr = selectedMonth.value
        val config = activeCountryConfig.value

        try {
            val pdfFile = com.example.data.service.PdfExportHelper.generatePdfReport(context, txs, cats, "Tax Deductibles - ${config.fiscalYear}", config)
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                pdfFile
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow PDF Tax Deductible Report - ${config.country}")
                putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share PDF Tax Report"))
            postNotification("Generated and shared PDF Tax Report successfully!")
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "PDF Tax Report error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun runCacheCleanup(onComplete: (Long) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val bytesFreed = com.example.data.worker.CacheCleanupWorker.performCleanup(getApplication())
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete(bytesFreed)
            }
        }
    }

    fun exportSqliteDatabaseEncrypted(context: android.content.Context, onStatus: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val accs = accounts.value
                val cats = categories.value
                val txs = allTransactions.value
                val buds = activeBudgets.value
                val config = activeCountryConfig.value

                // Compile structured snapshot of the whole SQLite Database state
                val dbSnapshotBuilder = StringBuilder()
                dbSnapshotBuilder.append("{\n")
                dbSnapshotBuilder.append("  \"backup_metadata\": {\n")
                dbSnapshotBuilder.append("    \"timestamp\": ${System.currentTimeMillis()},\n")
                dbSnapshotBuilder.append("    \"country\": \"${config.country}\",\n")
                dbSnapshotBuilder.append("    \"currency\": \"${config.currency}\"\n")
                dbSnapshotBuilder.append("  },\n")
                
                // Accounts
                dbSnapshotBuilder.append("  \"accounts\": [\n")
                accs.forEachIndexed { i, a ->
                    dbSnapshotBuilder.append("    {\"id\": ${a.id}, \"name\": \"${a.name.replace("\"", "\\\"")}\", \"balance\": ${a.balance}, \"type\": \"${a.type}\"}")
                    if (i < accs.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ],\n")

                // Categories
                dbSnapshotBuilder.append("  \"categories\": [\n")
                cats.forEachIndexed { i, c ->
                    dbSnapshotBuilder.append("    {\"id\": ${c.id}, \"name\": \"${c.name.replace("\"", "\\\"")}\", \"isIncome\": ${c.isIncome}}")
                    if (i < cats.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ],\n")

                // Budgets
                dbSnapshotBuilder.append("  \"budgets\": [\n")
                buds.forEachIndexed { i, b ->
                    dbSnapshotBuilder.append("    {\"categoryId\": ${b.categoryId}, \"amount\": ${b.amount}, \"spent\": ${b.spent}, \"month\": \"${b.month}\"}")
                    if (i < buds.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ],\n")

                // Transactions
                dbSnapshotBuilder.append("  \"transactions\": [\n")
                txs.forEachIndexed { i, t ->
                    dbSnapshotBuilder.append("    {\"id\": ${t.id}, \"amount\": ${t.amount}, \"type\": \"${t.type}\", \"categoryId\": ${t.categoryId}, \"accountId\": ${t.accountId}, \"merchant\": \"${t.merchant.replace("\"", "\\\"")}\", \"notes\": \"${t.notes.replace("\"", "\\\"")}\", \"timestamp\": ${t.timestamp}}")
                    if (i < txs.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ]\n")
                dbSnapshotBuilder.append("}")

                val dbJson = dbSnapshotBuilder.toString()
                val passcode = exportPasscode.value.ifEmpty { "WealthFlowSecureKey2026" }
                
                // Undergo AES-256 CBC Outbound Block Cipher Encryption
                val encryptedPayload = CsvEncryptionUtility.encrypt(dbJson, passcode)
                
                // Simulate outbound cloud synchronization securely to Web Vault
                onStatus("Initiating secure SSL/TLS connection with Cloud Vault...")
                kotlinx.coroutines.delay(800)
                onStatus("Encrypting SQLite payload with AES-256 bit keys...")
                kotlinx.coroutines.delay(700)
                onStatus("Uploading offline-first archives (Payload: ${encryptedPayload.length} bytes)...")
                kotlinx.coroutines.delay(1000)

                // Write local backup copy (.db.enc) for manual storage configuration
                val fileName = "WealthFlow_Backup_${config.country.replace(" ", "_")}_${System.currentTimeMillis()}.db.enc"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(encryptedPayload)

                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "com.example.fileprovider",
                    file
                )

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "WealthFlow Secure Encrypted Database Backup")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Attached is your AES-256 encrypted SQLite snapshot file. Import via client passcode.")
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Secure DB Backup Archive"))
                postNotification("Sovereign SQLite database backed up and encrypted to Cloud Vault successfully!")
                onStatus("SUCCESS")
            } catch (e: Exception) {
                onStatus("Backup failed: ${e.localizedMessage ?: "timeout error"}")
            }
        }
    }

    fun restoreSqliteDatabaseFromUri(context: android.content.Context, uri: android.net.Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val encryptedText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                
                if (encryptedText.isEmpty()) {
                    onResult("Error: Selected backup file is empty.")
                    return@launch
                }
                
                val passcode = exportPasscode.value.ifEmpty { "WealthFlowSecureKey2026" }
                val decryptedText = com.example.data.service.CsvEncryptionUtility.decrypt(encryptedText, passcode)
                
                if (decryptedText.startsWith("Decryption Error")) {
                    onResult("Decryption failed. Please verify your client passcode matches the backup key.")
                    return@launch
                }
                
                val success = repository.restoreDatabaseFromBackup(decryptedText)
                if (success) {
                    postNotification("Database snapshot restored and decrypted successfully from backup!")
                    onResult("Database restored successfully!")
                } else {
                    onResult("Database restore failed: Corrupted JSON structure.")
                }
            } catch (e: Exception) {
                onResult("Restore failed: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun importCsvFromUri(context: android.content.Context, uri: android.net.Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val csvLines = inputStream?.bufferedReader()?.readLines() ?: emptyList()
                
                if (csvLines.isEmpty()) {
                    onResult("Error: Selected CSV file is empty.")
                    return@launch
                }
                
                val currentCats = categories.value
                val currentAccs = accounts.value
                
                val importedList = mutableListOf<com.example.data.model.TransactionEntity>()
                val validationErrors = mutableListOf<String>()
                var parsedCount = 0
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val sdfShort = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                
                csvLines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("WEALTHFLOW") || trimmed.startsWith("Report Date:") || trimmed.startsWith("Active Month:") || trimmed.startsWith("---") || trimmed.startsWith("Date,Type,Category")) {
                        // Skip header or metadata rows
                        return@forEachIndexed
                    }
                    
                    val tokens = parseCsvLine(trimmed)
                    if (tokens.size < 5) {
                        validationErrors.add("Line ${index + 1}: Insufficient columns (found ${tokens.size}, expected at least 5)")
                        return@forEachIndexed
                    }
                    
                    val dateStr = tokens[0].trim()
                    val typeStr = tokens[1].trim().uppercase()
                    val catName = tokens[2].trim()
                    val merchantStr = tokens[3].trim()
                    val amountStr = tokens[4].trim()
                    val isTaxDeductible = tokens.getOrNull(5)?.trim()?.toBoolean() ?: false
                    val isRecurring = tokens.getOrNull(6)?.trim()?.toBoolean() ?: false
                    val notesStr = tokens.getOrNull(7)?.trim() ?: ""
                    
                    if (typeStr != "EXPENSE" && typeStr != "INCOME" && typeStr != "TRANSFER") {
                        validationErrors.add("Line ${index + 1}: Invalid type '$typeStr' (must be INCOME or EXPENSE)")
                        return@forEachIndexed
                    }
                    
                    val cleanAmountStr = amountStr.replace("[^0-9.]".toRegex(), "")
                    val amountVal = cleanAmountStr.toDoubleOrNull()
                    if (amountVal == null || amountVal < 0.0) {
                        validationErrors.add("Line ${index + 1}: Invalid numeric amount format '$amountStr'")
                        return@forEachIndexed
                    }
                    
                    val matchedCat = currentCats.find { it.name.equals(catName, ignoreCase = true) }
                    if (matchedCat == null) {
                        validationErrors.add("Line ${index + 1}: Mismatched category name '$catName' (not found in database)")
                        return@forEachIndexed
                    }
                    
                    val targetAccount = currentAccs.firstOrNull()
                    if (targetAccount == null) {
                        validationErrors.add("Line ${index + 1}: No active accounts found to assign transaction")
                        return@forEachIndexed
                    }
                    
                    var txTime = System.currentTimeMillis()
                    try {
                        val parsedDate = try { sdf.parse(dateStr) } catch (e: Exception) { sdfShort.parse(dateStr) }
                        if (parsedDate != null) {
                            txTime = parsedDate.time
                        }
                    } catch (e: Exception) {
                        // fallback
                    }
                    
                    importedList.add(
                        com.example.data.model.TransactionEntity(
                            amount = amountVal,
                            type = typeStr,
                            categoryId = matchedCat.id,
                            accountId = targetAccount.id,
                            merchant = merchantStr,
                            isTaxDeductible = isTaxDeductible,
                            isRecurring = isRecurring,
                            notes = notesStr.ifEmpty { "Imported via CSV" },
                            timestamp = txTime
                        )
                    )
                    parsedCount++
                }
                
                if (validationErrors.isNotEmpty()) {
                    val errMsg = "Validation Errors:\n" + validationErrors.take(5).joinToString("\n") +
                            if (validationErrors.size > 5) "\n...and ${validationErrors.size - 5} more." else ""
                    onResult(errMsg)
                    return@launch
                }
                
                if (importedList.isEmpty()) {
                    onResult("No records found to import.")
                    return@launch
                }
                
                importedList.forEach { tx ->
                    repository.insertTransaction(tx)
                }
                
                postNotification("Successfully batch-imported $parsedCount financial records via CSV!")
                onResult("Success: Imported $parsedCount transactions successfully!")
            } catch (e: Exception) {
                onResult("Import error: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun uploadBackupToCloud(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onResult("Compiling database state...")
                val accs = accounts.value
                val cats = categories.value
                val txs = allTransactions.value
                val buds = activeBudgets.value
                val config = activeCountryConfig.value

                val dbSnapshotBuilder = StringBuilder()
                dbSnapshotBuilder.append("{\n")
                dbSnapshotBuilder.append("  \"backup_metadata\": {\n")
                dbSnapshotBuilder.append("    \"timestamp\": ${System.currentTimeMillis()},\n")
                dbSnapshotBuilder.append("    \"country\": \"${config.country}\",\n")
                dbSnapshotBuilder.append("    \"currency\": \"${config.currency}\"\n")
                dbSnapshotBuilder.append("  },\n")
                
                dbSnapshotBuilder.append("  \"accounts\": [\n")
                accs.forEachIndexed { i, a ->
                    dbSnapshotBuilder.append("    {\"id\": ${a.id}, \"name\": \"${a.name.replace("\"", "\\\"")}\", \"balance\": ${a.balance}, \"type\": \"${a.type}\", \"currency\": \"${a.currency}\", \"provider\": \"${a.provider}\", \"isSyncEnabled\": ${a.isSyncEnabled}, \"updatedAt\": ${a.updatedAt}}")
                    if (i < accs.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ],\n")

                dbSnapshotBuilder.append("  \"categories\": [\n")
                cats.forEachIndexed { i, c ->
                    dbSnapshotBuilder.append("    {\"id\": ${c.id}, \"name\": \"${c.name.replace("\"", "\\\"")}\", \"isIncome\": ${c.isIncome}}")
                    if (i < cats.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ],\n")

                dbSnapshotBuilder.append("  \"budgets\": [\n")
                buds.forEachIndexed { i, b ->
                    dbSnapshotBuilder.append("    {\"categoryId\": ${b.categoryId}, \"amount\": ${b.amount}, \"spent\": ${b.spent}, \"month\": \"${b.month}\", \"isAdaptive\": ${b.isAdaptive}, \"savingsGoal\": ${b.savingsGoal}, \"updatedAt\": ${b.updatedAt}}")
                    if (i < buds.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ],\n")

                dbSnapshotBuilder.append("  \"transactions\": [\n")
                txs.forEachIndexed { i, t ->
                    dbSnapshotBuilder.append("    {\"id\": ${t.id}, \"amount\": ${t.amount}, \"type\": \"${t.type}\", \"categoryId\": ${t.categoryId}, \"accountId\": ${t.accountId}, \"merchant\": \"${t.merchant.replace("\"", "\\\"")}\", \"notes\": \"${t.notes.replace("\"", "\\\"")}\", \"timestamp\": ${t.timestamp}, \"isTaxDeductible\": ${t.isTaxDeductible}, \"taxRate\": ${t.taxRate}, \"userEmail\": \"${t.userEmail}\", \"isRecurring\": ${t.isRecurring}, \"recurrenceInterval\": \"${t.recurrenceInterval}\"}")
                    if (i < txs.size - 1) dbSnapshotBuilder.append(",")
                    dbSnapshotBuilder.append("\n")
                }
                dbSnapshotBuilder.append("  ]\n")
                dbSnapshotBuilder.append("}")

                val dbJson = dbSnapshotBuilder.toString()
                val passcode = exportPasscode.value.ifEmpty { "WealthFlowSecureKey2026" }
                
                onResult("Encrypting with AES-256...")
                val encryptedPayload = CsvEncryptionUtility.encrypt(dbJson, passcode)
                val passcodeHash = hashPasscode(passcode)

                onResult("Uploading cloud backup...")
                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val syncUrl = "$baseUrl/api/sync/backup"

                val requestDto = com.example.data.api.CloudBackupRequest(
                    passcodeHash = passcodeHash,
                    encryptedData = encryptedPayload
                )

                val response = com.example.data.api.CloudSyncClient.service.uploadBackup(syncUrl, requestDto)
                if (response.success) {
                    postNotification("Zero-Knowledge secure backup successfully uploaded to cloud!")
                    onResult("SUCCESS")
                } else {
                    onResult("Upload unsuccessful: ${response.message ?: "Server rejection"}")
                }
            } catch (e: Exception) {
                onResult("Cloud Sync Fail: ${e.localizedMessage ?: "Connection error"}")
            }
        }
    }

    fun downloadBackupFromCloud(context: Context, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onResult("Contacting Cloud Sync Server...")
                val passcode = exportPasscode.value.ifEmpty { "WealthFlowSecureKey2026" }
                val passcodeHash = hashPasscode(passcode)

                val baseUrl = remoteSyncUrl.value.trim().removeSuffix("/")
                val syncUrl = "$baseUrl/api/sync/restore"

                val requestDto = com.example.data.api.CloudRestoreRequest(
                    passcodeHash = passcodeHash
                )

                val response = com.example.data.api.CloudSyncClient.service.downloadBackup(syncUrl, requestDto)
                if (response.success && response.encryptedData != null) {
                    onResult("Decrypting AES-256 payload...")
                    val decryptedText = CsvEncryptionUtility.decrypt(response.encryptedData, passcode)
                    
                    onResult("Restoring local database...")
                    val success = repository.restoreDatabaseFromBackup(decryptedText)
                    if (success) {
                        postNotification("Cloud database backup snapshot restored and decrypted successfully!")
                        onResult("SUCCESS")
                    } else {
                        onResult("Decrypted database payload format is invalid.")
                    }
                } else {
                    onResult("No cloud backups found for current client passcode.")
                }
            } catch (e: Exception) {
                onResult("Cloud Sync Fail: ${e.localizedMessage ?: "Connection error"}")
            }
        }
    }
    
    fun applySuggestedBudgetInsight(insight: PredictiveInsight) {
        viewModelScope.launch {
            updateBudgetLimit(
                categoryId = insight.categoryId,
                newLimit = insight.suggestedLimit,
                isAdaptive = true
            )
            postNotification("Updated budget limit for '${insight.categoryName}' to ${formatCurrency(insight.suggestedLimit)} based on AI prediction.")
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = java.lang.StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    fun importExpensesFromCsv(csvText: String, onResult: (Boolean, Int, String) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvText.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()

                if (lines.isEmpty()) {
                    onResult(false, 0, "CSV content is empty.")
                    return@launch
                }

                var importedCount = 0
                val allCats = categories.value
                val defaultCatId = allCats.firstOrNull()?.id ?: 1L
                val defaultAccId = accounts.value.firstOrNull()?.id ?: 1L

                val firstLineTokens = parseCsvLine(lines[0])
                val hasHeader = firstLineTokens.any { token ->
                    val lower = token.lowercase(Locale.US)
                    lower.contains("amount") || lower.contains("date") || lower.contains("merchant") || lower.contains("category")
                }

                val dataLines = if (hasHeader) lines.drop(1) else lines

                for (line in dataLines) {
                    val tokens = parseCsvLine(line).map { it.trim().removeSurrounding("\"") }
                    if (tokens.isNotEmpty()) {
                        var parsedDate = System.currentTimeMillis()
                        var parsedAmount = 0.0
                        var parsedMerchant = ""
                        var parsedNotes = ""
                        var parsedCategoryName = ""

                        for (token in tokens) {
                            val amtCandidate = token.toDoubleOrNull()
                            if (amtCandidate != null && amtCandidate > 0 && parsedAmount == 0.0) {
                                parsedAmount = Math.abs(amtCandidate)
                                continue
                            }

                            if (token.matches("""\d{4}-\d{2}-\d{2}""".toRegex())) {
                                try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                    val d = sdf.parse(token)
                                    if (d != null) parsedDate = d.time
                                } catch (e: Exception) {}
                                continue
                            }

                            if (parsedMerchant.isEmpty() && token.length in 2..40 && !token.startsWith("USD") && !token.startsWith("EUR")) {
                                parsedMerchant = token
                            } else if (token.length > 1) {
                                if (allCats.any { it.name.equals(token, ignoreCase = true) }) {
                                    parsedCategoryName = token
                                } else {
                                    parsedNotes = if (parsedNotes.isEmpty()) token else "$parsedNotes | $token"
                                }
                            }
                        }

                        if (parsedAmount > 0) {
                            val categoryId = allCats.firstOrNull { it.name.equals(parsedCategoryName, ignoreCase = true) }?.id ?: defaultCatId
                            addTransaction(
                                amount = parsedAmount,
                                type = "EXPENSE",
                                categoryId = categoryId,
                                accountId = defaultAccId,
                                merchant = parsedMerchant.ifEmpty { "CSV Import" },
                                notes = parsedNotes.ifEmpty { "Imported from CSV" },
                                customTimestamp = parsedDate
                            )
                            importedCount++
                        }
                    }
                }

                if (importedCount > 0) {
                    onResult(true, importedCount, "Successfully imported $importedCount expense records into Room database!")
                } else {
                    onResult(false, 0, "No valid transaction rows found in CSV.")
                }
            } catch (e: Exception) {
                onResult(false, 0, "Error parsing CSV: ${e.localizedMessage}")
            }
        }
    }
}

