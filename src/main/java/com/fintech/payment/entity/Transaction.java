package com.fintech.payment.entity;

import com.fintech.payment.enums.TransactionStatus;
import com.fintech.payment.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_ref", columnList = "transactionRef", unique = true),
        @Index(name = "idx_txn_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_txn_idempotency_key", columnList = "idempotencyKey"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_created_at", columnList = "createdAt"),
        @Index(name = "idx_txn_wallet_created", columnList = "wallet_id, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transaction reference is required")
    @Column(name = "transaction_ref", nullable = false, unique = true, length = 36)
    private String transactionRef;

    /**
     * Alias for transactionRef used by service layer.
     */
    public String getReferenceNumber() {
        return transactionRef;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.transactionRef = referenceNumber;
    }

    @NotNull(message = "Wallet is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull(message = "Balance before is required")
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @NotNull(message = "Balance after is required")
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "counterparty_wallet_id")
    private Long counterpartyWalletId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
