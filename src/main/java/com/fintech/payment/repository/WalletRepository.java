package com.fintech.payment.repository;

import com.fintech.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByAccountId(Long accountId);

    @Query("SELECT w FROM Wallet w JOIN w.account a WHERE a.accountNumber = :accountNumber")
    Optional<Wallet> findByAccountAccountNumber(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    Optional<Wallet> findByIdWithPessimisticLock(@Param("walletId") Long walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.account.id = :accountId")
    Optional<Wallet> findByAccountIdWithPessimisticLock(@Param("accountId") Long accountId);

    boolean existsByAccountId(Long accountId);
}
