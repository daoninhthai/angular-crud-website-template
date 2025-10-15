package com.fintech.payment.service.impl;

import com.fintech.payment.exception.DuplicateResourceException;
import com.fintech.payment.exception.InvalidOperationException;
import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.request.CreateAccountRequest;
import com.fintech.payment.model.dto.request.UpdateAccountRequest;
import com.fintech.payment.model.dto.response.AccountResponse;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.AccountStatus;
import com.fintech.payment.model.enums.Currency;
import com.fintech.payment.model.enums.WalletStatus;
import com.fintech.payment.repository.AccountRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String ACCOUNT_PREFIX = "PAY";
    private static final int ACCOUNT_NUMBER_DIGITS = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for email: {}", request.getEmail());

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Account with email " + request.getEmail() + " already exists");
        }

        String accountNumber = generateUniqueAccountNumber();
        Currency currency = request.getCurrency() != null ? request.getCurrency() : Currency.USD;

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .currency(currency)
                .status(AccountStatus.ACTIVE)
                .kycVerified(false)
                .build();

        account = accountRepository.save(account);

        // Create associated wallet automatically
        Wallet wallet = Wallet.builder()
                .account(account)
                .balance(BigDecimal.ZERO)
                .frozenAmount(BigDecimal.ZERO)
                .currency(currency)
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.save(wallet);

        log.info("Account created successfully: accountNumber={}, id={}", accountNumber, account.getId());
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));
        return mapToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "accountNumber", accountNumber));
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        log.info("Updating account id: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Cannot update a closed account");
        }

        if (request.getFirstName() != null) {
            account.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            account.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().toLowerCase().trim();
            if (!newEmail.equals(account.getEmail()) && accountRepository.existsByEmail(newEmail)) {
                throw new DuplicateResourceException("Account with email " + newEmail + " already exists");
            }
            account.setEmail(newEmail);
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }

        account = accountRepository.save(account);
        log.info("Account updated successfully: id={}", id);
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse deactivateAccount(Long id) {
        log.info("Deactivating account id: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", id));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Account is already closed");
        }
        if (account.getStatus() == AccountStatus.INACTIVE) {
            throw new InvalidOperationException("Account is already inactive");
        }

        account.setStatus(AccountStatus.INACTIVE);
        account = accountRepository.save(account);

        // Also freeze the associated wallet
        walletRepository.findByAccountId(id).ifPresent(wallet -> {
            wallet.setStatus(WalletStatus.FROZEN);
            walletRepository.save(wallet);
        });

        log.info("Account deactivated successfully: id={}", id);
        return mapToResponse(account);
    }

    /**
     * Generates a unique account number with format PAY + 10 random digits.
     * Retries if a collision is detected (extremely unlikely with 10^10 combinations).
     */
    private String generateUniqueAccountNumber() {
        String accountNumber;
        int maxAttempts = 10;
        int attempt = 0;

        do {
            StringBuilder sb = new StringBuilder(ACCOUNT_PREFIX);
            for (int i = 0; i < ACCOUNT_NUMBER_DIGITS; i++) {
                sb.append(SECURE_RANDOM.nextInt(10));
            }
            accountNumber = sb.toString();
            attempt++;

            if (attempt > maxAttempts) {
                throw new IllegalStateException("Failed to generate unique account number after " + maxAttempts + " attempts");
            }
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .kycVerified(account.getKycVerified())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
