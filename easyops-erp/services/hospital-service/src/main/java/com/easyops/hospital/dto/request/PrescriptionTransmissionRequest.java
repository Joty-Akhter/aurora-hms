package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PrescriptionTransmission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionTransmissionRequest {
    
    @NotNull(message = "Prescription ID is required")
    private UUID prescriptionId;
    
    // Network Selection
    private UUID networkId; // Specific network to use (optional, will use default if not provided)
    private String networkName; // Network name (optional)
    
    // Pharmacy Information (if not already in prescription)
    private UUID pharmacyId;
    private String pharmacyNpi;
    private String pharmacyName;
    private String pharmacyAddressLine1;
    private String pharmacyAddressLine2;
    private String pharmacyCity;
    private String pharmacyState;
    private String pharmacyZip;
    private String pharmacyPhone;
    private String pharmacyFax;
    
    // Transmission Options
    private PrescriptionTransmission.TransmissionMethod transmissionMethod;
    private Boolean requireConfirmation; // Require confirmation from pharmacy
    private Boolean allowSubstitution; // Allow generic substitution
    
    // Override Flags
    private Boolean overrideInteractions;
    private Boolean overrideAllergies;
    private Boolean overridePdmpCheck;
    private String overrideReason;
}
