package com.fintech.payment.service;

import com.fintech.payment.dto.response.TransactionResponse;
import com.fintech.payment.entity.Transaction;
import com.fintech.payment.enums.TransactionType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for transaction recording and history queries.
 * All financial operations produce transaction records for audit trail.
 */
public interface TransactionService {

    /**
     * Creates a new transaction record with idempotency support.
     * Records the balance before and after the operation.
     *
     * @param walletId              the wallet involved
     * @param type                  the transaction type
     * @param amount                the transaction amount
     * @param balanceBefore         wallet balance before the operation
     * @param balanceAfter          wallet balance after the operation
     * @param description           optional description
     * @param idempotencyKey        idempotency key for deduplication
     * @param counterpartyWalletId  counterparty wallet ID (for transfers)
     * @return the created transaction entity
     */
    Transaction createTransaction(Long walletId, TransactionType type, BigDecimal amount,
                                  BigDecimal balanceBefore, BigDecimal balanceAfter,
                                  String description, String idempotencyKey,
                                  Long counterpartyWalletId);

    /**
     * Retrieves a transaction by its reference number.
     *
     * @param referenceNumber the unique transaction reference
     * @return the transaction details
     * @throws com.fintech.payment.exception.ResourceNotFoundException if not found
     */
    TransactionResponse getTransactionByRef(String referenceNumber);

    /**
     * Retrieves paginated transactions for a specific wallet.
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters
     * @return a page of transaction details
     */
    Page<TransactionResponse> getTransactionsByWallet(Long walletId, Pageable pageable);

    /**
     * Retrieves transaction history for a wallet within a date range.
     *
     * @param walletId  the wallet ID
     * @param startDate the start of the date range (inclusive)
     * @param endDate   the end of the date range (inclusive)
     * @return list of transactions within the date range
     */
    List<TransactionResponse> getTransactionHistory(Long walletId, LocalDateTime startDate, LocalDateTime endDate);
}
