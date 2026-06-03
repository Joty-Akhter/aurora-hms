package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** SS-48 / RPT-05: Headcount and cost per structure, grade, or band for a given date. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeHeadcountDto {
    private UUID structureId;
    private String structureCode;
    private String structureName;
    private UUID gradeId;
    private String gradeCode;
    private String gradeName;
    private UUID bandId;
    private String bandCode;
    private String bandName;
    private long headcount;
    /** RPT-05: Total monthly cost (sum of earning components) for this segment; null if not computed. */
    private BigDecimal totalCost;
    /** Currency for totalCost (from structure). */
    private String currency;
}
