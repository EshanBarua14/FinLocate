import { ExpenseSchema } from './databaseSchema';

export interface MonthlyCategoryTotal {
  month: string; // format "YYYY-MM"
  categoryName: string;
  totalAmount: number;
}

export interface ChartDatasetItem {
  value: number;
  label: string;
  frontColor?: string;
}

/**
 * Aggregates a raw list of expenses into a structured list of monthly spending totals by category.
 * 
 * @param expenses Array of saved expenses
 * @param categories Array of categories with names mapped
 * @returns Array of grouped totals formatted for visualization libraries
 */
export function calculateMonthlySpendingByCategory(
  expenses: ExpenseSchema[],
  categories: { id: number; name: string }[]
): MonthlyCategoryTotal[] {
  const categoryMap = new Map<number, string>(categories.map(c => [c.id, c.name]));
  const groupedMap = new Map<string, Map<string, number>>();

  expenses.forEach(exp => {
    // Determine the month key (YYYY-MM) from timestamp
    const date = new Date(exp.date);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const monthKey = `${year}-${month}`;

    const catName = categoryMap.get(exp.category_id) || 'Uncategorized';

    if (!groupedMap.has(monthKey)) {
      groupedMap.set(monthKey, new Map<string, number>());
    }

    const monthMap = groupedMap.get(monthKey)!;
    const currentTotal = monthMap.get(catName) || 0;
    monthMap.set(catName, currentTotal + exp.amount);
  });

  const results: MonthlyCategoryTotal[] = [];

  groupedMap.forEach((monthMap, month) => {
    monthMap.forEach((totalAmount, categoryName) => {
      results.push({
        month,
        categoryName,
        totalAmount: parseFloat(totalAmount.toFixed(2))
      });
    });
  });

  // Sort primarily by month (chronological), and secondarily by amount descending
  return results.sort((a, b) => {
    if (a.month !== b.month) {
      return a.month.localeCompare(b.month);
    }
    return b.totalAmount - a.totalAmount;
  });
}

/**
 * Transforms aggregated data into specific format required for "react-native-gifted-charts" Bar Charts.
 * Focuses on a single specified month (or defaults to the most recent month in the data).
 */
export function getGiftedChartsBarData(
  aggregatedData: MonthlyCategoryTotal[],
  targetMonth?: string,
  colorPalette: string[] = ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899']
): ChartDatasetItem[] {
  let monthToUse = targetMonth;

  if (!monthToUse && aggregatedData.length > 0) {
    // Pick the latest month chronologically
    monthToUse = aggregatedData[aggregatedData.length - 1].month;
  }

  if (!monthToUse) {
    return [];
  }

  const filtered = aggregatedData.filter(item => item.month === monthToUse);

  return filtered.map((item, index) => ({
    value: item.totalAmount,
    label: item.categoryName,
    frontColor: colorPalette[index % colorPalette.length]
  }));
}

/**
 * Formats aggregated category totals into standard Pie Chart input slices.
 */
export interface PieChartSlice {
  key: string;
  value: number;
  svg: { fill: string };
  label: string;
}

export function getPieChartDataset(
  aggregatedData: MonthlyCategoryTotal[],
  targetMonth: string,
  colorPalette: string[] = ['#10B981', '#3B82F6', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899']
): PieChartSlice[] {
  const filtered = aggregatedData.filter(item => item.month === targetMonth);

  return filtered.map((item, index) => ({
    key: `${item.month}-${item.categoryName}`,
    value: item.totalAmount,
    label: item.categoryName,
    svg: {
      fill: colorPalette[index % colorPalette.length]
    }
  }));
}
