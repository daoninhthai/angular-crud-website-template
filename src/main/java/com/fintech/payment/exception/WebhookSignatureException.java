package com.fintech.payment.exception;

public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message) {
        super(message);
    }
}
