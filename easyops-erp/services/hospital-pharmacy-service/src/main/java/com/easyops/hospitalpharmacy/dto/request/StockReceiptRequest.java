package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class StockReceiptRequest {

    @NotEmpty
    private List<Line> lines;

    @Data
    public static class Line {

        @NotNull
        private UUID drugId;

        @NotNull
        private BigDecimal quantity;

        private String batchNumber;

        private LocalDate expiryDate;

        private String referenceType;

        private UUID referenceId;

        private String notes;
    }
}

