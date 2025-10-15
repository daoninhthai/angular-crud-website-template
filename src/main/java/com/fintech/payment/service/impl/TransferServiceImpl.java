package com.fintech.payment.service.impl;

import com.fintech.payment.exception.InsufficientBalanceException;
import com.fintech.payment.exception.InvalidOperationException;
import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.response.TransferResponse;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Transfer;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.AccountStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.model.enums.TransferStatus;
import com.fintech.payment.repository.AccountRepository;
import com.fintech.payment.repository.TransferRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.IdempotencyService;
import com.fintech.payment.service.NotificationService;
import com.fintech.payment.service.TransactionService;
import com.fintech.payment.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final String TRANSFER_EVENTS_TOPIC = "transfer-events";

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public TransferResponse initiateTransfer(String fromAccountNumber, String toAccountNumber,
                                              BigDecimal amount, String idempotencyKey,
                                              String description) {
        log.info("Initiating transfer: from={}, to={}, amount={}, idempotencyKey={}",
                fromAccountNumber, toAccountNumber, amount, idempotencyKey);

        // Step 1: Check idempotency - return existing result if duplicate
        Optional<Transfer> existingTransfer = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransfer.isPresent()) {
            log.info("Duplicate transfer detected for idempotency key: {}", idempotencyKey);
            return mapToResponse(existingTransfer.get());
        }

        // Step 2: Validate accounts
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidOperationException("Cannot transfer to the same account");
        }

        Account sourceAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", fromAccountNumber));
        Account destAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", toAccountNumber));

        validateAccountActive(sourceAccount, "Source");
        validateAccountActive(destAccount, "Destination");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Transfer amount must be greater than zero");
        }

        // Step 3: Lock both wallets - ORDER BY ID to prevent deadlocks
        Wallet sourceWallet = walletRepository.findByAccountId(sourceAccount.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "accountId", sourceAccount.getId()));
        Wallet destWallet = walletRepository.findByAccountId(destAccount.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "accountId", destAccount.getId()));

        Wallet firstLock, secondLock;
        if (sourceWallet.getId() < destWallet.getId()) {
            firstLock = walletRepository.findByIdWithPessimisticLock(sourceWallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", sourceWallet.getId()));
            secondLock = walletRepository.findByIdWithPessimisticLock(destWallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", destWallet.getId()));
            sourceWallet = firstLock;
            destWallet = secondLock;
        } else {
            firstLock = walletRepository.findByIdWithPessimisticLock(destWallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", destWallet.getId()));
            secondLock = walletRepository.findByIdWithPessimisticLock(sourceWallet.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", sourceWallet.getId()));
            destWallet = firstLock;
            sourceWallet = secondLock;
        }

        // Step 4: Check source has sufficient available balance
        BigDecimal sourceAvailable = sourceWallet.getAvailableBalance();
        if (sourceAvailable.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, sourceAvailable);
        }

        // Step 5: Debit source wallet
        BigDecimal sourceBalanceBefore = sourceWallet.getBalance();
        sourceWallet.setBalance(sourceBalanceBefore.subtract(amount));
        walletRepository.save(sourceWallet);

        // Step 6: Credit destination wallet
        BigDecimal destBalanceBefore = destWallet.getBalance();
        destWallet.setBalance(destBalanceBefore.add(amount));
        walletRepository.save(destWallet);

        // Step 7: Create TRANSFER_OUT transaction for source
        String transferOutIdempotencyKey = idempotencyKey + "_OUT";
        Transaction outTransaction = transactionService.createTransaction(
                sourceWallet.getId(),
                TransactionType.TRANSFER_OUT,
                amount,
                sourceBalanceBefore,
                sourceWallet.getBalance(),
                description != null ? description : "Transfer to " + toAccountNumber,
                transferOutIdempotencyKey,
                destWallet.getId()
        );

        // Create TRANSFER_IN transaction for destination
        String transferInIdempotencyKey = idempotencyKey + "_IN";
        Transaction inTransaction = transactionService.createTransaction(
                destWallet.getId(),
                TransactionType.TRANSFER_IN,
                amount,
                destBalanceBefore,
                destWallet.getBalance(),
                description != null ? description : "Transfer from " + fromAccountNumber,
                transferInIdempotencyKey,
                sourceWallet.getId()
        );

        // Create transfer record
        String referenceNumber = UUID.randomUUID().toString();
        Transfer transfer = Transfer.builder()
                .referenceNumber(referenceNumber)
                .sourceAccount(sourceAccount)
                .destinationAccount(destAccount)
                .amount(amount)
                .currency(sourceWallet.getCurrency())
                .status(TransferStatus.COMPLETED)
                .description(description)
                .idempotencyKey(idempotencyKey)
                .sourceTransactionRef(outTransaction.getReferenceNumber())
                .destinationTransactionRef(inTransaction.getReferenceNumber())
                .build();

        transfer = transferRepository.save(transfer);

        // Step 8: Publish Kafka event
        publishTransferEvent(transfer);

        // Send notifications
        notificationService.sendTransferNotification(transfer);

        log.info("Transfer completed: ref={}, from={}, to={}, amount={}",
                referenceNumber, fromAccountNumber, toAccountNumber, amount);

        return mapToResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransferByRef(String referenceNumber) {
        Transfer transfer = transferRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", "referenceNumber", referenceNumber));
        return mapToResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponse> getTransferHistory(Long accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Account", "id", accountId);
        }

        return transferRepository.findByAccountId(accountId, pageable)
                .map(this::mapToResponse);
    }

    private void validateAccountActive(Account account, String label) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException(
                    label + " account is not active. Current status: " + account.getStatus());
        }
    }

    private void publishTransferEvent(Transfer transfer) {
        try {
            String event = String.format(
                    "{\"eventType\":\"TRANSFER_COMPLETED\",\"referenceNumber\":\"%s\"," +
                            "\"sourceAccountNumber\":\"%s\",\"destinationAccountNumber\":\"%s\"," +
                            "\"amount\":\"%s\",\"currency\":\"%s\",\"timestamp\":\"%s\"}",
                    transfer.getReferenceNumber(),
                    transfer.getSourceAccount().getAccountNumber(),
                    transfer.getDestinationAccount().getAccountNumber(),
                    transfer.getAmount().toPlainString(),
                    transfer.getCurrency(),
                    java.time.LocalDateTime.now()
            );
            kafkaTemplate.send(TRANSFER_EVENTS_TOPIC, transfer.getReferenceNumber(), event);
            log.debug("Transfer event published: ref={}", transfer.getReferenceNumber());
        } catch (Exception e) {
            log.error("Failed to publish transfer event: {}", e.getMessage(), e);
        }
    }

    private TransferResponse mapToResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .referenceNumber(transfer.getReferenceNumber())
                .sourceAccountNumber(transfer.getSourceAccount().getAccountNumber())
                .destinationAccountNumber(transfer.getDestinationAccount().getAccountNumber())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .status(transfer.getStatus())
                .description(transfer.getDescription())
                .failureReason(transfer.getFailureReason())
                .sourceTransactionRef(transfer.getSourceTransactionRef())
                .destinationTransactionRef(transfer.getDestinationTransactionRef())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
