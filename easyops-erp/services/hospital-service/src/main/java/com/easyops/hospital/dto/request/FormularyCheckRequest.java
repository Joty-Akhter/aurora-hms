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
public class FormularyCheckRequest {
    
    @NotNull(message = "Patient ID is required")
    private UUID patientId;
    
    // Set by the controller from the URL path variable — no @NotNull needed here
    private UUID prescriptionId;
    
    private UUID insuranceId; // Optional - if not provided, uses primary insurance
    
    private String medicationCode; // Optional - if not provided, uses prescription medication code
    
    private String medicationName; // Optional - if not provided, uses prescription medication name
    
    private Boolean includeAlternatives = true; // Whether to include formulary alternatives
    
    private Boolean estimateCosts = true; // Whether to estimate patient costs
}
