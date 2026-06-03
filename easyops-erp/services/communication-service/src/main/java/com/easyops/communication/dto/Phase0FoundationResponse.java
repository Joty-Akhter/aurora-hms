package com.easyops.communication.dto;

import java.util.List;
import java.util.Map;

public record Phase0FoundationResponse(
        String phase,
        String status,
        String brokerStrategy,
        String serviceBoundary,
        String templateEngine,
        List<String> adrs,
        List<String> requiredEnvelopeFields,
        Map<String, String> conventions,
        List<String> v1UseCases,
        List<String> acceptanceCriteria,
        List<String> backlogSnapshot,
        String timeline
) {
}
