package com.easyops.communication.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record TemplatePreviewRequest(
        @NotNull UUID templateId,
        Map<String, Object> variables
) {
}
