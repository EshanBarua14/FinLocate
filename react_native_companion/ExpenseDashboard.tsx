import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  FlatList,
  Alert,
  ActivityIndicator,
  Animated,
  Share,
} from 'react-native';
import { ExpenseSchema, CategorySchema } from './databaseSchema';
import { getAllExpenses, getAllCategories, deleteExpense } from './localDatabase';
import { calculateMonthlySpendingByCategory, MonthlyCategoryTotal } from './dataAggregation';
import { exportExpensesToCsv } from './csvExporter';

interface ExpenseDashboardProps {
  onAddPress?: () => void;
  monthlyBudgetLimit?: number; // Predefined budget limit, defaults to $1500
}

export default function ExpenseDashboard({
  onAddPress,
  monthlyBudgetLimit = 1500,
}: ExpenseDashboardProps) {
  // DB States
  const [expenses, setExpenses] = useState<(ExpenseSchema & { category_name?: string })[]>([]);
  const [categories, setCategories] = useState<CategorySchema[]>([]);
  const [loading, setLoading] = useState(true);

  // Filter States
  const [searchQuery, setSearchQuery] = useState('');
  const [startDateStr, setStartDateStr] = useState(''); // Format: YYYY-MM-DD
  const [endDateStr, setEndDateStr] = useState(''); // Format: YYYY-MM-DD

  // Animated entrance states
  const listFadeAnim = useRef(new Animated.Value(0)).current;

  // Load data from local SQLite DB
  const loadData = async () => {
    try {
      setLoading(true);
      const [expenseList, catList] = await Promise.all([
        getAllExpenses(),
        getAllCategories(),
      ]);
      setExpenses(expenseList);
      setCategories(catList);

      // Trigger animated entrance
      Animated.timing(listFadeAnim, {
        toValue: 1,
        duration: 600,
        useNativeDriver: true,
      }).start();
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
      Alert.alert('Database Error', 'Unable to synchronize local offline database records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Budget status evaluation: Exceeds 90% threshold watchdog
  const budgetAlertInfo = React.useMemo(() => {
    const now = new Date();
    const currentMonthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    
    // Sum all expense amounts for the current month
    const currentMonthExpenses = expenses.filter((exp) => {
      const expDate = new Date(exp.date);
      const expMonthKey = `${expDate.getFullYear()}-${String(expDate.getMonth() + 1).padStart(2, '0')}`;
      return expMonthKey === currentMonthKey;
    });

    const totalSpend = currentMonthExpenses.reduce((sum, item) => sum + item.amount, 0);
    const percentageOfBudget = monthlyBudgetLimit > 0 ? (totalSpend / monthlyBudgetLimit) * 100 : 0;
    const isExceeded90 = percentageOfBudget >= 90;

    return {
      currentMonthKey,
      totalSpend: parseFloat(totalSpend.toFixed(2)),
      percentage: parseFloat(percentageOfBudget.toFixed(1)),
      isExceeded90,
    };
  }, [expenses, monthlyBudgetLimit]);

  // Expose an interactive system dialog alert when limit threshold breached
  useEffect(() => {
    if (budgetAlertInfo.isExceeded90 && expenses.length > 0) {
      Alert.alert(
        '⚠️ Budget Alert threshold Exceeded!',
        `Your spending for the current month (${budgetAlertInfo.currentMonthKey}) has reached ${budgetAlertInfo.percentage}% of your limit.\nTotal spent: $${budgetAlertInfo.totalSpend} of $${monthlyBudgetLimit}.`,
        [{ text: 'Acknowledge Budget Limit', style: 'warning' }]
      );
    }
  }, [budgetAlertInfo.isExceeded90, budgetAlertInfo.currentMonthKey]);

  // CSV Exporter handler
  const handleCsvExport = async () => {
    if (expenses.length === 0) {
      Alert.alert('Export Notice', 'There are no offline records to backup.');
      return;
    }
    try {
      const csvData = exportExpensesToCsv(expenses);
      await Share.share({
        title: 'Local Financial Logs Backup',
        message: csvData,
      });
    } catch (err) {
      console.error('Failed to export CSV logs:', err);
      Alert.alert('Export Failure', 'System was unable to format or share CSV files.');
    }
  };

  // Safe item deletion handler
  const handleDeleteItem = (id: number) => {
    Alert.alert(
      'Confirm Deletion',
      'Are you sure you want to delete this expense record?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteExpense(id);
              loadData(); // Reload from SQLite storage
            } catch (err) {
              Alert.alert('Error', 'Failed to remove expense from database.');
            }
          },
        },
      ]
    );
  };

  // Interactive filtering function for search keyword and date ranges
  const filteredExpenses = React.useMemo(() => {
    return expenses.filter((exp) => {
      // 1. Text Search Filter (Description, Category Name, Tags)
      const descMatch = exp.description.toLowerCase().includes(searchQuery.toLowerCase());
      const catMatch = (exp.category_name || '').toLowerCase().includes(searchQuery.toLowerCase());
      const tagsMatch = (exp.tags || '').toLowerCase().includes(searchQuery.toLowerCase());
      const matchesSearch = descMatch || catMatch || tagsMatch;

      // 2. Date Range Filter
      let matchesStartDate = true;
      let matchesEndDate = true;

      const transactionTimestamp = exp.date;

      if (startDateStr.trim()) {
        const startTimestamp = Date.parse(startDateStr.trim());
        if (!isNaN(startTimestamp)) {
          matchesStartDate = transactionTimestamp >= startTimestamp;
        }
      }

      if (endDateStr.trim()) {
        const endTimestamp = Date.parse(endDateStr.trim() + 'T23:59:59');
        if (!isNaN(endTimestamp)) {
          matchesEndDate = transactionTimestamp <= endTimestamp;
        }
      }

      return matchesSearch && matchesStartDate && matchesEndDate;
    });
  }, [expenses, searchQuery, startDateStr, endDateStr]);

  // 6-Month spending aggregation for trends visualizer
  const trendDataset = React.useMemo(() => {
    const categoriesMapped = categories.map((c) => ({ id: c.id, name: c.name }));
    const aggregates = calculateMonthlySpendingByCategory(expenses, categoriesMapped);

    // Group items by unique month key
    const monthGroups: Record<string, number> = {};
    aggregates.forEach((item) => {
      monthGroups[item.month] = (monthGroups[item.month] || 0) + item.totalAmount;
    });

    // Take the last six months
    const sortedMonths = Object.keys(monthGroups).sort().slice(-6);
    return sortedMonths.map((month) => ({
      month,
      amount: monthGroups[month],
    }));
  }, [expenses, categories]);

  if (loading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color="#10B981" />
        <Text style={styles.loadingText}>Synchronizing secure finance logs...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Header Panel */}
      <View style={styles.header}>
        <View>
          <Text style={styles.headerTitle}>Offline Ledger Dashboard</Text>
          <Text style={styles.headerDesc}>Compliant Tax & Category Tracking</Text>
        </View>
        <TouchableOpacity style={styles.exportButton} onPress={handleCsvExport}>
          <Text style={styles.exportButtonText}>Export CSV</Text>
        </TouchableOpacity>
      </View>

      {/* Exceeded 90% Warning Banner */}
      {budgetAlertInfo.isExceeded90 && (
        <View style={styles.warningBanner}>
          <Text style={styles.warningTitle}>⚠️ High Spending Warning</Text>
          <Text style={styles.warningMessage}>
            Current Month Spend is at ${budgetAlertInfo.totalSpend} ({budgetAlertInfo.percentage}% of ${monthlyBudgetLimit} limit)
          </Text>
        </View>
      )}

      {/* 6-Month Visualizer Bar Chart (Built using core native elements) */}
      <View style={styles.chartCard}>
        <Text style={styles.chartTitle}>Last 6-Month Spending Trends</Text>
        <Text style={styles.chartDesc}>Aggregated offline volume (Base: {trendDataset.length} Active Months)</Text>
        {trendDataset.length === 0 ? (
          <View style={styles.emptyChart}>
            <Text style={styles.emptyChartText}>No records to generate trend visualizers</Text>
          </View>
        ) : (
          <View style={styles.chartRow}>
            {trendDataset.map((item, index) => {
              // Calculate relative height bar percentage
              const maxAmount = Math.max(...trendDataset.map((t) => t.amount), 1);
              const barHeightPct = (item.amount / maxAmount) * 80 + 10; // minimum height 10%

              return (
                <View key={item.month} style={styles.chartCol}>
                  <View style={styles.barWrapper}>
                    <View style={[styles.bar, { height: `${barHeightPct}%` }]} />
                  </View>
                  <Text style={styles.barLabel}>{item.month.split('-')[1]}/{item.month.split('-')[0].slice(2)}</Text>
                  <Text style={styles.barValue}>${Math.round(item.amount)}</Text>
                </View>
              );
            })}
          </View>
        )}
      </View>

      {/* Filter and Date Range Control Panel */}
      <View style={styles.filterCard}>
        <Text style={styles.filterTitle}>Interactive Filters & Compliance Audit</Text>
        
        <TextInput
          style={styles.input}
          placeholder="Search descriptions, categories, tags..."
          placeholderTextColor="#94A3B8"
          value={searchQuery}
          onChangeText={setSearchQuery}
        />

        <View style={styles.dateRow}>
          <View style={styles.dateCol}>
            <Text style={styles.dateLabel}>Start Date (YYYY-MM-DD)</Text>
            <TextInput
              style={styles.dateInput}
              placeholder="e.g. 2026-01-01"
              placeholderTextColor="#94A3B8"
              value={startDateStr}
              onChangeText={setStartDateStr}
            />
          </View>
          <View style={styles.dateCol}>
            <Text style={styles.dateLabel}>End Date (YYYY-MM-DD)</Text>
            <TextInput
              style={styles.dateInput}
              placeholder="e.g. 2026-12-31"
              placeholderTextColor="#94A3B8"
              value={endDateStr}
              onChangeText={setEndDateStr}
            />
          </View>
        </View>
      </View>

      {/* Main Expenses List with Entrance Fade-in */}
      <Animated.View style={[styles.listContainer, { opacity: listFadeAnim }]}>
        <FlatList
          data={filteredExpenses}
          keyExtractor={(item) => String(item.id)}
          refreshing={loading}
          onRefresh={loadData}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>No transaction records matched your filters.</Text>
            </View>
          }
          renderItem={({ item }) => (
            <View style={styles.expenseItem}>
              <View style={styles.itemLeft}>
                <Text style={styles.itemTitle}>{item.description}</Text>
                <View style={styles.metaRow}>
                  <View style={styles.categoryBadge}>
                    <Text style={styles.categoryBadgeText}>{item.category_name || 'Uncategorized'}</Text>
                  </View>
                  <Text style={styles.itemDate}>
                    {new Date(item.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                  </Text>
                </View>
                {item.tax_relevant === 1 && (
                  <View style={styles.taxBadge}>
                    <Text style={styles.taxBadgeText}>Deductible Tax ({item.tax_rate}%)</Text>
                  </View>
                )}
                {item.tags ? <Text style={styles.tagText}>Tags: {item.tags}</Text> : null}
              </View>
              <View style={styles.itemRight}>
                <Text style={styles.itemAmount}>
                  -{item.currency} {item.amount.toFixed(2)}
                </Text>
                <TouchableOpacity onPress={() => handleDeleteItem(item.id)}>
                  <Text style={styles.deleteText}>Delete</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        />
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8FAFC',
    padding: 16,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F8FAFC',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 14,
    color: '#64748B',
    fontWeight: '500',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: '800',
    color: '#0F172A',
  },
  headerDesc: {
    fontSize: 13,
    color: '#10B981',
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginTop: 2,
  },
  exportButton: {
    backgroundColor: '#1E293B',
    borderRadius: 8,
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  exportButtonText: {
    color: '#FFFFFF',
    fontSize: 12,
    fontWeight: '700',
  },
  warningBanner: {
    backgroundColor: '#FEF2F2',
    borderWidth: 1,
    borderColor: '#FCA5A5',
    borderRadius: 12,
    padding: 12,
    marginBottom: 16,
  },
  warningTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#991B1B',
    marginBottom: 2,
  },
  warningMessage: {
    fontSize: 12,
    fontWeight: '500',
    color: '#B91C1C',
  },
  chartCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.04,
    shadowRadius: 8,
    elevation: 2,
    marginBottom: 16,
  },
  chartTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#1E293B',
  },
  chartDesc: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 2,
    marginBottom: 16,
  },
  emptyChart: {
    height: 100,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FAFCFF',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderStyle: 'dashed',
  },
  emptyChartText: {
    color: '#94A3B8',
    fontSize: 12,
  },
  chartRow: {
    flexDirection: 'row',
    height: 120,
    alignItems: 'flex-end',
    justifyContent: 'space-around',
    paddingBottom: 4,
  },
  chartCol: {
    alignItems: 'center',
    flex: 1,
  },
  barWrapper: {
    height: 70,
    width: '100%',
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  bar: {
    width: 14,
    backgroundColor: '#10B981',
    borderRadius: 4,
  },
  barLabel: {
    fontSize: 9,
    fontWeight: '600',
    color: '#64748B',
    marginTop: 6,
  },
  barValue: {
    fontSize: 10,
    fontWeight: '700',
    color: '#0F172A',
    marginTop: 2,
  },
  filterCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.04,
    shadowRadius: 8,
    elevation: 2,
    marginBottom: 16,
  },
  filterTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#1E293B',
    marginBottom: 12,
  },
  input: {
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 14,
    color: '#0F172A',
    backgroundColor: '#FAFCFF',
    marginBottom: 12,
  },
  dateRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  dateCol: {
    flex: 1,
    marginRight: 8,
  },
  dateLabel: {
    fontSize: 10,
    fontWeight: '600',
    color: '#64748B',
    marginBottom: 4,
  },
  dateInput: {
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 12,
    color: '#0F172A',
    backgroundColor: '#FAFCFF',
  },
  listContainer: {
    flex: 1,
  },
  emptyContainer: {
    padding: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyText: {
    fontSize: 13,
    color: '#94A3B8',
    textAlign: 'center',
  },
  expenseItem: {
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  itemLeft: {
    flex: 1,
    marginRight: 12,
  },
  itemTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#1E293B',
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 6,
  },
  categoryBadge: {
    backgroundColor: '#EFF6FF',
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
    marginRight: 8,
  },
  categoryBadgeText: {
    color: '#2563EB',
    fontSize: 10,
    fontWeight: '600',
  },
  itemDate: {
    color: '#94A3B8',
    fontSize: 11,
    fontWeight: '500',
  },
  taxBadge: {
    backgroundColor: '#ECFDF5',
    paddingVertical: 2,
    paddingHorizontal: 6,
    borderRadius: 4,
    marginTop: 6,
    alignSelf: 'flex-start',
  },
  taxBadgeText: {
    color: '#059669',
    fontSize: 10,
    fontWeight: '600',
  },
  tagText: {
    fontSize: 11,
    color: '#64748B',
    fontStyle: 'italic',
    marginTop: 6,
  },
  itemRight: {
    alignItems: 'flex-end',
  },
  itemAmount: {
    fontSize: 15,
    fontWeight: '700',
    color: '#EF4444',
    marginBottom: 6,
  },
  deleteText: {
    color: '#EF4444',
    fontSize: 11,
    fontWeight: '600',
  },
});
