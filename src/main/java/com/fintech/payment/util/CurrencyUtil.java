package com.fintech.payment.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for currency formatting, conversion, and precision handling.
 */
public final class CurrencyUtil {

    private CurrencyUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Decimal places per currency code.
     * Zero-decimal currencies (VND, JPY) use 0; standard currencies use 2.
     */
    private static final Map<String, Integer> DECIMAL_PLACES = new HashMap<>();

    static {
        DECIMAL_PLACES.put("VND", 0);
        DECIMAL_PLACES.put("JPY", 0);
        DECIMAL_PLACES.put("USD", 2);
        DECIMAL_PLACES.put("EUR", 2);
        DECIMAL_PLACES.put("GBP", 2);
        DECIMAL_PLACES.put("SGD", 2);
        DECIMAL_PLACES.put("AUD", 2);
        DECIMAL_PLACES.put("CAD", 2);
    }

    /**
     * Static exchange rates for demonstration purposes.
     * In production, these would come from a real-time exchange rate service.
     * Rates are relative to USD (1 USD = X units of target currency).
     */
    private static final Map<String, BigDecimal> EXCHANGE_RATES_TO_USD = new HashMap<>();

    static {
        EXCHANGE_RATES_TO_USD.put("USD", BigDecimal.ONE);
        EXCHANGE_RATES_TO_USD.put("EUR", new BigDecimal("0.85"));
        EXCHANGE_RATES_TO_USD.put("GBP", new BigDecimal("0.73"));
        EXCHANGE_RATES_TO_USD.put("VND", new BigDecimal("24000"));
        EXCHANGE_RATES_TO_USD.put("JPY", new BigDecimal("110"));
        EXCHANGE_RATES_TO_USD.put("SGD", new BigDecimal("1.35"));
        EXCHANGE_RATES_TO_USD.put("AUD", new BigDecimal("1.38"));
        EXCHANGE_RATES_TO_USD.put("CAD", new BigDecimal("1.25"));
    }

    /**
     * Formats a BigDecimal amount according to the currency's decimal precision.
     *
     * @param amount   the amount to format
     * @param currency the ISO 4217 currency code (e.g., "VND", "USD")
     * @return the formatted amount string with currency symbol
     */
    public static String formatAmount(BigDecimal amount, String currency) {
        if (amount == null || currency == null) {
            return "0";
        }

        int decimals = getDecimalPlaces(currency);
        BigDecimal scaled = amount.setScale(decimals, RoundingMode.HALF_UP);

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(decimals);
        formatter.setMaximumFractionDigits(decimals);

        return formatter.format(scaled) + " " + currency;
    }

    /**
     * Converts an amount from one currency to another using static rates.
     * In production, this should use a real-time rate provider.
     *
     * @param amount       the amount to convert
     * @param fromCurrency the source currency code
     * @param toCurrency   the target currency code
     * @return the converted amount, rounded to the target currency's decimal precision
     * @throws IllegalArgumentException if either currency code is unsupported
     */
    public static BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal fromRate = EXCHANGE_RATES_TO_USD.get(fromCurrency.toUpperCase());
        BigDecimal toRate = EXCHANGE_RATES_TO_USD.get(toCurrency.toUpperCase());

        if (fromRate == null) {
            throw new IllegalArgumentException("Unsupported source currency: " + fromCurrency);
        }
        if (toRate == null) {
            throw new IllegalArgumentException("Unsupported target currency: " + toCurrency);
        }

        // Convert: amount in fromCurrency -> USD -> toCurrency
        // amountInUSD = amount / fromRate
        // amountInTarget = amountInUSD * toRate
        BigDecimal amountInUsd = amount.divide(fromRate, 10, RoundingMode.HALF_UP);
        BigDecimal converted = amountInUsd.multiply(toRate);

        int targetDecimals = getDecimalPlaces(toCurrency);
        return converted.setScale(targetDecimals, RoundingMode.HALF_UP);
    }

    /**
     * Returns the number of decimal places for the given currency code.
     *
     * @param currency the ISO 4217 currency code
     * @return the number of decimal places (0 for VND/JPY, 2 for most others)
     */
    public static int getDecimalPlaces(String currency) {
        if (currency == null) {
            return 2;
        }
        return DECIMAL_PLACES.getOrDefault(currency.toUpperCase(), 2);
    }
}
