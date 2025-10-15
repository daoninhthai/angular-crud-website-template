package com.fintech.payment.mapper;

import com.fintech.payment.dto.response.TransactionResponse;
import com.fintech.payment.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionMapper {

    /**
     * Maps a Transaction entity to a TransactionResponse DTO.
     */
    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionRef(transaction.getReferenceNumber())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Maps a list of Transaction entities to a list of TransactionResponse DTOs.
     */
    public List<TransactionResponse> toResponseList(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a Page of Transaction entities to a Page of TransactionResponse DTOs.
     * Preserves pagination metadata (page number, size, total elements, sort).
     */
    public Page<TransactionResponse> toResponsePage(Page<Transaction> transactionPage) {
        if (transactionPage == null) {
            return Page.empty();
        }

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, transactionPage.getPageable(), transactionPage.getTotalElements());
    }
}
