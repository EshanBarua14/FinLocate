package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FinanceRepository(private val db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val countrySettingDao = db.countrySettingDao()
    private val insightDao = db.insightDao()

    // --- Observable Flows ---
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allInsights: Flow<List<InsightEntity>> = insightDao.getAllInsights()
    val activeCountrySetting: Flow<CountrySettingEntity?> = countrySettingDao.getCountrySettingFlow()

    fun getTransactionsInRange(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(start, end)

    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(month)

    fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    // --- Write Actions ---
    suspend fun insertAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.deleteAccount(account)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        // Track the transaction
        val id = transactionDao.insertTransaction(transaction)
        
        // Auto-update the account balance!
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            val balanceDelta = when (transaction.type) {
                "INCOME" -> transaction.amount
                "EXPENSE" -> -transaction.amount
                "TRANSFER" -> -transaction.amount
                else -> 0.0
            }
            accountDao.updateAccount(account.copy(
                balance = account.balance + balanceDelta,
                updatedAt = System.currentTimeMillis()
            ))
        }

        // Auto-update toAccount balance if it's a TRANSFER
        if (transaction.type == "TRANSFER" && transaction.toAccountId != -1L) {
            val toAccount = accountDao.getAccountById(transaction.toAccountId)
            if (toAccount != null) {
                accountDao.updateAccount(toAccount.copy(
                    balance = toAccount.balance + transaction.amount,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }

        // Auto-update associated budget spent tracker for the current month!
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val monthStr = sdf.format(java.util.Date(transaction.timestamp))
        
        if (transaction.type == "EXPENSE") {
            val budget = budgetDao.getBudgetByCategoryAndMonth(transaction.categoryId, monthStr)
            if (budget != null) {
                budgetDao.updateBudget(budget.copy(
                    spent = budget.spent + transaction.amount,
                    updatedAt = System.currentTimeMillis()
                ))
            }
        }

        id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        // Reverse account balance impacts before deletion
        val account = accountDao.getAccountById(transaction.accountId)
        if (account != null) {
            val balanceDelta = when (transaction.type) {
                "INCOME" -> -transaction.amount
                "EXPENSE" -> transaction.amount
                "TRANSFER" -> transaction.amount
                else -> 0.0
            }
            accountDao.updateAccount(account.copy(balance = account.balance + balanceDelta))
        }

        if (transaction.type == "TRANSFER" && transaction.toAccountId != -1L) {
            val toAccount = accountDao.getAccountById(transaction.toAccountId)
            if (toAccount != null) {
                accountDao.updateAccount(toAccount.copy(balance = toAccount.balance - transaction.amount))
            }
        }

        // Clean from budget
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val monthStr = sdf.format(java.util.Date(transaction.timestamp))
        if (transaction.type == "EXPENSE") {
            val budget = budgetDao.getBudgetByCategoryAndMonth(transaction.categoryId, monthStr)
            if (budget != null) {
                budgetDao.updateBudget(budget.copy(spent = (budget.spent - transaction.amount).coerceAtLeast(0.0)))
            }
        }

        transactionDao.deleteTransaction(transaction)
    }

    suspend fun insertBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.updateBudget(budget)
    }

    suspend fun setCountrySetting(countryName: String, forceReSeed: Boolean = false) = withContext(Dispatchers.IO) {
        val currentSetting = countrySettingDao.getCountrySettingStatic()
        
        if (currentSetting == null || currentSetting.selectedCountry != countryName || forceReSeed) {
            countrySettingDao.insertCountrySetting(CountrySettingEntity(selectedCountry = countryName))
            seedInitialFinanceData(countryName)
        }
    }

    suspend fun insertInsight(insight: InsightEntity) = withContext(Dispatchers.IO) {
        insightDao.insertInsight(insight)
    }

    suspend fun markInsightRead(id: Long) = withContext(Dispatchers.IO) {
        insightDao.markAsRead(id)
    }

    suspend fun clearInsights() = withContext(Dispatchers.IO) {
        insightDao.clearInsights()
    }

    // --- Dynamic Seeding Logic matching Selected Country configuration ---
    suspend fun seedInitialFinanceData(countryName: String) = withContext(Dispatchers.IO) {
        val config = CountryConfig.find(countryName)

        // 1. Clear previous categories (since they differ by country tax regulations)
        categoryDao.clearCategories()

        // 2. Insert fresh standard category entities
        val preconfiguredCategories = listOf(
            CategoryEntity(name = "Salary", iconName = "payments", isIncome = true, subcategories = "Monthly Base,Bonus,Overtime"),
            CategoryEntity(name = "Freelance", iconName = "laptop_mac", isIncome = true, subcategories = "Contracting,Consulting,Side Hustle"),
            CategoryEntity(name = "Business", iconName = "storefront", isIncome = true, subcategories = "Sales,Affiliate,Services"),
            CategoryEntity(name = "Food & Dining", iconName = "restaurant", isIncome = false, subcategories = "Groceries,Restaurants,Fast food,Coffee"),
            CategoryEntity(name = "Transport", iconName = "directions_car", isIncome = false, subcategories = "Fuel,Taxi,Public Metro,Parking"),
            CategoryEntity(name = "Rent & Bills", iconName = "home", isIncome = false, subcategories = "Apartment Rent,Electricity,Water,Internet,Cable"),
            CategoryEntity(name = "Entertainment", iconName = "movie", isIncome = false, subcategories = "Streaming,Movies,Gaming,Subscribes"),
            CategoryEntity(name = "Taxes & Duties", iconName = "receipt_long", isIncome = false, subcategories = config.taxCategories.joinToString(",")),
            CategoryEntity(name = "Savings & Investments", iconName = "trending_up", isIncome = false, subcategories = "Mutual Funds,Gilt,Gold,Savings bonds"),
            CategoryEntity(name = "Shopping", iconName = "shopping_bag", isIncome = false, subcategories = "Apparel,Electronics,Home supplies")
        )

        val catMap = mutableMapOf<String, Long>()
        for (cat in preconfiguredCategories) {
            val id = categoryDao.insertCategory(cat)
            catMap[cat.name] = id
        }

        // 3. Clear and seed accounts specialized for the target country configuration
        // (We can drop previous accounts or let them stay. Let's delete to make it a pristine seed!)
        db.runInTransaction {
            // Drop accounts & transactions on hard country change to prevent mixed currency accounts
            db.openHelper.writableDatabase.execSQL("DELETE FROM accounts")
            db.openHelper.writableDatabase.execSQL("DELETE FROM transactions")
            db.openHelper.writableDatabase.execSQL("DELETE FROM budgets")
            db.openHelper.writableDatabase.execSQL("DELETE FROM insights")
        }

        // Create standard seeded accounts (1 cash, 1 primary bank, 1 regional wallet, 1 credit card)
        val defaultAccounts = listOf(
            AccountEntity(
                name = "Physical Cash",
                type = "CASH",
                balance = 15000.0,
                currency = config.currency,
                provider = "Wallet Sleeve",
                accountColorHex = "#10B981"
            ),
            AccountEntity(
                name = config.standardBanks.firstOrNull() ?: "Main Bank Account",
                type = "BANK",
                balance = 125000.0,
                currency = config.currency,
                provider = config.standardBanks.firstOrNull() ?: "Standard",
                accountColorHex = "#3B82F6"
            ),
            AccountEntity(
                name = config.wallets.firstOrNull() ?: "Mobile Wallet",
                type = "MOBILE_WALLET",
                balance = 8500.0,
                currency = config.currency,
                provider = config.wallets.firstOrNull() ?: "Mobile",
                accountColorHex = "#F59E0B"
            ),
            AccountEntity(
                name = "Emergency Credit Card",
                type = "CREDIT_CARD",
                balance = -1200.0, // outstanding debt
                currency = config.currency,
                provider = config.standardBanks.firstOrNull() ?: "Credit",
                accountColorHex = "#EF4444"
            )
        )

        val accIds = defaultAccounts.map { accountDao.insertAccount(it) }

        // 4. Seed indicative transactions
        val cashId = accIds.getOrNull(0) ?: 1L
        val bankId = accIds.getOrNull(1) ?: 2L
        val walletId = accIds.getOrNull(2) ?: 3L

        val salaryCatId = catMap["Salary"] ?: 1L
        val freelanceCatId = catMap["Freelance"] ?: 2L
        val foodCatId = catMap["Food & Dining"] ?: 4L
        val transportCatId = catMap["Transport"] ?: 5L
        val rentCatId = catMap["Rent & Bills"] ?: 6L
        val taxCatId = catMap["Taxes & Duties"] ?: 8L

        // Generate timestamps (e.g. today, yesterday, 2 days ago, 3 days ago)
        val dayMillis = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()

        val sampleTransactions = listOf(
            TransactionEntity(
                amount = 75000.0,
                type = "INCOME",
                categoryId = salaryCatId,
                accountId = bankId,
                timestamp = now - 5 * dayMillis,
                merchant = "Payroll Corp",
                notes = "Monthly standard base salary"
            ),
            TransactionEntity(
                amount = 12000.0,
                type = "INCOME",
                categoryId = freelanceCatId,
                accountId = walletId,
                timestamp = now - 3 * dayMillis,
                merchant = "Freelance Client LLC",
                notes = "UI redesign milestone pay"
            ),
            TransactionEntity(
                amount = 15000.0,
                type = "EXPENSE",
                categoryId = rentCatId,
                accountId = bankId,
                timestamp = now - 4 * dayMillis,
                merchant = "Metropark Rentals",
                notes = "Monthly flat rental",
                isTaxDeductible = true
            ),
            TransactionEntity(
                amount = 1250.0,
                type = "EXPENSE",
                categoryId = foodCatId,
                accountId = cashId,
                timestamp = now - 2 * dayMillis,
                merchant = "Downtown Bistro",
                notes = "Dinner with client",
                isTaxDeductible = true,
                taxRate = config.taxRateDefault
            ),
            TransactionEntity(
                amount = 450.0,
                type = "EXPENSE",
                categoryId = transportCatId,
                accountId = cashId,
                timestamp = now - 1 * dayMillis,
                merchant = "Metro Booking Ride",
                notes = "Office commute"
            ),
            TransactionEntity(
                amount = 3500.0,
                type = "EXPENSE",
                categoryId = taxCatId,
                accountId = bankId,
                timestamp = now - dayMillis / 2,
                merchant = "Tax Authority Office",
                notes = config.taxCategories.firstOrNull() ?: "Tax Duty Payment",
                isTaxDeductible = true
            )
        )

        for (tx in sampleTransactions) {
            // We call transactionDao directly to avoid double modification of seeded accounts since seeded accounts already have initial pre-populated final balances
            transactionDao.insertTransaction(tx)
        }

        // 5. Seed Category Budgets
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
        val currentMonth = sdf.format(java.util.Date(now))

        val seedBudgets = listOf(
            BudgetEntity(categoryId = foodCatId, amount = 10000.0, spent = 1250.0, month = currentMonth, isAdaptive = true),
            BudgetEntity(categoryId = transportCatId, amount = 3000.0, spent = 450.0, month = currentMonth, isAdaptive = false),
            BudgetEntity(categoryId = rentCatId, amount = 20000.0, spent = 15000.0, month = currentMonth, isAdaptive = false),
            BudgetEntity(categoryId = taxCatId, amount = 8000.0, spent = 3500.0, month = currentMonth, isAdaptive = true)
        )

        for (b in seedBudgets) {
            budgetDao.insertBudget(b)
        }

        // 6. Seed friendly introductory insights
        val seedInsights = listOf(
            InsightEntity(
                title = "Welcome to Finance Tracker!",
                description = "Successfully initialized configurations for $countryName. Standard currency is ${config.currency} (${config.currencySymbol}), with localized bank links, ${config.fiscalYear} fiscal constraints, and Mobile Wallets (${config.wallets.joinToString(", ")}).",
                category = "SAVINGS_IMPROVED",
                severity = "SUCCESS"
            ),
            InsightEntity(
                title = "Optimal Tax Saving Tip",
                description = "In $countryName, expenses categorized under '${config.taxCategories.firstOrNull()}' may qualify for direct deduction. Toggle the 'Tax Deductible' switch during expense entry to keep these optimized.",
                category = "TAX_TIP",
                severity = "INFO"
            )
        )

        for (ins in seedInsights) {
            insightDao.insertInsight(ins)
        }
    }
}
