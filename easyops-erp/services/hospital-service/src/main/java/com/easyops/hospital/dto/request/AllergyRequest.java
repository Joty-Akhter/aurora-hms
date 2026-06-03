package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.Allergy;
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
public class AllergyRequest {
    
    @NotBlank(message = "Allergen name is required")
    private String allergenName;
    
    @NotNull(message = "Allergen type is required")
    private Allergy.AllergenType allergenType;
    
    private String allergenCode;
    private String reactionType;
    
    @NotNull(message = "Severity is required")
    private Allergy.Severity severity;
    
    private LocalDate onsetDate;
    private Allergy.Status status;
    private Allergy.VerificationStatus verificationStatus;
    private UUID documentedBy;
    private LocalDate documentedDate;
    private String notes;
}
