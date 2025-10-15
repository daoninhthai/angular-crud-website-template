package com.fintech.payment.security;

import com.fintech.payment.exception.AccountNotFoundException;
import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.enums.AccountStatus;
import com.fintech.payment.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    /**
     * Loads user details by account number.
     * The account number is used as the username in the Spring Security context.
     *
     * @param accountNumber the account number to look up
     * @return UserDetails containing the account's credentials and role authority
     * @throws UsernameNotFoundException if no account is found with the given account number
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String accountNumber) throws UsernameNotFoundException {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Account not found with account number: " + accountNumber));

        boolean isActive = account.getStatus() == AccountStatus.ACTIVE;

        // Default role is ROLE_USER; this can be extended when roles are stored on the entity
        String role = "ROLE_USER";

        return new User(
                account.getAccountNumber(),
                account.getPasswordHash(),
                isActive,   // enabled
                true,       // accountNonExpired
                true,       // credentialsNonExpired
                account.getStatus() != AccountStatus.SUSPENDED, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
