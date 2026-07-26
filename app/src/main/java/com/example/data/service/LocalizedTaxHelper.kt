package com.example.data.service

import java.util.Locale

data class TaxRuleTemplate(
    val countryCode: String,
    val countryName: String,
    val currency: String,
    val standardVatOrSalesTax: Double,
    val taxFilingDeadline: String,
    val description: String,
    val deductibleCategories: List<String>,
    val taxBrackets: List<TaxBracket>,
    val localizedTaxTips: List<String>
)

data class TaxBracket(
    val incomeMin: Double,
    val incomeMax: Double,
    val rate: Double
)

data class TaxEvaluationResult(
    val isTaxDeductible: Boolean,
    val suggestedCategory: String,
    val taxRate: Double,
    val estimatedTaxAmount: Double,
    val taxTip: String,
    val regionName: String,
    val currency: String
)

class LocalizedTaxService {

    /**
     * Evaluates an expense or transaction amount against localized tax rules for a user-defined region.
     * Categorizes tax deductibility and suggests category alignment.
     */
    fun evaluateTransactionTax(
        amount: Double,
        descriptionOrNotes: String,
        categoryName: String,
        regionCode: String
    ): TaxEvaluationResult {
        val template = LocalizedTaxHelper.getTemplateForCountry(regionCode)
        val lowerText = "$descriptionOrNotes $categoryName".lowercase(Locale.ROOT)

        val isDeductible = template.deductibleCategories.any { cat ->
            lowerText.contains(cat.lowercase(Locale.ROOT)) ||
                    cat.lowercase(Locale.ROOT).split(" ").any { word -> word.length > 3 && lowerText.contains(word) }
        } || lowerText.contains("tax") || lowerText.contains("health") || lowerText.contains("ira") ||
                lowerText.contains("charity") || lowerText.contains("donation") || lowerText.contains("pension") ||
                lowerText.contains("medical") || lowerText.contains("business")

        val matchedCategory = template.deductibleCategories.firstOrNull { cat ->
            lowerText.contains(cat.lowercase(Locale.ROOT))
        } ?: if (isDeductible) template.deductibleCategories.firstOrNull() ?: "Tax Deductible" else categoryName

        val taxRate = template.standardVatOrSalesTax
        val estimatedTax = amount * (taxRate / 100.0)
        val tip = template.localizedTaxTips.randomOrNull() ?: "Keep itemized receipts to substantiate tax filings."

        return TaxEvaluationResult(
            isTaxDeductible = isDeductible,
            suggestedCategory = matchedCategory,
            taxRate = taxRate,
            estimatedTaxAmount = estimatedTax,
            taxTip = tip,
            regionName = template.countryName,
            currency = template.currency
        )
    }
}

object LocalizedTaxHelper {

