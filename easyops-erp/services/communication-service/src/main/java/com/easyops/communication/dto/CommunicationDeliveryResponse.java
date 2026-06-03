package com.easyops.communication.dto;

import java.time.Instant;
import java.util.UUID;

public record CommunicationDeliveryResponse(
        UUID id,
        String eventId,
        String correlationId,
        String eventType,
        String eventVersion,
        String organizationId,
        String entityId,
        String templateKey,
        String channel,
        String recipient,
        Integer templateVersion,
        String templateLocale,
        String idempotencyKey,
        String status,
        String policyDecision,
        String policyReason,
        String failureCategory,
        String failureReason,
        String providerName,
        String providerReference,
        Integer attemptCount,
        Instant nextAttemptAt,
        Instant lastAttemptAt,
        Instant createdAt,
        Instant updatedAt
) {
}
