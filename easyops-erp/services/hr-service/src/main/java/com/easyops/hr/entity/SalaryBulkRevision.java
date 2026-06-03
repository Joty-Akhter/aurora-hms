package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ES-21: Bulk salary revision request (e.g. X% to Basic by grade). Optional approval before apply.
 */
@Entity
@Table(name = "salary_bulk_revisions", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryBulkRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "bulk_revision_id")
    private UUID bulkRevisionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "revision_type", nullable = false, length = 50)
    private String revisionType = "BULK_PERCENTAGE";

    /** BY_GRADE or BY_STRUCTURE */
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_grade_id")
    private UUID targetGradeId;

    @Column(name = "target_structure_id")
    private UUID targetStructureId;

    /** Component code to apply revision to (e.g. BASIC). */
    @Column(name = "component_code", nullable = false, length = 100)
    private String componentCode;

    /** Percentage increase (e.g. 5 for 5%). */
    @Column(name = "percentage_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageValue;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** PENDING, APPROVED, REJECTED, APPLIED */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "comment", length = 1000)
    private String comment;

    /** Set after apply: number of employee detail rows created. */
    @Column(name = "rows_applied")
    private Integer rowsApplied;
}
