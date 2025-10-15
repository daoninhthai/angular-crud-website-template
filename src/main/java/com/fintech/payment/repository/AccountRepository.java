package com.fintech.payment.repository;

import com.fintech.payment.model.entity.Account;
import com.fintech.payment.model.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.status = :status")
    Page<Account> findByStatus(@Param("status") AccountStatus status, Pageable pageable);

    @Query("SELECT a FROM Account a WHERE a.email = :email AND a.status = 'ACTIVE'")
    Optional<Account> findActiveByEmail(@Param("email") String email);

    @Query("SELECT a FROM Account a WHERE a.phoneNumber = :phone")
    Optional<Account> findByPhone(@Param("phone") String phone);

    long countByStatus(AccountStatus status);
}
