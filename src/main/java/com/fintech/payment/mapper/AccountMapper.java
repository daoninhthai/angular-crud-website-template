package com.fintech.payment.mapper;

import com.fintech.payment.dto.response.AccountResponse;
import com.fintech.payment.model.entity.Account;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AccountMapper {

    /**
     * Maps an Account entity to an AccountResponse DTO.
     * Combines firstName and lastName into fullName.
     */
    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        String fullName = buildFullName(account.getFirstName(), account.getLastName());

        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .fullName(fullName)
                .email(account.getEmail())
                .phone(account.getPhoneNumber())
                .status(account.getStatus())
                .kycVerified(account.getKycVerified())
                .createdAt(account.getCreatedAt())
                .build();
    }

    /**
     * Maps a list of Account entities to a list of AccountResponse DTOs.
     */
    public List<AccountResponse> toResponseList(List<Account> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Collections.emptyList();
        }

        return accounts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
