package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_categories", schema = "hr",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "code"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "code", nullable = false, length = 50, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 30)
    private LoanCategoryType categoryType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "max_principal_amount", precision = 15, scale = 2)
    private BigDecimal maxPrincipalAmount;

    @Column(name = "max_tenure_months")
    private Integer maxTenureMonths;

    /** LC-04: interest model for category; disbursement builds schedule via {@code LoanScheduleBuilder}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_method", nullable = false, length = 30)
    private LoanInterestMethod interestMethod = LoanInterestMethod.NONE;

    @Column(name = "flat_annual_rate_percent", precision = 9, scale = 4)
    private BigDecimal flatAnnualRatePercent;

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
