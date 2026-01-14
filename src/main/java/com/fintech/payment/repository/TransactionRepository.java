package com.fintech.payment.repository;

import com.fintech.payment.entity.Transaction;
import com.fintech.payment.enums.TransactionStatus;
import com.fintech.payment.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.transactionRef = :referenceNumber")
    Optional<Transaction> findByReferenceNumber(@Param("referenceNumber") String referenceNumber);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId " +
            "AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId " +
            "AND t.type = :type ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletIdAndType(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId " +
            "AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByWalletIdAndStatus(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    long countByWalletIdAndStatus(Long walletId, TransactionStatus status);
}
