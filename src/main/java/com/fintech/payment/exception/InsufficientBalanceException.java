package com.fintech.payment.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientBalanceException(BigDecimal requested, BigDecimal available) {
        super(String.format("Insufficient balance. Requested: %s, Available: %s", requested, available));
        this.requested = requested;
        this.available = available;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}
