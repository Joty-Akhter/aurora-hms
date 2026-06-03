package com.easyops.hospital.dto.response;

import com.easyops.hospital.entity.PrescriptionHistory;
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
public class PrescriptionHistoryResponse {
    
    private UUID historyId;
    private UUID prescriptionId;
    
    private PrescriptionHistory.ChangeType changeType;
    private UUID changedBy;
    private LocalDateTime changedDate;
    
    private String previousValue;
    private String newValue;
    private String changeReason;
    
    private String fieldName;
    private String notes;
    
    private LocalDateTime createdAt;
}
