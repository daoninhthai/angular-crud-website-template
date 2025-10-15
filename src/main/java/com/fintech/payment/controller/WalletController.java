package com.fintech.payment.controller;

import com.fintech.payment.model.dto.request.WalletOperationRequest;
import com.fintech.payment.model.dto.response.ApiResponse;
import com.fintech.payment.model.dto.response.BalanceResponse;
import com.fintech.payment.model.dto.response.WalletResponse;
import com.fintech.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for wallet operations.
 * Provides endpoints for wallet queries, deposits, and withdrawals.
 * Deposit and withdrawal operations require an Idempotency-Key header
 * to prevent duplicate operations from network retries.
 */
@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Retrieves the wallet associated with the given account.
     *
     * @param accountId the account ID
     * @return the wallet details including balance information
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable Long accountId) {
        log.info("REST request to get wallet for account: {}", accountId);
        WalletResponse wallet = walletService.getWalletByAccountId(accountId);
        return ResponseEntity.ok(ApiResponse.ok(wallet));
    }

    /**
     * Returns the current balance breakdown for a wallet.
     * Includes total balance, available balance, and frozen amount.
     *
     * @param accountId the account ID
     * @return balance details
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long accountId) {
        log.info("REST request to get balance for account: {}", accountId);
        BalanceResponse balance = walletService.getBalance(accountId);
        return ResponseEntity.ok(ApiResponse.ok(balance));
    }

    /**
     * Deposits funds into the wallet.
     * Requires an Idempotency-Key header to prevent duplicate deposits.
     *
     * @param accountId      the account ID
     * @param idempotencyKey unique key for idempotent processing
     * @param request        the deposit amount and optional description
     * @return the updated wallet details
     */
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(
            @PathVariable Long accountId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WalletOperationRequest request) {
        log.info("REST request to deposit: accountId={}, amount={}, idempotencyKey={}",
                accountId, request.getAmount(), idempotencyKey);
        WalletResponse wallet = walletService.deposit(
                accountId, request.getAmount(), request.getDescription(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(wallet, "Deposit successful"));
    }

    /**
     * Withdraws funds from the wallet.
     * Requires an Idempotency-Key header to prevent duplicate withdrawals.
     * Will fail if the available balance is insufficient.
     *
     * @param accountId      the account ID
     * @param idempotencyKey unique key for idempotent processing
     * @param request        the withdrawal amount and optional description
     * @return the updated wallet details
     */
    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdraw(
            @PathVariable Long accountId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WalletOperationRequest request) {
        log.info("REST request to withdraw: accountId={}, amount={}, idempotencyKey={}",
                accountId, request.getAmount(), idempotencyKey);
        WalletResponse wallet = walletService.withdraw(
                accountId, request.getAmount(), request.getDescription(), idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok(wallet, "Withdrawal successful"));
    }
}
