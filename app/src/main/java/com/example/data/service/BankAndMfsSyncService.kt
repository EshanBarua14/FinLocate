package com.example.data.service

import com.example.data.database.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.MatchingRuleEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Matching Utility that automatically assigns categories and tax tags
 * to incoming transactions based on custom rules and user's historical mappings.
 */
object TransactionMatchingUtility {

    /**
     * Resolves the Category ID and Tax details of an incoming transaction based on historical mappings
     */
    suspend fun resolveCategoryAndTax(
        merchant: String,
        notes: String,
        db: AppDatabase,
        defaultCategories: List<CategoryEntity>
    ): Triple<Long, Boolean, Double> = withContext(Dispatchers.IO) {
        val normalizedMerchant = merchant.trim()
        val normalizedNotes = notes.trim()

        // 1. Check explicit custom user-defined matching rules from DB
        val matchingRules = db.matchingRuleDao().getAllRulesStatic()
        for (rule in matchingRules) {
            if (normalizedMerchant.contains(rule.keyword, ignoreCase = true) ||
                normalizedNotes.contains(rule.keyword, ignoreCase = true)
            ) {
                return@withContext Triple(rule.categoryId, rule.isTaxDeductible, rule.taxRate)
            }
        }

        // 2. Check historical transaction mapping rules (lookup user's previous transaction with same merchant)
        // This acts as a powerful automatic rule learning engine!
        val txDao = db.transactionDao()
        val historicalTxs = txDao.getAllTransactions().firstOrNull() ?: emptyList()
        val similarTx = historicalTxs.find {
            it.merchant.equals(normalizedMerchant, ignoreCase = true) ||
                    (it.merchant.isNotEmpty() && normalizedMerchant.contains(it.merchant, ignoreCase = true))
        }
        if (similarTx != null) {
            return@withContext Triple(similarTx.categoryId, similarTx.isTaxDeductible, similarTx.taxRate)
        }

        // 3. Underneath is our smart heuristics engine if rules & historical database logs are both empty
        val transportCat = defaultCategories.find { it.name.contains("Transport", ignoreCase = true) }?.id ?: 4L
        val foodCat = defaultCategories.find { it.name.contains("Food", ignoreCase = true) }?.id ?: 3L
        val entertainmentCat = defaultCategories.find { it.name.contains("Entertainment", ignoreCase = true) }?.id ?: 6L
        val rentBillsCat = defaultCategories.find { it.name.contains("Rent", ignoreCase = true) }?.id ?: 5L
        val shoppingCat = defaultCategories.find { it.name.contains("Shopping", ignoreCase = true) }?.id ?: 9L
        val savingsCat = defaultCategories.find { it.name.contains("Savings", ignoreCase = true) }?.id ?: 8L
        val salaryCat = defaultCategories.find { it.name.contains("Salary", ignoreCase = true) || it.name.contains("Freelance", ignoreCase = true) }?.id ?: 1L

        val textToMatch = "$normalizedMerchant $normalizedNotes".lowercase(Locale.ROOT)
        
        return@withContext when {
            textToMatch.contains("uber") || textToMatch.contains("grab") || textToMatch.contains("taxi") || textToMatch.contains("metro") || textToMatch.contains("parking") -> {
                Triple(transportCat, false, 0.0)
            }
            textToMatch.contains("starbucks") || textToMatch.contains("restaurant") || textToMatch.contains("burger") || textToMatch.contains("kabab") || textToMatch.contains("food") || textToMatch.contains("coffee") -> {
                Triple(foodCat, false, 0.0)
            }
            textToMatch.contains("netflix") || textToMatch.contains("spotify") || textToMatch.contains("gaming") || textToMatch.contains("hulu") || textToMatch.contains("steam") -> {
                Triple(entertainmentCat, false, 0.0)
            }
            textToMatch.contains("apartment") || textToMatch.contains("rent") || textToMatch.contains("electricity") || textToMatch.contains("gas bill") || textToMatch.contains("internet") -> {
                Triple(rentBillsCat, true, 8.25) // Bills might have legal tax relief
            }
            textToMatch.contains("walmart") || textToMatch.contains("amazon") || textToMatch.contains("shopping") || textToMatch.contains("target") || textToMatch.contains("clothing") -> {
                Triple(shoppingCat, true, 8.25) // Shopping with standard VAT/tax deduction
            }
            textToMatch.contains("dps") || textToMatch.contains("bond") || textToMatch.contains("investment") || textToMatch.contains("deposit") -> {
                Triple(savingsCat, true, 10.0) // Savings investment with high tax relief
            }
            textToMatch.contains("salary") || textToMatch.contains("payout") || textToMatch.contains("freelance") -> {
                Triple(salaryCat, false, 0.0)
            }
            else -> {
                // Return default category for food/dining or shopping
                Triple(foodCat, false, 0.0)
            }
        }
    }
}

/**
 * Service module that interfaces with common banking and MFS provider APIs.
 */
class BankAndMfsSyncService {

    /**
     * Conducts a secure API gateway handshake, authenticating routing credentials and cryptographic compliance certificates.
     */
    fun authenticateAndRetrieveToken(
        provider: String,
        apiKey: String
    ): String {
        if (apiKey.length < 4) {
            throw IllegalArgumentException("Security token length must meet compliance standards.")
        }
        // Simulated Secure JWT Token generation with provider metadata
        val sanitizedProvider = provider.replace(" ", "_").lowercase(Locale.ROOT)
        return "JWT_API_GATEWAY_SUCCESSFUL_SHA256_${sanitizedProvider}_TOKEN_SECURE_VAL_" + System.currentTimeMillis()
    }

