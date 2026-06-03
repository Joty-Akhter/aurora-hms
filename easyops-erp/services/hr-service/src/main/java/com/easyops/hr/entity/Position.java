package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "positions", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Position {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "position_id")
    private UUID positionId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "department_id")
    private UUID departmentId;
    
    @Column(name = "level", length = 50)
    private String level;

    /** HR-MD-03: numeric rank for sorting / hierarchy (optional). */
    @Column(name = "hierarchy_rank")
    private Integer hierarchyRank;
    
    @Column(name = "salary_range_min", precision = 15, scale = 2)
    private BigDecimal salaryRangeMin;
    
    @Column(name = "salary_range_max", precision = 15, scale = 2)
    private BigDecimal salaryRangeMax;
    
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "BDT";

    /** Default salary structure for this position (SS-26). New hires/transfers may inherit. */
    @Column(name = "default_salary_structure_id")
    private UUID defaultSalaryStructureId;

    /** Default salary grade; must belong to default_salary_structure (SS-26, SS-27). */
    @Column(name = "default_salary_grade_id")
    private UUID defaultSalaryGradeId;

    /** Default salary band; must belong to default grade (SS-26, SS-27). */
    @Column(name = "default_salary_band_id")
    private UUID defaultSalaryBandId;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}

