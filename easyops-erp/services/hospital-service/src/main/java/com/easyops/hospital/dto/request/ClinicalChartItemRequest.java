package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalChartItemRequest {

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
