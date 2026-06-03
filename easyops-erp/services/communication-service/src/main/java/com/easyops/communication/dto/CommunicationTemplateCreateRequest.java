package com.easyops.communication.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunicationTemplateCreateRequest(
        @NotBlank @Size(max = 120) String templateKey,
        @NotBlank @Pattern(regexp = "SMS|EMAIL") String channel,
        @NotBlank @Size(max = 15) String locale,
        @Min(1) @Max(9999) Integer version,
        @NotBlank @Pattern(regexp = "DRAFT|ACTIVE|ARCHIVED") String status,
        @Size(max = 500) String subjectTemplate,
        @NotBlank @Size(max = 4000) String bodyTemplate,
        @NotBlank @Size(max = 4000) String variablesSchema
) {
}
