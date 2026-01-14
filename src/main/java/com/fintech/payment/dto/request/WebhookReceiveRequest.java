package com.fintech.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookReceiveRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Payload is required")
    private String payload;
}
