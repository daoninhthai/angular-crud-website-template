package com.fintech.payment.service;

import com.fintech.payment.dto.response.BalanceResponse;
import com.fintech.payment.dto.response.WalletResponse;
import com.fintech.payment.entity.Wallet;

import java.math.BigDecimal;

/**
 * Service interface for wallet operations including deposits, withdrawals,
 * and amount freezing for payment holds.
 */
public interface WalletService {

    /**
     * Creates a new wallet associated with the given account.
     * Each account can only have one wallet.
     *
     * @param accountId the account to associate with the wallet
     * @return the created wallet details
     */
    WalletResponse createWallet(Long accountId);

    /**
     * Retrieves the wallet associated with the given account.
     *
     * @param accountId the account ID
     * @return the wallet details
     * @throws com.fintech.payment.exception.ResourceNotFoundException if wallet not found
     */
    WalletResponse getWalletByAccountId(Long accountId);

    /**
     * Returns the current balance information for a wallet.
     *
     * @param accountId the account ID
     * @return balance details including total, available, and frozen amounts
     */
    BalanceResponse getBalance(Long accountId);

    /**
     * Deposits funds into the wallet. Uses pessimistic locking to ensure
     * consistency. Creates a DEPOSIT transaction and publishes a Kafka event.
     *
     * @param accountId      the account ID
     * @param amount         the deposit amount (must be positive)
     * @param description    optional description for the transaction
     * @param idempotencyKey unique key to prevent duplicate deposits
     * @return the updated wallet details
     */
    WalletResponse deposit(Long accountId, BigDecimal amount, String description, String idempotencyKey);

    /**
     * Withdraws funds from the wallet. Validates sufficient available balance.
     * Uses pessimistic locking and creates a WITHDRAWAL transaction.
     *
     * @param accountId      the account ID
     * @param amount         the withdrawal amount (must be positive)
     * @param description    optional description for the transaction
     * @param idempotencyKey unique key to prevent duplicate withdrawals
     * @return the updated wallet details
     * @throws com.fintech.payment.exception.InsufficientBalanceException if balance insufficient
     */
    WalletResponse withdraw(Long accountId, BigDecimal amount, String description, String idempotencyKey);

    /**
     * Freezes (holds) an amount in the wallet for a pending payment.
     * The frozen amount reduces available balance but not total balance.
     *
     * @param walletId the wallet ID
     * @param amount   the amount to freeze
     * @return the wallet entity with updated frozen amount
     */
    Wallet freezeAmount(Long walletId, BigDecimal amount);

    /**
     * Releases a previously frozen amount back to available balance.
     *
     * @param walletId the wallet ID
     * @param amount   the amount to unfreeze
     * @return the wallet entity with updated frozen amount
     */
    Wallet unfreezeAmount(Long walletId, BigDecimal amount);
}
