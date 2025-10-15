package com.fintech.payment.repository;

import com.fintech.payment.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByKeyValue(String keyValue);

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :cutoffTime")
    int deleteByExpiresAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    boolean existsByKeyValue(String keyValue);
}
