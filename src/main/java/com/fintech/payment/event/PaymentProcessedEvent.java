package com.fintech.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private String paymentRef;
    private Long walletId;
    private BigDecimal amount;
    private String merchantName;
    private String status;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
