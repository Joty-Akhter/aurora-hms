package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Immunization;
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
public class ImmunizationResponse {
    
    private UUID immunizationId;
    private UUID patientId;
    private String vaccineName;
    private String cvxCode;
    private LocalDate administrationDate;
    private String lotNumber;
    private String manufacturer;
    private Immunization.Route route;
    private String site;
    private String dose;
    private UUID administeredBy;
    private UUID administeredLocationId;
    private String reaction;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
