package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.Immunization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImmunizationRequest {
    
    @NotBlank(message = "Vaccine name is required")
    private String vaccineName;
    
    private String cvxCode;
    
    @NotNull(message = "Administration date is required")
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
}
