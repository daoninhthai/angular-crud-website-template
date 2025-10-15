package com.fintech.payment.service;

import java.util.Optional;

/**
 * Service interface for idempotency key management.
 * Uses Redis as primary store with database fallback.
 * Ensures that duplicate requests return the same response.
 */
public interface IdempotencyService {

    /**
     * Checks if a response already exists for the given idempotency key.
     * First checks Redis, then falls back to the database.
     *
     * @param key the idempotency key
     * @return the cached JSON response if key was previously processed, empty otherwise
     */
    Optional<String> checkIdempotency(String key);

    /**
     * Saves the result of a processed request for the given idempotency key.
     * Stores in both Redis (with TTL) and the database for durability.
     *
     * @param key        the idempotency key
     * @param response   the JSON response body to cache
     * @param statusCode the HTTP status code of the response
     * @param ttlMinutes the time-to-live in minutes (default: 1440 = 24 hours)
     */
    void saveIdempotencyResult(String key, String response, int statusCode, long ttlMinutes);

    /**
     * Removes expired idempotency records from the database.
     * Runs on a daily schedule. Redis keys expire automatically via TTL.
     */
    void cleanExpiredKeys();
}
