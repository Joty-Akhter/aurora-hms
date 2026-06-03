package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PDMPQueryResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PDMPQueryResponse {
    
    private UUID queryResultId;
    private UUID prescriptionId;
    private UUID patientId;
    
    private LocalDateTime queryDate;
    private String queryState;
    private PDMPQueryResult.QueryType queryType;
    
    private UUID queryingProviderId;
    private String queryingProviderNpi;
    private String queryingProviderName;
    private String deaNumber;
    
    private PDMPQueryResult.QueryStatus queryStatus;
    private Boolean querySuccess;
    private String errorMessage;
    
    // Summary Information
    private Integer totalPrescriptions;
    private Integer totalPharmacies;
    private Integer totalPrescribers;
    private LocalDate dateRangeStart;
    private LocalDate dateRangeEnd;
    private Boolean hasControlledSubstances;
    private Integer riskScore;
    private PDMPQueryResult.RiskLevel riskLevel;
    
    // Detailed Results
    private List<PrescriptionHistoryItem> prescriptionHistory;
    private List<PharmacyInfo> pharmacyList;
    private List<PrescriberInfo> prescriberList;
    
    // Flags and Warnings
    private Boolean hasDuplicatePrescriptions;
    private Boolean hasOverlappingPrescriptions;
    private Boolean hasEarlyRefills;
    private Boolean hasMultiplePrescribers;
    private Boolean hasMultiplePharmacies;
    private String warnings;
    
    // Documentation
    private String queryReason;
    private String clinicalNotes;
    private String actionTaken;
    
    // External System Information
    private String pdmpSystemName;
    private String pdmpSystemId;
    private String pdmpQueryId;
    private String pdmpResponseId;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    
    // Nested classes for detailed results
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionHistoryItem {
        private String prescriptionId;
        private String medicationName;
        private String medicationCode;
        private String dosageStrength;
        private String dosageUnit;
        private String quantity;
        private String schedule;
        private LocalDate prescribedDate;
        private LocalDate filledDate;
        private String prescriberName;
        private String prescriberNpi;
        private String pharmacyName;
        private String pharmacyNpi;
        private String pharmacyAddress;
        private Integer daysSupply;
        private Integer refillsAuthorized;
        private Integer refillsRemaining;
        private String status;
        private Map<String, Object> additionalInfo;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PharmacyInfo {
        private String pharmacyName;
        private String pharmacyNpi;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String zip;
        private String phone;
        private Integer prescriptionCount;
        private LocalDate firstPrescriptionDate;
        private LocalDate lastPrescriptionDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriberInfo {
        private String prescriberName;
        private String prescriberNpi;
        private String deaNumber;
        private String specialty;
        private String address;
        private Integer prescriptionCount;
        private LocalDate firstPrescriptionDate;
        private LocalDate lastPrescriptionDate;
    }
}
