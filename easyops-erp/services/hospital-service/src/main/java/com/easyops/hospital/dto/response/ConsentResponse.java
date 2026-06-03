package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PatientConsent;
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
public class ConsentResponse {
    
    private UUID consentId;
    private UUID patientId;
    private PatientConsent.ConsentType consentType;
    private PatientConsent.ConsentStatus consentStatus;
    private LocalDate consentDate;
    private String signature;
    private LocalDate expiresDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
