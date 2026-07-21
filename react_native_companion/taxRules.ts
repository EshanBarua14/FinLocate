/**
 * Localized Tax Rules and Bracket Templates
 * Supports automated compliance thresholds, deductibility categories, and filing guidelines.
 */

export interface TaxBracket {
  incomeMin: number;
  incomeMax: number;
  rate: number; // Percentage (e.g. 15.0 for 15%)
}

export interface TaxRuleTemplate {
  countryCode: string;
  countryName: string;
  currency: string;
  standardVatOrSalesTax: number;
  taxFilingDeadline: string;
  description: string;
  deductibleCategories: string[];
  taxBrackets: TaxBracket[];
  localizedTaxTips: string[];
}

export const LOCALIZED_TAX_TEMPLATES: Record<string, TaxRuleTemplate> = {
  US: {
    countryCode: "US",
    countryName: "United States",
    currency: "USD",
    standardVatOrSalesTax: 8.25,
    taxFilingDeadline: "April 15",
    description: "Federal progressive system. Deductions are available for W2 Health Savings, HSA, IRA, and charitable contributions.",
    deductibleCategories: ["W2 Deductible Health", "Traditional IRA", "Charitable Charity (501c3)", "Business Travel"],
    taxBrackets: [
      { incomeMin: 0, incomeMax: 11600, rate: 10.0 },
      { incomeMin: 11600, incomeMax: 47150, rate: 12.0 },
      { incomeMin: 47150, incomeMax: 100525, rate: 22.0 },
      { incomeMin: 100525, incomeMax: 191950, rate: 24.0 },
      { incomeMin: 191950, incomeMax: 243725, rate: 32.0 },
      { incomeMin: 243725, incomeMax: 609350, rate: 35.0 },
      { incomeMin: 609350, incomeMax: Infinity, rate: 37.0 }
    ],
    localizedTaxTips: [
      "Contribute to your Traditional IRA before the filing deadline to lower your taxable gross income.",
      "Keep structured receipt logs of business miles and charity donations to substantiate W2/1099 schedules.",
      "Health Savings Accounts (HSAs) offer a triple tax advantage — contributions are 100% tax-deductible."
    ]
  },
  DE: {
    countryCode: "DE",
    countryName: "Germany",
    currency: "EUR",
    standardVatOrSalesTax: 19.0,
    taxFilingDeadline: "July 31",
    description: "Progressive Einkommensteuer. Standard deductible allowance (Werbungskosten) is available for work and training expenses.",
    deductibleCategories: ["Riester Pension", "Werbungskosten (Professional)", "Krankenkasse Health Plan", "Spenden (Donation)"],
    taxBrackets: [
      { incomeMin: 0, incomeMax: 11604, rate: 0.0 },
      { incomeMin: 11604, incomeMax: 66760, rate: 14.0 },
      { incomeMin: 66760, incomeMax: 277825, rate: 42.0 },
      { incomeMin: 277825, incomeMax: Infinity, rate: 45.0 }
    ],
    localizedTaxTips: [
      "You can deduct up to €1,200 annually for work-related professional expenses (Werbungskosten) without submitting receipts.",
      "Riester pension plan contributions receive direct government subsidies and are deductible from annual taxes.",
      "Home office lump sum allowances allow you to deduct up to €6 per day (€1,260 max per year) for working from home."
    ]
  },
  IN: {
    countryCode: "IN",
    countryName: "India",
    currency: "INR",
    standardVatOrSalesTax: 18.0,
    taxFilingDeadline: "July 31",
    description: "Dual tax regimes. Tax-saving deductions are claimable under traditional old-regime Section 80C and 80D investments.",
    deductibleCategories: ["Section 80C Provident Fund", "Section 80D Health Premium", "National Pension System", "House Rent Allowance"],
    taxBrackets: [
      { incomeMin: 0, incomeMax: 300000, rate: 0.0 },
      { incomeMin: 300000, incomeMax: 600000, rate: 5.0 },
      { incomeMin: 600000, incomeMax: 900000, rate: 10.0 },
      { incomeMin: 900000, incomeMax: 1200000, rate: 15.0 },
      { incomeMin: 1200000, incomeMax: 1500000, rate: 20.0 },
      { incomeMin: 1500000, incomeMax: Infinity, rate: 30.0 }
    ],
    localizedTaxTips: [
      "Maximize your Section 80C deductions (up to ₹1.5 Lakhs) using ELSS Mutual Funds, PPF, or Term Insurance premiums.",
      "Save additional tax under Section 80CCD(1B) with an extra deduction of ₹50,000 via National Pension System (NPS).",
      "Health insurance premiums paid for parents are deductible up to ₹50,000 under Section 80D."
    ]
  },
  BD: {
    countryCode: "BD",
    countryName: "Bangladesh",
    currency: "BDT",
    standardVatOrSalesTax: 15.0,
    taxFilingDeadline: "November 30",
    description: "Universal self-assessment rules. Earn dynamic 15% rebate credits by investing in Sanchayapatra and DPS accounts.",
    deductibleCategories: ["DPS & Sanchayapatra", "Government Stock & Bonds", "Life Insurance Premium", "Benevolent Fund contribution"],
    taxBrackets: [
      { incomeMin: 0, incomeMax: 350000, rate: 0.0 },
      { incomeMin: 350000, incomeMax: 450000, rate: 5.0 },
      { incomeMin: 450000, incomeMax: 750000, rate: 10.0 },
      { incomeMin: 750000, incomeMax: 1150000, rate: 15.0 },
      { incomeMin: 1150000, incomeMax: 1650000, rate: 20.0 },
      { incomeMin: 1650000, incomeMax: Infinity, rate: 25.0 }
    ],
    localizedTaxTips: [
      "Invest in government-approved savings tools (Sanchayapatra) or DPS to avail a flat 15% tax rebate on investments.",
      "Keep records of your house rent and medical allowance exemptions to claim allowances during universal assessment.",
      "Filing after Tax Day (November 30) incurs a penalty and restricts dynamic rebate carryforwards."
    ]
  }
};

/**
 * Helper to fetch tax templates with an automatic safe US fallback.
 */
export function getTaxTemplate(countryCode: string): TaxRuleTemplate {
  const code = countryCode.toUpperCase().trim();
  return LOCALIZED_TAX_TEMPLATES[code] || LOCALIZED_TAX_TEMPLATES.US;
}