    fun getTemplateForCountry(countryCode: String): TaxRuleTemplate {
        return when (countryCode.uppercase().trim()) {
            "US", "USA" -> TaxRuleTemplate(
                countryCode = "US",
                countryName = "United States",
                currency = "USD",
                standardVatOrSalesTax = 8.25,
                taxFilingDeadline = "April 15",
                description = "Federal income tax with state-level sales tax variation. Charitable contributions and Traditional IRA are major deductions.",
                deductibleCategories = listOf("W2 Deductible Health", "Traditional IRA", "Charitable Charity (501c3)", "Business Travel"),
                taxBrackets = listOf(
                    TaxBracket(0.0, 11600.0, 10.0),
                    TaxBracket(11600.0, 47150.0, 12.0),
                    TaxBracket(47150.0, 100525.0, 22.0),
                    TaxBracket(100525.0, 191950.0, 24.0),
                    TaxBracket(191950.0, 243725.0, 32.0),
                    TaxBracket(243725.0, 609350.0, 35.0),
                    TaxBracket(609350.0, Double.MAX_VALUE, 37.0)
                ),
                localizedTaxTips = listOf(
                    "Contribute to your Traditional IRA before the filing deadline to lower your taxable gross income.",
                    "Keep structured receipt logs of business miles and charity donations to substantiate W2/1099 schedules.",
                    "Health Savings Accounts (HSAs) offer a triple tax advantage — contributions are 100% tax-deductible."
                )
            )
            "DE", "GER", "GERMANY" -> TaxRuleTemplate(
                countryCode = "DE",
                countryName = "Germany",
                currency = "EUR",
                standardVatOrSalesTax = 19.0,
                taxFilingDeadline = "July 31",
                description = "Progressive income tax (Einkommensteuer) with solidarity surcharge. Extensive professional deductions (Werbungskosten) allowed.",
                deductibleCategories = listOf("Riester Pension", "Werbungskosten (Professional)", "Krankenkasse Health Plan", "Spenden (Donation)"),
                taxBrackets = listOf(
                    TaxBracket(0.0, 11604.0, 0.0),
                    TaxBracket(11604.0, 66760.0, 14.0),
                    TaxBracket(66760.0, 277825.0, 42.0),
                    TaxBracket(277825.0, Double.MAX_VALUE, 45.0)
                ),
                localizedTaxTips = listOf(
                    "You can deduct up to €1,200 annually for work-related professional expenses (Werbungskosten) without submitting receipts.",
                    "Riester pension plan contributions receive direct government subsidies and are deductible from annual taxes.",
                    "Home office lump sum allowances allow you to deduct up to €6 per day (€1,260 max per year) for working from home."
                )
            )
            "IN", "IND", "INDIA" -> TaxRuleTemplate(
                countryCode = "IN",
                countryName = "India",
                currency = "INR",
                standardVatOrSalesTax = 18.0,
                taxFilingDeadline = "July 31",
                description = "Dual tax regime (Old vs New). Deductions like Section 80C, 80D are available only under the Old Tax Regime.",
                deductibleCategories = listOf("Section 80C Provident Fund", "Section 80D Health Premium", "National Pension System", "House Rent Allowance"),
                taxBrackets = listOf(
                    TaxBracket(0.0, 300000.0, 0.0),
                    TaxBracket(300000.0, 600000.0, 5.0),
                    TaxBracket(600000.0, 900000.0, 10.0),
                    TaxBracket(900000.0, 1200000.0, 15.0),
                    TaxBracket(1200000.0, 1500000.0, 20.0),
                    TaxBracket(1500000.0, Double.MAX_VALUE, 30.0)
                ),
                localizedTaxTips = listOf(
                    "Maximize your Section 80C deductions (up to ₹1.5 Lakhs) using ELSS Mutual Funds, PPF, or Term Insurance premiums.",
                    "Save additional tax under Section 80CCD(1B) with an extra deduction of ₹50,000 via National Pension System (NPS).",
                    "Health insurance premiums paid for parents are deductible up to ₹50,000 under Section 80D."
                )
            )
            "BD", "BGD", "BANGLADESH" -> TaxRuleTemplate(
                countryCode = "BD",
                countryName = "Bangladesh",
                currency = "BDT",
                standardVatOrSalesTax = 15.0,
                taxFilingDeadline = "November 30",
                description = "Universal self-assessment income tax brackets. Tax rebate eligibility of 15% on qualifying investments.",
                deductibleCategories = listOf("DPS & Sanchayapatra", "Government Stock & Bonds", "Life Insurance Premium", "Benevolent Fund contribution"),
                taxBrackets = listOf(
                    TaxBracket(0.0, 350000.0, 0.0),
                    TaxBracket(350000.0, 450000.0, 5.0),
                    TaxBracket(450000.0, 750000.0, 10.0),
                    TaxBracket(750000.0, 1150000.0, 15.0),
                    TaxBracket(1150000.0, 1650000.0, 20.0),
                    TaxBracket(1650000.0, Double.MAX_VALUE, 25.0)
                ),
                localizedTaxTips = listOf(
                    "Invest in government-approved savings tools (Sanchayapatra) or DPS to avail a flat 15% tax rebate on investments.",
                    "Keep records of your house rent and medical allowance exemptions to claim allowances during universal assessment.",
                    "Filing after Tax Day (November 30) incurs a penalty and restricts dynamic rebate carryforwards."
                )
            )
            else -> getTemplateForCountry("US") // fallback to US rules
        }
    }
}
