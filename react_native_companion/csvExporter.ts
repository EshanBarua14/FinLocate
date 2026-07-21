import { ExpenseSchema } from './databaseSchema';

/**
 * Escapes CSV values to handle double quotes, commas, and newlines properly.
 */
function escapeCsvValue(val: any): string {
  if (val === undefined || val === null) {
    return '';
  }
  let str = String(val);
  // If contains double quotes, commas, or newlines, wrap in quotes and escape existing quotes
  if (/[",\n\r]/.test(str)) {
    str = '"' + str.replace(/"/g, '""') + '"';
  }
  return str;
}

/**
 * Converts a list of expense records with resolved category names into a standard CSV string.
 * This is suitable for external financial backup, auditing, and ledger synchronizations.
 */
export function exportExpensesToCsv(
  expenses: (ExpenseSchema & { category_name?: string })[]
): string {
  const headers = [
    'ID',
    'Date',
    'Formatted Date',
    'Description',
    'Amount',
    'Currency',
    'Category',
    'Tax Relevant',
    'Tax Rate (%)',
    'Tags',
    'Notes'
  ];

  const rows = expenses.map(exp => {
    const formattedDate = new Date(exp.date).toISOString().split('T')[0];
    return [
      exp.id,
      exp.date,
      formattedDate,
      exp.description,
      exp.amount,
      exp.currency,
      exp.category_name || 'Uncategorized',
      exp.tax_relevant === 1 ? 'YES' : 'NO',
      exp.tax_rate,
      exp.tags || '',
      exp.notes || ''
    ];
  });

  // Combine headers and rows with escaped values
  const csvContent = [
    headers.join(','),
    ...rows.map(row => row.map(escapeCsvValue).join(','))
  ].join('\r\n');

  return csvContent;
}
