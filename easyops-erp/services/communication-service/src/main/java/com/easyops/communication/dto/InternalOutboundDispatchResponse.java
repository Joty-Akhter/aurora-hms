package com.easyops.communication.dto;

public record InternalOutboundDispatchResponse(
        String channel,
        String providerName,
        String status,
        String providerReference
) {
}
