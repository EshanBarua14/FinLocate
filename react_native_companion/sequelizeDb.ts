/**
 * Sequelize ORM Configuration & Model Synchronization Helper
 * Manages SQLite database connection, directory creation, model definitions,
 * and startup table synchronization for backend and companion desktop services.
 */

import path from 'path';
import fs from 'fs';

export interface SequelizeConfigOptions {
  storagePath?: string;
  logging?: boolean;
}

/**
 * Initializes and configures a Sequelize ORM connection instance using SQLite dialect.
 * Automatically verifies target directory existence and creates the database file.
 */
export function initializeSequelizeConnection(options: SequelizeConfigOptions = {}) {
  let Sequelize: any;
  try {
    Sequelize = require('sequelize').Sequelize;
  } catch (e) {
    console.warn('Sequelize package is not installed in the companion environment.');
    return null;
  }

  const defaultPath = path.join(process.cwd(), 'data', 'finance_ledger.sqlite');
  const targetStoragePath = options.storagePath || defaultPath;
  const targetDir = path.dirname(targetStoragePath);

  // Guarantee target directory exists before SQLite connection is initiated
  if (!fs.existsSync(targetDir)) {
    try {
      fs.mkdirSync(targetDir, { recursive: true });
      console.log(`Created database directory: ${targetDir}`);
    } catch (err) {
      console.error(`Failed to create directory ${targetDir}:`, err);
    }
  }

  const sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: targetStoragePath,
    logging: options.logging ?? false,
    dialectOptions: {
      mode: 6 // Read/Write/Create
    }
  });

  return sequelize;
}

/**
 * Safely synchronizes all defined Sequelize models with the SQLite database file on startup.
 */
export async function syncDatabaseModels(sequelizeInstance: any): Promise<boolean> {
  if (!sequelizeInstance) {
    console.warn('Cannot sync models: Sequelize instance is null.');
    return false;
  }

  try {
    await sequelizeInstance.authenticate();
    console.log('Sequelize authentication successful.');

    // SQLite does not support complex ALTER TABLE constraints; use safe sync options
    await sequelizeInstance.sync({ force: false });
    console.log('Sequelize models successfully synchronized with SQLite database.');
    return true;
  } catch (error: any) {
    console.error('Error synchronizing Sequelize database models:', error?.message || error);
    try {
      // Fallback sync attempt
      await sequelizeInstance.sync();
      console.log('Sequelize models synchronized on retry.');
      return true;
    } catch (retryError) {
      console.error('Fatal failure during Sequelize sync retry:', retryError);
      return false;
    }
  }
}
