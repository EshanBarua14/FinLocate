/**
 * WealthFlow Finance Tracker - Production-Grade Secure Sync Gateway
 * Zero-Knowledge Cloud Storage & Authenticated REST API Hub
 */

const express = require('express');
const cors = require('cors');
const { Sequelize, DataTypes, Op } = require('sequelize');
const path = require('path');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;
const JWT_SECRET = process.env.JWT_SECRET || 'WealthFlowSuperSecretJWTKey2026';

// Middleware
app.use(cors());
app.use(express.json({ limit: '50mb' })); // Allow larger backup payloads

// --- DATABASE CONNECTION CONFIGURATION ---
let sequelize;
if (process.env.DATABASE_URL) {
  console.log('Connecting to PostgreSQL production database via Sequelize...');
  sequelize = new Sequelize(process.env.DATABASE_URL, {
    dialect: 'postgres',
    protocol: 'postgres',
    dialectOptions: {
      ssl: process.env.NODE_ENV === 'production' ? {
        require: true,
        rejectUnauthorized: false
      } : false
    },
    logging: false
  });
} else {
  console.log('No DATABASE_URL environment variable found. Falling back to local SQLite...');
  sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: path.join(__dirname, 'wealthflow_cloud.sqlite'),
    logging: false
  });
}

// --- SEQUELIZE MODELS DEFINITIONS ---

// 1. User Model
const User = sequelize.define('User', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true
  },
  username: {
    type: DataTypes.STRING,
    allowNull: false
  },
  email: {
    type: DataTypes.STRING,
    allowNull: false,
    unique: true,
    validate: { isEmail: true }
  },
  passwordHash: {
    type: DataTypes.STRING,
    allowNull: false
  },
  taxProfile: {
    type: DataTypes.STRING,
    defaultValue: 'USA'
  }
});

// 2. Expense Model (REST direct sync storage)
const Expense = sequelize.define('Expense', {
  id: {
    type: DataTypes.STRING,
    primaryKey: true
  },
  userEmail: {
    type: DataTypes.STRING,
    allowNull: false
  },
  amount: {
    type: DataTypes.DOUBLE,
    allowNull: false
  },
  type: {
    type: DataTypes.STRING,
    allowNull: false // 'Income' or 'Expense'
  },
  merchant: {
    type: DataTypes.STRING,
    defaultValue: ''
  },
  categoryName: {
    type: DataTypes.STRING,
    defaultValue: ''
  },
  accountName: {
    type: DataTypes.STRING,
    defaultValue: ''
  },
  notes: {
    type: DataTypes.TEXT,
    defaultValue: ''
  },
  timestamp: {
    type: DataTypes.BIGINT,
    allowNull: false
  },
  isTaxDeductible: {
    type: DataTypes.BOOLEAN,
    defaultValue: false
  },
  taxRate: {
    type: DataTypes.DOUBLE,
    defaultValue: 0.0
  }
});

// 3. Account Model (REST direct sync storage)
const Account = sequelize.define('Account', {
  id: {
    type: DataTypes.STRING,
    primaryKey: true
  },
  userEmail: {
    type: DataTypes.STRING,
    allowNull: false
  },
  name: {
    type: DataTypes.STRING,
    allowNull: false
  },
  type: {
    type: DataTypes.STRING,
    allowNull: false
  },
  balance: {
    type: DataTypes.DOUBLE,
    allowNull: false
  },
  currency: {
    type: DataTypes.STRING,
    allowNull: false
  },
  provider: {
    type: DataTypes.STRING,
    allowNull: false
  },
  updatedAt: {
    type: DataTypes.BIGINT,
    allowNull: false
  }
}, {
  timestamps: false
});

// 4. Backup Model (Zero-Knowledge secure vaults)
const Backup = sequelize.define('Backup', {
  passcodeHash: {
    type: DataTypes.STRING,
    primaryKey: true
  },
  encryptedData: {
    type: DataTypes.TEXT,
    allowNull: false
  },
  updatedAt: {
    type: DataTypes.BIGINT,
    allowNull: false
  }
}, {
  timestamps: false
});

