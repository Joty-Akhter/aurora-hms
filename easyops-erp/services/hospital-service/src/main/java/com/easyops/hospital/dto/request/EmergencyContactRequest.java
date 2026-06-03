package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PatientEmergencyContact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactRequest {
    
    @NotBlank(message = "Contact name is required")
    private String contactName;
    
    @NotNull(message = "Relationship is required")
    private PatientEmergencyContact.Relationship relationship;
    
    @NotBlank(message = "Primary phone is required")
    private String primaryPhone;
    
    private String secondaryPhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String email;
    private Boolean isPrimary;
}
