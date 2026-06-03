package com.easyops.auth.service;

import com.easyops.auth.dto.ApiKeyValidateResponse;
import com.easyops.auth.entity.ApiKey;
import com.easyops.auth.repository.ApiKeyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;

@Service
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public ApiKeyValidateResponse validate(String rawKey) {
        String hash = sha256Hex(rawKey);
        ApiKey key = apiKeyRepository.findByKeyHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key"));

        if (!key.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key is inactive");
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key has expired");
        }

        apiKeyRepository.updateLastUsed(key.getId(), OffsetDateTime.now());

        return new ApiKeyValidateResponse(key.getUserId(), key.getOrganizationId());
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
