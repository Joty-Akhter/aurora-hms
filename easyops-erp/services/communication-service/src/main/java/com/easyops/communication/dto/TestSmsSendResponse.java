package com.easyops.communication.dto;

public record TestSmsSendResponse(
        String message,
        String channel,
        String providerName,
        String status,
        String providerReference
) {
}

