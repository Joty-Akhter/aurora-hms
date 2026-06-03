package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticalValueEscalationRequest {
    
    @NotNull(message = "Escalated to user ID is required")
    private UUID escalatedToUserId;
    
    private String escalationReason;
}
