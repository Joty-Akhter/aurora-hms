package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class StockTransferRequest {

    /** Optional approving user. If omitted the requesting user is recorded as the approver. */
    private UUID approvedByUserId;

    @NotNull
    private UUID destinationPharmacyLocationId;

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

        private String notes;
    }
}

