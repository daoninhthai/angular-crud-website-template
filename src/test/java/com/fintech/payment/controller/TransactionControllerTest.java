package com.fintech.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.payment.model.dto.response.TransactionResponse;
import com.fintech.payment.model.enums.Currency;
import com.fintech.payment.model.enums.TransactionStatus;
import com.fintech.payment.model.enums.TransactionType;
import com.fintech.payment.security.CustomUserDetailsService;
import com.fintech.payment.security.JwtTokenProvider;
import com.fintech.payment.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/transactions/{ref} - returns 200 with transaction details")
    @WithMockUser(username = "PAY1234567890", roles = {"USER"})
    void getTransaction_returns200() throws Exception {
        String referenceNumber = UUID.randomUUID().toString();

        TransactionResponse transactionResponse = TransactionResponse.builder()
                .id(1L)
                .referenceNumber(referenceNumber)
                .walletId(1L)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("500.00"))
                .currency(Currency.USD)
                .balanceBefore(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .description("Test deposit")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionService.getTransactionByRef(referenceNumber))
                .thenReturn(transactionResponse);

        mockMvc.perform(get("/api/transactions/" + referenceNumber)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referenceNumber").value(referenceNumber))
                .andExpect(jsonPath("$.data.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.amount").value(500.00));
    }

    @Test
    @DisplayName("GET /api/transactions/wallet/{walletId} - returns 200 with paginated history")
    @WithMockUser(username = "PAY1234567890", roles = {"USER"})
    void getTransactionHistory_returns200() throws Exception {
        String referenceNumber = UUID.randomUUID().toString();

        TransactionResponse transactionResponse = TransactionResponse.builder()
                .id(1L)
                .referenceNumber(referenceNumber)
                .walletId(1L)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(new BigDecimal("500.00"))
                .currency(Currency.USD)
                .balanceBefore(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1000.00"))
                .description("Test deposit")
                .createdAt(LocalDateTime.now())
                .build();

        Page<TransactionResponse> page = new PageImpl<>(
                Collections.singletonList(transactionResponse),
                PageRequest.of(0, 20),
                1);

        when(transactionService.getTransactionsByWallet(eq(1L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/transactions/wallet/1")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].referenceNumber").value(referenceNumber))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
