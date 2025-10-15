package com.fintech.payment.repository;

import com.fintech.payment.entity.Transfer;
import com.fintech.payment.enums.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByTransferRef(String transferRef);

    @Query("SELECT t FROM Transfer t WHERE t.fromWallet.id = :walletId " +
            "OR t.toWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transfer> findByFromWalletIdOrToWalletId(
            @Param("walletId") Long walletId,
            Pageable pageable);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transfer t WHERE t.status = :status")
    Page<Transfer> findByStatus(@Param("status") TransferStatus status, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.fromWallet.id = :fromWalletId " +
            "AND t.toWallet.id = :toWalletId ORDER BY t.createdAt DESC")
    Page<Transfer> findByFromWalletIdAndToWalletId(
            @Param("fromWalletId") Long fromWalletId,
            @Param("toWalletId") Long toWalletId,
            Pageable pageable);

    long countByStatus(TransferStatus status);
}
