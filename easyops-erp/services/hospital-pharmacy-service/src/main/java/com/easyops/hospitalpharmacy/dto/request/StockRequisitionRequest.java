package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class StockRequisitionRequest {

    @NotNull
    private UUID fromLocationId;

    @NotNull
    private UUID toLocationId;

    private String notes;

    @NotEmpty
    private List<Line> lines;

    @Data
    public static class Line {

        @NotNull
        private UUID drugId;

        @NotNull
        @Positive
        private BigDecimal requestedQuantity;

        private String batchNumber;

        private String notes;
    }
}
