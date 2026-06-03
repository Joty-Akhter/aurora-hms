package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.SocialHistory;
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
public class SocialHistoryResponse {
    
    private UUID socialHistoryId;
    private UUID patientId;
    private SocialHistory.Category category;
    private SocialHistory.Status status;
    private String frequency;
    private String quantity;
    private Integer durationYears;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private LocalDate documentedDate;
    private UUID documentedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
