package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.Allergy;
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
public class AllergyResponse {
    
    private UUID allergyId;
    private UUID patientId;
    private String allergenName;
    private Allergy.AllergenType allergenType;
    private String allergenCode;
    private String reactionType;
    private Allergy.Severity severity;
    private LocalDate onsetDate;
    private Allergy.Status status;
    private Allergy.VerificationStatus verificationStatus;
    private UUID documentedBy;
    private LocalDate documentedDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
