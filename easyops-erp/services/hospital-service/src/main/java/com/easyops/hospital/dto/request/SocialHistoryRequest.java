package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.SocialHistory;
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
public class SocialHistoryRequest {
    
    @NotNull(message = "Category is required")
    private SocialHistory.Category category;
    
    @NotNull(message = "Status is required")
    private SocialHistory.Status status;
    
    private String frequency;
    private String quantity;
    private Integer durationYears;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private LocalDate documentedDate;
    private UUID documentedBy;
}
