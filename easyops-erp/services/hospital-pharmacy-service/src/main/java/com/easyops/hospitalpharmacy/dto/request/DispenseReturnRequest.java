package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class DispenseReturnRequest {

    @NotEmpty
    private List<@Valid Line> lines;

    @Data
    public static class Line {

        @NotNull
        private UUID dispenseLineId;

        @NotNull
        private BigDecimal quantityReturned;

        private String reason;
    }
}

