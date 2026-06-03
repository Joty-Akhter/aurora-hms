package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRefillRequest {
    
    @NotNull(message = "Prescription ID is required")
    private UUID prescriptionId;
    
    private UUID refillRequestId;
    
    @NotNull(message = "Refill date is required")
    private LocalDate refillDate;
    
    private BigDecimal quantityDispensed;
    private String quantityUnit;
    
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    
    private UUID filledBy;
    private String filledByName;
    
    private String notes;
    private String lotNumber;
    private LocalDate expirationDate;
}
