package com.example.data.model

import androidx.room.*

// --- Country Config & Plugins ---

data class CountryConfig(
    val country: String,
    val currency: String,
    val currencySymbol: String,
    val fiscalYear: String, // e.g. "July-June" or "January-December"
    val wallets: List<String>,       // country-specific mobile wallets (e.g. bKash, Paytm, Venmo)
    val standardBanks: List<String>,  // country-specific popular banks
    val taxRateDefault: Double,      // standard default income/expense tax or VAT
    val taxCategories: List<String>, // tax deductible items
    val language: String,
    val numberFormat: String         // format style e.g. "en-US" or "bn-BD"
) {
    companion object {
        val Bangladesh = CountryConfig(
            country = "Bangladesh",
            currency = "BDT",
            currencySymbol = "৳",
            fiscalYear = "July-June",
            wallets = listOf("bKash", "Nagad", "Rocket"),
            standardBanks = listOf("Brac Bank", "Dutch-Bangla Bank", "City Bank", "Sonali Bank"),
            taxRateDefault = 15.0, // standard general VAT
            taxCategories = listOf("Hajj Expense", "Government Bond Savings", "DPS Investment", "Charity Contribution"),
            language = "Bengali/English",
            numberFormat = "bn-BD"
        )

        val USA = CountryConfig(
            country = "USA",
            currency = "USD",
            currencySymbol = "$",
            fiscalYear = "January-December",
            wallets = listOf("Venmo", "PayPal", "Apple Pay", "Cash App"),
            standardBanks = listOf("Chase", "Bank of America", "Wells Fargo", "Citi"),
            taxRateDefault = 8.25, // standard state/sales tax average
            taxCategories = listOf("W2 Deductible Health", "Traditional IRA", "Charitable Charity (501c3)", "Business Travel"),
            language = "English",
            numberFormat = "en-US"
        )

        val India = CountryConfig(
            country = "India",
            currency = "INR",
            currencySymbol = "₹",
            fiscalYear = "April-March",
            wallets = listOf("Paytm", "PhonePe", "Google Pay UPI", "Amazon Pay"),
            standardBanks = listOf("SBI", "HDFC Bank", "ICICI Bank", "Axis Bank"),
            taxRateDefault = 18.0, // standard GST rate bracket
            taxCategories = listOf("Section 80C ELSS", "Section 80D Health Premium", "NPS contribution", "Home Loan Principal"),
            language = "Hindi/English",
            numberFormat = "en-IN"
        )

        val Germany = CountryConfig(
            country = "Germany",
            currency = "EUR",
            currencySymbol = "€",
            fiscalYear = "January-December",
            wallets = listOf("PayPal", "Giropay", "Apple Pay", "Google Pay"),
            standardBanks = listOf("N26", "Sparkasse", "Deutsche Bank", "Commerzbank"),
            taxRateDefault = 19.0, // standard MwSt
            taxCategories = listOf("Riester Pension", "Werbungskosten (Professional)", "Krankenkasse Health Plan", "Spenden (Donation)"),
            language = "German",
            numberFormat = "de-DE"
        )

        val DefaultList = listOf(Bangladesh, USA, India, Germany)
        fun find(countryName: String): CountryConfig {
            return DefaultList.find { it.country.equals(countryName, ignoreCase = true) } ?: USA
        }
    }
}

data class RealTimeTaxBracket(
    val incomeRange: String,
    val rate: Double
)

data class RealTimeTaxData(
    val country: String,
    val standardVatRate: Double,
    val brackets: List<RealTimeTaxBracket>
)

// --- Room Database Entities ---

