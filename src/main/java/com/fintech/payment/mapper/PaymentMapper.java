package com.fintech.payment.mapper;

import com.fintech.payment.dto.response.PaymentResponse;
import com.fintech.payment.model.entity.Payment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMapper {

    /**
     * Maps a Payment entity to a PaymentResponse DTO.
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentRef(payment.getReferenceNumber())
                .amount(payment.getAmount())
                .currency(payment.getCurrency() != null ? payment.getCurrency().name() : null)
                .merchantName(payment.getMerchantName())
                .status(payment.getStatus())
                .refundedAmount(payment.getRefundedAmount())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    /**
     * Maps a list of Payment entities to a list of PaymentResponse DTOs.
     */
    public List<PaymentResponse> toResponseList(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return Collections.emptyList();
        }

        return payments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
