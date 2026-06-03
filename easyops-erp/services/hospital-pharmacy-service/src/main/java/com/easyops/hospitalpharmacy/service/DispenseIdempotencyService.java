package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.response.DispenseOrderResponse;
import com.easyops.hospitalpharmacy.entity.DispenseIdempotencyRecord;
import com.easyops.hospitalpharmacy.repository.DispenseIdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Idempotent replay for POST /dispense-orders/{id}/lines and /returns (Phase P1 — plan K2).
 * Uses PostgreSQL {@code pg_advisory_xact_lock} to serialize concurrent requests with the same key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispenseIdempotencyService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final DispenseIdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public DispenseOrderResponse executePostDispenseLines(UUID orderId, String idempotencyKey, Supplier<DispenseOrderResponse> action) {
        return execute("POST_DISPENSE_LINES:" + orderId, idempotencyKey, action);
    }

    @Transactional
    public DispenseOrderResponse executePostReturns(UUID orderId, String idempotencyKey, Supplier<DispenseOrderResponse> action) {
        return execute("POST_DISPENSE_RETURNS:" + orderId, idempotencyKey, action);
    }

    private DispenseOrderResponse execute(String scope, String idempotencyKey, Supplier<DispenseOrderResponse> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }
        String key = idempotencyKey.trim();
        if (key.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key must be at most " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }

        long lock = stableHash64(scope + "\0" + key);
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(CAST(:k AS BIGINT))")
                .setParameter("k", lock)
                .getResultList();

        return repository.findByScopeAndIdempotencyKey(scope, key)
                .map(rec -> readResponse(rec.getResponseBody()))
                .orElseGet(() -> {
                    DispenseOrderResponse result = action.get();
                    try {
                        String json = objectMapper.writeValueAsString(result);
                        try {
                            repository.save(DispenseIdempotencyRecord.builder()
                                    .scope(scope)
                                    .idempotencyKey(key)
                                    .responseBody(json)
                                    .httpStatus(200)
                                    .build());
                        } catch (DataIntegrityViolationException duplicate) {
                            // Rare: unique race across nodes; replay stored response
                            log.debug("Idempotency insert conflict for scope={} key={} — replaying", scope, key);
                            return repository.findByScopeAndIdempotencyKey(scope, key)
                                    .map(rec -> readResponse(rec.getResponseBody()))
                                    .orElseThrow(() -> duplicate);
                        }
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Failed to serialize idempotent response", e);
                    }
                    return result;
                });
    }

    private DispenseOrderResponse readResponse(String json) {
        try {
            return objectMapper.readValue(json, DispenseOrderResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Corrupt idempotency payload: {}", e.getMessage());
            throw new IllegalStateException("Stored idempotent response could not be read", e);
        }
    }

    /**
     * Map two strings to a 64-bit lock key with low collision risk for advisory lock.
     */
    static long stableHash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
