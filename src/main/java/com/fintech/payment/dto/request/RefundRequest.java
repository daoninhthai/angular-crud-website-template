package com.fintech.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "Refund amount is required")
    @Positive(message = "Refund amount must be positive")
    private BigDecimal amount;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
