package com.fintech.payment.controller;

import com.fintech.payment.model.dto.request.TransferRequest;
import com.fintech.payment.model.dto.response.ApiResponse;
import com.fintech.payment.model.dto.response.TransferResponse;
import com.fintech.payment.service.TransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for account-to-account transfers.
 * Transfer initiation requires an Idempotency-Key header.
 */
@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Initiates a transfer between two accounts.
     * The operation is atomic: either both accounts are updated or neither is.
     * Requires an Idempotency-Key header to prevent duplicate transfers.
     *
     * @param idempotencyKey unique key for idempotent processing
     * @param request        the transfer details (source, destination, amount)
     * @return the transfer details with HTTP 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> initiateTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        log.info("REST request to initiate transfer: from={}, to={}, amount={}, idempotencyKey={}",
                request.getFromAccountNumber(), request.getToAccountNumber(),
                request.getAmount(), idempotencyKey);

        TransferResponse transfer = transferService.initiateTransfer(
                request.getFromAccountNumber(),
                request.getToAccountNumber(),
                request.getAmount(),
                idempotencyKey,
                request.getDescription());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(transfer, "Transfer completed successfully"));
    }

    /**
     * Retrieves a transfer by its unique reference number.
     *
     * @param ref the transfer reference number (UUID format)
     * @return the transfer details
     */
    @GetMapping("/{ref}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransferByRef(@PathVariable String ref) {
        log.info("REST request to get transfer by ref: {}", ref);
        TransferResponse transfer = transferService.getTransferByRef(ref);
        return ResponseEntity.ok(ApiResponse.ok(transfer));
    }

    /**
     * Retrieves paginated transfer history for an account.
     * Includes both sent and received transfers.
     *
     * @param accountId the account ID
     * @param pageable  pagination parameters
     * @return a page of transfer records
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<TransferResponse>>> getTransferHistory(
            @PathVariable Long accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to get transfer history: accountId={}, page={}", accountId, pageable);
        Page<TransferResponse> transfers = transferService.getTransferHistory(accountId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(transfers));
    }
}