// 5. Budget Model (REST direct sync storage)
const Budget = sequelize.define('Budget', {
  id: {
    type: DataTypes.STRING,
    primaryKey: true
  },
  userEmail: {
    type: DataTypes.STRING,
    allowNull: false
  },
  categoryId: {
    type: DataTypes.BIGINT,
    allowNull: false
  },
  categoryName: {
    type: DataTypes.STRING,
    allowNull: false
  },
  amount: {
    type: DataTypes.DOUBLE,
    allowNull: false
  },
  month: {
    type: DataTypes.STRING, // format "YYYY-MM"
    allowNull: false
  }
}, {
  timestamps: false
});

// 6. Notification Model (Backend Budget Limit Overruns)
const Notification = sequelize.define('Notification', {
  id: {
    type: DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey: true
  },
  userEmail: {
    type: DataTypes.STRING,
    allowNull: false
  },
  title: {
    type: DataTypes.STRING,
    allowNull: false
  },
  message: {
    type: DataTypes.TEXT,
    allowNull: false
  },
  categoryName: {
    type: DataTypes.STRING,
    allowNull: false
  },
  timestamp: {
    type: DataTypes.BIGINT,
    allowNull: false
  },
  isRead: {
    type: DataTypes.BOOLEAN,
    defaultValue: false
  }
}, {
  timestamps: false
});

// Sync Database Tables
sequelize.sync({ alter: true })
  .then(() => {
    console.log('Database tables successfully synchronized & ready.');
  })
  .catch((err) => {
    console.error('Failed to sync tables:', err);
  });

// --- JWT AUTHENTICATION MIDDLEWARE ---
const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ success: false, error: 'Access token is missing or unauthorized.' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ success: false, error: 'Access token is invalid or has expired.' });
    }
    req.user = user;
    next();
  });
};

// --- REST API ENDPOINTS ---

/**
 * Health & Connectivity Status check
 */
app.get('/api/status', (req, res) => {
  res.json({
    status: 'healthy',
    service: 'WealthFlow Secure Cloud Sync Gateway',
    version: '2.0.0',
    db_dialect: sequelize.getDialect(),
    security: 'JWT / Zero-Knowledge AES-256 Enabled',
    timestamp: new Date().toISOString()
  });
});

/**
 * USER AUTHENTICATION: Registration
 */
app.post('/api/auth/register', async (req, res, next) => {
  try {
    const { username, email, password, taxProfile } = req.body;

    if (!username || !email || !password) {
      return res.status(400).json({ success: false, error: 'Please provide username, email, and password.' });
    }

    const existingUser = await User.findOne({ where: { email } });
    if (existingUser) {
      return res.status(400).json({ success: false, error: 'Email is already registered.' });
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({
      username,
      email,
      passwordHash,
      taxProfile: taxProfile || 'USA'
    });

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });

    res.status(201).json({
      success: true,
      message: 'User registered successfully!',
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        taxProfile: user.taxProfile
      }
    });
  } catch (error) {
    next(error);
  }
});

/**
 * USER AUTHENTICATION: Login
 */
app.post('/api/auth/login', async (req, res, next) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ success: false, error: 'Please enter email and password.' });
    }

    const user = await User.findOne({ where: { email } });
    if (!user) {
      return res.status(401).json({ success: false, error: 'Invalid email or password.' });
    }

    const isMatch = await bcrypt.compare(password, user.passwordHash);
    if (!isMatch) {
      return res.status(401).json({ success: false, error: 'Invalid email or password.' });
    }

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });

    res.json({
      success: true,
      message: 'Logged in successfully!',
      token,
      user: {
        id: user.id,
        username: user.username,
        email: user.email,
        taxProfile: user.taxProfile
      }
    });
  } catch (error) {
    next(error);
  }
});

