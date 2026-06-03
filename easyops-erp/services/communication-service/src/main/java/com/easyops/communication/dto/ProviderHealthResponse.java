package com.easyops.communication.dto;

public record ProviderHealthResponse(
        String provider,
        String channel,
        String status,
        String details
) {
}
