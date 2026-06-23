package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        CountrySettingEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        InsightEntity::class,
        UserEntity::class,
        CountryConfigEntity::class,
        MatchingRuleEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 7, // bumped version to 7 to support transaction custom tags
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun countrySettingDao(): CountrySettingDao
    abstract fun insightDao(): InsightDao
    abstract fun userDao(): UserDao
    abstract fun countryConfigDao(): CountryConfigDao
    abstract fun matchingRuleDao(): MatchingRuleDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_tracker_db"
                )
                .fallbackToDestructiveMigration() // safe schema migration fallback during updates
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
