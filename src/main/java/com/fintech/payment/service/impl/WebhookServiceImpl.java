package com.fintech.payment.service.impl;

import com.fintech.payment.entity.WebhookEvent;
import com.fintech.payment.enums.WebhookStatus;
import com.fintech.payment.repository.WebhookEventRepository;
import com.fintech.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";
    private static final String EVENT_TYPE_HEADER = "X-Webhook-Event";

    /**
     * Exponential backoff intervals in minutes: 1, 5, 15, 60, 240
     */
    private static final int[] RETRY_BACKOFF_MINUTES = {1, 5, 15, 60, 240};

    private final WebhookEventRepository webhookEventRepository;
    private final RestTemplate restTemplate;

    @Value("${webhook.secret.default:default-webhook-secret-key-change-in-production}")
    private String defaultWebhookSecret;

    @Override
    @Transactional
    public void sendWebhook(String eventType, String payload, String targetUrl) {
        log.info("Sending webhook: eventType={}, targetUrl={}", eventType, targetUrl);

        // Generate HMAC-SHA256 signature
        String signature = computeHmacSha256(payload, defaultWebhookSecret);

        // Persist webhook event before sending
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .targetUrl(targetUrl)
                .status(WebhookStatus.PENDING)
                .signature(signature)
                .secretKey(defaultWebhookSecret)
                .retryCount(0)
                .maxRetries(RETRY_BACKOFF_MINUTES.length)
                .build();

        webhookEvent = webhookEventRepository.save(webhookEvent);

        // Attempt delivery
        deliverWebhook(webhookEvent);
    }

    @Override
    @Scheduled(fixedDelayString = "${webhook.retry.interval-ms:60000}")
    @Transactional
    public void processWebhookRetries() {
        List<WebhookStatus> retryableStatuses = Arrays.asList(
                WebhookStatus.FAILED, WebhookStatus.RETRYING);

        List<WebhookEvent> retryableWebhooks = webhookEventRepository.findRetryableWebhooks(
                retryableStatuses, LocalDateTime.now());

        if (!retryableWebhooks.isEmpty()) {
            log.info("Processing {} webhook retries", retryableWebhooks.size());
        }

        for (WebhookEvent webhook : retryableWebhooks) {
            try {
                webhook.setStatus(WebhookStatus.RETRYING);
                webhook.setRetryCount(webhook.getRetryCount() + 1);
                webhookEventRepository.save(webhook);

                deliverWebhook(webhook);
            } catch (Exception e) {
                log.error("Error retrying webhook id={}: {}", webhook.getId(), e.getMessage());
            }
        }
    }

    @Override
    public boolean verifySignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }

        String expectedSignature = computeHmacSha256(payload, secret);

        // Constant-time comparison to prevent timing attacks
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = signature.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    /**
     * Attempts to deliver a webhook via HTTP POST.
     * Updates the webhook event status based on the response.
     */
    private void deliverWebhook(WebhookEvent webhookEvent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SIGNATURE_HEADER, webhookEvent.getSignature());
            headers.set(TIMESTAMP_HEADER, String.valueOf(System.currentTimeMillis()));
            headers.set(EVENT_TYPE_HEADER, webhookEvent.getEventType());

            HttpEntity<String> request = new HttpEntity<>(webhookEvent.getPayload(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webhookEvent.getTargetUrl(),
                    HttpMethod.POST,
                    request,
                    String.class
            );

            webhookEvent.setHttpStatusCode(response.getStatusCodeValue());
            webhookEvent.setResponseBody(truncateResponse(response.getBody()));
            webhookEvent.setLastAttemptedAt(LocalDateTime.now());

            if (response.getStatusCode().is2xxSuccessful()) {
                webhookEvent.setStatus(WebhookStatus.DELIVERED);
                log.info("Webhook delivered successfully: id={}, eventType={}",
                        webhookEvent.getId(), webhookEvent.getEventType());
            } else {
                handleDeliveryFailure(webhookEvent, "HTTP " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            webhookEvent.setLastAttemptedAt(LocalDateTime.now());
            handleDeliveryFailure(webhookEvent, e.getMessage());
        }

        webhookEventRepository.save(webhookEvent);
    }

    /**
     * Handles webhook delivery failure. Schedules next retry with
     * exponential backoff or marks as EXHAUSTED if max retries exceeded.
     */
    private void handleDeliveryFailure(WebhookEvent webhookEvent, String reason) {
        int currentRetry = webhookEvent.getRetryCount();
        int maxRetries = webhookEvent.getMaxRetries();

        if (currentRetry >= maxRetries) {
            webhookEvent.setStatus(WebhookStatus.EXHAUSTED);
            webhookEvent.setResponseBody("Exhausted after " + maxRetries + " retries. Last error: " + reason);
            log.warn("Webhook exhausted: id={}, eventType={}, retries={}",
                    webhookEvent.getId(), webhookEvent.getEventType(), currentRetry);
        } else {
            webhookEvent.setStatus(WebhookStatus.FAILED);
            int backoffIndex = Math.min(currentRetry, RETRY_BACKOFF_MINUTES.length - 1);
            int backoffMinutes = RETRY_BACKOFF_MINUTES[backoffIndex];
            webhookEvent.setNextRetryAt(LocalDateTime.now().plusMinutes(backoffMinutes));

            log.info("Webhook delivery failed: id={}, retry={}/{}, nextRetryIn={}min, reason={}",
                    webhookEvent.getId(), currentRetry, maxRetries, backoffMinutes, reason);
        }
    }

    /**
     * Computes HMAC-SHA256 signature of the payload using the provided secret.
     */
    private String computeHmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256 signature", e);
        }
    }

    /**
     * Truncates response body to prevent storing excessively large responses.
     */
    private String truncateResponse(String response) {
        if (response == null) {
            return null;
        }
        int maxLength = 2000;
        return response.length() > maxLength ? response.substring(0, maxLength) + "..." : response;
    }
}
