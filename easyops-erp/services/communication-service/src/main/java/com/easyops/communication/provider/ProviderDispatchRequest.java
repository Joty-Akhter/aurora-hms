package com.easyops.communication.provider;

public record ProviderDispatchRequest(
        String recipient,
        String subject,
        String body
) {
}
