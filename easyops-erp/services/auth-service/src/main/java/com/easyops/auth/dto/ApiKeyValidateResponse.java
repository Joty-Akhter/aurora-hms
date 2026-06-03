package com.easyops.auth.dto;

import java.util.UUID;

public class ApiKeyValidateResponse {

    private UUID userId;
    private UUID organizationId;

    public ApiKeyValidateResponse() {}

    public ApiKeyValidateResponse(UUID userId, UUID organizationId) {
        this.userId = userId;
        this.organizationId = organizationId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
