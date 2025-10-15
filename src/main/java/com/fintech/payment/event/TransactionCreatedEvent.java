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
public class TransactionCreatedEvent {

    private String transactionRef;
    private Long walletId;
    private String type;
    private BigDecimal amount;
    private String status;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
