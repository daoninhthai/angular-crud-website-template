package com.fintech.payment.service;

import com.fintech.payment.model.entity.IdempotencyRecord;
import com.fintech.payment.repository.IdempotencyRecordRepository;
import com.fintech.payment.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    private static final String TEST_KEY = "test-idempotency-key";
    private static final String TEST_RESPONSE = "{\"success\":true,\"data\":{\"id\":1}}";

    @BeforeEach
    void setUp() {
        // Setup is done via @Mock annotations
    }

    @Test
    @DisplayName("checkIdempotency - key exists in Redis: should return cached response")
    void checkIdempotency_keyExists_returnsResponse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:" + TEST_KEY)).thenReturn(TEST_RESPONSE);

        Optional<String> result = idempotencyService.checkIdempotency(TEST_KEY);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(TEST_RESPONSE);
        verify(redisTemplate).opsForValue();
        verify(idempotencyRecordRepository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    @DisplayName("checkIdempotency - key not in Redis or DB: should return empty")
    void checkIdempotency_keyNotExists_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:" + TEST_KEY)).thenReturn(null);
        when(idempotencyRecordRepository.findByIdempotencyKey(TEST_KEY)).thenReturn(Optional.empty());

        Optional<String> result = idempotencyService.checkIdempotency(TEST_KEY);

        assertThat(result).isEmpty();
        verify(redisTemplate).opsForValue();
        verify(idempotencyRecordRepository).findByIdempotencyKey(TEST_KEY);
    }

    @Test
    @DisplayName("saveIdempotencyResult - success: should save to both Redis and DB")
    void saveIdempotencyResult_success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString(), any());

        IdempotencyRecord savedRecord = IdempotencyRecord.builder()
                .idempotencyKey(TEST_KEY)
                .responseBody(TEST_RESPONSE)
                .statusCode(200)
                .expiresAt(LocalDateTime.now().plusMinutes(1440))
                .build();
        savedRecord.setId(1L);
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenReturn(savedRecord);

        idempotencyService.saveIdempotencyResult(TEST_KEY, TEST_RESPONSE, 200, 1440);

        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq("idempotency:" + TEST_KEY), eq(TEST_RESPONSE), any());
        verify(idempotencyRecordRepository).save(any(IdempotencyRecord.class));
    }

    @Test
    @DisplayName("cleanExpiredKeys - should delete expired records from DB")
    void cleanExpiredKeys_deletesExpired() {
        when(idempotencyRecordRepository.deleteExpiredRecords(any(LocalDateTime.class))).thenReturn(5);

        idempotencyService.cleanExpiredKeys();

        verify(idempotencyRecordRepository).deleteExpiredRecords(any(LocalDateTime.class));
    }
}
