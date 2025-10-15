package com.fintech.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== Custom Business Exceptions ====================

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        log.warn("Insufficient funds: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Insufficient Funds", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTransaction(
            DuplicateTransactionException ex, HttpServletRequest request) {
        log.warn("Duplicate transaction: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Duplicate Transaction", ex.getMessage(), request);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex, HttpServletRequest request) {
        log.warn("Account not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Account Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessing(
            PaymentProcessingException ex, HttpServletRequest request) {
        log.error("Payment processing error: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Payment Processing Error", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(
            InvalidTransferException ex, HttpServletRequest request) {
        log.warn("Invalid transfer: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Transfer", ex.getMessage(), request);
    }

    // ==================== Legacy Exceptions (backward compatibility) ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Duplicate Resource", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest request) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Balance", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {
        log.warn("Invalid operation: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid Operation", ex.getMessage(), request);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<String> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .header("Content-Type", "application/json")
                .body(ex.getCachedResponse());
    }

    @ExceptionHandler(WebhookSignatureException.class)
    public ResponseEntity<ErrorResponse> handleWebhookSignature(
            WebhookSignatureException ex, HttpServletRequest request) {
        log.warn("Webhook signature verification failed: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid Signature", ex.getMessage(), request);
    }

    // ==================== Validation Exceptions ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return buildResponse(HttpStatus.BAD_REQUEST, "Constraint Violation", message, request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing required header: {}", ex.getHeaderName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Missing Header",
                "Missing required header: " + ex.getHeaderName(), request);
    }

    // ==================== Generic Exception ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // ==================== Helper ====================

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }
}
