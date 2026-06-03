package com.easyops.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class LoanApplicationCreateRequest {

    @NotNull
    private UUID employeeId;

    @NotNull
    private UUID categoryId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal requestedAmount;

    @NotNull
    @Min(1)
    private Integer requestedTenureMonths;

    private String purposeNotes;

    /** Optional attachment references (URLs or document ids), AL-01. */
    private List<String> attachmentReferences;

    /** AD-02: required when requesting above policy/category cap (validated on submit). */
    private String limitOverrideReason;

    /** LC-05: when another facility would block submission (validated on submit). */
    private String facilityOverrideReason;
}
