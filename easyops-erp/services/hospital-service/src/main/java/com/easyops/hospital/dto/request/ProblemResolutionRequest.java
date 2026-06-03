package com.easyops.hospital.dto.request;

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
public class ProblemResolutionRequest {
    
    @NotNull(message = "Resolution date is required")
    private LocalDate resolutionDate;
    
    private String resolutionNotes;
}
