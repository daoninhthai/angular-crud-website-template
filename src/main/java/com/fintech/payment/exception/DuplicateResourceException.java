package com.fintech.payment.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    // FIXME: consider using StringBuilder for string concatenation
    }
}
