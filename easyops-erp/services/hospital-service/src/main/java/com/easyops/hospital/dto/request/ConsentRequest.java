package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.PatientConsent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentRequest {
    
    @NotNull(message = "Consent type is required")
    private PatientConsent.ConsentType consentType;
    
    @NotNull(message = "Consent status is required")
    private PatientConsent.ConsentStatus consentStatus;
    
    @NotNull(message = "Consent date is required")
    private LocalDate consentDate;
    
    private String signature;
    private LocalDate expiresDate;
    private String notes;
}
