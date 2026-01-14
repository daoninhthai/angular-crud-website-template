package com.fintech.payment.dto.response;


import com.fintech.payment.enums.AccountRole;
import com.fintech.payment.enums.AccountStatus;
import com.fintech.payment.enums.Currency;
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
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String phoneNumber;
    private AccountRole role;
    private AccountStatus status;
    private Currency currency;
    private Boolean kycVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
