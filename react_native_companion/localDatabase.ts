import * as SQLite from 'expo-sqlite';
import {
  DATABASE_SCHEMA_SQL,
  ExpenseSchema,
  IncomeSchema,
  CategorySchema,
  TaxThresholdSchema,
} from './databaseSchema';

// Connection instance to the SQLite database
let dbInstance: SQLite.SQLiteDatabase | null = null;

/**
 * Retrieves or initializes the SQLite database instance.
 */
export async function getDatabase(): Promise<SQLite.SQLiteDatabase> {
  if (dbInstance) {
    return dbInstance;
  }
  // Open the SQLite file database locally
  dbInstance = await SQLite.openDatabaseAsync('finance_ledger_offline.db');
  return dbInstance;
}

/**
 * Bootstraps local database tables, indexes, and initial category seeds.
 */
export async function initializeDatabase(): Promise<void> {
  const db = await getDatabase();

  // Execute table creation statements
  await db.execAsync(DATABASE_SCHEMA_SQL.createCategoriesTable);
  await db.execAsync(DATABASE_SCHEMA_SQL.createExpensesTable);
  await db.execAsync(DATABASE_SCHEMA_SQL.createIncomeTable);
  await db.execAsync(DATABASE_SCHEMA_SQL.createTaxThresholdsTable);

  // Create indexes for fast query lookups
  for (const createIndexSql of DATABASE_SCHEMA_SQL.createIndexes) {
    await db.execAsync(createIndexSql);
  }

  // Seed categories table if empty
  const categoryCountResult = await db.getFirstAsync<{ count: number }>(
    'SELECT COUNT(*) as count FROM categories;'
  );

  if (categoryCountResult && categoryCountResult.count === 0) {
    for (const cat of DATABASE_SCHEMA_SQL.initialCategories) {
      await db.runAsync(
        'INSERT INTO categories (name, is_income, icon_name) VALUES (?, ?, ?);',
        [cat.name, cat.is_income, cat.icon_name]
      );
    }
  }
}

/**
 * Inserts a new expense transaction into SQLite database.
 */
export async function insertExpense(
  expense: Omit<ExpenseSchema, 'id'>
): Promise<number> {
  const db = await getDatabase();
  const result = await db.runAsync(
    `INSERT INTO expenses (description, amount, date, category_id, currency, tax_relevant, tax_rate, tax_deductible_percentage, tags, notes)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
    [
      expense.description,
      expense.amount,
      expense.date,
      expense.category_id,
      expense.currency || 'USD',
      expense.tax_relevant,
      expense.tax_rate,
      expense.tax_deductible_percentage ?? 100.0,
      expense.tags || '',
      expense.notes || null,
    ]
  );
  return result.lastInsertRowId;
}

/**
 * Inserts a new income transaction into SQLite database.
 */
export async function insertIncome(
  income: Omit<IncomeSchema, 'id'>
): Promise<number> {
  const db = await getDatabase();
  const result = await db.runAsync(
    `INSERT INTO income (source, amount, date, category_id, currency, is_taxable, notes)
     VALUES (?, ?, ?, ?, ?, ?, ?);`,
    [
      income.source,
      income.amount,
      income.date,
      income.category_id,
      income.currency || 'USD',
      income.is_taxable,
      income.notes || null,
    ]
  );
  return result.lastInsertRowId;
}

/**
 * Retrieves all saved expenses with Category name resolved.
 */
export async function getAllExpenses(): Promise<
  (ExpenseSchema & { category_name: string })[]
> {
  const db = await getDatabase();
  return await db.getAllAsync<ExpenseSchema & { category_name: string }>(
    `SELECT e.*, c.name as category_name 
     FROM expenses e 
     LEFT JOIN categories c ON e.category_id = c.id 
     ORDER BY e.date DESC;`
  );
}

/**
 * Retrieves all saved income records with Category name resolved.
 */
export async function getAllIncome(): Promise<
  (IncomeSchema & { category_name: string })[]
> {
  const db = await getDatabase();
  return await db.getAllAsync<IncomeSchema & { category_name: string }>(
    `SELECT i.*, c.name as category_name 
     FROM income i 
     LEFT JOIN categories c ON i.category_id = c.id 
     ORDER BY i.date DESC;`
  );
}

/**
 * Saves or updates custom tax-deductible category threshold percentage for a country.
 */
export async function saveTaxThreshold(
  countryCode: string,
  categoryId: number,
  deductiblePercentage: number
): Promise<void> {
  const db = await getDatabase();
  await db.runAsync(
    `INSERT INTO tax_thresholds (country_code, category_id, custom_deductible_percentage)
     VALUES (?, ?, ?)
     ON CONFLICT(country_code, category_id) DO UPDATE SET custom_deductible_percentage = excluded.custom_deductible_percentage;`,
    [countryCode.toUpperCase().trim(), categoryId, deductiblePercentage]
  );
}

/**
 * Retrieves saved tax thresholds for a specific country code.
 */
export async function getTaxThresholdsForCountry(
  countryCode: string
): Promise<TaxThresholdSchema[]> {
  const db = await getDatabase();
  return await db.getAllAsync<TaxThresholdSchema>(
    `SELECT * FROM tax_thresholds WHERE country_code = ?;`,
    [countryCode.toUpperCase().trim()]
  );
}

/**
 * Retrieves all categories available for expenses or income.
 */
export async function getAllCategories(): Promise<CategorySchema[]> {
  const db = await getDatabase();
  return await db.getAllAsync<CategorySchema>(
    'SELECT * FROM categories ORDER BY name ASC;'
  );
}

/**
 * Deletes an expense record by ID.
 */
export async function deleteExpense(id: number): Promise<void> {
  const db = await getDatabase();
  await db.runAsync('DELETE FROM expenses WHERE id = ?;', [id]);
}
