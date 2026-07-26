/**
 * Currency Conversion Utility based on stored exchange rate snapshots.
 * Ensures multi-currency financial records remain consistent and auditable globally.
 */

export interface ExchangeRateSnapshot {
  baseCurrency: string;
  timestamp: number;
  rates: Record<string, number>; // e.g. { "USD": 1.0, "EUR": 0.92, "GBP": 0.78, "INR": 83.5, "BDT": 117.2, "CAD": 1.36, "AUD": 1.51, "JPY": 155.4 }
}

// Default benchmark snapshot
export const DEFAULT_EXCHANGE_RATE_SNAPSHOT: ExchangeRateSnapshot = {
  baseCurrency: "USD",
  timestamp: 1721520000000,
  rates: {
    USD: 1.0,
    EUR: 0.92,
    GBP: 0.78,
    INR: 83.5,
    BDT: 117.2,
    CAD: 1.36,
    AUD: 1.51,
    JPY: 155.4,
  },
};

// In-memory snapshot cache sorted by timestamp
let snapshotCache: ExchangeRateSnapshot[] = [{ ...DEFAULT_EXCHANGE_RATE_SNAPSHOT }];
let activeSnapshot: ExchangeRateSnapshot = snapshotCache[0];

/**
 * Caches a new exchange rate snapshot and sets it as active if it is the latest.
 */
export function cacheSnapshot(snapshot: ExchangeRateSnapshot): ExchangeRateSnapshot {
  if (!snapshot || !snapshot.rates) {
    return activeSnapshot;
  }
  // Prevent duplicate timestamp entries
  snapshotCache = snapshotCache.filter(s => s.timestamp !== snapshot.timestamp);
  snapshotCache.push(snapshot);
  // Sort descending by timestamp
  snapshotCache.sort((a, b) => b.timestamp - a.timestamp);

  // Update active snapshot to the most recent one
  activeSnapshot = snapshotCache[0];
  return activeSnapshot;
}

/**
 * Retrieves the latest cached exchange rate snapshot.
 * Optionally filters by baseCurrency.
 */
export function getLatestSnapshot(baseCurrency?: string): ExchangeRateSnapshot {
  if (baseCurrency) {
    const code = baseCurrency.toUpperCase().trim();
    const match = snapshotCache.find(s => s.baseCurrency.toUpperCase() === code);
    if (match) return match;
  }
  
  if (snapshotCache.length > 0) {
    // Return latest cached snapshot
    return snapshotCache[0];
  }
  
  return activeSnapshot || DEFAULT_EXCHANGE_RATE_SNAPSHOT;
}

/**
 * Returns the currently active exchange rate snapshot.
 */
export function getActiveSnapshot(): ExchangeRateSnapshot {
  return getLatestSnapshot();
}

/**
 * Updates the active exchange rate snapshot table and adds it to cache.
 */
export function updateExchangeRateSnapshot(
  newRates: Record<string, number>,
  baseCurrency: string = "USD"
): ExchangeRateSnapshot {
  const newSnapshot: ExchangeRateSnapshot = {
    baseCurrency,
    timestamp: Date.now(),
    rates: { ...DEFAULT_EXCHANGE_RATE_SNAPSHOT.rates, ...activeSnapshot.rates, ...newRates },
  };
  return cacheSnapshot(newSnapshot);
}

/**
 * Converts an amount from one currency to another using the latest cached exchange rate snapshot.
 * 
 * @param amount Amount to convert
 * @param fromCurrency Source ISO code (e.g. "EUR")
 * @param toCurrency Target ISO code (e.g. "USD")
 * @param snapshot Optional custom snapshot override (if omitted, uses latest cached snapshot)
 * @returns Converted numerical amount rounded to 2 decimal places
 */
export function convertCurrency(
  amount: number,
  fromCurrency: string,
  toCurrency: string,
  snapshot?: ExchangeRateSnapshot
): number {
  if (isNaN(amount) || amount === 0) return 0;
  
  const fromCode = (fromCurrency || "USD").toUpperCase().trim();
  const toCode = (toCurrency || "USD").toUpperCase().trim();

  if (fromCode === toCode) return parseFloat(amount.toFixed(2));

  // Retrieve the latest cached snapshot if no custom override was provided
  const targetSnapshot = snapshot || getLatestSnapshot();
  const rates = targetSnapshot.rates || DEFAULT_EXCHANGE_RATE_SNAPSHOT.rates;

  // Fallback to default benchmark rates if missing in targetSnapshot
  const fromRate = rates[fromCode] ?? DEFAULT_EXCHANGE_RATE_SNAPSHOT.rates[fromCode] ?? 1.0;
  const toRate = rates[toCode] ?? DEFAULT_EXCHANGE_RATE_SNAPSHOT.rates[toCode] ?? 1.0;

  // Convert to base currency first, then to target currency
  const amountInBase = amount / fromRate;
  const convertedAmount = amountInBase * toRate;

  return parseFloat(convertedAmount.toFixed(2));
}

/**
 * Calculates total expenses converted to a specified target currency using latest cached exchange rates.
 */
export function calculateConvertedExpensesTotal(
  expenses: { amount: number; currency?: string }[],
  targetCurrency: string = "USD",
  snapshot?: ExchangeRateSnapshot
): number {
  const latestSnap = snapshot || getLatestSnapshot();
  const total = expenses.reduce((sum, exp) => {
    const converted = convertCurrency(exp.amount, exp.currency || "USD", targetCurrency, latestSnap);
    return sum + converted;
  }, 0);
  return parseFloat(total.toFixed(2));
}

/**
 * Formats a given monetary amount with standard locale currency symbols.
 */
export function formatCurrencyAmount(
  amount: number,
  currencyCode: string = "USD"
): string {
  const code = (currencyCode || "USD").toUpperCase().trim();
  const symbols: Record<string, string> = {
    USD: "$",
    EUR: "€",
    GBP: "£",
    INR: "₹",
    BDT: "৳",
    CAD: "CA$",
    AUD: "A$",
    JPY: "¥",
  };

  const symbol = symbols[code] || `${code} `;
  const formattedNumber = amount.toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  return `${symbol}${formattedNumber}`;
}
