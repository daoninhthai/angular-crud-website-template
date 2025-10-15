package com.fintech.payment.dto.response;

import com.fintech.payment.enums.AccountRole;
import com.fintech.payment.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String fullName;
    private String email;
    private String phone;
    private AccountRole role;
    private AccountStatus status;
    private Boolean kycVerified;
    private LocalDateTime createdAt;
}
