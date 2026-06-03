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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "epf_filings", schema = "hr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EpfFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "filing_id")
    private UUID filingId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "filing_month", nullable = false)
    private Integer filingMonth;

    @Column(name = "filing_year", nullable = false)
    private Integer filingYear;

    @Column(name = "filing_type", nullable = false, length = 30)
    private String filingType; // ECR, CHALLAN

    @Column(name = "filing_status", nullable = false, length = 30)
    @Builder.Default
    private String filingStatus = "draft"; // draft, generated, submitted, verified, rejected

    @Column(name = "artifact_format", length = 20)
    private String artifactFormat; // CSV, TEXT

    @Column(name = "artifact_content", columnDefinition = "TEXT")
    private String artifactContent;

    @Column(name = "artifact_checksum", length = 64)
    private String artifactChecksum;

    @Column(name = "submission_reference", length = 120)
    private String submissionReference;

    @Column(name = "submission_date")
    private LocalDate submissionDate;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Column(name = "compliance_record_id")
    private UUID complianceRecordId;

    @Column(name = "employee_contribution_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal employeeContributionTotal = BigDecimal.ZERO;

    @Column(name = "employer_contribution_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal employerContributionTotal = BigDecimal.ZERO;

    @Column(name = "employer_pension_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal employerPensionTotal = BigDecimal.ZERO;

    @Column(name = "employer_edli_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal employerEdliTotal = BigDecimal.ZERO;

    @Column(name = "employer_admin_charge_total", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal employerAdminChargeTotal = BigDecimal.ZERO;

    @Column(name = "total_liability_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalLiabilityAmount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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
