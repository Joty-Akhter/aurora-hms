package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriorAuthorizationRequest {
    
    @NotNull(message = "Prescription ID is required")
    private UUID prescriptionId;
    
    private UUID formularyCheckId; // Optional - link to formulary check
    
    private UUID insuranceId; // Optional - if not provided, uses prescription insurance
    
    @NotNull(message = "Clinical justification is required")
    private String clinicalJustification;
    
    private String supportingDocumentation; // References to supporting documents
    
    private String notes;
}
