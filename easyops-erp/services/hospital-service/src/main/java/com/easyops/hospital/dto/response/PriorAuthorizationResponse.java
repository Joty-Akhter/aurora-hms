package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PriorAuthorization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorAuthorizationResponse {
    
    private UUID priorAuthId;
    private UUID prescriptionId;
    private UUID formularyCheckId;
    private UUID insuranceId;
    private String insuranceCompanyName;
    private String policyNumber;
    private String medicationCode;
    private String medicationName;
    private String priorAuthNumber;
    private LocalDate requestDate;
    private PriorAuthorization.PriorAuthStatus status;
    private LocalDate submittedDate;
    private LocalDate approvedDate;
    private LocalDate deniedDate;
    private LocalDate expirationDate;
    private String denialReason;
    private String clinicalJustification;
    private String supportingDocumentation;
    private UUID requestedBy;
    private UUID reviewedBy;
    private String pbmName;
    private String pbmRequestId;
    private String pbmResponseId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
