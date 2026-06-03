package com.easyops.communication.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunicationTemplateUpdateRequest(
        @Size(max = 120) String templateKey,
        @Pattern(regexp = "SMS|EMAIL") String channel,
        @Size(max = 15) String locale,
        @Min(1) @Max(9999) Integer version,
        @Pattern(regexp = "DRAFT|ACTIVE|ARCHIVED") String status,
        @Size(max = 500) String subjectTemplate,
        @Size(max = 4000) String bodyTemplate,
        @Size(max = 4000) String variablesSchema
) {
}
