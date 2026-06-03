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

@Entity
@Table(name = "salary_structures", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "salary_structure_id")
    private UUID salaryStructureId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /** Unique per organization; immutable after creation (updatable = false). Required for new structures; nullable for legacy data. */
    @Column(name = "code", nullable = true, length = 50, updatable = false)
    private String code;

    @Column(name = "structure_name", nullable = false, length = 200)
    private String structureName;

    @Column(name = "description", length = 1000)
    private String description;

    /** Pay frequency (e.g. Monthly, Bi-Weekly). SS-03. */
    @Column(name = "pay_frequency", length = 50)
    private String payFrequency = "monthly";

    /** Default currency for all grade/band amounts in this structure. SS-04. */
    @Column(name = "currency", length = 3)
    private String currency = "BDT";

    /** Default structure for the organization (or org + pay frequency). Only one per org should be true. SS-06. */
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
    
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
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

