package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.MedicationReconciliation;
import com.easyops.hospital.entity.MedicationReconciliationSource;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReconciliationRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    private UUID encounterId;
    
    @NotNull(message = "Reconciliation date is required")
    private LocalDate reconciliationDate;
    
    @NotNull(message = "Reconciliation type is required")
    private MedicationReconciliation.ReconciliationType reconciliationType;
    
    private String notes;
    
    // Source medications to compare
    private List<MedicationSourceRequest> sources;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationSourceRequest {
        private MedicationReconciliationSource.SourceType sourceType;
        private String sourceName;
        private String sourceDescription;
        private Map<String, Object> sourceData;
        private LocalDate sourceDate;
        private String sourceProviderName;
        private String sourceFacilityName;
        private String sourceContactInfo;
        private MedicationReconciliationSource.ImportMethod importMethod;
        private List<MedicationItemRequest> medications;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationItemRequest {
        private String medicationName;
        private String genericName;
        private String medicationCode;
        private String medicationCodeType;
        private String dosageStrength;
        private String dosageUnit;
        private String dosageForm;
        private String route;
        private String frequency;
        private String instructions;
        private String indication;
        private String startDate;
        private String endDate;
    }
}