/**
 * EXPENSES CRUD: Retrieve (supporting localized tax category filtering, search, and date filters)
 */
app.get('/api/expenses', authenticateToken, async (req, res, next) => {
  try {
    const { isTaxDeductible, categoryName, category, type, startDate, endDate, keyword } = req.query;
    const whereClause = { userEmail: req.user.email };

    if (isTaxDeductible !== undefined) {
      whereClause.isTaxDeductible = isTaxDeductible === 'true';
    }
    const cat = categoryName || category;
    if (cat) {
      whereClause.categoryName = cat;
    }
    if (type) {
      whereClause.type = type;
    }
    if (startDate || endDate) {
      whereClause.timestamp = {};
      if (startDate) {
        whereClause.timestamp[Op.gte] = isNaN(Number(startDate)) ? new Date(startDate).getTime() : parseInt(startDate);
      }
      if (endDate) {
        whereClause.timestamp[Op.lte] = isNaN(Number(endDate)) ? new Date(endDate).getTime() : parseInt(endDate);
      }
    }
    if (keyword) {
      whereClause[Op.or] = [
        { merchant: { [Op.like]: `%${keyword}%` } },
        { notes: { [Op.like]: `%${keyword}%` } },
        { categoryName: { [Op.like]: `%${keyword}%` } }
      ];
    }

    const expenses = await Expense.findAll({
      where: whereClause,
      order: [['timestamp', 'DESC']]
    });

    res.json({ success: true, count: expenses.length, expenses });
  } catch (error) {
    next(error);
  }
});

// Helper function to check and trigger budget exceed notifications
async function triggerBudgetLimitNotificationCheck(userEmail, expense) {
  try {
    const { categoryName, timestamp, amount, type } = expense;
    if (!categoryName || type !== 'Expense') return;

    // Determine Year-Month (YYYY-MM) from the expense timestamp
    const dateObj = new Date(Number(timestamp));
    const year = dateObj.getFullYear();
    const monthPart = String(dateObj.getMonth() + 1).padStart(2, '0');
    const monthStr = `${year}-${monthPart}`;

    // Find if a budget exists for this user, category, and month
    const budget = await Budget.findOne({
      where: { userEmail, categoryName, month: monthStr }
    });

    if (!budget) return; // No budget defined for this category/month

    // Calculate total spent in this category and month
    const startOfMonth = new Date(year, dateObj.getMonth(), 1).getTime();
    const endOfMonth = new Date(year, dateObj.getMonth() + 1, 1).getTime() - 1;

    const expensesInMonth = await Expense.findAll({
      where: {
        userEmail,
        categoryName,
        type: 'Expense',
        timestamp: {
          [Op.between]: [startOfMonth, endOfMonth]
        }
      }
    });

    const totalSpent = expensesInMonth.reduce((sum, exp) => sum + exp.amount, 0);

    if (totalSpent > budget.amount) {
      const overspentAmount = totalSpent - budget.amount;
      const alertTitle = `Budget Limit Exceeded: ${categoryName}`;
      const alertMsg = `Your total expenses for ${categoryName} in ${monthStr} have reached ${totalSpent.toFixed(2)}, which exceeds your budget of ${budget.amount.toFixed(2)} by ${overspentAmount.toFixed(2)}!`;

      // Check if we already have this exact notification to avoid spamming
      const existingNotif = await Notification.findOne({
        where: {
          userEmail,
          title: alertTitle,
          message: alertMsg
        }
      });

      if (!existingNotif) {
        await Notification.create({
          userEmail,
          title: alertTitle,
          message: alertMsg,
          categoryName,
          timestamp: Date.now()
        });
        console.log(`[Budget Alarm] Limit overrun detected for ${categoryName}: spent=${totalSpent}, limit=${budget.amount}`);
      }
    }
  } catch (err) {
    console.error('Error during budget limit check:', err);
  }
}

/**
 * EXPENSES CRUD: Create/Upsert (Single or Bulk) with Budget Alarm Checks
 */
