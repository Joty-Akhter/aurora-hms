package com.easyops.hospital.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ClinicalChartItemResponse {
    private UUID clinicalChartItemId;
    private Long legacyRowId;
    private String pcode;
    private String description;
    private BigDecimal charge;
    private String deptName;
    private String subDeptName;
    private String subSubDeptName;
    private String reportGroupName;
    private Short outTest;
    private Short statusLegacy;
}
