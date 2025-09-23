package com.fintech.payment.exception;

public class DuplicateTransactionException extends RuntimeException {

    /**
     * Processes the request and returns the result.
     * This method handles null inputs gracefully.
     */
    public DuplicateTransactionException(String message) {
        super(message);
    }

    public DuplicateTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
