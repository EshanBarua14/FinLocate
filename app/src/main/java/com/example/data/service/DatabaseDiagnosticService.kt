package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DiagnosticReport(
    val isHealthy: Boolean,
    val totalTransactionsChecked: Int,
    val orphanedRecordsFixed: Int,
    val corruptedAmountsFixed: Int,
    val currencyCacheStatus: String,
    val schemaIntegrityOk: Boolean,
    val logDetails: List<String>
)

object DatabaseDiagnosticService {
    private const val TAG = "DatabaseDiagnosticService"

    suspend fun runDiagnosticAndRepair(context: Context, db: AppDatabase): DiagnosticReport = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        var orphanedFixed = 0
        var corruptedFixed = 0
        var schemaIntegrityOk = true

        try {
            logs.add("Checking SQLite database schema integrity...")
            val cursor = db.openHelper.readableDatabase.query("PRAGMA quick_check")
            var pragmaResult = "OK"
            if (cursor.moveToFirst()) {
                pragmaResult = cursor.getString(0) ?: "OK"
            }
            cursor.close()
            schemaIntegrityOk = pragmaResult.equals("ok", ignoreCase = true)
            logs.add("PRAGMA quick_check result: $pragmaResult")

            // Get all static data
            val accounts = db.accountDao().getAllAccountsStatic()
            val categories = db.categoryDao().getAllCategoriesStatic()
            val allTx = db.transactionDao().getAllTransactionsStatic()

            val validAccountIds = accounts.map { it.id }.toSet()
            val validCategoryIds = categories.map { it.id }.toSet()

            // Ensure fallback default account and category exist
            var defaultAccount: AccountEntity? = accounts.firstOrNull()
            if (defaultAccount == null) {
                val newAccId = db.accountDao().insertAccount(AccountEntity(name = "Default Cash", type = "CASH", balance = 0.0, currency = "USD"))
                defaultAccount = AccountEntity(id = newAccId, name = "Default Cash", type = "CASH", balance = 0.0, currency = "USD")
                logs.add("Created fallback default account (ID: $newAccId)")
            }

            var defaultCategory: CategoryEntity? = categories.firstOrNull()
            if (defaultCategory == null) {
                val newCatId = db.categoryDao().insertCategory(CategoryEntity(name = "General", iconName = "category", isIncome = false))
                defaultCategory = CategoryEntity(id = newCatId, name = "General", iconName = "category", isIncome = false)
                logs.add("Created fallback default category (ID: $newCatId)")
            }

            logs.add("Scanning ${allTx.size} transaction records for schema & orphaned consistency...")

            allTx.forEach { tx ->
                var needsUpdate = false
                var fixedTx = tx

                // Check for orphaned account
                if (!validAccountIds.contains(tx.accountId)) {
                    fixedTx = fixedTx.copy(accountId = defaultAccount!!.id)
                    orphanedFixed++
                    needsUpdate = true
                    logs.add("Fixed orphaned account link in transaction #${tx.id} -> reassigned to account '${defaultAccount!!.name}'")
                }

                // Check for orphaned category
                if (!validCategoryIds.contains(tx.categoryId)) {
                    fixedTx = fixedTx.copy(categoryId = defaultCategory!!.id)
                    orphanedFixed++
                    needsUpdate = true
                    logs.add("Fixed orphaned category link in transaction #${tx.id} -> reassigned to category '${defaultCategory!!.name}'")
                }

                // Check for corrupted amounts (NaN or Infinite)
                if (tx.amount.isNaN() || tx.amount.isInfinite()) {
                    fixedTx = fixedTx.copy(amount = 0.0)
                    corruptedFixed++
                    needsUpdate = true
                    logs.add("Fixed corrupted NaN/Infinite amount in transaction #${tx.id} -> reset to 0.0")
                }

                if (needsUpdate) {
                    db.transactionDao().updateTransaction(fixedTx)
                }
            }

            // Validate against local currency cache
            val rates = db.exchangeRateDao().getAllRatesStatic()
            val cachedCurrencies = rates.map { it.currency }.toSet()
            val currencyCacheStatus = "Local currency cache synced with ${rates.size} active rate definitions (${cachedCurrencies.joinToString(", ")})"
            logs.add(currencyCacheStatus)

            val isHealthy = schemaIntegrityOk && orphanedFixed == 0 && corruptedFixed == 0
            logs.add("Diagnostic completed. Status: ${if (isHealthy) "HEALTHY" else "REPAIRED"}")

            DiagnosticReport(
                isHealthy = isHealthy,
                totalTransactionsChecked = allTx.size,
                orphanedRecordsFixed = orphanedFixed,
                corruptedAmountsFixed = corruptedFixed,
                currencyCacheStatus = currencyCacheStatus,
                schemaIntegrityOk = schemaIntegrityOk,
                logDetails = logs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error running diagnostic routine", e)
            logs.add("Diagnostic error: ${e.localizedMessage}")
            DiagnosticReport(
                isHealthy = false,
                totalTransactionsChecked = 0,
                orphanedRecordsFixed = 0,
                corruptedAmountsFixed = 0,
                currencyCacheStatus = "Error loading currency cache",
                schemaIntegrityOk = false,
                logDetails = logs
            )
        }
    }
}
