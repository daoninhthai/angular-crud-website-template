package com.fintech.payment.dto.response;

import com.fintech.payment.enums.PaymentMethod;
import com.fintech.payment.model.enums.PaymentStatus;
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
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String merchantName;
    private PaymentStatus status;
    private BigDecimal refundedAmount;
    private String description;
    private LocalDateTime createdAt;
}
