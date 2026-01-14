package com.fintech.payment.dto.response;

import com.fintech.payment.enums.WebhookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventResponse {

    private Long id;
    private String eventType;
    private WebhookStatus status;
    private Integer httpStatusCode;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRetryAt;
    private LocalDateTime lastAttemptedAt;
    private LocalDateTime createdAt;
}
