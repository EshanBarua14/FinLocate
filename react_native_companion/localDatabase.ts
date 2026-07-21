import * as SQLite from 'expo-sqlite';
import { DATABASE_SCHEMA_SQL, ExpenseSchema, CategorySchema } from './databaseSchema';

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
 * Bootstraps the local database tables, runs indexes, and inserts initial seed data
 * for budget categories if they are currently empty.
 */
export async function initializeDatabase(): Promise<void> {
  const db = await getDatabase();

  // Execute table creation statements sequentially
  await db.execAsync(DATABASE_SCHEMA_SQL.createCategoriesTable);
  await db.execAsync(DATABASE_SCHEMA_SQL.createExpensesTable);

  // Create indexes for blazing-fast query lookups
  for (const createIndexSql of DATABASE_SCHEMA_SQL.createIndexes) {
    await db.execAsync(createIndexSql);
  }

  // Seed categories table if it is currently empty
  const categoryCountResult = await db.getFirstAsync<{ count: number }>(
    'SELECT COUNT(*) as count FROM categories;'
  );

  if (categoryCountResult && categoryCountResult.count === 0) {
    console.log('Seeding initial offline expense categories...');
    for (const cat of DATABASE_SCHEMA_SQL.initialCategories) {
      await db.runAsync(
        'INSERT INTO categories (name, is_income, icon_name) VALUES (?, ?, ?);',
        [cat.name, cat.is_income, cat.icon_name]
      );
    }
    console.log('Seeding completed successfully!');
  }
}

/**
 * Inserts a new expense transaction into the SQLite database.
 */
export async function insertExpense(expense: Omit<ExpenseSchema, 'id'>): Promise<number> {
  const db = await getDatabase();
  const result = await db.runAsync(
    `INSERT INTO expenses (description, amount, date, category_id, currency, tax_relevant, tax_rate, tags, notes)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);`,
    [
      expense.description,
      expense.amount,
      expense.date,
      expense.category_id,
      expense.currency,
      expense.tax_relevant,
      expense.tax_rate,
      expense.tags,
      expense.notes || null,
    ]
  );
  return result.lastInsertRowId;
}

/**
 * Retrieves all saved expenses with Category name resolved via Join.
 */
export async function getAllExpenses(): Promise<(ExpenseSchema & { category_name: string })[]> {
  const db = await getDatabase();
  return await db.getAllAsync<ExpenseSchema & { category_name: string }>(
    `SELECT e.*, c.name as category_name 
     FROM expenses e 
     LEFT JOIN categories c ON e.category_id = c.id 
     ORDER BY e.date DESC;`
  );
}

/**
 * Retrieves all categories available for budgeting/tagging.
 */
export async function getAllCategories(): Promise<CategorySchema[]> {
  const db = await getDatabase();
  return await db.getAllAsync<CategorySchema>('SELECT * FROM categories ORDER BY name ASC;');
}

/**
 * Deletes a given expense record by ID.
 */
export async function deleteExpense(id: number): Promise<void> {
  const db = await getDatabase();
  await db.runAsync('DELETE FROM expenses WHERE id = ?;', [id]);
}
