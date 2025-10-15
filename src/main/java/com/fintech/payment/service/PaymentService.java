package com.fintech.payment.service;

import com.fintech.payment.model.dto.request.CreatePaymentRequest;
import com.fintech.payment.model.dto.request.RefundRequest;
import com.fintech.payment.model.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;

/**
 * Service interface for payment lifecycle management.
 * Payments go through: CREATED -> AMOUNT_HELD -> COMPLETED/FAILED -> optionally REFUNDED.
 */
public interface PaymentService {

    /**
     * Creates a new payment record with idempotency check.
     * Initial status is CREATED.
     *
     * @param request        the payment creation details
     * @param idempotencyKey unique key to prevent duplicate payments
     * @return the created payment details
     */
    PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey);

    /**
     * Processes a payment through its lifecycle:
     * 1. Freeze the payment amount in the wallet
     * 2. Attempt to process (simulate external payment gateway)
     * 3. On success: deduct from wallet, create PAYMENT transaction, mark COMPLETED
     * 4. On failure: unfreeze amount, mark FAILED with reason
     * 5. Send webhook notification on status change
     *
     * @param referenceNumber the payment reference number
     * @return the updated payment details
     */
    PaymentResponse processPayment(String referenceNumber);

    /**
     * Refunds a payment (full or partial).
     * Validates that refund amount <= (paid amount - already refunded amount).
     * Creates a REFUND transaction and credits back to the wallet.
     * Updates the payment's refundedAmount and status.
     *
     * @param referenceNumber the payment reference number
     * @param request         the refund details
     * @return the updated payment details
     */
    PaymentResponse refundPayment(String referenceNumber, RefundRequest request);

    /**
     * Retrieves a payment by its reference number.
     *
     * @param referenceNumber the unique payment reference
     * @return the payment details
     * @throws com.fintech.payment.exception.ResourceNotFoundException if not found
     */
    PaymentResponse getPaymentByRef(String referenceNumber);

    /**
     * Retrieves paginated payments for a specific wallet.
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters
     * @return a page of payment details
     */
    Page<PaymentResponse> getPaymentsByWallet(Long walletId, Pageable pageable);
}
