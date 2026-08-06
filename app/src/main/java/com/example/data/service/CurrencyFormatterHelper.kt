package com.example.data.service

import com.example.data.model.CountryConfig
import java.text.NumberFormat
import java.util.Currency
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

    /**
     * Formats transaction amounts dynamically using explicit Currency code and user's preferred Locale.
     */
    fun format(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val formatInstance = NumberFormat.getCurrencyInstance(locale)
            try {
                formatInstance.currency = Currency.getInstance(currencyCode)
            } catch (_: Exception) {}
            formatInstance.format(amount)
        } catch (e: Exception) {
            val numberFormat = NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            "$currencyCode ${numberFormat.format(amount)}"
        }
    }

    /**
     * Utility method to format amount with custom symbol and custom target locale.
     */
    fun formatAmount(amount: Double, currencySymbol: String, locale: Locale = Locale.getDefault()): String {
        val numberFormat = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currencySymbol${numberFormat.format(amount)}"
    }

    /**
     * Returns country flag emoji for popular currency codes.
     */
    fun getCurrencyFlagEmoji(currencyCode: String): String {
        return when (currencyCode.uppercase(Locale.US)) {
            "USD" -> "🇺🇸"
            "EUR" -> "🇪🇺"
            "GBP" -> "🇬🇧"
            "JPY" -> "🇯🇵"
            "CAD" -> "🇨🇦"
            "AUD" -> "🇦🇺"
            "INR" -> "🇮🇳"
            "BDT" -> "🇧🇩"
            "SGD" -> "🇸🇬"
            "CNY" -> "🇨🇳"
            "CHF" -> "🇨🇭"
            "AED" -> "🇦🇪"
            "SAR" -> "🇸🇦"
            else -> "🌐"
        }
    }
}

