import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TextInput,
  TouchableOpacity,
  ScrollView,
  Switch,
  Alert,
  ActivityIndicator,
  Animated,
} from 'react-native';
import { CategorySchema, ExpenseSchema } from './databaseSchema';
import { getAllCategories, insertExpense } from './localDatabase';
import { getTaxTemplate, LOCALIZED_TAX_TEMPLATES } from './taxRules';

interface NewExpenseFormProps {
  onExpenseAdded?: (insertedId: number) => void;
  selectedCountryCode?: string; // Optional default country code e.g. "US", "DE", "IN", "BD"
}

export default function NewExpenseForm({
  onExpenseAdded,
  selectedCountryCode = 'US',
}: NewExpenseFormProps) {
  // Database categories list
  const [categories, setCategories] = useState<CategorySchema[]>([]);
  const [loading, setLoading] = useState(true);

  // Entrance Animation Values
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(40)).current;

  // Form State
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [currency, setCurrency] = useState('USD');
  const [taxRelevant, setTaxRelevant] = useState(false);
  const [taxRate, setTaxRate] = useState('0.0');
  const [tags, setTags] = useState('');
  const [notes, setNotes] = useState('');
  const [selectedCountry, setSelectedCountry] = useState(selectedCountryCode);

  // Load offline categories on component mount
  useEffect(() => {
    async function loadData() {
      try {
        const list = await getAllCategories();
        setCategories(list);
        if (list.length > 0) {
          setSelectedCategoryId(list[0].id);
        }
      } catch (err) {
        console.error('Failed to load local categories:', err);
        Alert.alert('Database Error', 'Could not load local categories from SQLite storage.');
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

  // Trigger entrance animations when loading finishes successfully
  useEffect(() => {
    if (!loading) {
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 1,
          duration: 550,
          useNativeDriver: true,
        }),
        Animated.timing(slideAnim, {
          toValue: 0,
          duration: 550,
          useNativeDriver: true,
        }),
      ]).start();
    }
  }, [loading]);

  // Update defaults whenever selected country changes
  useEffect(() => {
    const taxTemplate = getTaxTemplate(selectedCountry);
    setCurrency(taxTemplate.currency);
    setTaxRate(String(taxTemplate.standardVatOrSalesTax));
    if (taxTemplate.deductibleCategories.length > 0) {
      setTags(taxTemplate.deductibleCategories[0]);
    } else {
      setTags('');
    }
  }, [selectedCountry]);

  // Form Submission
  const handleSubmit = async () => {
    if (!description.trim()) {
      Alert.alert('Validation Error', 'Please enter an expense description.');
      return;
    }

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      Alert.alert('Validation Error', 'Please enter a valid amount greater than 0.');
      return;
    }

    if (selectedCategoryId === null) {
      Alert.alert('Validation Error', 'Please select a spending category.');
      return;
    }

    const newExpense: Omit<ExpenseSchema, 'id'> = {
      description: description.trim(),
      amount: parsedAmount,
      date: Date.now(),
      category_id: selectedCategoryId,
      currency,
      tax_relevant: taxRelevant ? 1 : 0,
      tax_rate: taxRelevant ? parseFloat(taxRate) || 0.0 : 0.0,
      tags: tags.trim(),
      notes: notes.trim(),
    };

    try {
      const insertedId = await insertExpense(newExpense);
      Alert.alert('Success', 'Expense recorded offline in SQLite database!');
      
      // Reset Form fields
      setDescription('');
      setAmount('');
      setNotes('');
      setTaxRelevant(false);

      if (onExpenseAdded) {
        onExpenseAdded(insertedId);
      }
    } catch (err) {
      console.error('Failed to save offline expense:', err);
      Alert.alert('Database Error', 'Failed to store expense in local SQLite storage.');
    }
  };

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#10B981" />
        <Text style={styles.loadingText}>Initializing Secure SQLite Storage...</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <Animated.View 
        style={[
          styles.card,
          {
            opacity: fadeAnim,
            transform: [{ translateY: slideAnim }]
          }
        ]}
      >
        <Text style={styles.headerTitle}>Record Secure Expense</Text>
        <Text style={styles.headerSubtitle}>Offline-First Ledger Entry</Text>

        {/* Country Picker Row */}
        <Text style={styles.label}>Select Local Compliance Zone</Text>
        <View style={styles.row}>
          {Object.keys(LOCALIZED_TAX_TEMPLATES).map((code) => {
            const isSelected = selectedCountry === code;
            return (
              <TouchableOpacity
                key={code}
                style={[styles.chip, isSelected && styles.activeChip]}
                onPress={() => setSelectedCountry(code)}
              >
                <Text style={[styles.chipText, isSelected && styles.activeChipText]}>
                  {code} ({LOCALIZED_TAX_TEMPLATES[code].currency})
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Description Field */}
        <Text style={styles.label}>Description / Merchant</Text>
        <TextInput
          style={styles.input}
          placeholder="e.g. Uber Ride, Grocery, AWS Cloud"
          placeholderTextColor="#94A3B8"
          value={description}
          onChangeText={setDescription}
        />

        {/* Amount and Currency */}
        <View style={styles.rowContainer}>
          <View style={[styles.colContainer, { flex: 2 }]}>
            <Text style={styles.label}>Amount</Text>
            <TextInput
              style={styles.input}
              placeholder="0.00"
              placeholderTextColor="#94A3B8"
              keyboardType="decimal-pad"
              value={amount}
              onChangeText={setAmount}
            />
          </View>
          <View style={[styles.colContainer, { flex: 1, marginLeft: 12 }]}>
            <Text style={styles.label}>Currency</Text>
            <View style={[styles.input, styles.disabledInput]}>
              <Text style={styles.disabledInputText}>{currency}</Text>
            </View>
          </View>
        </View>

        {/* Category Selection */}
        <Text style={styles.label}>Spending Category</Text>
        <View style={styles.categoryGrid}>
          {categories.map((cat) => {
            const isSelected = selectedCategoryId === cat.id;
            return (
              <TouchableOpacity
                key={cat.id}
                style={[styles.categorySelector, isSelected && styles.activeCategorySelector]}
                onPress={() => setSelectedCategoryId(cat.id)}
              >
                <Text style={[styles.categoryText, isSelected && styles.activeCategoryText]}>
                  {cat.name}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Tax Deductible Toggle */}
        <View style={styles.switchRow}>
          <View style={styles.switchTextContainer}>
            <Text style={styles.switchLabel}>Tax-Relevant Deduction</Text>
            <Text style={styles.switchDesc}>Deductible under localized rules</Text>
          </View>
          <Switch
            value={taxRelevant}
            onValueChange={setTaxRelevant}
            trackColor={{ false: '#CBD5E1', true: '#A7F3D0' }}
            thumbColor={taxRelevant ? '#10B981' : '#F1F5F9'}
          />
        </View>

        {/* Conditional Tax Rate and Tax Tags */}
        {taxRelevant && (
          <View style={styles.taxFieldsContainer}>
            <Text style={styles.label}>Applicable Tax Rate (%)</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. 19.0"
              placeholderTextColor="#94A3B8"
              keyboardType="decimal-pad"
              value={taxRate}
              onChangeText={setTaxRate}
            />

            <Text style={styles.label}>Tax Deductible Rule Tags</Text>
            <TextInput
              style={styles.input}
              placeholder="e.g. traditional_ira, medical_premium"
              placeholderTextColor="#94A3B8"
              value={tags}
              onChangeText={setTags}
            />
            <Text style={styles.helperText}>
              Matched standard rules: {getTaxTemplate(selectedCountry).deductibleCategories.join(', ')}
            </Text>
          </View>
        )}

        {/* Notes */}
        <Text style={styles.label}>Audit Notes (Optional)</Text>
        <TextInput
          style={[styles.input, styles.textArea]}
          placeholder="Enter receipts references or transaction details..."
          placeholderTextColor="#94A3B8"
          multiline
          numberOfLines={3}
          value={notes}
          onChangeText={setNotes}
        />

        {/* Submit Button */}
        <TouchableOpacity style={styles.submitButton} onPress={handleSubmit} activeOpacity={0.8}>
          <Text style={styles.submitButtonText}>Commit Expense to SQLite Ledger</Text>
        </TouchableOpacity>
      </Animated.View>
    </ScrollView>
  );
}
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: '#F8FAFC',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F8FAFC',
    padding: 24,
  },
  loadingText: {
    marginTop: 12,
    fontSize: 14,
    color: '#64748B',
    fontWeight: '500',
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 20,
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 12,
    elevation: 3,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#1E293B',
    marginBottom: 2,
  },
  headerSubtitle: {
    fontSize: 12,
    fontWeight: '500',
    color: '#10B981',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 20,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#475569',
    marginBottom: 6,
    marginTop: 14,
  },
  input: {
    borderWidth: 1,
    borderColor: '#E2E8F0',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: '#0F172A',
    backgroundColor: '#FAFCFF',
  },
  textArea: {
    height: 70,
    textAlignVertical: 'top',
  },
  disabledInput: {
    backgroundColor: '#F1F5F9',
    borderColor: '#E2E8F0',
    justifyContent: 'center',
  },
  disabledInputText: {
    color: '#64748B',
    fontSize: 15,
    fontWeight: '600',
  },
  rowContainer: {
    flexDirection: 'row',
  },
  colContainer: {
    flexDirection: 'column',
  },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginVertical: 4,
  },
  chip: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 16,
    backgroundColor: '#F1F5F9',
    marginRight: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  activeChip: {
    backgroundColor: '#ECFDF5',
    borderColor: '#10B981',
  },
  chipText: {
    fontSize: 12,
    color: '#475569',
    fontWeight: '500',
  },
  activeChipText: {
    color: '#059669',
    fontWeight: '700',
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 4,
  },
  categorySelector: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    backgroundColor: '#FAFCFF',
    marginRight: 8,
    marginBottom: 8,
  },
  activeCategorySelector: {
    backgroundColor: '#EFF6FF',
    borderColor: '#3B82F6',
  },
  categoryText: {
    fontSize: 13,
    color: '#475569',
    fontWeight: '500',
  },
  activeCategoryText: {
    color: '#1D4ED8',
    fontWeight: '600',
  },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 20,
    paddingVertical: 12,
    borderTopWidth: 1,
    borderBottomWidth: 1,
    borderColor: '#F1F5F9',
  },
  switchTextContainer: {
    flex: 1,
  },
  switchLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#334155',
  },
  switchDesc: {
    fontSize: 11,
    color: '#64748B',
    marginTop: 2,
  },
  taxFieldsContainer: {
    backgroundColor: '#F8FAFC',
    borderRadius: 8,
    padding: 12,
    marginTop: 12,
    borderWidth: 1,
    borderColor: '#F1F5F9',
  },
  helperText: {
    fontSize: 11,
    color: '#64748B',
    fontStyle: 'italic',
    marginTop: 6,
  },
  submitButton: {
    backgroundColor: '#10B981',
    borderRadius: 8,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 24,
  },
  submitButtonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
});
