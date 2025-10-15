package com.fintech.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", indexes = {
        @Index(name = "idx_idempotency_key_value", columnList = "keyValue", unique = true),
        @Index(name = "idx_idempotency_expires_at", columnList = "expiresAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Key value is required")
    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Lob
    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "status_code")
    private int statusCode;

    @NotNull(message = "Expiration time is required")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this idempotency key has expired.
     *
     * @return true if the key has expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
