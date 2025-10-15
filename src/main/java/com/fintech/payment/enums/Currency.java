package com.fintech.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Currency {

    VND("VND", "Vietnamese Dong", "\u20AB"),
    USD("USD", "United States Dollar", "$"),
    EUR("EUR", "Euro", "\u20AC");

    private final String code;
    private final String displayName;
    private final String symbol;

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