@Entity(tableName = "countries")
data class CountrySettingEntity(
    @PrimaryKey val id: Int = 1, // only 1 active setting
    val selectedCountry: String = "USA"
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val selectedCountry: String = "USA",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "country_configs",
    indices = [Index(value = ["country"], unique = true)]
)
data class CountryConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val country: String,
    val currency: String,
    val currencySymbol: String,
    val fiscalYear: String,
    val wallets: String, // comma-separated values
    val standardBanks: String, // comma-separated values
    val taxRateDefault: Double,
    val taxCategories: String, // comma-separated values
    val language: String,
    val numberFormat: String
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // CASH, BANK, MOBILE_WALLET, CREDIT_CARD, LOAN
    val balance: Double,
    val currency: String,
    val provider: String = "Cash", // e.g. "bKash", "Chase", "State Bank of India"
    val accountColorHex: String = "#10B981",
    val isSyncEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String, // Icon descriptor e.g. "shopping_bag", "restaurant", "directions_car"
    val isIncome: Boolean,
    val subcategories: String = "" // comma separated subcategories e.g. "Fast food,Groceries,Snacks"
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["timestamp"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val categoryId: Long, // 0 for TRANSFER
    val accountId: Long, // source account or fromAccount
    val toAccountId: Long = -1, // for TRANSFER type
    val timestamp: Long = System.currentTimeMillis(),
    val merchant: String = "",
    val isTaxDeductible: Boolean = false,
    val taxRate: Double = 0.0,
    val notes: String = "",
    val isRecurring: Boolean = false,
    val recurrenceInterval: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val splitCount: Int = 1, // support simple split expense math
    val userEmail: String = "", // sandbox SaaS partition/audit field
    val tags: String = "",
    val receiptPath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["taxCategoryId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val currency: String = "USD",
    val date: Long = System.currentTimeMillis(),
    val taxCategoryId: Long = 0L,
    val merchant: String = "",
    val notes: String = "",
    val receiptPath: String = "",
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tax_categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class TaxCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val description: String = "",
    val isDeductible: Boolean = true,
    val defaultRate: Double = 0.0,
    val monthlyCap: Double = 0.0 // monthly spending cap limit
)

@Entity(
    tableName = "recurring_expenses",
    indices = [
        Index(value = ["taxCategoryId"])
    ]
)
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val currency: String = "USD",
    val taxCategoryId: Long = 0L,
    val merchant: String = "",
    val notes: String = "",
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, YEARLY
    val lastExecutedAt: Long = 0L,
    val nextDueAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId", "month"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val spent: Double = 0.0,
    val month: String, // format YYYY-MM
    val rolloverAmount: Double = 0.0,
    val isAdaptive: Boolean = false, // smart AI recommendations toggling
    val savingsGoal: Double = 0.0, // target savings goal set for this category budget
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "insights",
    indices = [Index(value = ["timestamp"])]
)
data class InsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // "BUDGET_EXCEED", "BURN_RATE", "SAVINGS_IMPROVED", "TAX_TIP", "AI_PREDICTION"
    val severity: String, // ALERT, WARNING, SUCCESS, INFO
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// --- Relational Schema Mappings ---
data class TransactionWithCategoryAndAccount(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "accountId",
        entityColumn = "id"
    )
    val account: AccountEntity?,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)

@Entity(
    tableName = "matching_rules",
    indices = [Index(value = ["keyword"], unique = true)]
)
data class MatchingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String, // transaction description/merchant substring
    val categoryId: Long,
    val isTaxDeductible: Boolean = false,
    val taxRate: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // INCOME, EXPENSE, TRANSFER
    val categoryId: Long,
    val accountId: Long,
    val toAccountId: Long = -1,
    val merchant: String = "",
    val notes: String = "",
    val recurrenceInterval: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY
    val lastExecutionTimestamp: Long = 0L,
    val nextExecutionTimestamp: Long = System.currentTimeMillis(),
    val isTaxDeductible: Boolean = false,
    val taxRate: Double = 0.0,
    val isActive: Boolean = true,
    val userEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currency: String, // e.g. "EUR", "BDT", "USD"
    val rate: Double, // rate against USD base currency
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_debts")
data class UserDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double, // current loan/debt principal
    val interestRate: Double, // annual interest rate in percent, e.g. 5.5 for 5.5%
    val termMonths: Int, // loan term in months, e.g. 36
    val monthlyPayment: Double = 0.0, // monthly payment
    val startDate: Long = System.currentTimeMillis(),
    val userEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val targetDate: Long, // timestamp
    val savedAmount: Double,
    val userEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transaction_exchange_rate_logs",
    indices = [
        Index(value = ["transactionId"])
    ]
)
data class TransactionExchangeRateLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val originalCurrency: String,
    val targetCurrency: String,
    val exchangeRateUsed: Double,
    val timestamp: Long = System.currentTimeMillis()
)


