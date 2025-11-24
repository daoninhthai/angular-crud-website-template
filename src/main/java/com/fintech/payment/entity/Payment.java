package com.fintech.payment.entity;

import com.fintech.payment.enums.PaymentMethod;
import com.fintech.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_ref", columnList = "paymentRef", unique = true),
        @Index(name = "idx_payment_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_payment_idempotency_key", columnList = "idempotencyKey"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_merchant_ref", columnList = "merchantRef"),
        @Index(name = "idx_payment_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Payment reference is required")
    @Column(name = "payment_ref", nullable = false, unique = true, length = 36)
    private String paymentRef;

    @NotNull(message = "Wallet is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "merchant_ref", length = 100)
    private String merchantRef;

    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @NotNull(message = "Refunded amount is required")
    @PositiveOrZero(message = "Refunded amount must be zero or positive")
    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
