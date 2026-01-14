package com.fintech.payment.entity;

import com.fintech.payment.enums.TransferStatus;
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
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfer_ref", columnList = "transferRef", unique = true),
        @Index(name = "idx_transfer_from_wallet", columnList = "from_wallet_id"),
        @Index(name = "idx_transfer_to_wallet", columnList = "to_wallet_id"),
        @Index(name = "idx_transfer_idempotency_key", columnList = "idempotencyKey"),
        @Index(name = "idx_transfer_status", columnList = "status"),
        @Index(name = "idx_transfer_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Transfer reference is required")
    @Column(name = "transfer_ref", nullable = false, unique = true, length = 36)
    private String transferRef;

    /**
     * Alias for transferRef used by service layer.
     */
    public String getReferenceNumber() {
        return transferRef;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.transferRef = referenceNumber;
    }

    @NotNull(message = "Source wallet is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id", nullable = false)
    private Wallet fromWallet;

    @NotNull(message = "Destination wallet is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id", nullable = false)
    private Wallet toWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull(message = "Fee is required")
    @PositiveOrZero(message = "Fee must be zero or positive")
    @Column(name = "fee", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Transfer status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "source_transaction_ref", length = 36)
    private String sourceTransactionRef;

    @Column(name = "destination_transaction_ref", length = 36)
    private String destinationTransactionRef;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
