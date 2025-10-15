package com.fintech.payment.service.impl;

import com.fintech.payment.exception.InsufficientBalanceException;
import com.fintech.payment.exception.InvalidOperationException;
import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.response.BalanceResponse;
import com.fintech.payment.model.dto.response.WalletResponse;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.model.enums.WalletStatus;
import com.fintech.payment.repository.AccountRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.NotificationService;
import com.fintech.payment.service.TransactionService;
import com.fintech.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final String WALLET_EVENTS_TOPIC = "wallet-events";

    private final WalletRepository walletRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public WalletResponse createWallet(Long accountId) {
        log.info("Creating wallet for account id: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        if (walletRepository.existsByAccountId(accountId)) {
            throw new InvalidOperationException("Wallet already exists for account " + accountId);
        }

        Wallet wallet = Wallet.builder()
                .account(account)
                .balance(BigDecimal.ZERO)
                .frozenAmount(BigDecimal.ZERO)
                .currency(account.getCurrency())
                .status(WalletStatus.ACTIVE)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("Wallet created: id={}, accountId={}", wallet.getId(), accountId);
        return mapToResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByAccountId(Long accountId) {
        Wallet wallet = walletRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "accountId", accountId));
        return mapToResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long accountId) {
        Wallet wallet = walletRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "accountId", accountId));

        return BalanceResponse.builder()
                .walletId(wallet.getId())
                .totalBalance(wallet.getBalance())
                .availableBalance(wallet.getAvailableBalance())
                .frozenAmount(wallet.getFrozenAmount())
                .currency(wallet.getCurrency())
                .build();
    }

    @Override
    @Transactional
    public WalletResponse deposit(Long accountId, BigDecimal amount, String description, String idempotencyKey) {
        log.info("Processing deposit: accountId={}, amount={}, idempotencyKey={}", accountId, amount, idempotencyKey);

        validateAmount(amount);

        // Acquire pessimistic lock on wallet to prevent concurrent modifications
        Wallet wallet = walletRepository.findByAccountIdWithPessimisticLock(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "accountId", accountId));

        validateWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        wallet.setBalance(balanceAfter);
        wallet = walletRepository.save(wallet);

        // Create transaction record
        Transaction transaction = transactionService.createTransaction(
                wallet.getId(),
                TransactionType.DEPOSIT,
                amount,
                balanceBefore,
                balanceAfter,
                description != null ? description : "Deposit to wallet",
                idempotencyKey,
                null
        );

        // Publish Kafka event for downstream processing
        publishWalletEvent(wallet, "DEPOSIT", amount);

        // Send notification
        notificationService.sendTransactionNotification(transaction);

        log.info("Deposit completed: walletId={}, amount={}, newBalance={}", wallet.getId(), amount, balanceAfter);
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public WalletResponse withdraw(Long accountId, BigDecimal amount, String description, String idempotencyKey) {
        log.info("Processing withdrawal: accountId={}, amount={}, idempotencyKey={}", accountId, amount, idempotencyKey);

        validateAmount(amount);

        // Acquire pessimistic lock
        Wallet wallet = walletRepository.findByAccountIdWithPessimisticLock(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "accountId", accountId));

        validateWalletActive(wallet);

        // Check sufficient available balance (total minus frozen)
        BigDecimal availableBalance = wallet.getAvailableBalance();
        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, availableBalance);
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        wallet.setBalance(balanceAfter);
        wallet = walletRepository.save(wallet);

        // Create transaction record
        Transaction transaction = transactionService.createTransaction(
                wallet.getId(),
                TransactionType.WITHDRAWAL,
                amount,
                balanceBefore,
                balanceAfter,
                description != null ? description : "Withdrawal from wallet",
                idempotencyKey,
                null
        );

        // Publish Kafka event
        publishWalletEvent(wallet, "WITHDRAWAL", amount);

        // Send notification
        notificationService.sendTransactionNotification(transaction);

        log.info("Withdrawal completed: walletId={}, amount={}, newBalance={}", wallet.getId(), amount, balanceAfter);
        return mapToResponse(wallet);
    }

    @Override
    @Transactional
    public Wallet freezeAmount(Long walletId, BigDecimal amount) {
        log.info("Freezing amount: walletId={}, amount={}", walletId, amount);

        validateAmount(amount);

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        validateWalletActive(wallet);

        BigDecimal availableBalance = wallet.getAvailableBalance();
        if (availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, availableBalance);
        }

        wallet.setFrozenAmount(wallet.getFrozenAmount().add(amount));
        wallet = walletRepository.save(wallet);

        log.info("Amount frozen: walletId={}, frozenAmount={}, newFrozenTotal={}",
                walletId, amount, wallet.getFrozenAmount());
        return wallet;
    }

    @Override
    @Transactional
    public Wallet unfreezeAmount(Long walletId, BigDecimal amount) {
        log.info("Unfreezing amount: walletId={}, amount={}", walletId, amount);

        validateAmount(amount);

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", walletId));

        BigDecimal currentFrozen = wallet.getFrozenAmount();
        if (currentFrozen.compareTo(amount) < 0) {
            throw new InvalidOperationException(
                    "Cannot unfreeze " + amount + ". Current frozen amount is " + currentFrozen);
        }

        wallet.setFrozenAmount(currentFrozen.subtract(amount));
        wallet = walletRepository.save(wallet);

        log.info("Amount unfrozen: walletId={}, unfrozenAmount={}, remainingFrozen={}",
                walletId, amount, wallet.getFrozenAmount());
        return wallet;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Amount must be greater than zero");
        }
    }

    private void validateWalletActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new InvalidOperationException(
                    "Wallet is not active. Current status: " + wallet.getStatus());
        }
    }

    private void publishWalletEvent(Wallet wallet, String eventType, BigDecimal amount) {
        try {
            String event = String.format(
                    "{\"eventType\":\"%s\",\"walletId\":%d,\"accountId\":%d,\"amount\":\"%s\",\"balance\":\"%s\",\"currency\":\"%s\",\"timestamp\":\"%s\"}",
                    eventType,
                    wallet.getId(),
                    wallet.getAccount().getId(),
                    amount.toPlainString(),
                    wallet.getBalance().toPlainString(),
                    wallet.getCurrency(),
                    java.time.LocalDateTime.now()
            );
            kafkaTemplate.send(WALLET_EVENTS_TOPIC, wallet.getId().toString(), event);
            log.debug("Wallet event published: {}", eventType);
        } catch (Exception e) {
            // Log but don't fail the transaction for Kafka errors
            log.error("Failed to publish wallet event: {}", e.getMessage(), e);
        }
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .accountId(wallet.getAccount().getId())
                .balance(wallet.getBalance())
                .frozenAmount(wallet.getFrozenAmount())
                .availableBalance(wallet.getAvailableBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
}
