/**
 * better-sqlite3 / Node SQLite Adapter Layer
 * Provides direct synchronous database access for Node.js / Desktop runtimes or offline backend services.
 */

import { DATABASE_SCHEMA_SQL, ExpenseSchema, IncomeSchema, CategorySchema } from './databaseSchema';

export class BetterSqliteFinanceStorage {
  private db: any;

  constructor(dbPath: string = ':memory:') {
    try {
      const Database = require('better-sqlite3');
      this.db = new Database(dbPath);
      this.initSchema();
    } catch (e) {
      console.warn('better-sqlite3 native driver not available in current environment. Using memory fallback stub.');
      this.db = null;
    }
  }

  private initSchema() {
    if (!this.db) return;
    this.db.exec(DATABASE_SCHEMA_SQL.createCategoriesTable);
    this.db.exec(DATABASE_SCHEMA_SQL.createExpensesTable);
    this.db.exec(DATABASE_SCHEMA_SQL.createIncomeTable);
    this.db.exec(DATABASE_SCHEMA_SQL.createTaxThresholdsTable);

    DATABASE_SCHEMA_SQL.createIndexes.forEach(idxSql => {
      this.db.exec(idxSql);
    });
  }

  public addExpense(expense: Omit<ExpenseSchema, 'id'>): number {
    if (!this.db) return Date.now();
    const stmt = this.db.prepare(`
      INSERT INTO expenses (description, amount, date, category_id, currency, tax_relevant, tax_rate, tax_deductible_percentage, tags, notes)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `);
    const info = stmt.run(
      expense.description,
      expense.amount,
      expense.date,
      expense.category_id,
      expense.currency,
      expense.tax_relevant,
      expense.tax_rate,
      expense.tax_deductible_percentage,
      expense.tags,
      expense.notes || null
    );
    return info.lastInsertRowid;
  }

  public addIncome(income: Omit<IncomeSchema, 'id'>): number {
    if (!this.db) return Date.now();
    const stmt = this.db.prepare(`
      INSERT INTO income (source, amount, date, category_id, currency, is_taxable, notes)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `);
    const info = stmt.run(
      income.source,
      income.amount,
      income.date,
      income.category_id,
      income.currency,
      income.is_taxable,
      income.notes || null
    );
    return info.lastInsertRowid;
  }

  public getAllExpenses(): ExpenseSchema[] {
    if (!this.db) return [];
    return this.db.prepare('SELECT * FROM expenses ORDER BY date DESC').all();
  }

  public getAllIncome(): IncomeSchema[] {
    if (!this.db) return [];
    return this.db.prepare('SELECT * FROM income ORDER BY date DESC').all();
  }
}
