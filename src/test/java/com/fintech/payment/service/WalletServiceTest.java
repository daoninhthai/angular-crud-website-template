package com.fintech.payment.service;

import com.fintech.payment.exception.InsufficientBalanceException;
import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.response.BalanceResponse;
import com.fintech.payment.model.dto.response.WalletResponse;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.AccountStatus;
import com.fintech.payment.model.enums.Currency;
import com.fintech.payment.model.enums.TransactionStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.model.enums.WalletStatus;
import com.fintech.payment.repository.AccountRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.impl.WalletServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private WalletServiceImpl walletService;

    private Account testAccount;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .accountNumber("PAY1234567890")
                .passwordHash("hashedpassword")
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .build();
        testAccount.setId(1L);
        testAccount.setCreatedAt(LocalDateTime.now());

        testWallet = Wallet.builder()
                .account(testAccount)
                .balance(new BigDecimal("1000.0000"))
                .frozenAmount(BigDecimal.ZERO)
                .currency(Currency.USD)
                .status(WalletStatus.ACTIVE)
                .build();
        testWallet.setId(1L);
        testWallet.setCreatedAt(LocalDateTime.now());
        testWallet.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("deposit - success: should add amount to wallet balance")
    void deposit_success() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal expectedBalance = testWallet.getBalance().add(depositAmount);

        when(walletRepository.findByAccountIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction mockTransaction = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .wallet(testWallet)
                .type(TransactionType.DEPOSIT)
                .amount(depositAmount)
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionService.createTransaction(anyLong(), eq(TransactionType.DEPOSIT),
                eq(depositAmount), any(), any(), anyString(), eq(idempotencyKey), isNull()))
                .thenReturn(mockTransaction);

        WalletResponse response = walletService.deposit(1L, depositAmount, "Test deposit", idempotencyKey);

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(expectedBalance);
        verify(walletRepository).findByAccountIdWithPessimisticLock(1L);
        verify(walletRepository).save(any(Wallet.class));
        verify(transactionService).createTransaction(anyLong(), eq(TransactionType.DEPOSIT),
                eq(depositAmount), any(), any(), anyString(), eq(idempotencyKey), isNull());
    }

    @Test
    @DisplayName("deposit - idempotent: existing transaction returns same wallet state")
    void deposit_idempotent() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        String idempotencyKey = "existing-key";

        when(walletRepository.findByAccountIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction existingTransaction = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .wallet(testWallet)
                .type(TransactionType.DEPOSIT)
                .amount(depositAmount)
                .idempotencyKey(idempotencyKey)
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionService.createTransaction(anyLong(), eq(TransactionType.DEPOSIT),
                eq(depositAmount), any(), any(), anyString(), eq(idempotencyKey), isNull()))
                .thenReturn(existingTransaction);

        WalletResponse response = walletService.deposit(1L, depositAmount, "Test deposit", idempotencyKey);

        assertThat(response).isNotNull();
        verify(walletRepository).findByAccountIdWithPessimisticLock(1L);
    }

    @Test
    @DisplayName("withdraw - success: should subtract amount from wallet balance")
    void withdraw_success() {
        BigDecimal withdrawAmount = new BigDecimal("200.00");
        String idempotencyKey = UUID.randomUUID().toString();
        BigDecimal expectedBalance = testWallet.getBalance().subtract(withdrawAmount);

        when(walletRepository.findByAccountIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction mockTransaction = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .wallet(testWallet)
                .type(TransactionType.WITHDRAWAL)
                .amount(withdrawAmount)
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionService.createTransaction(anyLong(), eq(TransactionType.WITHDRAWAL),
                eq(withdrawAmount), any(), any(), anyString(), eq(idempotencyKey), isNull()))
                .thenReturn(mockTransaction);

        WalletResponse response = walletService.withdraw(1L, withdrawAmount, "Test withdrawal", idempotencyKey);

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(expectedBalance);
        verify(walletRepository).findByAccountIdWithPessimisticLock(1L);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    @DisplayName("withdraw - insufficient funds: should throw InsufficientBalanceException")
    void withdraw_insufficientFunds_throwsException() {
        BigDecimal withdrawAmount = new BigDecimal("5000.00"); // More than balance of 1000
        String idempotencyKey = UUID.randomUUID().toString();

        when(walletRepository.findByAccountIdWithPessimisticLock(1L))
                .thenReturn(Optional.of(testWallet));

        assertThatThrownBy(() ->
                walletService.withdraw(1L, withdrawAmount, "Large withdrawal", idempotencyKey))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(walletRepository).findByAccountIdWithPessimisticLock(1L);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("getBalance - success: should return correct balance breakdown")
    void getBalance_success() {
        testWallet.setFrozenAmount(new BigDecimal("100.0000"));

        when(walletRepository.findByAccountId(1L)).thenReturn(Optional.of(testWallet));

        BalanceResponse response = walletService.getBalance(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTotalBalance()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(response.getFrozenAmount()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(response.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("900.0000"));
        verify(walletRepository).findByAccountId(1L);
    }

    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }


    /**
     * Formats a timestamp for logging purposes.
     * @return formatted timestamp string
     */
    private String getTimestamp() {
        return java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}
