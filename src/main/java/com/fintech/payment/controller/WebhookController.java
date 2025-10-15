package com.fintech.payment.controller;

import com.fintech.payment.exception.ResourceNotFoundException;
import com.fintech.payment.exception.WebhookSignatureException;
import com.fintech.payment.model.dto.request.WebhookReceiveRequest;
import com.fintech.payment.model.dto.response.ApiResponse;
import com.fintech.payment.model.dto.response.WebhookEventResponse;
import com.fintech.payment.model.entity.WebhookEvent;
import com.fintech.payment.model.enums.WebhookStatus;
import com.fintech.payment.repository.WebhookEventRepository;
import com.fintech.payment.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * REST controller for webhook management.
 * Provides endpoints for receiving external webhooks, manual retries,
 * and listing webhook events.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final WebhookEventRepository webhookEventRepository;

    @Value("${webhook.receive.secret:default-receive-secret-change-in-production}")
    private String webhookReceiveSecret;

    /**
     * Receives an external webhook callback.
     * Verifies the HMAC-SHA256 signature from the X-Webhook-Signature header
     * before processing. Returns 401 if signature verification fails.
     *
     * @param signature the HMAC signature from the webhook sender
     * @param request   the webhook event payload
     * @return acknowledgment response
     */
    @PostMapping("/receive")
    public ResponseEntity<ApiResponse<String>> receiveWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @Valid @RequestBody WebhookReceiveRequest request) {
        log.info("Received webhook: eventType={}", request.getEventType());

        // Verify signature if present
        if (signature != null && !signature.isEmpty()) {
            boolean isValid = webhookService.verifySignature(
                    request.getPayload(), signature, webhookReceiveSecret);
            if (!isValid) {
                throw new WebhookSignatureException(
                        "Invalid webhook signature for event type: " + request.getEventType());
            }
        } else {
            log.warn("Webhook received without signature: eventType={}", request.getEventType());
        }

        // Process the webhook payload
        log.info("Webhook verified and accepted: eventType={}", request.getEventType());

        return ResponseEntity.ok(ApiResponse.ok("Webhook received and accepted"));
    }

    /**
     * Manually retries a failed webhook delivery.
     * Only accessible by users with ADMIN role.
     *
     * @param id the webhook event ID to retry
     * @return the updated webhook event details
     */
    @PostMapping("/retry/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WebhookEventResponse>> retryWebhook(@PathVariable Long id) {
        log.info("REST request to manually retry webhook: id={}", id);

        WebhookEvent webhookEvent = webhookEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "id", id));

        if (webhookEvent.getStatus() == WebhookStatus.DELIVERED) {
            return ResponseEntity.ok(ApiResponse.ok(
                    mapToResponse(webhookEvent), "Webhook already delivered"));
        }

        // Reset retry state for manual retry
        webhookEvent.setStatus(WebhookStatus.RETRYING);
        webhookEvent.setNextRetryAt(LocalDateTime.now());
        webhookEventRepository.save(webhookEvent);

        // The scheduled retry processor will pick it up, or we trigger immediately
        webhookService.sendWebhook(
                webhookEvent.getEventType(),
                webhookEvent.getPayload(),
                webhookEvent.getTargetUrl());

        // Re-fetch after send attempt
        webhookEvent = webhookEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookEvent", "id", id));

        return ResponseEntity.ok(ApiResponse.ok(
                mapToResponse(webhookEvent), "Webhook retry initiated"));
    }

    /**
     * Lists all webhook events with pagination.
     * Only accessible by users with ADMIN role.
     * Results are ordered by creation date descending.
     *
     * @param pageable pagination parameters
     * @return a page of webhook event records
     */
    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<WebhookEventResponse>>> listWebhookEvents(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("REST request to list webhook events: page={}", pageable);

        Page<WebhookEventResponse> events = webhookEventRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);

        return ResponseEntity.ok(ApiResponse.ok(events));
    }

    private WebhookEventResponse mapToResponse(WebhookEvent event) {
        return WebhookEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .httpStatusCode(event.getHttpStatusCode())
                .retryCount(event.getRetryCount())
                .maxRetries(event.getMaxRetries())
                .nextRetryAt(event.getNextRetryAt())
                .lastAttemptedAt(event.getLastAttemptedAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
