package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionRefillRequest;
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
public class PrescriptionRefillRequestResponse {
    
    private UUID refillRequestId;
    private UUID prescriptionId;
    private String prescriptionNumber;
    private String medicationName;
    
    private PrescriptionRefillRequest.RequestSource requestSource;
    private LocalDateTime requestDate;
    private UUID requestedBy;
    private String requestedByName;
    
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    private String pharmacyPhone;
    
    private Integer refillsRequested;
    private Integer refillsRemaining;
    private LocalDate lastFillDate;
    private Integer daysSinceLastFill;
    
    private PrescriptionRefillRequest.RequestStatus requestStatus;
    
    private UUID approvedBy;
    private LocalDateTime approvedDate;
    private String approvalNotes;
    
    private UUID deniedBy;
    private LocalDateTime deniedDate;
    private String denialReason;
    
    private UUID modifiedBy;
    private LocalDateTime modifiedDate;
    private String modificationNotes;
    private Integer originalRefillsRequested;
    
    private String notes;
    private PrescriptionRefillRequest.UrgencyLevel urgencyLevel;
    
    private Boolean wasAutoApproved;
    private UUID autoApprovalRuleId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Related Data
    private List<PrescriptionRefillResponse> refills;
    private Integer refillCount;
}
