package com.fintech.payment.util;

import com.fintech.payment.exception.AccountNotFoundException;
import com.fintech.payment.exception.InsufficientFundsException;
import com.fintech.payment.exception.InvalidTransferException;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.enums.AccountStatus;
import com.fintech.payment.model.enums.TransactionStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionValidator {

    private final TransactionRepository transactionRepository;

    /**
     * Validates that the transfer amount is within the allowed range.
     *
     * @param amount    the transfer amount
     * @param minAmount the minimum allowed amount
     * @param maxAmount the maximum allowed amount
     * @throws InvalidTransferException if the amount is outside the allowed range
     */
    public void validateTransferAmount(BigDecimal amount, BigDecimal minAmount, BigDecimal maxAmount) {
        if (amount == null) {
            throw new InvalidTransferException("Transfer amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be positive");
        }
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            throw new InvalidTransferException(
                    String.format("Transfer amount %s is below minimum %s", amount, minAmount));
        }
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            throw new InvalidTransferException(
                    String.format("Transfer amount %s exceeds maximum %s", amount, maxAmount));
        }
    }

    /**
     * Validates that the wallet's total daily transfer amount does not exceed the daily limit.
     * Calculates the sum of all completed outbound transfers for the current day.
     *
     * @param walletId   the wallet ID to check
     * @param amount     the new transfer amount to be added
     * @param dailyLimit the maximum daily transfer limit
     * @throws InvalidTransferException if the daily limit would be exceeded
     */
    public void validateDailyLimit(Long walletId, BigDecimal amount, BigDecimal dailyLimit) {
        if (dailyLimit == null) {
            return;
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<Transaction> todayTransactions = transactionRepository.findByWalletIdAndDateRange(
                walletId, startOfDay, endOfDay);

        BigDecimal totalToday = todayTransactions.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT
                        && t.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectedTotal = totalToday.add(amount);
        if (projectedTotal.compareTo(dailyLimit) > 0) {
            throw new InvalidTransferException(
                    String.format("Daily transfer limit exceeded. Today's total: %s, Requested: %s, Limit: %s",
                            totalToday, amount, dailyLimit));
        }

        log.debug("Daily limit check passed. Today's total: {}, New amount: {}, Limit: {}",
                totalToday, amount, dailyLimit);
    }

    /**
     * Validates that the account is in an active state and eligible for transactions.
     *
     * @param account the account to validate
     * @throws AccountNotFoundException if the account is null
     * @throws InvalidTransferException if the account is not in ACTIVE status
     */
    public void validateAccountStatus(Account account) {
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidTransferException(
                    String.format("Account %s is not active. Current status: %s",
                            account.getAccountNumber(), account.getStatus()));
        }
    }
}
