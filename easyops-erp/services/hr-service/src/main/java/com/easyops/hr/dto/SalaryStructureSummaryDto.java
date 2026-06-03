package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** SS-42, SS-47: Structure with nested grades and bands for get-by-id and structure summary report. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructureSummaryDto {
    private UUID salaryStructureId;
    private UUID organizationId;
    private String code;
    private String structureName;
    private String description;
    private String currency;
    private String payFrequency;
    private Boolean isDefault;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    @Builder.Default
    private List<SalaryGradeSummaryDto> grades = new ArrayList<>();
}
