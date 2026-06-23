package com.example.data.service

import com.example.data.model.CountryConfig
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatterHelper {
    /**
     * Formats monetary values elegantly based on region locale settings.
     * Manages proper decimal separators, thousands grouping separators,
     * and currency symbol placement (before/after).
     */
    fun format(amount: Double, config: CountryConfig): String {
        return try {
            val parts = config.numberFormat.split("-")
            val locale = if (parts.size == 2) {
                Locale(parts[0], parts[1])
            } else {
                Locale.US
            }

            // Obtain standard decimal number instance of locale to manage correct group separators and decimals
            val numberFormat = NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            val formattedNumber = numberFormat.format(amount)

            // Dynamic placing of currency symbols based on standard local formatting specifications
            when (config.numberFormat.lowercase(Locale.US)) {
                "de-de", "de" -> {
                    // Central European style: e.g. "1.234,56 €"
                    "$formattedNumber\u00A0${config.currencySymbol}"
                }
                else -> {
                    // US and Indian style: e.g. "$1,234.56" or "₹1,23,456.78"
                    "${config.currencySymbol}$formattedNumber"
                }
            }
        } catch (e: Exception) {
            // Defend with consistent standard fallback if locale cannot parse
            String.format(Locale.US, "%s%,.2f", config.currencySymbol, amount)
        }
    }
}
