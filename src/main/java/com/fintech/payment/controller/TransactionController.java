package com.fintech.payment.controller;

import com.fintech.payment.model.dto.response.ApiResponse;
import com.fintech.payment.model.dto.response.TransactionResponse;
import com.fintech.payment.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for transaction queries.
 * Provides read-only endpoints for retrieving transaction records.
 * Transactions are created implicitly by wallet operations, transfers, and payments.
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Retrieves a transaction by its unique reference number.
     *
     * @param ref the transaction reference number (UUID format)
     * @return the transaction details
     */
    @GetMapping("/{ref}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionByRef(
            @PathVariable String ref) {
        log.info("REST request to get transaction by ref: {}", ref);
        TransactionResponse transaction = transactionService.getTransactionByRef(ref);
        return ResponseEntity.ok(ApiResponse.ok(transaction));
    }

    /**
     * Retrieves paginated transactions for a specific wallet.
     * Results are ordered by creation date descending (most recent first).
     *
     * @param walletId the wallet ID
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of transaction records
     */
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionsByWallet(
            @PathVariable Long walletId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to get transactions for wallet: {}, page: {}", walletId, pageable);
        Page<TransactionResponse> transactions = transactionService.getTransactionsByWallet(walletId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(transactions));
    }

    /**
     * Retrieves transaction history for a wallet within a date range.
     * Both startDate and endDate are inclusive.
     *
     * @param walletId  the wallet ID
     * @param startDate the start of the date range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @param endDate   the end of the date range (ISO format: yyyy-MM-ddTHH:mm:ss)
     * @return list of transactions within the specified date range
     */
    @GetMapping("/wallet/{walletId}/history")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionHistory(
            @PathVariable Long walletId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("REST request to get transaction history: walletId={}, from={}, to={}",
                walletId, startDate, endDate);
        List<TransactionResponse> transactions = transactionService.getTransactionHistory(
                walletId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(transactions));
    }
}
