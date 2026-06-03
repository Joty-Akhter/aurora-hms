package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRefillResponse {
    
    private UUID refillId;
    private UUID prescriptionId;
    private UUID refillRequestId;
    
    private Integer refillNumber;
    private LocalDate refillDate;
    
    private BigDecimal quantityDispensed;
    private String quantityUnit;
    
    private UUID pharmacyId;
    private String pharmacyName;
    private String pharmacyNpi;
    
    private UUID filledBy;
    private String filledByName;
    private LocalDateTime filledDate;
    
    private String notes;
    private String lotNumber;
    private LocalDate expirationDate;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
