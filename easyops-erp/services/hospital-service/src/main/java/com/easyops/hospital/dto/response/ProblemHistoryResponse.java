package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.ProblemHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemHistoryResponse {
    
    private UUID historyId;
    private UUID problemId;
    
    // Change Information
    private ProblemHistory.ChangeType changeType;
    private UUID changedBy;
    private LocalDateTime changedDate;
    
    // Change Details
    private String previousValue;
    private String newValue;
    private String changeReason;
    
    // Additional Context
    private String fieldName;
    private String notes;
    
    // Audit Fields
    private LocalDateTime createdAt;
}
