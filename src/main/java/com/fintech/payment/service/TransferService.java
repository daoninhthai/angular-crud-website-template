package com.fintech.payment.service;

import com.fintech.payment.dto.response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Service interface for account-to-account transfers.
 * Transfers are atomic operations that debit the source and credit the destination.
 */
public interface TransferService {

    /**
     * Initiates a transfer between two accounts. The operation is fully atomic:
     * 1. Check idempotency to prevent duplicate transfers
     * 2. Validate both accounts exist and are active
     * 3. Lock both wallets (ordered by ID to prevent deadlocks)
     * 4. Check source has sufficient balance
     * 5. Debit source wallet
     * 6. Credit destination wallet
     * 7. Create TRANSFER_OUT and TRANSFER_IN transaction records
     * 8. Publish Kafka event for downstream processing
     *
     * @param fromAccountNumber source account number
     * @param toAccountNumber   destination account number
     * @param amount            the transfer amount
     * @param idempotencyKey    unique key to prevent duplicate transfers
     * @param description       optional description
     * @return the transfer details
     */
    TransferResponse initiateTransfer(String fromAccountNumber, String toAccountNumber,
                                      BigDecimal amount, String idempotencyKey,
                                      String description);

    /**
     * Retrieves a transfer by its reference number.
     *
     * @param referenceNumber the unique transfer reference
     * @return the transfer details
     * @throws com.fintech.payment.exception.ResourceNotFoundException if not found
     */
    TransferResponse getTransferByRef(String referenceNumber);

    /**
     * Retrieves paginated transfer history for an account
     * (includes both sent and received transfers).
     *
     * @param accountId the account ID
     * @param pageable  pagination parameters
     * @return a page of transfer details
     */
    Page<TransferResponse> getTransferHistory(Long accountId, Pageable pageable);
}
