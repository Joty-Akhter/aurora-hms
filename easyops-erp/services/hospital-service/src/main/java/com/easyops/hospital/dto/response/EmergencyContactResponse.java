package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PatientEmergencyContact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContactResponse {
    
    private UUID contactId;
    private UUID patientId;
    private String contactName;
    private PatientEmergencyContact.Relationship relationship;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
