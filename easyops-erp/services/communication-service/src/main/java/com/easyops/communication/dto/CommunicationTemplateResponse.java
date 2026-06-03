package com.easyops.communication.dto;

import java.time.Instant;
import java.util.UUID;

public record CommunicationTemplateResponse(
        UUID id,
        String templateKey,
        String channel,
        String locale,
        Integer version,
        String status,
        String subjectTemplate,
        String bodyTemplate,
        String variablesSchema,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt,
        String activatedBy
) {
}
