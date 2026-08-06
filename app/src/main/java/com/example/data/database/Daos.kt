package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    suspend fun getAllAccountsStatic(): List<AccountEntity>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesStatic(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsStatic(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :startMillis AND :endMillis ORDER BY timestamp DESC")
    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE timestamp < :timestamp")
    suspend fun getTransactionsOlderThan(timestamp: Long): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE timestamp < :timestamp")
    suspend fun deleteTransactionsOlderThan(timestamp: Long): Int

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month = :month")
    suspend fun getBudgetsForMonthStatic(month: String): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month LIMIT 1")
    suspend fun getBudgetByCategoryAndMonth(categoryId: Long, month: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

@Dao
interface CountrySettingDao {
    @Query("SELECT * FROM countries WHERE id = 1 LIMIT 1")
    fun getCountrySettingFlow(): Flow<CountrySettingEntity?>

    @Query("SELECT * FROM countries WHERE id = 1 LIMIT 1")
    suspend fun getCountrySettingStatic(): CountrySettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountrySetting(setting: CountrySettingEntity)
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights ORDER BY timestamp DESC")
    fun getAllInsights(): Flow<List<InsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: InsightEntity): Long

    @Query("UPDATE insights SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM insights WHERE isRead = 1 AND timestamp < :thresholdTime")
    suspend fun pruneOldReadInsights(thresholdTime: Long)

    @Query("DELETE FROM insights")
    suspend fun clearInsights()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmailStatic(email: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getActiveUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface CountryConfigDao {
    @Query("SELECT * FROM country_configs")
    fun getAllConfigsFlow(): Flow<List<CountryConfigEntity>>

    @Query("SELECT * FROM country_configs WHERE country = :country LIMIT 1")
    suspend fun getConfigByCountryStatic(country: String): CountryConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: CountryConfigEntity)
}

@Dao
interface MatchingRuleDao {
    @Query("SELECT * FROM matching_rules ORDER BY keyword ASC")
    fun getAllRules(): Flow<List<MatchingRuleEntity>>

    @Query("SELECT * FROM matching_rules ORDER BY keyword ASC")
    suspend fun getAllRulesStatic(): List<MatchingRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MatchingRuleEntity): Long

    @Update
    suspend fun updateRule(rule: MatchingRuleEntity)

    @Delete
    suspend fun deleteRule(rule: MatchingRuleEntity)

    @Query("DELETE FROM matching_rules")
    suspend fun clearRules()
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY createdAt DESC")
    fun getAllRecurringFlow(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1")
    suspend fun getActiveRecurringStatic(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity): Long

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity)
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    fun getAllRatesFlow(): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates")
    suspend fun getAllRatesStatic(): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRates(rates: List<ExchangeRateEntity>)

    @Query("DELETE FROM exchange_rates WHERE updatedAt < :thresholdTime AND currency NOT IN ('USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'INR', 'SGD', 'BDT')")
    suspend fun pruneOldRates(thresholdTime: Long)
}

@Dao
interface UserDebtDao {
    @Query("SELECT * FROM user_debts ORDER BY createdAt DESC")
    fun getAllDebtsFlow(): Flow<List<UserDebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: UserDebtEntity): Long

    @Update
    suspend fun updateDebt(debt: UserDebtEntity)

    @Delete
    suspend fun deleteDebt(debt: UserDebtEntity)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoalsFlow(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesStatic(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?
}

@Dao
interface TaxCategoryDao {
    @Query("SELECT * FROM tax_categories ORDER BY name ASC")
    fun getAllTaxCategories(): Flow<List<TaxCategoryEntity>>

    @Query("SELECT * FROM tax_categories ORDER BY name ASC")
    suspend fun getAllTaxCategoriesStatic(): List<TaxCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxCategory(taxCategory: TaxCategoryEntity): Long

    @Update
    suspend fun updateTaxCategory(taxCategory: TaxCategoryEntity)

    @Query("DELETE FROM tax_categories")
    suspend fun clearTaxCategories()
}

@Dao
interface RecurringExpenseDao {
    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY nextDueAt ASC")
    fun getAllActiveRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY nextDueAt ASC")
    suspend fun getAllActiveRecurringExpensesStatic(): List<RecurringExpenseEntity>

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND nextDueAt <= :currentTime")
    suspend fun getDueRecurringExpenses(currentTime: Long): List<RecurringExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpenseEntity): Long

    @Update
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpenseEntity)

    @Delete
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpenseEntity)
}

@Dao
interface TransactionExchangeRateLogDao {
    @Query("SELECT * FROM transaction_exchange_rate_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<TransactionExchangeRateLogEntity>>

    @Query("SELECT * FROM transaction_exchange_rate_logs WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getLogByTransactionId(transactionId: Long): TransactionExchangeRateLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionExchangeRateLogEntity): Long

    @Query("DELETE FROM transaction_exchange_rate_logs WHERE transactionId = :transactionId")
    suspend fun deleteLogsByTransactionId(transactionId: Long): Int
}


