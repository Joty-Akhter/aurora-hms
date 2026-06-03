package com.easyops.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record TemplateTestSendRequest(
        @NotNull UUID templateId,
        @NotBlank String recipient,
        Map<String, Object> variables
) {
}
