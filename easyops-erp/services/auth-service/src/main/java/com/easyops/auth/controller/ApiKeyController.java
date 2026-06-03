package com.easyops.auth.controller;

import com.easyops.auth.dto.ApiKeyValidateRequest;
import com.easyops.auth.dto.ApiKeyValidateResponse;
import com.easyops.auth.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/apikey")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * Internal endpoint called by the API Gateway to validate a raw API key.
     * Returns the associated userId and organizationId so the gateway can inject
     * X-User-Id / X-Organization-Id headers before forwarding the request.
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiKeyValidateResponse> validate(@Valid @RequestBody ApiKeyValidateRequest request) {
        return ResponseEntity.ok(apiKeyService.validate(request.getApiKey()));
    }
}
