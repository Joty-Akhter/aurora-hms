package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Salary band within a grade: pay range min–max with optional mid (SS-13–SS-19).
 * Validation: minimum ≤ mid ≤ maximum, minimum &lt; maximum.
 */
@Entity
@Table(name = "salary_bands", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalaryBand {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "salary_band_id")
    private UUID salaryBandId;

    @Column(name = "salary_grade_id", nullable = false, updatable = false)
    private UUID salaryGradeId;

    @Column(name = "minimum_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maximumAmount;

    /** Optional; may be derived as (min+max)/2 if not entered (SS-14). */
    @Column(name = "mid_point", precision = 12, scale = 2)
    private BigDecimal midPoint;

    /** Default from structure if not overridden (SS-14). */
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "code", length = 50)
    private String code;

    /** Sequence for listing and progression when grade has multiple bands (SS-17). */
    @Column(name = "display_order")
    private Integer displayOrder;

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
