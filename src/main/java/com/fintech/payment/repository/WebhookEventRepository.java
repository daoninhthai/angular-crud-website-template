package com.fintech.payment.repository;

import com.fintech.payment.entity.WebhookEvent;
import com.fintech.payment.enums.WebhookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    @Query("SELECT w FROM WebhookEvent w WHERE w.status = :status " +
            "AND w.nextRetryAt <= :cutoffTime ORDER BY w.nextRetryAt ASC")
    List<WebhookEvent> findByStatusAndNextRetryAtBefore(
            @Param("status") WebhookStatus status,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT COUNT(w) FROM WebhookEvent w WHERE w.targetUrl = :targetUrl AND w.status = :status")
    long countByTargetUrlAndStatus(
            @Param("targetUrl") String targetUrl,
            @Param("status") WebhookStatus status);

    @Query("SELECT w FROM WebhookEvent w WHERE w.status IN (:statuses) " +
            "AND w.nextRetryAt <= :now AND w.retryCount < w.maxRetries")
    List<WebhookEvent> findRetryableWebhooks(
            @Param("statuses") List<WebhookStatus> statuses,
            @Param("now") LocalDateTime now);

    Page<WebhookEvent> findByEventType(String eventType, Pageable pageable);

    Page<WebhookEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