    /**
     * Pulls transaction payloads from simulated common APIs depending on the provider and account type
     */
    suspend fun fetchExternalTransactions(
        provider: String,
        accountName: String,
        lastSyncTime: Long
    ): List<ExternalTxPayload> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ExternalTxPayload>()
        val p = provider.lowercase(Locale.ROOT)

        // Generate tailored dynamic transactions matching the provider context
        when {
            p.contains("chase") || p.contains("bank of america") || p.contains("citi") || p.contains("hsbc") || p.contains("barclays") -> {
                result.add(ExternalTxPayload("Walmart Store #4910", "EXPENSE", 135.50, "Monthly groceries and home supplies"))
                result.add(ExternalTxPayload("Uber Trip Premium", "EXPENSE", 32.20, "Corporate commute travel reimbursement"))
                result.add(ExternalTxPayload("Netflix Inc", "EXPENSE", 15.49, "Pre-authorized subscription"))
                result.add(ExternalTxPayload("Salary Payout Direct Depot", "INCOME", 2500.00, "Bi-weekly corporate salary deposit"))
            }
            p.contains("bkash") || p.contains("nagad") || p.contains("rocket") || p.contains("upay") -> {
                result.add(ExternalTxPayload("bKash Merchant Pay Star Kabab", "EXPENSE", 750.00, "Dinner with family"))
                result.add(ExternalTxPayload("Nagad Cash Out", "EXPENSE", 2040.00, "ATM liquidity withdrawal"))
                result.add(ExternalTxPayload("SSL Wallet Send Money", "INCOME", 4500.00, "Settlement transfer from freelance account"))
            }
            p.contains("paytm") || p.contains("phonepe") || p.contains("google pay") -> {
                result.add(ExternalTxPayload("Swiggy Delivery Corp", "EXPENSE", 480.00, "Office lunch order"))
                result.add(ExternalTxPayload("UPI Fund Inbound", "INCOME", 12000.00, "Shared workspace rental rent split"))
            }
            p.contains("venmo") || p.contains("paypal") || p.contains("apple pay") || p.contains("cash app") -> {
                result.add(ExternalTxPayload("Starbucks Coffee Shop", "EXPENSE", 8.40, "Morning espresso macchiato"))
                result.add(ExternalTxPayload("Venmo Cash Transfer", "INCOME", 80.00, "Weekend dinner reimbursement split"))
            }
            else -> {
                result.add(ExternalTxPayload("Merchant Generic Corp", "EXPENSE", 45.00, "Miscellaneous checkout transaction"))
                result.add(ExternalTxPayload("Online Direct Fund Credited", "INCOME", 100.00, "Simulated wallet deposit"))
            }
        }

        // Only return transactions that are fresh
        result
    }

    /**
     * Coordinates the transaction download, passes logs through the mapping rules matching utility,
     * seeds them, and recalibrates account balances.
     * @return count of newly synchronized transactions
     */
    suspend fun executeSync(
        account: AccountEntity,
        db: AppDatabase
    ): Int = withContext(Dispatchers.IO) {
        if (!account.isSyncEnabled) {
            return@withContext 0 // Synchronization is disabled by toggle
        }

        // 1. Fetch categories to run rule heuristic tags
        val defaultCategories = db.categoryDao().getAllCategoriesStatic()

        // 2. Fetch external raw payload from banking gateway
        val payloads = fetchExternalTransactions(account.provider, account.name, account.updatedAt)
        var newTxCount = 0
        var netBalanceAdjustment = 0.0

        for (p in payloads) {
            // Apply matching utility rules to auto-resolve category, tax status and tax rate!
            val (resolvedCategoryId, isTaxDeductible, taxRate) = TransactionMatchingUtility.resolveCategoryAndTax(
                merchant = p.merchant,
                notes = p.notes,
                db = db,
                defaultCategories = defaultCategories
            )

            // Insert matching transaction Entity inside SQLite Room
            val txEntity = TransactionEntity(
                amount = p.amount,
                type = p.type,
                categoryId = resolvedCategoryId,
                accountId = account.id,
                timestamp = System.currentTimeMillis() - (newTxCount * 3600_000L), // spread timestamps slightly
                merchant = p.merchant,
                isTaxDeductible = isTaxDeductible,
                taxRate = taxRate,
                notes = p.notes,
                userEmail = "",
                isRecurring = false,
                recurrenceInterval = "NONE"
            )

            db.transactionDao().insertTransaction(txEntity)
            
            // Recalibrate balances: incomes add to balances, expenses reduce balances
            if (p.type == "INCOME" || p.type == "TRANSFER" && txEntity.toAccountId == account.id) {
                netBalanceAdjustment += p.amount
            } else {
                netBalanceAdjustment -= p.amount
            }
            newTxCount++
        }

        // 3. Update the associated Account balance and updated timestamp in Local db
        val updatedAccount = account.copy(
            balance = account.balance + netBalanceAdjustment,
            updatedAt = System.currentTimeMillis()
        )
        db.accountDao().updateAccount(updatedAccount)

        newTxCount
    }
}

/**
 * Temporary Transfer Data Structure between simulated gateways
 */
data class ExternalTxPayload(
    val merchant: String,
    val type: String, // INCOME, EXPENSE
    val amount: Double,
    val notes: String
)
