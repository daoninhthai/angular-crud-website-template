package com.fintech.payment.service.impl;

import com.fintech.payment.exception.InvalidOperationException;
import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.model.dto.request.CreatePaymentRequest;
import com.fintech.payment.model.dto.request.RefundRequest;
import com.fintech.payment.model.dto.response.PaymentResponse;
import com.fintech.payment.model.entity.Payment;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Wallet;
import com.fintech.payment.model.enums.PaymentStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.repository.PaymentRepository;
import com.fintech.payment.repository.WalletRepository;
import com.fintech.payment.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final WebhookService webhookService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey) {
        log.info("Creating payment: walletId={}, amount={}, merchant={}, idempotencyKey={}",
                request.getWalletId(), request.getAmount(), request.getMerchantName(), idempotencyKey);

        // Idempotency check
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Duplicate payment detected for idempotency key: {}", idempotencyKey);
            return mapToResponse(existing.get());
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", request.getWalletId()));

        String referenceNumber = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .referenceNumber(referenceNumber)
                .wallet(wallet)
                .amount(request.getAmount())
                .refundedAmount(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .status(PaymentStatus.CREATED)
                .merchantName(request.getMerchantName())
                .merchantReference(request.getMerchantReference())
                .description(request.getDescription())
                .idempotencyKey(idempotencyKey)
                .webhookUrl(request.getWebhookUrl())
                .metadata(request.getMetadata())
                .build();

        payment = paymentRepository.save(payment);

        log.info("Payment created: ref={}, walletId={}, amount={}",
                referenceNumber, request.getWalletId(), request.getAmount());

        return mapToResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(String referenceNumber) {
        log.info("Processing payment: ref={}", referenceNumber);

        Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "referenceNumber", referenceNumber));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new InvalidOperationException(
                    "Payment cannot be processed. Current status: " + payment.getStatus());
        }

        Wallet wallet = payment.getWallet();
        PaymentStatus previousStatus = payment.getStatus();

        try {
            // Step 1: Freeze the payment amount in the wallet
            walletService.freezeAmount(wallet.getId(), payment.getAmount());
            payment.setStatus(PaymentStatus.AMOUNT_HELD);
            paymentRepository.save(payment);

            // Step 2: Simulate external payment gateway processing
            boolean paymentSuccessful = processWithPaymentGateway(payment);

            if (paymentSuccessful) {
                // Step 3a: Success - unfreeze and deduct the amount
                walletService.unfreezeAmount(wallet.getId(), payment.getAmount());

                // Re-fetch wallet with lock for balance update
                Wallet lockedWallet = walletRepository.findByIdWithPessimisticLock(wallet.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", wallet.getId()));

                BigDecimal balanceBefore = lockedWallet.getBalance();
                BigDecimal balanceAfter = balanceBefore.subtract(payment.getAmount());
                lockedWallet.setBalance(balanceAfter);
                walletRepository.save(lockedWallet);

                // Create payment transaction
                Transaction transaction = transactionService.createTransaction(
                        wallet.getId(),
                        TransactionType.PAYMENT,
                        payment.getAmount(),
                        balanceBefore,
                        balanceAfter,
                        "Payment to " + payment.getMerchantName() + " - " + referenceNumber,
                        payment.getIdempotencyKey() + "_TXN",
                        null
                );

                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionRef(transaction.getReferenceNumber());
            } else {
                // Step 3b: Failure - unfreeze the amount
                walletService.unfreezeAmount(wallet.getId(), payment.getAmount());
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Payment gateway rejected the transaction");
            }

            payment = paymentRepository.save(payment);

            // Send webhook notification on status change
            sendPaymentWebhook(payment, previousStatus);

            // Send notification
            notificationService.sendPaymentNotification(payment);

            log.info("Payment processed: ref={}, status={}", referenceNumber, payment.getStatus());
            return mapToResponse(payment);

        } catch (Exception e) {
            log.error("Payment processing failed for ref={}: {}", referenceNumber, e.getMessage(), e);

            // Attempt to unfreeze if we froze the amount
            if (payment.getStatus() == PaymentStatus.AMOUNT_HELD) {
                try {
                    walletService.unfreezeAmount(wallet.getId(), payment.getAmount());
                } catch (Exception unfreezeEx) {
                    log.error("Failed to unfreeze amount for payment ref={}: {}",
                            referenceNumber, unfreezeEx.getMessage());
                }
            }

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Processing error: " + e.getMessage());
            payment = paymentRepository.save(payment);

            sendPaymentWebhook(payment, previousStatus);
            return mapToResponse(payment);
        }
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(String referenceNumber, RefundRequest request) {
        log.info("Processing refund: paymentRef={}, amount={}", referenceNumber, request.getAmount());

        Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "referenceNumber", referenceNumber));

        // Validate payment is in a refundable state
        if (payment.getStatus() != PaymentStatus.COMPLETED &&
                payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidOperationException(
                    "Payment cannot be refunded. Current status: " + payment.getStatus());
        }

        // Validate refund amount does not exceed (paid - already refunded)
        BigDecimal maxRefundable = payment.getAmount().subtract(payment.getRefundedAmount());
        if (request.getAmount().compareTo(maxRefundable) > 0) {
            throw new InvalidOperationException(
                    "Refund amount " + request.getAmount() +
                            " exceeds maximum refundable amount " + maxRefundable);
        }

        Wallet wallet = payment.getWallet();

        // Credit the refund amount back to the wallet
        Wallet lockedWallet = walletRepository.findByIdWithPessimisticLock(wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", wallet.getId()));

        BigDecimal balanceBefore = lockedWallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.getAmount());
        lockedWallet.setBalance(balanceAfter);
        walletRepository.save(lockedWallet);

        // Create refund transaction
        String refundIdempotencyKey = payment.getIdempotencyKey() + "_REFUND_" + System.currentTimeMillis();
        transactionService.createTransaction(
                wallet.getId(),
                TransactionType.REFUND,
                request.getAmount(),
                balanceBefore,
                balanceAfter,
                "Refund for payment " + referenceNumber +
                        (request.getReason() != null ? " - " + request.getReason() : ""),
                refundIdempotencyKey,
                null
        );

        // Update payment refunded amount and status
        PaymentStatus previousStatus = payment.getStatus();
        payment.setRefundedAmount(payment.getRefundedAmount().add(request.getAmount()));

        if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        payment = paymentRepository.save(payment);

        // Send webhook and notification
        sendPaymentWebhook(payment, previousStatus);
        notificationService.sendPaymentNotification(payment);

        log.info("Refund processed: paymentRef={}, refundAmount={}, totalRefunded={}, newStatus={}",
                referenceNumber, request.getAmount(), payment.getRefundedAmount(), payment.getStatus());

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByRef(String referenceNumber) {
        Payment payment = paymentRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "referenceNumber", referenceNumber));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByWallet(Long walletId, Pageable pageable) {
        if (!walletRepository.existsById(walletId)) {
            throw new ResourceNotFoundException("Wallet", "id", walletId);
        }

        return paymentRepository.findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Simulates external payment gateway processing.
     * In a real implementation, this would call an external API.
     */
    private boolean processWithPaymentGateway(Payment payment) {
        // Simulate gateway processing
        // In production, this would integrate with Stripe, PayPal, etc.
        log.info("Processing payment with gateway: ref={}, merchant={}, amount={}",
                payment.getReferenceNumber(), payment.getMerchantName(), payment.getAmount());

        // For now, always return success. In production, this would make
        // an HTTP call to the payment gateway and handle the response.
        return true;
    }

    private void sendPaymentWebhook(Payment payment, PaymentStatus previousStatus) {
        if (payment.getWebhookUrl() != null && !payment.getWebhookUrl().isEmpty()
                && payment.getStatus() != previousStatus) {
            try {
                String payload = String.format(
                        "{\"event\":\"payment.status_changed\",\"referenceNumber\":\"%s\"," +
                                "\"previousStatus\":\"%s\",\"currentStatus\":\"%s\"," +
                                "\"amount\":\"%s\",\"currency\":\"%s\",\"merchantName\":\"%s\"," +
                                "\"timestamp\":\"%s\"}",
                        payment.getReferenceNumber(),
                        previousStatus,
                        payment.getStatus(),
                        payment.getAmount().toPlainString(),
                        payment.getCurrency(),
                        payment.getMerchantName(),
                        java.time.LocalDateTime.now()
                );
                webhookService.sendWebhook("payment.status_changed", payload, payment.getWebhookUrl());
            } catch (Exception e) {
                log.error("Failed to send payment webhook for ref={}: {}",
                        payment.getReferenceNumber(), e.getMessage());
            }
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .referenceNumber(payment.getReferenceNumber())
                .walletId(payment.getWallet().getId())
                .amount(payment.getAmount())
                .refundedAmount(payment.getRefundedAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .merchantName(payment.getMerchantName())
                .merchantReference(payment.getMerchantReference())
                .description(payment.getDescription())
                .webhookUrl(payment.getWebhookUrl())
                .failureReason(payment.getFailureReason())
                .transactionRef(payment.getTransactionRef())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