app.post('/api/expenses', authenticateToken, async (req, res, next) => {
  try {
    const data = Array.isArray(req.body) ? req.body : [req.body];
    const upsertedExpenses = [];

    for (const item of data) {
      const expenseId = item.id || Math.random().toString(36).substring(2, 11);
      
      const [expense, created] = await Expense.upsert({
        id: expenseId,
        userEmail: req.user.email,
        amount: parseFloat(item.amount) || 0.0,
        type: item.type || 'Expense',
        merchant: item.merchant || '',
        categoryName: item.categoryName || '',
        accountName: item.accountName || '',
        notes: item.notes || '',
        timestamp: item.timestamp || Date.now(),
        isTaxDeductible: item.isTaxDeductible === true || item.isTaxDeductible === 1,
        taxRate: parseFloat(item.taxRate) || 0.0
      });
      upsertedExpenses.push(expense);

      // Trigger modern budget alarm check
      await triggerBudgetLimitNotificationCheck(req.user.email, expense);
    }

    res.json({ success: true, count: upsertedExpenses.length, message: 'Expense ledger synchronized.' });
  } catch (error) {
    next(error);
  }
});

/**
 * BUDGETS: Retrieve user budget settings
 */
app.get('/api/budgets', authenticateToken, async (req, res, next) => {
  try {
    const budgets = await Budget.findAll({ where: { userEmail: req.user.email } });
    res.json({ success: true, count: budgets.length, budgets });
  } catch (error) {
    next(error);
  }
});

/**
 * BUDGETS: Sync / Upsert Bulk from client
 */
app.post('/api/budgets', authenticateToken, async (req, res, next) => {
  try {
    const data = Array.isArray(req.body) ? req.body : [req.body];
    const upsertedBudgets = [];

    for (const item of data) {
      const budgetId = item.id || `${req.user.email}_${item.categoryId}_${item.month}`;
      const [budget, created] = await Budget.upsert({
        id: budgetId,
        userEmail: req.user.email,
        categoryId: item.categoryId || 0,
        categoryName: item.categoryName || 'Unknown',
        amount: parseFloat(item.amount) || 0.0,
        month: item.month || ''
      });
      upsertedBudgets.push(budget);
    }

    res.json({ success: true, count: upsertedBudgets.length, message: 'Budgets synchronized successfully.' });
  } catch (error) {
    next(error);
  }
});

/**
 * NOTIFICATIONS: Retrieve backend generated system alerts
 */
app.get('/api/notifications', authenticateToken, async (req, res, next) => {
  try {
    const notifications = await Notification.findAll({
      where: { userEmail: req.user.email },
      order: [['timestamp', 'DESC']]
    });
    res.json({ success: true, count: notifications.length, notifications });
  } catch (error) {
    next(error);
  }
});

/**
 * EXPENSES CRUD: Update Single Expense Record
 */
app.put('/api/expenses/:id', authenticateToken, async (req, res, next) => {
  try {
    const { id } = req.params;
    const { amount, type, merchant, categoryName, accountName, notes, timestamp, isTaxDeductible, taxRate } = req.body;

    const expense = await Expense.findOne({ where: { id, userEmail: req.user.email } });
    if (!expense) {
      return res.status(404).json({ success: false, error: 'Expense record not found.' });
    }

    await expense.update({
      amount: amount !== undefined ? parseFloat(amount) : expense.amount,
      type: type || expense.type,
      merchant: merchant !== undefined ? merchant : expense.merchant,
      categoryName: categoryName !== undefined ? categoryName : expense.categoryName,
      accountName: accountName !== undefined ? accountName : expense.accountName,
      notes: notes !== undefined ? notes : expense.notes,
      timestamp: timestamp || expense.timestamp,
      isTaxDeductible: isTaxDeductible !== undefined ? isTaxDeductible : expense.isTaxDeductible,
      taxRate: taxRate !== undefined ? parseFloat(taxRate) : expense.taxRate
    });

    res.json({ success: true, message: 'Expense updated successfully.', expense });
  } catch (error) {
    next(error);
  }
});

