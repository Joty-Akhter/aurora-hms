package com.easyops.communication.provider;

public record ProviderDispatchResult(
        String channel,
        String providerName,
        String status,
        String providerReference
) {
}
