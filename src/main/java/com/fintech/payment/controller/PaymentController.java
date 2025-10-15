package com.fintech.payment.controller;

import com.fintech.payment.model.dto.request.CreatePaymentRequest;
import com.fintech.payment.model.dto.request.RefundRequest;
import com.fintech.payment.model.dto.response.ApiResponse;
import com.fintech.payment.model.dto.response.PaymentResponse;
import com.fintech.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for payment lifecycle management.
 * Handles payment creation, processing, refunds, and queries.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment. Initial status is CREATED.
     * The payment must be processed separately via the /process endpoint.
     * Requires an Idempotency-Key header.
     *
     * @param idempotencyKey unique key for idempotent processing
     * @param request        the payment creation details
     * @return the created payment with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        log.info("REST request to create payment: walletId={}, amount={}, merchant={}, idempotencyKey={}",
                request.getWalletId(), request.getAmount(), request.getMerchantName(), idempotencyKey);
        PaymentResponse payment = paymentService.createPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(payment, "Payment created successfully"));
    }

    /**
     * Processes a previously created payment.
     * Freezes funds, processes with payment gateway, and completes or fails the payment.
     * Triggers webhook notification on status change.
     *
     * @param ref the payment reference number
     * @return the updated payment details
     */
    @PostMapping("/{ref}/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@PathVariable String ref) {
        log.info("REST request to process payment: ref={}", ref);
        PaymentResponse payment = paymentService.processPayment(ref);
        return ResponseEntity.ok(ApiResponse.ok(payment, "Payment processed"));
    }

    /**
     * Refunds a completed payment (full or partial).
     * The refund amount must not exceed (original amount - already refunded amount).
     * Credits the refund back to the wallet and creates a REFUND transaction.
     *
     * @param ref     the payment reference number
     * @param request the refund amount and optional reason
     * @return the updated payment details
     */
    @PostMapping("/{ref}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable String ref,
            @Valid @RequestBody RefundRequest request) {
        log.info("REST request to refund payment: ref={}, amount={}", ref, request.getAmount());
        PaymentResponse payment = paymentService.refundPayment(ref, request);
        return ResponseEntity.ok(ApiResponse.ok(payment, "Refund processed successfully"));
    }

    /**
     * Retrieves a payment by its unique reference number.
     *
     * @param ref the payment reference number
     * @return the payment details
     */
    @GetMapping("/{ref}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByRef(@PathVariable String ref) {
        log.info("REST request to get payment by ref: {}", ref);
        PaymentResponse payment = paymentService.getPaymentByRef(ref);
        return ResponseEntity.ok(ApiResponse.ok(payment));
    }

    /**
     * Retrieves paginated payments for a specific wallet.
     * Results are ordered by creation date descending.
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters
     * @return a page of payment records
     */
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByWallet(
            @PathVariable Long walletId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to get payments for wallet: {}", walletId);
        Page<PaymentResponse> payments = paymentService.getPaymentsByWallet(walletId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }
}
