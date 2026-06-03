package com.easyops.communication.provider;

public record ProviderHealthStatus(
        String providerName,
        String channel,
        String status,
        String details
) {
}
