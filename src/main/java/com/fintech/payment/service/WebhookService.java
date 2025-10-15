package com.fintech.payment.service;

/**
 * Service interface for outbound webhook delivery and retry management.
 * Webhooks notify external systems about payment events with HMAC-SHA256 signed payloads.
 */
public interface WebhookService {

    /**
     * Sends a webhook event to the specified target URL.
     * Creates an HMAC-SHA256 signature of the payload and includes it in the
     * X-Webhook-Signature header. Stores the event for retry on failure.
     *
     * @param eventType the type of event (e.g., "payment.completed", "transfer.completed")
     * @param payload   the JSON payload to deliver
     * @param targetUrl the URL to POST the webhook to
     */
    void sendWebhook(String eventType, String payload, String targetUrl);

    /**
     * Processes pending webhook retries. Runs on a schedule (every 60 seconds).
     * Finds FAILED or RETRYING webhooks where nextRetryAt is in the past.
     * Applies exponential backoff: 1min, 5min, 15min, 60min, 240min.
     * Marks as EXHAUSTED after max retries are exceeded.
     */
    void processWebhookRetries();

    /**
     * Verifies the HMAC-SHA256 signature of an incoming webhook payload.
     *
     * @param payload   the raw request body
     * @param signature the signature from the X-Webhook-Signature header
     * @param secret    the shared secret used to compute the expected signature
     * @return true if the signature is valid
     */
    boolean verifySignature(String payload, String signature, String secret);
}
