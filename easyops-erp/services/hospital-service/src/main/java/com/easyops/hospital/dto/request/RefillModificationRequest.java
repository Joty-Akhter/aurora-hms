package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefillModificationRequest {
    
    @NotNull(message = "Refills approved is required")
    private Integer refillsApproved;
    
    private String modificationNotes;
}
