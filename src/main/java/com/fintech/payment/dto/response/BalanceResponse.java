package com.fintech.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {

    private Long walletId;
    private BigDecimal totalBalance;
    private BigDecimal availableBalance;
    private BigDecimal frozenAmount;
    private String currency;
}
