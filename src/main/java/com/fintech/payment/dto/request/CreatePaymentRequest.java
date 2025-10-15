package com.fintech.payment.dto.request;

import com.fintech.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "VND";

    private PaymentMethod method;

    @NotBlank(message = "Merchant name is required")
    @Size(max = 255, message = "Merchant name must not exceed 255 characters")
    private String merchantName;

    @Size(max = 100, message = "Merchant reference must not exceed 100 characters")
    private String merchantRef;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Webhook URL must not exceed 500 characters")
    private String webhookUrl;
}
