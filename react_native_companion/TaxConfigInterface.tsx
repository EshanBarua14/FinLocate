import React, { useState, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { CategorySchema, TaxThresholdSchema } from './databaseSchema';
import { getAllCategories, getTaxThresholdsForCountry, saveTaxThreshold } from './localDatabase';
import { getTaxTemplate, LOCALIZED_TAX_TEMPLATES } from './taxRules';

interface TaxConfigInterfaceProps {
  initialCountryCode?: string;
  onConfigSaved?: (countryCode: string) => void;
}

export default function TaxConfigInterface({
  initialCountryCode = 'US',
  onConfigSaved,
}: TaxConfigInterfaceProps) {
  const [selectedCountry, setSelectedCountry] = useState(initialCountryCode);
  const [categories, setCategories] = useState<CategorySchema[]>([]);
  // Map categoryId -> deductible percentage string (e.g. "100", "80", "50")
  const [thresholdsMap, setThresholdsMap] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // Load categories & existing country thresholds
  useEffect(() => {
    async function loadData() {
      try {
        setLoading(true);
        const catList = await getAllCategories();
        // Filter to expense categories
        const expenseCats = catList.filter((c) => c.is_income === 0);
        setCategories(expenseCats);

        // Fetch existing thresholds for this country from SQLite
        const existingThresholds = await getTaxThresholdsForCountry(selectedCountry);
        const initialMap: Record<number, string> = {};

        const taxTemplate = getTaxTemplate(selectedCountry);

        expenseCats.forEach((cat) => {
          const match = existingThresholds.find((t) => t.category_id === cat.id);
          if (match) {
            initialMap[cat.id] = String(match.custom_deductible_percentage);
          } else {
            // Default threshold based on category name match or 100%
            const isMatch = taxTemplate.deductibleCategories.some((dCat) =>
              cat.name.toLowerCase().includes(dCat.toLowerCase()) || dCat.toLowerCase().includes(cat.name.toLowerCase())
            );
            initialMap[cat.id] = isMatch ? '100' : '50';
          }
        });

        setThresholdsMap(initialMap);
      } catch (err) {
        console.error('Failed to load tax thresholds:', err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, [selectedCountry]);

  // Handle threshold percentage text change for a category
  const handleThresholdChange = (catId: number, value: string) => {
    setThresholdsMap((prev) => ({
      ...prev,
      [catId]: value,
    }));
  };

  // Validate and Save Custom Tax Thresholds
  const handleSaveConfig = async () => {
    // Validation
    const validationErrors: string[] = [];

    for (const cat of categories) {
      const valStr = thresholdsMap[cat.id] || '0';
      const parsedVal = parseFloat(valStr);

      if (isNaN(parsedVal) || parsedVal < 0 || parsedVal > 100) {
        validationErrors.push(`Category "${cat.name}" threshold must be a number between 0% and 100%.`);
      }
    }

    if (validationErrors.length > 0) {
      Alert.alert('Validation Error', validationErrors[0]);
      return;
    }

    try {
      setSaving(true);
      // Save all category thresholds to local SQLite storage
      for (const cat of categories) {
        const percentage = parseFloat(thresholdsMap[cat.id]);
        await saveTaxThreshold(selectedCountry, cat.id, percentage);
      }

      Alert.alert(
        'Configuration Saved',
        `Tax deductible thresholds for ${getTaxTemplate(selectedCountry).countryName} have been saved successfully.`
      );

      if (onConfigSaved) {
        onConfigSaved(selectedCountry);
      }
    } catch (err) {
      console.error('Failed to save tax config:', err);
      Alert.alert('Database Error', 'Could not persist tax threshold preferences.');
    } finally {
      setSaving(false);
    }
  };

  const taxTemplate = getTaxTemplate(selectedCountry);

  if (loading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#10B981" />
        <Text style={styles.loadingText}>Loading Tax Rules & Compliance Data...</Text>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
      <View style={styles.card}>
        <Text style={styles.headerTitle}>Tax Compliance & Rule Thresholds</Text>
        <Text style={styles.headerSubtitle}>Select Country & Custom Deductible Percentages</Text>

        {/* Country Selector */}
        <Text style={styles.label}>Select Country Jurisdiction</Text>
        <View style={styles.countryRow}>
          {Object.keys(LOCALIZED_TAX_TEMPLATES).map((code) => {
            const isSelected = selectedCountry === code;
            return (
              <TouchableOpacity
                key={code}
                style={[styles.countryChip, isSelected && styles.activeCountryChip]}
                onPress={() => setSelectedCountry(code)}
              >
                <Text style={[styles.countryChipText, isSelected && styles.activeCountryChipText]}>
                  {code} ({LOCALIZED_TAX_TEMPLATES[code].currency})
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {/* Country Summary Info Box */}
        <View style={styles.infoBox}>
          <Text style={styles.infoBoxTitle}>{taxTemplate.countryName} Tax Rules Summary</Text>
          <Text style={styles.infoBoxText}>• Currency: {taxTemplate.currency}</Text>
          <Text style={styles.infoBoxText}>• Standard VAT/Sales Tax: {taxTemplate.standardVatOrSalesTax}%</Text>
          <Text style={styles.infoBoxText}>• Tax Filing Deadline: {taxTemplate.taxFilingDeadline}</Text>
          <Text style={styles.infoBoxDesc}>{taxTemplate.description}</Text>
        </View>

        {/* Category Deductible Threshold Form */}
        <Text style={styles.sectionTitle}>Custom Category Deductible Thresholds (%)</Text>
        <Text style={styles.sectionDesc}>
          Set the percentage of spending in each category that qualifies as tax-deductible under {taxTemplate.countryCode} regulations:
        </Text>

        {categories.map((cat) => (
          <View key={cat.id} style={styles.categoryRow}>
            <View style={styles.catLeft}>
              <Text style={styles.categoryName}>{cat.name}</Text>
              <Text style={styles.categorySub}>Category ID: {cat.id}</Text>
            </View>
            <View style={styles.catRight}>
              <TextInput
                style={styles.percentageInput}
                keyboardType="decimal-pad"
                value={thresholdsMap[cat.id] || '0'}
                onChangeText={(val) => handleThresholdChange(cat.id, val)}
                placeholder="100"
                maxLength={5}
              />
              <Text style={styles.percentSymbol}>%</Text>
            </View>
          </View>
        ))}

        {/* Action Button */}
        <TouchableOpacity
          style={styles.saveButton}
          onPress={handleSaveConfig}
          disabled={saving}
          activeOpacity={0.8}
        >
          {saving ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <Text style={styles.saveButtonText}>Save Tax Rules & Category Thresholds</Text>
          )}
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 16,
    backgroundColor: '#F8FAFC',
  },
  loadingContainer: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F8FAFC',
  },
  loadingText: {
    marginTop: 12,
    fontSize: 13,
    color: '#64748B',
  },
  card: {
    backgroundColor: '#FFFFFF',
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    shadowColor: '#0F172A',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 3,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '800',
    color: '#0F172A',
  },
  headerSubtitle: {
    fontSize: 12,
    fontWeight: '600',
    color: '#10B981',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginTop: 2,
    marginBottom: 16,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#334155',
    marginBottom: 8,
  },
  countryRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
  },
  countryChip: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    backgroundColor: '#F1F5F9',
    marginRight: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  activeCountryChip: {
    backgroundColor: '#ECFDF5',
    borderColor: '#10B981',
  },
  countryChipText: {
    fontSize: 12,
    color: '#475569',
    fontWeight: '600',
  },
  activeCountryChipText: {
    color: '#059669',
    fontWeight: '700',
  },
  infoBox: {
    backgroundColor: '#F8FAFC',
    borderRadius: 12,
    padding: 14,
    borderWidth: 1,
    borderColor: '#E2E8F0',
    marginBottom: 20,
  },
  infoBoxTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: '#1E293B',
    marginBottom: 6,
  },
  infoBoxText: {
    fontSize: 12,
    color: '#475569',
    marginBottom: 2,
  },
  infoBoxDesc: {
    fontSize: 11,
    color: '#64748B',
    fontStyle: 'italic',
    marginTop: 6,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#1E293B',
    marginTop: 4,
  },
  sectionDesc: {
    fontSize: 12,
    color: '#64748B',
    marginTop: 2,
    marginBottom: 16,
  },
  categoryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
  },
  catLeft: {
    flex: 1,
  },
  categoryName: {
    fontSize: 14,
    fontWeight: '600',
    color: '#0F172A',
  },
  categorySub: {
    fontSize: 11,
    color: '#94A3B8',
    marginTop: 2,
  },
  catRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  percentageInput: {
    width: 60,
    borderWidth: 1,
    borderColor: '#CBD5E1',
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 6,
    fontSize: 14,
    fontWeight: '600',
    color: '#0F172A',
    textAlign: 'center',
    backgroundColor: '#FAFCFF',
  },
  percentSymbol: {
    fontSize: 14,
    fontWeight: '600',
    color: '#64748B',
    marginLeft: 4,
  },
  saveButton: {
    backgroundColor: '#10B981',
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 24,
  },
  saveButtonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '700',
  },
});
