package com.easyops.accounting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
public class JournalLineRequest {
    
    @NotNull(message = "Account ID is required")
    private UUID accountId;
    
    private BigDecimal debitAmount = BigDecimal.ZERO;
    private BigDecimal creditAmount = BigDecimal.ZERO;
    private String description;
    private UUID departmentId;
    private UUID costCenterId;
    private Map<String, Object> tags;
}

