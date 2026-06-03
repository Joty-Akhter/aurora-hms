package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** SS-42, SS-47: Grade with nested bands for structure summary. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryGradeSummaryDto {
    private UUID salaryGradeId;
    private UUID salaryStructureId;
    private String code;
    private String name;
    private Integer displayOrder;
    private String description;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    @Builder.Default
    private List<SalaryBandSummaryDto> bands = new ArrayList<>();
}
