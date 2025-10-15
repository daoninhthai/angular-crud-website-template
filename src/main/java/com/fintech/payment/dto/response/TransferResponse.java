package com.fintech.payment.dto.response;

import com.fintech.payment.model.enums.TransferStatus;
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
public class TransferResponse {

    private Long id;
    private String transferRef;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currency;
    private TransferStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
