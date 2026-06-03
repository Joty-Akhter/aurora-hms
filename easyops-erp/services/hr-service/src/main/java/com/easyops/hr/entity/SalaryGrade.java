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
 * Salary grade within a salary structure (SS-08–SS-12).
 * Code is unique within the structure and immutable after creation.
 */
@Entity
@Table(name = "salary_grades", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = { "salary_structure_id", "code" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalaryGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "salary_grade_id")
    private UUID salaryGradeId;

    @Column(name = "salary_structure_id", nullable = false, updatable = false)
    private UUID salaryStructureId;

    /** Unique within structure; immutable after creation (SS-09, SS-12). */
    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Integer for sorting; used for lists, reports, progression (SS-10). */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

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
