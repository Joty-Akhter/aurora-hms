package com.easyops.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record InboundCommunicationEvent(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotBlank String eventVersion,
        @NotNull Instant occurredAt,
        @NotBlank String organizationId,
        @NotBlank String entityId,
        @NotBlank String actorId,
        @NotBlank String correlationId,
        @NotNull Map<String, Object> payload
) {
}
