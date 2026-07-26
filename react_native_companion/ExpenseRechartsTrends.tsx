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
import { formatCurrencyAmount } from './currencyConverter';

interface ExpenseRechartsTrendsProps {
  expenses: (ExpenseSchema & { category_name?: string })[];
  categories: { id: number; name: string }[];
  limitAmount?: number; // Optional reference limit
  displayCurrency?: string; // Display currency code
}

export default function ExpenseRechartsTrends({
  expenses,
  categories,
  limitAmount = 1500,
  displayCurrency = 'USD'
}: ExpenseRechartsTrendsProps) {

  // Aggregate expenses normalized by month and category
  const aggregatedData = useMemo(() => {
    return calculateMonthlySpendingByCategory(expenses, categories, displayCurrency);
  }, [expenses, categories, displayCurrency]);

  // Extract all unique category names
  const uniqueCategories = useMemo(() => {
    const catSet = new Set<string>();
    categories.forEach(c => catSet.add(c.name));
    expenses.forEach(e => {
      if (e.category_name) catSet.add(e.category_name);
    });
    aggregatedData.forEach(item => catSet.add(item.categoryName));
    return Array.from(catSet);
  }, [categories, expenses, aggregatedData]);

  // Pivot the data into Recharts friendly structures, grouped by month
  const chartData = useMemo(() => {
    const monthsSet = new Set<string>();

    aggregatedData.forEach(item => {
      monthsSet.add(item.month);
    });

    // If no months present, generate default recent month keys
    if (monthsSet.size === 0) {
      const now = new Date();
      for (let i = 5; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const mKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
        monthsSet.add(mKey);
      }
    }

    const sortedMonths = Array.from(monthsSet).sort();
    const lastSixMonths = sortedMonths.slice(-6);

    return lastSixMonths.map(month => {
      const monthData: Record<string, any> = { month };

      // Crucial: Initialize ALL category keys to 0 so Recharts stacked bars do not get undefined NaN values!
      uniqueCategories.forEach(cat => {
        monthData[cat] = 0;
      });

      let totalForMonth = 0;

      aggregatedData
        .filter(item => item.month === month)
        .forEach(item => {
          monthData[item.categoryName] = item.totalAmount;
          totalForMonth += item.totalAmount;
        });

      monthData.total = parseFloat(totalForMonth.toFixed(2));
      return monthData;
    });
  }, [aggregatedData, uniqueCategories]);

  // Vibrant color palette for stacked categories
  const colorPalette = [
    '#10B981', // Emerald Green
    '#3B82F6', // Royal Blue
    '#F59E0B', // Amber Gold
    '#EF4444', // Coral Red
    '#8B5CF6', // Vivid Violet
    '#EC4899', // Pink
    '#06B6D4', // Cyan
    '#14B8A6'  // Teal
  ];

  return (
    <div style={containerStyle}>
      <div style={headerStyle}>
        <h3 style={titleStyle}>6-Month Spending Trends ({displayCurrency})</h3>
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
            <div style={{ width: '100%', height: 300, minHeight: 300 }}>
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
                    tickFormatter={(val) => `${val}`}
                  />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#1E293B',
                      borderRadius: 8,
                      border: 'none',
                      color: '#FFFFFF',
                      fontSize: 13
                    }}
                    formatter={(value: any, name: any) => [
                      formatCurrencyAmount(Number(value) || 0, displayCurrency),
                      name
                    ]}
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

          {/* Area Chart for Aggregate Volume Trend with Reference Limit */}
          <div style={chartContainerStyle}>
            <h4 style={chartLabelStyle}>Aggregate Spending Volume vs Budget Limit ({formatCurrencyAmount(limitAmount, displayCurrency)})</h4>
            <div style={{ width: '100%', height: 260, minHeight: 260 }}>
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
                    formatter={(val: any) => [
                      formatCurrencyAmount(Number(val) || 0, displayCurrency),
                      'Total Monthly Spend'
                    ]}
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

// Styling Constants
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
