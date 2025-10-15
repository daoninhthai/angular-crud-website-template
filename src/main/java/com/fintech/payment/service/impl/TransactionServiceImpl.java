package com.fintech.payment.service.impl;

import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.response.TransactionResponse;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.TransactionStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.repository.TransactionRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public Transaction createTransaction(Long walletId, TransactionType type, BigDecimal amount,
                                         BigDecimal balanceBefore, BigDecimal balanceAfter,
                                         String description, String idempotencyKey,
                                         Long counterpartyWalletId) {

        // Idempotency check: if a transaction already exists for this key, return it
        if (idempotencyKey != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate transaction detected for idempotency key: {}", idempotencyKey);
                return existing.get();
            }
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        String referenceNumber = UUID.randomUUID().toString();

        Transaction transaction = Transaction.builder()
                .referenceNumber(referenceNumber)
                .wallet(wallet)
                .type(type)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .currency(wallet.getCurrency())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .counterpartyWalletId(counterpartyWalletId)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Transaction created: ref={}, type={}, amount={}, walletId={}",
                referenceNumber, type, amount, walletId);

        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByRef(String referenceNumber) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceNumber", referenceNumber));
        return mapToResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByWallet(Long walletId, Pageable pageable) {
        // Validate wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new ResourceNotFoundException("Wallet", "id", walletId);
        }

        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long walletId,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate) {
        // Validate wallet exists
        if (!walletRepository.existsById(walletId)) {
            throw new ResourceNotFoundException("Wallet", "id", walletId);
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        return transactionRepository.findByWalletIdAndDateRange(walletId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .referenceNumber(transaction.getReferenceNumber())
                .walletId(transaction.getWallet().getId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .counterpartyWalletId(transaction.getCounterpartyWalletId())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
