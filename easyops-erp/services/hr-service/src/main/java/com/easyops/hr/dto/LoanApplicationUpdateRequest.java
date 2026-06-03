package com.easyops.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class LoanApplicationUpdateRequest {

    @NotNull
    private UUID categoryId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal requestedAmount;

    @NotNull
    @Min(1)
    private Integer requestedTenureMonths;

    private String purposeNotes;

    private List<String> attachmentReferences;

    private String limitOverrideReason;

    private String facilityOverrideReason;
}
