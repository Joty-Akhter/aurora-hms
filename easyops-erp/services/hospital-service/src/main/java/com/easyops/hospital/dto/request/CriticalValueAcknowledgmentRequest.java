package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalValueAcknowledgmentRequest {
    
    @NotBlank(message = "Provider response is required")
    private String providerResponse;
    
    private String acknowledgmentNotes;
}
