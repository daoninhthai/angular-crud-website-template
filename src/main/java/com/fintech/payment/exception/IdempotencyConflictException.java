package com.fintech.payment.exception;

public class IdempotencyConflictException extends RuntimeException {

    private final String cachedResponse;
    private final int statusCode;

    public IdempotencyConflictException(String cachedResponse, int statusCode) {
        super("Duplicate request detected with idempotency key");
        this.cachedResponse = cachedResponse;
        this.statusCode = statusCode;
    }

    public String getCachedResponse() {
        return cachedResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
