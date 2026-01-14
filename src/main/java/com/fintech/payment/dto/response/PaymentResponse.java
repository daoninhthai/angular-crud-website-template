package com.fintech.payment.dto.response;

import com.fintech.payment.enums.PaymentMethod;
import com.fintech.payment.enums.PaymentStatus;
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
public class PaymentResponse {

    private Long id;
    private String paymentRef;
    private String referenceNumber;
    private Long walletId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String merchantName;
    private String merchantReference;
    private PaymentStatus status;
    private BigDecimal refundedAmount;
    private String description;
    private String webhookUrl;
    private String failureReason;
    private String transactionRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
