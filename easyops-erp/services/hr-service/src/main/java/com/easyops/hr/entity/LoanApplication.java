package com.easyops.hr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
@Table(name = "loan_applications", schema = "hr")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id")
    private UUID applicationId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "employee_id", nullable = false, updatable = false)
    private UUID employeeId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_tenure_months", nullable = false)
    private Integer requestedTenureMonths;

    @Column(name = "purpose_notes", length = 2000)
    private String purposeNotes;

    /** JSON array of attachment reference strings (AL-01). */
    @Column(name = "attachment_references", columnDefinition = "TEXT")
    private String attachmentReferencesJson;

    @Column(name = "delegated_to_user_id")
    private UUID delegatedToUserId;

    @Column(name = "clarification_message", length = 2000)
    private String clarificationMessage;

    @Column(name = "clarification_requested_by_user_id")
    private UUID clarificationRequestedByUserId;

    @Column(name = "limit_override_reason", length = 2000)
    private String limitOverrideReason;

    @Column(name = "limit_override_approved_by_user_id")
    private UUID limitOverrideApprovedByUserId;

    @Column(name = "limit_override_expires_at")
    private LocalDate limitOverrideExpiresAt;

    @Column(name = "facility_override_reason", length = 2000)
    private String facilityOverrideReason;

    @Column(name = "facility_override_approved_by_user_id")
    private UUID facilityOverrideApprovedByUserId;

    @Column(name = "facility_override_expires_at")
    private LocalDate facilityOverrideExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LoanApplicationStatus status = LoanApplicationStatus.DRAFT;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

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
