package com.fintech.payment.service;

import com.fintech.payment.dto.request.CreateAccountRequest;
import com.fintech.payment.dto.request.UpdateAccountRequest;
import com.fintech.payment.dto.response.AccountResponse;

/**
 * Service interface for account management operations.
 * Handles account creation, retrieval, updates, and deactivation.
 */
public interface AccountService {

    /**
     * Creates a new account with an associated wallet.
     * Generates a unique account number in format PAY + 10 random digits.
     * Password is hashed using BCrypt before storage.
     *
     * @param request the account creation details
     * @return the created account details
     */
    AccountResponse createAccount(CreateAccountRequest request);

    /**
     * Retrieves an account by its internal ID.
     *
     * @param id the account ID
     * @return the account details
     * @throws com.fintech.payment.exception.ResourceNotFoundException if account not found
     */
    AccountResponse getAccountById(Long id);

    /**
     * Retrieves an account by its unique account number.
     *
     * @param accountNumber the account number (e.g., PAY1234567890)
     * @return the account details
     * @throws com.fintech.payment.exception.ResourceNotFoundException if account not found
     */
    AccountResponse getAccountByNumber(String accountNumber);

    /**
     * Updates an existing account's mutable fields (name, email, phone).
     *
     * @param id      the account ID to update
     * @param request the fields to update (nulls are ignored)
     * @return the updated account details
     */
    AccountResponse updateAccount(Long id, UpdateAccountRequest request);

    /**
     * Deactivates an account by setting its status to INACTIVE.
     * Associated wallet operations will be blocked for inactive accounts.
     *
     * @param id the account ID to deactivate
     * @return the deactivated account details
     */
    AccountResponse deactivateAccount(Long id);
}
