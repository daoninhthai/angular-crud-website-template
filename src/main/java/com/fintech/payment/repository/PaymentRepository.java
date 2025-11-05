package com.fintech.payment.repository;

import com.fintech.payment.entity.Payment;
import com.fintech.payment.enums.PaymentStatus;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentRef(String paymentRef);

    Page<Payment> findByWalletId(Long walletId, Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :cutoffDate")
    List<Payment> findByStatusAndCreatedAtBefore(
            @Param("status") PaymentStatus status,
            @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT p FROM Payment p WHERE p.wallet.id = :walletId " +
            "AND p.status = :status ORDER BY p.createdAt DESC")
    Page<Payment> findByWalletIdAndStatus(
            @Param("walletId") Long walletId,
            @Param("status") PaymentStatus status,
            Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.merchantRef = :merchantRef")
    Optional<Payment> findByMerchantRef(@Param("merchantRef") String merchantRef);

    long countByWalletIdAndStatus(Long walletId, PaymentStatus status);

    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

}
