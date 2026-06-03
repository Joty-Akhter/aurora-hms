package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** SS-47: Band summary for structure report (code, name, min, mid, max, currency). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryBandSummaryDto {
    private UUID salaryBandId;
    private UUID salaryGradeId;
    private String code;
    private String name;
    private Integer displayOrder;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal midPoint;
    private String currency;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
