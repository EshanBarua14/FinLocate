package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object JsonBackupMigrationUtility {
    private const val TAG = "JsonBackupMigration"

    /**
     * Scans app storage directories for previously exported JSON backup files.
     */
    fun scanForJsonBackups(context: Context): List<File> {
        val foundFiles = mutableListOf<File>()
        val dirsToScan = listOfNotNull(
            context.cacheDir,
            context.filesDir,
            context.getExternalFilesDir(null)
        )

        dirsToScan.forEach { dir ->
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.extension.equals("json", ignoreCase = true) &&
                        (file.name.contains("wealthflow", ignoreCase = true) || file.name.contains("backup", ignoreCase = true))
                    ) {
                        foundFiles.add(file)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning directory ${dir.absolutePath}", e)
            }
        }
        return foundFiles.sortedByDescending { it.lastModified() }
    }

    /**
     * Parses a JSON backup file and restores accounts, categories, and transaction records into Room DB.
     */
    suspend fun restoreFromJsonFile(context: Context, db: AppDatabase, file: File): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            val root = JSONObject(content)
            val txArray: JSONArray = root.optJSONArray("transactions") ?: JSONArray()

            if (txArray.length() == 0) {
                return@withContext Pair(false, "Selected JSON backup file contains no transaction records.")
            }

            var restoredCount = 0
            val accountDao = db.accountDao()
            val categoryDao = db.categoryDao()
            val transactionDao = db.transactionDao()

            // Ensure fallback default account and category
            val accounts = accountDao.getAllAccountsStatic()
            var defaultAcc = accounts.firstOrNull()
            if (defaultAcc == null) {
                val accId = accountDao.insertAccount(AccountEntity(name = "Restored Cash Account", type = "CASH", balance = 0.0, currency = "USD"))
                defaultAcc = AccountEntity(id = accId, name = "Restored Cash Account", type = "CASH", balance = 0.0, currency = "USD")
            }

            val categories = categoryDao.getAllCategoriesStatic()
            var defaultCat = categories.firstOrNull()
            if (defaultCat == null) {
                val catId = categoryDao.insertCategory(CategoryEntity(name = "Restored Expenses", iconName = "category", isIncome = false))
                defaultCat = CategoryEntity(id = catId, name = "Restored Expenses", iconName = "category", isIncome = false)
            }

            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val type = obj.optString("type", "EXPENSE")
                val amount = obj.optDouble("amount", 0.0)
                val merchant = obj.optString("merchant", "Imported Ledger")
                val notes = obj.optString("notes", "")
                val tags = obj.optString("tags", "")
                val isTaxDeductible = obj.optBoolean("isTaxDeductible", false)
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                val tx = TransactionEntity(
                    type = type,
                    amount = amount,
                    merchant = merchant,
                    categoryId = defaultCat.id,
                    accountId = defaultAcc.id,
                    notes = notes,
                    tags = tags,
                    isTaxDeductible = isTaxDeductible,
                    timestamp = timestamp
                )
                transactionDao.insertTransaction(tx)
                restoredCount++
            }

            Pair(true, "Successfully restored $restoredCount transaction records from '${file.name}'!")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring JSON backup from ${file.name}", e)
            Pair(false, "JSON restore error: ${e.localizedMessage}")
        }
    }
}
