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
import { ExpenseSchema, IncomeSchema, CategorySchema } from './databaseSchema';
import { getAllExpenses, getAllIncome, getAllCategories, deleteExpense } from './localDatabase';
import { convertCurrency, formatCurrencyAmount } from './currencyConverter';
import { exportExpensesToCsv } from './csvExporter';
import ExpenseRechartsTrends from './ExpenseRechartsTrends';

interface ExpenseDashboardProps {
  onAddPress?: () => void;
  monthlyBudgetLimit?: number; // Predefined budget limit, defaults to $1500
  baseCurrency?: string; // Default base currency for total balance summary e.g. "USD"
}

export default function ExpenseDashboard({
  onAddPress,
  monthlyBudgetLimit = 1500,
  baseCurrency = 'USD',
}: ExpenseDashboardProps) {
  // DB States
  const [expenses, setExpenses] = useState<(ExpenseSchema & { category_name?: string })[]>([]);
  const [incomeList, setIncomeList] = useState<(IncomeSchema & { category_name?: string })[]>([]);
  const [categories, setCategories] = useState<CategorySchema[]>([]);
  const [loading, setLoading] = useState(true);

  // Filter States
  const [searchQuery, setSearchQuery] = useState('');
  const [startDateStr, setStartDateStr] = useState(''); // Format: YYYY-MM-DD
  const [endDateStr, setEndDateStr] = useState(''); // Format: YYYY-MM-DD
  const [selectedDisplayCurrency, setSelectedDisplayCurrency] = useState(baseCurrency);

  // Animated entrance states
  const listFadeAnim = useRef(new Animated.Value(0)).current;

  // Load data from local SQLite DB
  const loadData = async () => {
    try {
      setLoading(true);
      const [expenseData, incomeData, catData] = await Promise.all([
        getAllExpenses(),
        getAllIncome(),
        getAllCategories(),
      ]);
      setExpenses(expenseData);
      setIncomeList(incomeData);
      setCategories(catData);

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

  // Multi-Currency Total Balance, Total Income, and Total Expenses Calculations
  const summaryMetrics = React.useMemo(() => {
    let totalIncomeConverted = 0;
    let totalExpensesConverted = 0;

    // Convert each income item to selected display currency
    incomeList.forEach((inc) => {
      const converted = convertCurrency(inc.amount, inc.currency || 'USD', selectedDisplayCurrency);
      totalIncomeConverted += converted;
    });

    // Convert each expense item to selected display currency
    expenses.forEach((exp) => {
      const converted = convertCurrency(exp.amount, exp.currency || 'USD', selectedDisplayCurrency);
      totalExpensesConverted += converted;
    });

    const netBalanceConverted = totalIncomeConverted - totalExpensesConverted;

    return {
      totalIncome: parseFloat(totalIncomeConverted.toFixed(2)),
      totalExpenses: parseFloat(totalExpensesConverted.toFixed(2)),
      netBalance: parseFloat(netBalanceConverted.toFixed(2)),
    };
  }, [incomeList, expenses, selectedDisplayCurrency]);

  // Budget status evaluation: Exceeds 90% threshold watchdog
  const budgetAlertInfo = React.useMemo(() => {
    const now = new Date();
    const currentMonthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    
    // Sum all expense amounts for the current month in base currency
    const currentMonthExpenses = expenses.filter((exp) => {
      const expDate = new Date(exp.date);
      const expMonthKey = `${expDate.getFullYear()}-${String(expDate.getMonth() + 1).padStart(2, '0')}`;
      return expMonthKey === currentMonthKey;
    });

    const totalSpend = currentMonthExpenses.reduce((sum, item) => {
      return sum + convertCurrency(item.amount, item.currency || 'USD', selectedDisplayCurrency);
    }, 0);

    const percentageOfBudget = monthlyBudgetLimit > 0 ? (totalSpend / monthlyBudgetLimit) * 100 : 0;
    const isExceeded90 = percentageOfBudget >= 90;

    return {
      currentMonthKey,
      totalSpend: parseFloat(totalSpend.toFixed(2)),
      percentage: parseFloat(percentageOfBudget.toFixed(1)),
      isExceeded90,
    };
  }, [expenses, monthlyBudgetLimit, selectedDisplayCurrency]);

  // Expose an interactive system dialog alert when limit threshold breached
  useEffect(() => {
    if (budgetAlertInfo.isExceeded90 && expenses.length > 0) {
      Alert.alert(
        '⚠️ Budget Threshold Exceeded!',
        `Your spending for the current month (${budgetAlertInfo.currentMonthKey}) has reached ${budgetAlertInfo.percentage}% of your limit.\nTotal spent: ${formatCurrencyAmount(budgetAlertInfo.totalSpend, selectedDisplayCurrency)} of ${formatCurrencyAmount(monthlyBudgetLimit, selectedDisplayCurrency)}.`,
        [{ text: 'Acknowledge Limit', style: 'default' }]
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
        title: 'Local Financial Ledger Backup',
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
      'Are you sure you want to delete this expense record from SQLite storage?',
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
          <Text style={styles.headerDesc}>Multi-Currency Balance & Tax Tracking</Text>
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
            Current Month Spend is at {formatCurrencyAmount(budgetAlertInfo.totalSpend, selectedDisplayCurrency)} ({budgetAlertInfo.percentage}% of {formatCurrencyAmount(monthlyBudgetLimit, selectedDisplayCurrency)} limit)
          </Text>
        </View>
      )}

      {/* Summary Cards: Total Balance, Total Income, Total Expenses */}
      <View style={styles.summaryRow}>
        <View style={[styles.summaryCard, styles.balanceCard]}>
          <Text style={styles.summaryLabel}>Total Net Balance</Text>
          <Text style={[styles.summaryValue, summaryMetrics.netBalance < 0 && styles.negativeValue]}>
            {formatCurrencyAmount(summaryMetrics.netBalance, selectedDisplayCurrency)}
          </Text>
          <Text style={styles.summarySubText}>All offline accounts combined</Text>
        </View>

        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>Total Income</Text>
          <Text style={[styles.summaryValue, styles.incomeValue]}>
            {formatCurrencyAmount(summaryMetrics.totalIncome, selectedDisplayCurrency)}
          </Text>
          <Text style={styles.summarySubText}>Converted ({selectedDisplayCurrency})</Text>
        </View>

        <View style={styles.summaryCard}>
          <Text style={styles.summaryLabel}>Total Expenses</Text>
          <Text style={[styles.summaryValue, styles.expenseValue]}>
            {formatCurrencyAmount(summaryMetrics.totalExpenses, selectedDisplayCurrency)}
          </Text>
          <Text style={styles.summarySubText}>Converted ({selectedDisplayCurrency})</Text>
        </View>
      </View>

      {/* Display Currency Switcher Bar */}
      <View style={styles.currencySwitchRow}>
        <Text style={styles.currencySwitchLabel}>Display Currency Snapshot:</Text>
        {['USD', 'EUR', 'GBP', 'INR', 'BDT', 'CAD', 'AUD'].map((c) => (
          <TouchableOpacity
            key={c}
            style={[styles.currencyTag, selectedDisplayCurrency === c && styles.activeCurrencyTag]}
            onPress={() => setSelectedDisplayCurrency(c)}
          >
            <Text style={[styles.currencyTagText, selectedDisplayCurrency === c && styles.activeCurrencyTagText]}>
              {c}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Recharts 6-Month Spending Trends Component */}
      <ExpenseRechartsTrends
        expenses={expenses}
        categories={categories.map((c) => ({ id: c.id, name: c.name }))}
        limitAmount={monthlyBudgetLimit}
        displayCurrency={selectedDisplayCurrency}
      />

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
                    {new Date(item.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                  </Text>
                </View>
                {item.tax_relevant === 1 && (
                  <View style={styles.taxBadge}>
                    <Text style={styles.taxBadgeText}>
                      Deductible ({item.tax_deductible_percentage ?? 100}% @ {item.tax_rate}% VAT)
                    </Text>
                  </View>
                )}
                {item.tags ? <Text style={styles.tagText}>Tags: {item.tags}</Text> : null}
              </View>
              <View style={styles.itemRight}>
                <Text style={styles.itemAmount}>
                  -{formatCurrencyAmount(item.amount, item.currency || 'USD')}
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
  summaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  summaryCard: {
    flex: 1,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    padding: 12,
    marginHorizontal: 4,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  balanceCard: {
    backgroundColor: '#0F172A',
    borderColor: '#1E293B',
  },
  summaryLabel: {
    fontSize: 11,
    fontWeight: '600',
    color: '#64748B',
  },
  summaryValue: {
    fontSize: 16,
    fontWeight: '800',
    color: '#FFFFFF',
    marginVertical: 4,
  },
  incomeValue: {
    color: '#10B981',
  },
  expenseValue: {
    color: '#EF4444',
  },
  negativeValue: {
    color: '#F87171',
  },
  summarySubText: {
    fontSize: 9,
    color: '#94A3B8',
  },
  currencySwitchRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  currencySwitchLabel: {
    fontSize: 11,
    fontWeight: '600',
    color: '#64748B',
    marginRight: 6,
  },
  currencyTag: {
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 6,
    backgroundColor: '#E2E8F0',
    marginRight: 4,
  },
  activeCurrencyTag: {
    backgroundColor: '#10B981',
  },
  currencyTagText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#334155',
  },
  activeCurrencyTagText: {
    color: '#FFFFFF',
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
    marginTop: 16,
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
