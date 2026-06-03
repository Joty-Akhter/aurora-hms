package com.easyops.communication.dto;

public record TemplatePreviewResponse(
        String renderedSubject,
        String renderedBody
) {
}
