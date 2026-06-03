package com.easyops.hospital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Legacy clinical chart / charge master row (seeded from CSV).
 * EP investigation autosuggest uses rows whose {@code sub_dept_name} is Diagnostic, Radiology, or LabTest (trimmed, case-insensitive).
 */
@Entity
@Table(name = "clinical_chart_items", schema = "hospital")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalChartItem {

    @Id
    @Column(name = "clinical_chart_item_id")
    private UUID clinicalChartItemId;

    @Column(name = "legacy_row_id", nullable = false)
    private Long legacyRowId;

    @Column(name = "pcode", length = 64)
    private String pcode;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "charge", precision = 18, scale = 4)
    private BigDecimal charge;

    @Column(name = "dept_name")
    private String deptName;

    @Column(name = "sub_dept_name")
    private String subDeptName;

    @Column(name = "sub_sub_dept_name")
    private String subSubDeptName;

    @Column(name = "report_group_name")
    private String reportGroupName;

    @Column(name = "out_test", nullable = false)
    private Short outTest;

    @Column(name = "status_legacy", nullable = false)
    private Short statusLegacy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
