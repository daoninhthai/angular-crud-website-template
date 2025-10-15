package com.fintech.payment.entity;

import com.fintech.payment.enums.AccountRole;
import com.fintech.payment.enums.AccountStatus;
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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
        @Index(name = "idx_account_email", columnList = "email", unique = true),
        @Index(name = "idx_account_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format")
    @Column(name = "phone", length = 20)
    private String phone;

    @NotBlank(message = "Password hash is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private AccountRole role = AccountRole.USER;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "kyc_verified", nullable = false)
    @Builder.Default
    private boolean kycVerified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
