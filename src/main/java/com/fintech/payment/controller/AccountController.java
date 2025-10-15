package com.fintech.payment.controller;

import com.fintech.payment.model.dto.request.CreateAccountRequest;
import com.fintech.payment.model.dto.request.UpdateAccountRequest;
import com.fintech.payment.model.dto.response.AccountResponse;
import com.fintech.payment.model.dto.response.ApiResponse;
import com.fintech.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for account management operations.
 * Provides endpoints for creating, retrieving, updating, and deactivating accounts.
 */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Creates a new account with an associated wallet.
     *
     * @param request the account creation details
     * @return the created account with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("REST request to create account for email: {}", request.getEmail());
        AccountResponse account = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(account, "Account created successfully"));
    }

    /**
     * Retrieves an account by its internal ID.
     *
     * @param id the account ID
     * @return the account details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable Long id) {
        log.info("REST request to get account by id: {}", id);
        AccountResponse account = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.ok(account));
    }

    /**
     * Retrieves an account by its unique account number.
     *
     * @param accountNumber the account number (format: PAY + 10 digits)
     * @return the account details
     */
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(
            @PathVariable String accountNumber) {
        log.info("REST request to get account by number: {}", accountNumber);
        AccountResponse account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok(account));
    }

    /**
     * Updates an existing account's mutable fields.
     * Only non-null fields in the request body will be updated.
     *
     * @param id      the account ID to update
     * @param request the fields to update
     * @return the updated account details
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request) {
        log.info("REST request to update account id: {}", id);
        AccountResponse account = accountService.updateAccount(id, request);
        return ResponseEntity.ok(ApiResponse.ok(account, "Account updated successfully"));
    }

    /**
     * Deactivates an account by setting its status to INACTIVE.
     *
     * @param id the account ID to deactivate
     * @return the deactivated account details
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> deactivateAccount(@PathVariable Long id) {
        log.info("REST request to deactivate account id: {}", id);
        AccountResponse account = accountService.deactivateAccount(id);
        return ResponseEntity.ok(ApiResponse.ok(account, "Account deactivated successfully"));
    }
}
