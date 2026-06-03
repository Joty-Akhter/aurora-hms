package com.easyops.hospitalpharmacy.integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InHouseDispenseFillPayload {
    private UUID prescriptionId;
    private UUID dispenseOrderId;
    private String idempotencyKey;
    private String fillStatus;
    private LocalDateTime fillStatusDate;
    private LocalDateTime filledDate;
    private String fillStatusMessage;
    private List<Line> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private UUID prescriptionMedicationId;
        private java.math.BigDecimal quantityDispensed;
    }
}