/**
 * EXPENSES CRUD: Delete Expense Record
 */
app.delete('/api/expenses/:id', authenticateToken, async (req, res, next) => {
  try {
    const { id } = req.params;
    const deletedCount = await Expense.destroy({ where: { id, userEmail: req.user.email } });

    if (deletedCount === 0) {
      return res.status(404).json({ success: false, error: 'Expense record not found or unauthorized.' });
    }

    res.json({ success: true, message: 'Expense deleted successfully.' });
  } catch (error) {
    next(error);
  }
});

/**
 * ACCOUNTS: Fetch
 */
app.get('/api/accounts', authenticateToken, async (req, res, next) => {
  try {
    const accounts = await Account.findAll({ where: { userEmail: req.user.email } });
    res.json({ success: true, count: accounts.length, accounts });
  } catch (error) {
    next(error);
  }
});

/**
 * ACCOUNTS: Upsert / Bulk Synced Sync
 */
app.post('/api/accounts', authenticateToken, async (req, res, next) => {
  try {
    const data = Array.isArray(req.body) ? req.body : [req.body];
    const upsertedAccounts = [];

    for (const item of data) {
      const accId = item.id || Math.random().toString(36).substring(2, 11);
      const [account, created] = await Account.upsert({
        id: accId,
        userEmail: req.user.email,
        name: item.name,
        type: item.type,
        balance: parseFloat(item.balance) || 0.0,
        currency: item.currency || 'USD',
        provider: item.provider || 'Local',
        updatedAt: item.updatedAt || Date.now()
      });
      upsertedAccounts.push(account);
    }

    res.json({ success: true, count: upsertedAccounts.length, message: 'Accounts synchronized successfully.' });
  } catch (error) {
    next(error);
  }
});

/**
 * SECURE BACKUPS: Upload / Backup full encrypted database state (Zero-Knowledge)
 */
app.post('/api/sync/backup', async (req, res, next) => {
  try {
    const { passcodeHash, encryptedData } = req.body;

    if (!passcodeHash || !encryptedData) {
      return res.status(400).json({ success: false, error: 'Missing passcodeHash or encryptedData.' });
    }

    await Backup.upsert({
      passcodeHash,
      encryptedData,
      updatedAt: Date.now()
    });

    res.json({
      success: true,
      message: 'Zero-knowledge database backup saved successfully.',
      timestamp: Date.now()
    });
  } catch (error) {
    next(error);
  }
});

/**
 * SECURE BACKUPS: Download / Restore encrypted database state (Zero-Knowledge)
 */
app.post('/api/sync/restore', async (req, res, next) => {
  try {
    const { passcodeHash } = req.body;

    if (!passcodeHash) {
      return res.status(400).json({ success: false, error: 'Missing passcodeHash.' });
    }

    const backup = await Backup.findByPk(passcodeHash);
    if (!backup) {
      return res.status(404).json({ success: false, error: 'No cloud backups found for the client passcode.' });
    }

    res.json({
      success: true,
      encryptedData: backup.encryptedData,
      updatedAt: backup.updatedAt
    });
  } catch (error) {
    next(error);
  }
});

// --- GLOBAL ERROR HANDLING MIDDLEWARE ---
app.use((err, req, res, next) => {
  console.error('=== UNHANDLED EXCEPTION CATCH ===');
  console.error(err.stack || err);
  console.error('=================================');
  
  const statusCode = err.status || 500;
  const message = err.message || 'Internal Server Error';
  
  res.status(statusCode).json({
    success: false,
    error: message,
    stack: process.env.NODE_ENV === 'development' ? err.stack : undefined,
    timestamp: new Date().toISOString()
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`================================================================`);
  console.log(` WealthFlow Production API Server is running on port ${PORT}`);
  console.log(` Healthcheck: http://localhost:${PORT}/api/status`);
  console.log(`================================================================`);
});
