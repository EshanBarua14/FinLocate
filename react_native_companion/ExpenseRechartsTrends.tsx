import React, { useMemo } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  AreaChart,
  Area
} from 'recharts';
import { ExpenseSchema } from './databaseSchema';
import { calculateMonthlySpendingByCategory } from './dataAggregation';

interface ExpenseRechartsTrendsProps {
  expenses: ExpenseSchema[];
  categories: { id: number; name: string }[];
  limitAmount?: number; // Optional reference line/limit
}

export default function ExpenseRechartsTrends({
  expenses,
  categories,
  limitAmount = 1500
}: ExpenseRechartsTrendsProps) {

  // Get aggregated list: Array of { month: "2026-07", categoryName: "Utilities", totalAmount: 120.0 }
  const aggregatedData = useMemo(() => {
    return calculateMonthlySpendingByCategory(expenses, categories);
  }, [expenses, categories]);

  // Pivot the data for Recharts, grouped by month, over the last 6 months
  const chartData = useMemo(() => {
    // 1. Find all unique months and categories
    const monthsSet = new Set<string>();
    const categoriesSet = new Set<string>();

    aggregatedData.forEach(item => {
      monthsSet.add(item.month);
      categoriesSet.add(item.categoryName);
    });

    // Sort months chronologically
    const sortedMonths = Array.from(monthsSet).sort();
    
    // Take the last 6 months for trend visualization
    const lastSixMonths = sortedMonths.slice(-6);

    // 2. Build final pivot items
    return lastSixMonths.map(month => {
      const monthData: Record<string, any> = { month };
      let totalForMonth = 0;

      // Populate category totals
      aggregatedData
        .filter(item => item.month === month)
        .forEach(item => {
          monthData[item.categoryName] = item.totalAmount;
          totalForMonth += item.totalAmount;
        });

      monthData.total = parseFloat(totalForMonth.toFixed(2));
      return monthData;
    });
  }, [aggregatedData]);

  // Extract unique categories for bars
  const uniqueCategories = useMemo(() => {
    const cats = new Set<string>();
    aggregatedData.forEach(item => cats.add(item.categoryName));
    return Array.from(cats);
  }, [aggregatedData]);

  // Color palette for category mapping
  const colorPalette = [
    '#10B981', // Fintech Green
    '#3B82F6', // Royal Blue
    '#F59E0B', // Warm Amber
    '#EF4444', // Coral Red
    '#8B5CF6', // Vivid Purple
    '#EC4899', // Hot Pink
    '#06B6D4', // Cool Cyan
    '#14B8A6'  // Deep Teal
  ];

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <h3 style={titleStyle}>6-Month Spending Trends</h3>
        <p style={subtitleStyle}>Monthly Categorized Spending Stack Overview (Recharts)</p>
      </div>

      {chartData.length === 0 ? (
        <div style={emptyStateStyle}>
          <p>No transaction history found to compute 6-month trends.</p>
        </div>
      ) : (
        <div style={chartsWrapperStyle}>
          {/* Main Stacked Bar Chart for Category breakdown */}
          <div style={chartContainerStyle}>
            <h4 style={chartLabelStyle}>Categorized Breakdown</h4>
            <div style={{ width: '100%', height: 300 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={chartData}
                  margin={{ top: 20, right: 30, left: 10, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                  <XAxis 
                    dataKey="month" 
                    stroke="#64748B" 
                    fontSize={12}
                    tickLine={false}
                  />
                  <YAxis 
                    stroke="#64748B" 
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1E293B',
                      borderRadius: 8,
                      border: 'none',
                      color: '#FFFFFF',
                      fontSize: 13
                    }}
                  />
                  <Legend iconType="circle" wrapperStyle={{ fontSize: 12, paddingTop: 10 }} />
                  {uniqueCategories.map((cat, index) => (
                    <Bar
                      key={cat}
                      dataKey={cat}
                      stackId="a"
                      fill={colorPalette[index % colorPalette.length]}
                      radius={[index === uniqueCategories.length - 1 ? 4 : 0, index === uniqueCategories.length - 1 ? 4 : 0, 0, 0]}
                    />
                  ))}
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Area Chart for Aggregate Volume Trend with Reference Budget */}
          <div style={chartContainerStyle}>
            <h4 style={chartLabelStyle}>Aggregate Spending Volume vs Budget Limit</h4>
            <div style={{ width: '100%', height: 300 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={chartData}
                  margin={{ top: 20, right: 30, left: 10, bottom: 5 }}
                >
                  <defs>
                    <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10B981" stopOpacity={0.4}/>
                      <stop offset="95%" stopColor="#10B981" stopOpacity={0.0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#F1F5F9" />
                  <XAxis 
                    dataKey="month" 
                    stroke="#64748B" 
                    fontSize={12}
                    tickLine={false}
                  />
                  <YAxis 
                    stroke="#64748B" 
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1E293B',
                      borderRadius: 8,
                      border: 'none',
                      color: '#FFFFFF',
                      fontSize: 13
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="total"
                    stroke="#10B981"
                    strokeWidth={2.5}
                    fillOpacity={1}
                    fill="url(#colorTotal)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// Styling Constants for clean, modern design system presentation
const containerStyle: React.CSSProperties = {
  fontFamily: 'Inter, system-ui, -apple-system, sans-serif',
  backgroundColor: '#FFFFFF',
  borderRadius: '16px',
  padding: '24px',
  boxShadow: '0 4px 18px rgba(15, 23, 42, 0.04)',
  border: '1px solid #F1F5F9',
  marginTop: '20px',
};

const headerStyle: React.CSSProperties = {
  marginBottom: '24px',
};

const titleStyle: React.CSSProperties = {
  fontSize: '20px',
  fontWeight: 700,
  color: '#0F172A',
  margin: '0 0 4px 0',
};

const subtitleStyle: React.CSSProperties = {
  fontSize: '13px',
  color: '#10B981',
  fontWeight: 600,
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  margin: 0,
};

const emptyStateStyle: React.CSSProperties = {
  padding: '40px',
  textAlign: 'center',
  color: '#64748B',
  fontSize: '14px',
  backgroundColor: '#F8FAFC',
  borderRadius: '12px',
  border: '1px dashed #CBD5E1',
};

const chartsWrapperStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: '24px',
};

const chartContainerStyle: React.CSSProperties = {
  backgroundColor: '#F8FAFC',
  borderRadius: '12px',
  padding: '16px',
  border: '1px solid #E2E8F0',
};

const chartLabelStyle: React.CSSProperties = {
  fontSize: '14px',
  fontWeight: 600,
  color: '#334155',
  margin: '0 0 16px 0',
};
