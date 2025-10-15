package com.fintech.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment.model.dto.response.BalanceResponse;
import com.fintech.payment.model.dto.response.WalletResponse;
import com.fintech.payment.model.enums.Currency;
import com.fintech.payment.model.enums.WalletStatus;
import com.fintech.payment.security.CustomUserDetailsService;
import com.fintech.payment.security.JwtTokenProvider;
import com.fintech.payment.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/wallets/{accountId}/deposit - returns 200 on success")
    @WithMockUser(username = "PAY1234567890", roles = {"USER"})
    void deposit_returns200() throws Exception {
        WalletResponse walletResponse = WalletResponse.builder()
                .id(1L)
                .accountId(1L)
                .balance(new BigDecimal("1500.0000"))
                .frozenAmount(BigDecimal.ZERO)
                .availableBalance(new BigDecimal("1500.0000"))
                .currency(Currency.USD)
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(walletService.deposit(eq(1L), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(walletResponse);

        String requestBody = "{\"amount\": 500.00, \"description\": \"Test deposit\"}";

        mockMvc.perform(post("/api/wallets/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-key-123")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(1500.0000));
    }

    @Test
    @DisplayName("POST /api/wallets/{accountId}/deposit - returns 400 without Idempotency-Key")
    @WithMockUser(username = "PAY1234567890", roles = {"USER"})
    void deposit_missingIdempotencyKey_returns400() throws Exception {
        String requestBody = "{\"amount\": 500.00, \"description\": \"Test deposit\"}";

        mockMvc.perform(post("/api/wallets/1/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/wallets/{accountId}/balance - returns 200 with balance details")
    @WithMockUser(username = "PAY1234567890", roles = {"USER"})
    void getBalance_returns200() throws Exception {
        BalanceResponse balanceResponse = BalanceResponse.builder()
                .walletId(1L)
                .totalBalance(new BigDecimal("1000.0000"))
                .availableBalance(new BigDecimal("900.0000"))
                .frozenAmount(new BigDecimal("100.0000"))
                .currency(Currency.USD)
                .build();

        when(walletService.getBalance(1L)).thenReturn(balanceResponse);

        mockMvc.perform(get("/api/wallets/1/balance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBalance").value(1000.0000))
                .andExpect(jsonPath("$.data.availableBalance").value(900.0000))
                .andExpect(jsonPath("$.data.frozenAmount").value(100.0000));
    }
}
