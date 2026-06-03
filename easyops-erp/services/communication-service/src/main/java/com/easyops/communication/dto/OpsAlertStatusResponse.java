package com.easyops.communication.dto;

import java.time.Instant;
import java.util.List;

public record OpsAlertStatusResponse(
        Instant evaluatedAt,
        List<AlertItem> alerts
) {
    public record AlertItem(
            String key,
            String level,
            boolean triggered,
            String message
    ) {
    }
}
