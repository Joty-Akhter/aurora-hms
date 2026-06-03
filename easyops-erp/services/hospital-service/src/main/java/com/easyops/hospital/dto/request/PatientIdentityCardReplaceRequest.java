package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatientIdentityCardReplaceRequest {
    @NotBlank(message = "reason is required")
    private String reason;
}
