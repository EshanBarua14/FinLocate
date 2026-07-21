/**
 * Database Schema Definitions for Expo SQLite (expenses & categories)
 * This schema defines offline-first storage fields for personal finance tracking,
 * including multi-currency and localized tax compliance attributes.
 */

export interface CategorySchema {
  id: number;
  name: string;
  is_income: number; // 0 = Expense, 1 = Income
  icon_name: string;
}

export interface ExpenseSchema {
  id: number;
  description: string;
  amount: number;
  date: number; // Timestamp in milliseconds
  category_id: number;
  currency: string; // e.g. "USD", "EUR", "INR", "BDT"
  tax_relevant: number; // 0 = No, 1 = Yes
  tax_rate: number; // e.g. 8.25, 19.0, 15.0 (percentage)
  tags: string; // Comma-separated list e.g. "business,travel,deductible"
  notes?: string;
}

export const DATABASE_SCHEMA_SQL = {
  createCategoriesTable: `
    CREATE TABLE IF NOT EXISTS categories (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL UNIQUE,
      is_income INTEGER DEFAULT 0,
      icon_name TEXT DEFAULT 'folder'
    );
  `,

  createExpensesTable: `
    CREATE TABLE IF NOT EXISTS expenses (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      description TEXT NOT NULL,
      amount REAL NOT NULL,
      date INTEGER NOT NULL,
      category_id INTEGER NOT NULL,
      currency TEXT NOT NULL,
      tax_relevant INTEGER DEFAULT 0,
      tax_rate REAL DEFAULT 0.0,
      tags TEXT,
      notes TEXT,
      FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
    );
  `,

  createIndexes: [
    `CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date);`,
    `CREATE INDEX IF NOT EXISTS idx_expenses_category_id ON expenses(category_id);`,
    `CREATE INDEX IF NOT EXISTS idx_expenses_tax_relevant ON expenses(tax_relevant);`
  ],

  initialCategories: [
    { name: 'Food & Dining', is_income: 0, icon_name: 'restaurant' },
    { name: 'Rent & Utilities', is_income: 0, icon_name: 'home' },
    { name: 'Transportation', is_income: 0, icon_name: 'car' },
    { name: 'Health & Medical', is_income: 0, icon_name: 'heartbeat' },
    { name: 'Business Travel', is_income: 0, icon_name: 'briefcase' },
    { name: 'Salary', is_income: 1, icon_name: 'cash' },
    { name: 'Charitable Donations', is_income: 0, icon_name: 'gift' },
    { name: 'Education & Training', is_income: 0, icon_name: 'book' }
  ]
};
