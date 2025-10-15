package com.fintech.payment.service;

import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.response.TransactionResponse;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.Currency;
import com.fintech.payment.model.enums.TransactionStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.model.enums.WalletStatus;
import com.fintech.payment.repository.TransactionRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Wallet testWallet;
    private Transaction testTransaction;
    private String referenceNumber;

    @BeforeEach
    void setUp() {
        referenceNumber = UUID.randomUUID().toString();

        Account testAccount = Account.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .accountNumber("PAY1234567890")
                .passwordHash("hash")
                .build();
        testAccount.setId(1L);

        testWallet = Wallet.builder()
                .account(testAccount)
                .balance(new BigDecimal("1000.0000"))
                .frozenAmount(BigDecimal.ZERO)
                .currency(Currency.USD)
                .status(WalletStatus.ACTIVE)
                .build();
        testWallet.setId(1L);
        testWallet.setCreatedAt(LocalDateTime.now());

        testTransaction = Transaction.builder()
                .referenceNumber(referenceNumber)
                .wallet(testWallet)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("500.00"))
                .currency(Currency.USD)
                .balanceBefore(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .description("Test deposit")
                .idempotencyKey("idempotency-key-1")
                .build();
        testTransaction.setId(1L);
        testTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("createTransaction - success: should save and return transaction")
    void createTransaction_success() {
        String idempotencyKey = UUID.randomUUID().toString();

        when(transactionRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());
        when(walletRepository.findById(1L)).thenReturn(Optional.of(testWallet));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction saved = invocation.getArgument(0);
                    saved.setId(1L);
                    saved.setCreatedAt(LocalDateTime.now());
                    return saved;
                });

        Transaction result = transactionService.createTransaction(
                1L, TransactionType.DEPOSIT, new BigDecimal("500.00"),
                new BigDecimal("500.00"), new BigDecimal("1000.00"),
                "Test deposit", idempotencyKey, null);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransactionByRef - found: should return transaction response")
    void getTransactionByRef_found() {
        when(transactionRepository.findByReferenceNumber(referenceNumber))
                .thenReturn(Optional.of(testTransaction));

        TransactionResponse response = transactionService.getTransactionByRef(referenceNumber);

        assertThat(response).isNotNull();
        assertThat(response.getReferenceNumber()).isEqualTo(referenceNumber);
        assertThat(response.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(transactionRepository).findByReferenceNumber(referenceNumber);
    }

    @Test
    @DisplayName("getTransactionByRef - not found: should throw ResourceNotFoundException")
    void getTransactionByRef_notFound() {
        String missingRef = "non-existent-ref";
        when(transactionRepository.findByReferenceNumber(missingRef))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionByRef(missingRef))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository).findByReferenceNumber(missingRef);
    }

    @Test
    @DisplayName("getTransactionHistory - returns page of transactions")
    void getTransactionHistory_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaction> page = new PageImpl<>(
                Collections.singletonList(testTransaction), pageable, 1);

        when(walletRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(page);

        Page<TransactionResponse> result = transactionService.getTransactionsByWallet(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReferenceNumber()).isEqualTo(referenceNumber);
        verify(transactionRepository).findByWalletIdOrderByCreatedAtDesc(1L, pageable);
    }
}
