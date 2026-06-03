package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ES-01–ES-06: Assignment of a salary structure (and optionally grade/band) to an employee
 * for an effective period. One active assignment per employee per date.
 * Source indicates whether from position default (POSITION) or manual override (OVERRIDE).
 */
@Entity
@Table(name = "employee_salary_assignments", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EmployeeSalaryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "assignment_id")
    private UUID assignmentId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "salary_structure_id", nullable = false)
    private UUID salaryStructureId;

    @Column(name = "salary_grade_id", nullable = false)
    private UUID salaryGradeId;

    /** Optional: band within the grade (ES-01). */
    @Column(name = "salary_band_id")
    private UUID salaryBandId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null = open-ended (ES-01). */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private AssignmentSource source = AssignmentSource.OVERRIDE;

    /** ES-17: Reason for this revision (e.g. "Annual increment 2024"). */
    @Column(name = "revision_reason", length = 500)
    private String revisionReason;

    /** ES-17: Type of revision (e.g. ANNUAL_INCREMENT, PROMOTION). */
    @Column(name = "revision_type", length = 50)
    private String revisionType;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
