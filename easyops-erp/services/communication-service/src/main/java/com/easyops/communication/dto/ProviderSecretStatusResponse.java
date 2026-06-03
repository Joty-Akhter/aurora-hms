package com.easyops.communication.dto;

import java.util.List;

public record ProviderSecretStatusResponse(
        List<ProviderSecretItem> providers
) {
    public record ProviderSecretItem(
            String provider,
            String channel,
            boolean credentialsConfigured,
            String sourcePolicy
    ) {
    }
}
