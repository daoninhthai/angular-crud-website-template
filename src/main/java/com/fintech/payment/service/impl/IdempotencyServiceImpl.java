package com.fintech.payment.service.impl;

import com.fintech.payment.model.entity.IdempotencyRecord;
import com.fintech.payment.repository.IdempotencyRecordRepository;
import com.fintech.payment.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final String REDIS_KEY_PREFIX = "idempotency:";
    private static final long DEFAULT_TTL_MINUTES = 1440; // 24 hours

    private final StringRedisTemplate redisTemplate;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    public Optional<String> checkIdempotency(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        // Step 1: Check Redis first for fast lookup
        try {
            String redisKey = REDIS_KEY_PREFIX + key;
            String cachedResponse = redisTemplate.opsForValue().get(redisKey);
            if (cachedResponse != null) {
                log.debug("Idempotency hit in Redis: key={}", key);
                return Optional.of(cachedResponse);
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for idempotency key {}: {}", key, e.getMessage());
            // Fall through to DB lookup
        }

        // Step 2: Fallback to database
        Optional<IdempotencyRecord> dbRecord = idempotencyRecordRepository.findByIdempotencyKey(key);
        if (dbRecord.isPresent()) {
            IdempotencyRecord record = dbRecord.get();

            // Check if the record has expired
            if (record.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.debug("Idempotency hit in DB: key={}", key);

                // Re-populate Redis cache for future lookups
                repopulateRedisCache(key, record.getResponseBody(), record.getExpiresAt());

                return Optional.of(record.getResponseBody());
            } else {
                log.debug("Idempotency record expired in DB: key={}", key);
                // Expired record - treat as not found
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public void saveIdempotencyResult(String key, String response, int statusCode, long ttlMinutes) {
        if (key == null || key.isBlank()) {
            return;
        }

        long effectiveTtl = ttlMinutes > 0 ? ttlMinutes : DEFAULT_TTL_MINUTES;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(effectiveTtl);

        // Save to Redis with TTL
        try {
            String redisKey = REDIS_KEY_PREFIX + key;
            redisTemplate.opsForValue().set(redisKey, response, Duration.ofMinutes(effectiveTtl));
            log.debug("Idempotency result saved to Redis: key={}, ttl={}min", key, effectiveTtl);
        } catch (Exception e) {
            log.warn("Failed to save idempotency result to Redis: key={}, error={}", key, e.getMessage());
            // Continue to save to DB even if Redis fails
        }

        // Save to database for durability
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(key)
                .responseBody(response)
                .statusCode(statusCode)
                .expiresAt(expiresAt)
                .build();

        try {
            idempotencyRecordRepository.save(record);
            log.debug("Idempotency result saved to DB: key={}, expiresAt={}", key, expiresAt);
        } catch (Exception e) {
            // Handle unique constraint violation (concurrent save)
            log.warn("Failed to save idempotency record to DB (possible duplicate): key={}, error={}",
                    key, e.getMessage());
        }
    }

    @Override
    @Scheduled(cron = "${idempotency.cleanup.cron:0 0 3 * * ?}")  // Daily at 3 AM
    @Transactional
    public void cleanExpiredKeys() {
        log.info("Starting idempotency records cleanup");

        try {
            int deletedCount = idempotencyRecordRepository.deleteExpiredRecords(LocalDateTime.now());
            log.info("Cleaned up {} expired idempotency records", deletedCount);
        } catch (Exception e) {
            log.error("Error during idempotency cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Re-populates the Redis cache when a record is found in the database but
     * missing from Redis (e.g., after Redis restart).
     */
    private void repopulateRedisCache(String key, String response, LocalDateTime expiresAt) {
        try {
            Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
            if (!ttl.isNegative() && !ttl.isZero()) {
                String redisKey = REDIS_KEY_PREFIX + key;
                redisTemplate.opsForValue().set(redisKey, response, ttl);
            }
        } catch (Exception e) {
            log.warn("Failed to repopulate Redis cache for key {}: {}", key, e.getMessage());
        }
    }
}
