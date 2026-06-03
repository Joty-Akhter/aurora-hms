package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.MedicationReconciliation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReconciliationResponse {
    
    private UUID reconciliationId;
    private UUID patientId;
    private UUID encounterId;
    private LocalDate reconciliationDate;
    private MedicationReconciliation.ReconciliationType reconciliationType;
    private MedicationReconciliation.ReconciliationStatus reconciliationStatus;
    private UUID performedBy;
    private String performedByName;
    private UUID verifiedBy;
    private String verifiedByName;
    private LocalDateTime verificationDate;
    private Integer totalMedicationsBefore;
    private Integer totalMedicationsAfter;
    private Integer medicationsAdded;
    private Integer medicationsModified;
    private Integer medicationsDiscontinued;
    private Integer medicationsUnchanged;
    private String notes;
    private String reconciliationSummary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private List<MedicationReconciliationSourceResponse> sources;
    private List<MedicationReconciliationComparisonResponse> comparisons;
}
