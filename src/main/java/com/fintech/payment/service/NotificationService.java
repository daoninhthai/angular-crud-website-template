package com.fintech.payment.service;

import com.fintech.payment.entity.Payment;
import com.fintech.payment.entity.Transaction;
import com.fintech.payment.entity.Transfer;

/**
 * Service interface for sending notification events via Kafka.
 * Notification events are consumed by downstream services for
 * push notifications, emails, SMS, etc.
 */
public interface NotificationService {

    /**
     * Sends a notification event for a completed transaction
     * (deposit, withdrawal, etc.) to the Kafka notification topic.
     *
     * @param transaction the transaction that triggered the notification
     */
    void sendTransactionNotification(Transaction transaction);

    /**
     * Sends a notification event for a completed transfer to the
     * Kafka notification topic. Notifies both sender and receiver.
     *
     * @param transfer the transfer that triggered the notification
     */
    void sendTransferNotification(Transfer transfer);

    /**
     * Sends a notification event for a payment status change
     * (completed, failed, refunded) to the Kafka notification topic.
     *
     * @param payment the payment that triggered the notification
     */
    void sendPaymentNotification(Payment payment);
}
