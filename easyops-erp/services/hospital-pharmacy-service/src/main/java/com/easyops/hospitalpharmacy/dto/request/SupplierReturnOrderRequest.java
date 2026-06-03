package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SupplierReturnOrderRequest {

    @NotNull
    private UUID manufacturerId;

    @NotNull
    private UUID fromLocationId;

    private String returnReference;

    private String notes;

    @NotEmpty
    private List<Line> lines;

    @Data
    public static class Line {

        @NotNull
        private UUID drugId;

        private String batchNumber;

        private LocalDate expiryDate;

        @NotNull
        @Positive
        private BigDecimal quantity;

        @NotNull
        private String returnReason;

        private String notes;
    }
}
