package com.easyops.communication.dto;

public record TemplateTestSendResponse(
        String channel,
        String provider,
        String status,
        String providerReference
) {
}
