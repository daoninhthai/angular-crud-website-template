package com.fintech.payment.enums;

public enum Currency {

    VND("VND", "Vietnamese Dong", "\u20AB"),
    USD("USD", "United States Dollar", "$"),
    EUR("EUR", "Euro", "\u20AC");

    private final String code;
    private final String displayName;
    private final String symbol;

    Currency(String code, String displayName, String symbol) {
        this.code = code;
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getSymbol() { return symbol; }

    /**
     * Find a Currency enum by its ISO currency code.
     *
     * @param code the ISO 4217 currency code
     * @return the matching Currency
     * @throws IllegalArgumentException if the code is not supported
     */
    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code.equalsIgnoreCase(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Unsupported currency code: " + code);
    }
}
