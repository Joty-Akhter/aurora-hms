package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PrescriptionRefillRequest;
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
public class RefillRequestRequest {
    
    @NotNull(message = "Prescription ID is required")
    private UUID prescriptionId;
    
    private PrescriptionRefillRequest.RequestSource requestSource;
    
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    private String pharmacyPhone;
    
    private Integer refillsRequested;
    
    private PrescriptionRefillRequest.UrgencyLevel urgencyLevel;
    
    private String notes;
}
