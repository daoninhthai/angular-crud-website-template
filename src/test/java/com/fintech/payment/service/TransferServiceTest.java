package com.fintech.payment.service;

import com.fintech.payment.exception.InsufficientBalanceException;
import com.fintech.payment.exception.InvalidOperationException;
import com.fintech.payment.model.dto.response.TransferResponse;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Transfer;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.AccountStatus;
import com.fintech.payment.model.enums.Currency;
import com.fintech.payment.model.enums.TransactionStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.model.enums.TransferStatus;
import com.fintech.payment.model.enums.WalletStatus;
import com.fintech.payment.repository.AccountRepository;
import com.fintech.payment.repository.TransferRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.impl.TransferServiceImpl;
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
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private TransferServiceImpl transferService;


    private Account sourceAccount;
    private Account destAccount;
    private Wallet sourceWallet;
    private Wallet destWallet;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();

        sourceAccount = Account.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .accountNumber("PAY0000000001")
                .passwordHash("hash1")
                .status(AccountStatus.ACTIVE)
                .currency(Currency.USD)
                .build();
        sourceAccount.setId(1L);

        destAccount = Account.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .accountNumber("PAY0000000002")
                .passwordHash("hash2")
                .status(AccountStatus.ACTIVE)
                .currency(Currency.USD)
                .build();
        destAccount.setId(2L);

        sourceWallet = Wallet.builder()
                .account(sourceAccount)
                .balance(new BigDecimal("5000.0000"))
                .frozenAmount(BigDecimal.ZERO)
                .currency(Currency.USD)
                .status(WalletStatus.ACTIVE)
                .build();
        sourceWallet.setId(1L);
        sourceWallet.setCreatedAt(LocalDateTime.now());

        destWallet = Wallet.builder()
                .account(destAccount)
                .balance(new BigDecimal("2000.0000"))
                .frozenAmount(BigDecimal.ZERO)
                .currency(Currency.USD)
                .status(WalletStatus.ACTIVE)
                .build();
        destWallet.setId(2L);
        destWallet.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("initiateTransfer - success: should debit source and credit destination")
    void initiateTransfer_success() {
        BigDecimal transferAmount = new BigDecimal("1000.00");

        when(transferRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("PAY0000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("PAY0000000002")).thenReturn(Optional.of(destAccount));
        when(walletRepository.findByAccountId(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByAccountId(2L)).thenReturn(Optional.of(destWallet));
        when(walletRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(destWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction outTxn = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .wallet(sourceWallet)
                .type(TransactionType.TRANSFER_OUT)
                .amount(transferAmount)
                .status(TransactionStatus.COMPLETED)
                .build();
        Transaction inTxn = Transaction.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .wallet(destWallet)
                .type(TransactionType.TRANSFER_IN)
                .amount(transferAmount)
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionService.createTransaction(eq(1L), eq(TransactionType.TRANSFER_OUT),
                eq(transferAmount), any(), any(), anyString(), anyString(), eq(2L)))
                .thenReturn(outTxn);
        when(transactionService.createTransaction(eq(2L), eq(TransactionType.TRANSFER_IN),
                eq(transferAmount), any(), any(), anyString(), anyString(), eq(1L)))
                .thenReturn(inTxn);

        Transfer savedTransfer = Transfer.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .sourceAccount(sourceAccount)
                .destinationAccount(destAccount)
                .amount(transferAmount)
                .currency(Currency.USD)
                .status(TransferStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();
        savedTransfer.setId(1L);
        savedTransfer.setCreatedAt(LocalDateTime.now());

        when(transferRepository.save(any(Transfer.class))).thenReturn(savedTransfer);

        TransferResponse response = transferService.initiateTransfer(
                "PAY0000000001", "PAY0000000002", transferAmount, idempotencyKey, "Test transfer");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo(transferAmount);
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    @DisplayName("initiateTransfer - same account: should throw InvalidOperationException")
    void initiateTransfer_sameAccount_throwsException() {
        when(transferRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                transferService.initiateTransfer(
                        "PAY0000000001", "PAY0000000001",
                        new BigDecimal("1000.00"), idempotencyKey, "Self transfer"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("same account");

        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("initiateTransfer - insufficient funds: should throw InsufficientBalanceException")
    void initiateTransfer_insufficientFunds_throwsException() {
        BigDecimal largeAmount = new BigDecimal("99999.00"); // More than source balance (5000)

        when(transferRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("PAY0000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("PAY0000000002")).thenReturn(Optional.of(destAccount));
        when(walletRepository.findByAccountId(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByAccountId(2L)).thenReturn(Optional.of(destWallet));
        when(walletRepository.findByIdWithPessimisticLock(1L)).thenReturn(Optional.of(sourceWallet));
        when(walletRepository.findByIdWithPessimisticLock(2L)).thenReturn(Optional.of(destWallet));

        assertThatThrownBy(() ->
                transferService.initiateTransfer(
                        "PAY0000000001", "PAY0000000002",
                        largeAmount, idempotencyKey, "Large transfer"))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("initiateTransfer - idempotent: duplicate key returns existing transfer")
    void initiateTransfer_idempotent() {
        Transfer existingTransfer = Transfer.builder()
                .referenceNumber(UUID.randomUUID().toString())
                .sourceAccount(sourceAccount)
                .destinationAccount(destAccount)
                .amount(new BigDecimal("1000.00"))
                .currency(Currency.USD)
                .status(TransferStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();
        existingTransfer.setId(1L);
        existingTransfer.setCreatedAt(LocalDateTime.now());

        when(transferRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTransfer));

        TransferResponse response = transferService.initiateTransfer(
                "PAY0000000001", "PAY0000000002",
                new BigDecimal("1000.00"), idempotencyKey, "Duplicate transfer");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    /**
     * Safely parses an integer from a string value.
     * @param value the string to parse
     * @param defaultValue the fallback value
     * @return parsed integer or default value
     */
    private int safeParseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
