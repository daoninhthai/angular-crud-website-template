package com.fintech.payment.service.impl;

import com.fintech.payment.model.entity.Payment;
import com.fintech.payment.model.entity.Transaction;
import com.fintech.payment.model.entity.Transfer;
import com.fintech.payment.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String NOTIFICATION_TOPIC = "notification-events";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void sendTransactionNotification(Transaction transaction) {
        try {
            String formattedAmount = formatAmount(
                    transaction.getAmount(), transaction.getCurrency().name());

            String event = String.format(
                    "{\"notificationType\":\"TRANSACTION\"," +
                            "\"referenceNumber\":\"%s\"," +
                            "\"transactionType\":\"%s\"," +
                            "\"status\":\"%s\"," +
                            "\"walletId\":%d," +
                            "\"amount\":\"%s\"," +
                            "\"formattedAmount\":\"%s\"," +
                            "\"currency\":\"%s\"," +
                            "\"balanceBefore\":\"%s\"," +
                            "\"balanceAfter\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"timestamp\":\"%s\"}",
                    transaction.getReferenceNumber(),
                    transaction.getType(),
                    transaction.getStatus(),
                    transaction.getWallet().getId(),
                    transaction.getAmount().toPlainString(),
                    formattedAmount,
                    transaction.getCurrency(),
                    transaction.getBalanceBefore().toPlainString(),
                    transaction.getBalanceAfter().toPlainString(),
                    escapeJson(transaction.getDescription()),
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER)
            );

            kafkaTemplate.send(NOTIFICATION_TOPIC, transaction.getReferenceNumber(), event);
            log.debug("Transaction notification sent: ref={}, type={}",
                    transaction.getReferenceNumber(), transaction.getType());

        } catch (Exception e) {
            log.error("Failed to send transaction notification: ref={}, error={}",
                    transaction.getReferenceNumber(), e.getMessage(), e);
        }
    }

    @Override
    public void sendTransferNotification(Transfer transfer) {
        try {
            String formattedAmount = formatAmount(
                    transfer.getAmount(), transfer.getCurrency().name());

            // Notification for the sender
            String senderEvent = String.format(
                    "{\"notificationType\":\"TRANSFER_SENT\"," +
                            "\"referenceNumber\":\"%s\"," +
                            "\"accountId\":%d," +
                            "\"accountNumber\":\"%s\"," +
                            "\"counterpartyAccountNumber\":\"%s\"," +
                            "\"amount\":\"%s\"," +
                            "\"formattedAmount\":\"%s\"," +
                            "\"currency\":\"%s\"," +
                            "\"status\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"timestamp\":\"%s\"}",
                    transfer.getReferenceNumber(),
                    transfer.getSourceAccount().getId(),
                    transfer.getSourceAccount().getAccountNumber(),
                    transfer.getDestinationAccount().getAccountNumber(),
                    transfer.getAmount().toPlainString(),
                    formattedAmount,
                    transfer.getCurrency(),
                    transfer.getStatus(),
                    escapeJson(transfer.getDescription()),
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER)
            );

            kafkaTemplate.send(NOTIFICATION_TOPIC, transfer.getReferenceNumber() + "_SENDER", senderEvent);

            // Notification for the receiver
            String receiverEvent = String.format(
                    "{\"notificationType\":\"TRANSFER_RECEIVED\"," +
                            "\"referenceNumber\":\"%s\"," +
                            "\"accountId\":%d," +
                            "\"accountNumber\":\"%s\"," +
                            "\"counterpartyAccountNumber\":\"%s\"," +
                            "\"amount\":\"%s\"," +
                            "\"formattedAmount\":\"%s\"," +
                            "\"currency\":\"%s\"," +
                            "\"status\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"timestamp\":\"%s\"}",
                    transfer.getReferenceNumber(),
                    transfer.getDestinationAccount().getId(),
                    transfer.getDestinationAccount().getAccountNumber(),
                    transfer.getSourceAccount().getAccountNumber(),
                    transfer.getAmount().toPlainString(),
                    formattedAmount,
                    transfer.getCurrency(),
                    transfer.getStatus(),
                    escapeJson(transfer.getDescription()),
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER)
            );

            kafkaTemplate.send(NOTIFICATION_TOPIC, transfer.getReferenceNumber() + "_RECEIVER", receiverEvent);

            log.debug("Transfer notifications sent: ref={}, sender={}, receiver={}",
                    transfer.getReferenceNumber(),
                    transfer.getSourceAccount().getAccountNumber(),
                    transfer.getDestinationAccount().getAccountNumber());

        } catch (Exception e) {
            log.error("Failed to send transfer notification: ref={}, error={}",
                    transfer.getReferenceNumber(), e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentNotification(Payment payment) {
        try {
            String formattedAmount = formatAmount(
                    payment.getAmount(), payment.getCurrency().name());

            String event = String.format(
                    "{\"notificationType\":\"PAYMENT\"," +
                            "\"referenceNumber\":\"%s\"," +
                            "\"walletId\":%d," +
                            "\"amount\":\"%s\"," +
                            "\"formattedAmount\":\"%s\"," +
                            "\"refundedAmount\":\"%s\"," +
                            "\"currency\":\"%s\"," +
                            "\"status\":\"%s\"," +
                            "\"merchantName\":\"%s\"," +
                            "\"description\":\"%s\"," +
                            "\"timestamp\":\"%s\"}",
                    payment.getReferenceNumber(),
                    payment.getWallet().getId(),
                    payment.getAmount().toPlainString(),
                    formattedAmount,
                    payment.getRefundedAmount().toPlainString(),
                    payment.getCurrency(),
                    payment.getStatus(),
                    escapeJson(payment.getMerchantName()),
                    escapeJson(payment.getDescription()),
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER)
            );

            kafkaTemplate.send(NOTIFICATION_TOPIC, payment.getReferenceNumber(), event);
            log.debug("Payment notification sent: ref={}, status={}",
                    payment.getReferenceNumber(), payment.getStatus());

        } catch (Exception e) {
            log.error("Failed to send payment notification: ref={}, error={}",
                    payment.getReferenceNumber(), e.getMessage(), e);
        }
    }

    /**
     * Formats a monetary amount using the appropriate currency format.
     * Example: 1234.56 USD -> "$1,234.56"
     */
    private String formatAmount(BigDecimal amount, String currencyCode) {
        try {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setCurrency(Currency.getInstance(currencyCode));
            return formatter.format(amount);
        } catch (Exception e) {
            return amount.toPlainString() + " " + currencyCode;
        }
    }

    /**
     * Escapes special characters for JSON string values.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
