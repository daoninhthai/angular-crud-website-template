package com.fintech.payment.dto.response;

import com.fintech.payment.enums.TransactionStatus;
import com.fintech.payment.enums.TransactionType;
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
public class TransactionResponse {

    private Long id;
    private String transactionRef;
    private String referenceNumber;
    private Long walletId;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;

    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private TransactionStatus status;
    private Long counterpartyWalletId;
    private LocalDateTime createdAt;
}
