package com.easyops.hospital.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefillApprovalRequest {
    
    private String approvalNotes;
    private Integer refillsApproved; // If different from requested
}
