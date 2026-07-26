/**
 * Database Schema Definitions for Offline Storage (Expenses, Income, Categories, & Tax Thresholds)
 * Supports multi-currency conversion snapshots, income records, and category tax-deductible percentages.
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
  currency: string; // e.g. "USD", "EUR", "INR", "BDT", "GBP"
  tax_relevant: number; // 0 = No, 1 = Yes
  tax_rate: number; // e.g. 8.25, 19.0, 15.0 (percentage)
  tax_deductible_percentage: number; // Custom deductible % e.g. 100, 80, 50
  tags: string; // Comma-separated tags
  notes?: string;
}

export interface IncomeSchema {
  id: number;
  source: string; // e.g. "Employer Salary", "Freelance Client", "Investments"
  amount: number;
  date: number; // Timestamp in milliseconds
  category_id: number;
  currency: string;
  is_taxable: number; // 0 = Exempt, 1 = Taxable Income
  notes?: string;
}

export interface TaxThresholdSchema {
  id: number;
  country_code: string; // e.g. "US", "DE", "IN", "BD"
  category_id: number;
  custom_deductible_percentage: number; // 0.0 to 100.0
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
      currency TEXT NOT NULL DEFAULT 'USD',
      tax_relevant INTEGER DEFAULT 0,
      tax_rate REAL DEFAULT 0.0,
      tax_deductible_percentage REAL DEFAULT 100.0,
      tags TEXT,
      notes TEXT,
      FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
    );
  `,

  createIncomeTable: `
    CREATE TABLE IF NOT EXISTS income (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      source TEXT NOT NULL,
      amount REAL NOT NULL,
      date INTEGER NOT NULL,
      category_id INTEGER NOT NULL,
      currency TEXT NOT NULL DEFAULT 'USD',
      is_taxable INTEGER DEFAULT 1,
      notes TEXT,
      FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
    );
  `,

  createTaxThresholdsTable: `
    CREATE TABLE IF NOT EXISTS tax_thresholds (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      country_code TEXT NOT NULL,
      category_id INTEGER NOT NULL,
      custom_deductible_percentage REAL NOT NULL DEFAULT 100.0,
      UNIQUE(country_code, category_id)
    );
  `,

  createIndexes: [
    `CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date);`,
    `CREATE INDEX IF NOT EXISTS idx_expenses_category_id ON expenses(category_id);`,
    `CREATE INDEX IF NOT EXISTS idx_expenses_tax_relevant ON expenses(tax_relevant);`,
    `CREATE INDEX IF NOT EXISTS idx_income_date ON income(date);`,
    `CREATE INDEX IF NOT EXISTS idx_tax_thresholds_country ON tax_thresholds(country_code);`
  ],

  initialCategories: [
    { name: 'Food & Dining', is_income: 0, icon_name: 'restaurant' },
    { name: 'Rent & Utilities', is_income: 0, icon_name: 'home' },
    { name: 'Transportation', is_income: 0, icon_name: 'car' },
    { name: 'Health & Medical', is_income: 0, icon_name: 'heartbeat' },
    { name: 'Business Travel', is_income: 0, icon_name: 'briefcase' },
    { name: 'Salary & Wages', is_income: 1, icon_name: 'cash' },
    { name: 'Freelance Income', is_income: 1, icon_name: 'laptop' },
    { name: 'Charitable Donations', is_income: 0, icon_name: 'gift' },
    { name: 'Education & Training', is_income: 0, icon_name: 'book' }
  ]
};
