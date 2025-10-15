package com.fintech.payment.entity;

import com.fintech.payment.enums.WebhookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events", indexes = {
        @Index(name = "idx_webhook_status_next_retry", columnList = "status, nextRetryAt"),
        @Index(name = "idx_webhook_target_url", columnList = "targetUrl"),
        @Index(name = "idx_webhook_event_type", columnList = "eventType"),
        @Index(name = "idx_webhook_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Event type is required")
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @NotNull(message = "Payload is required")
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotBlank(message = "Target URL is required")
    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "signature", length = 255)
    private String signature;

    @NotNull(message = "Webhook status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WebhookStatus status = WebhookStatus.PENDING;

    @PositiveOrZero(message = "Retry count must be zero or positive")
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Increments the retry count and calculates the next retry time
     * using an exponential backoff strategy.
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastAttemptAt = LocalDateTime.now();

        if (this.retryCount >= this.maxRetries) {
            this.status = WebhookStatus.EXHAUSTED;
            this.nextRetryAt = null;
        } else {
            // Exponential backoff: 1min, 2min, 4min, 8min, 16min...
            long backoffMinutes = (long) Math.pow(2, this.retryCount - 1);
            this.nextRetryAt = LocalDateTime.now().plusMinutes(backoffMinutes);
            this.status = WebhookStatus.FAILED;
        }
    }
}
