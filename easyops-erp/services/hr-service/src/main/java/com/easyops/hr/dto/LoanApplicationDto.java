package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanApplicationStatus;
import com.easyops.hr.entity.LoanCategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationDto {

    private UUID applicationId;
    private UUID organizationId;
    private UUID employeeId;
    private UUID categoryId;
    /** From loan category (LC-03): term loan vs salary advance. */
    private LoanCategoryType categoryType;
    private BigDecimal requestedAmount;
    private Integer requestedTenureMonths;
    private String purposeNotes;
    private List<String> attachmentReferences;
    private UUID delegatedToUserId;
    private String clarificationMessage;
    private UUID clarificationRequestedByUserId;
    /** AD-02: when amount exceeds policy cap. */
    private String limitOverrideReason;
    private UUID limitOverrideApprovedByUserId;
    private LocalDate limitOverrideExpiresAt;
    /** LC-05: when second facility would otherwise be blocked. */
    private String facilityOverrideReason;
    private UUID facilityOverrideApprovedByUserId;
    private LocalDate facilityOverrideExpiresAt;
    private LoanApplicationStatus status;
    private LocalDate applicationDate;
    private LocalDateTime submittedAt;
    private LocalDateTime decidedAt;
    private UUID decidedByUserId;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- AL-02: zero-interest v1 preview (equal principal / month; total recovery = principal) ---
    private BigDecimal recommendedInstallmentAmount;
    private BigDecimal totalScheduledRecovery;
    /** Human-readable model note for client acknowledgment. */
    private String installmentPreviewNote;
}
