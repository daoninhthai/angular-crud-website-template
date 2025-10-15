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
public class TransferCompletedEvent {

    private String transferRef;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private String status;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
