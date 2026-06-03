package com.easyops.ap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BillLineRequest {
    @NotNull(message = "Line number is required")
    private Integer lineNumber;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private BigDecimal quantity = BigDecimal.ONE;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private BigDecimal taxPercent = BigDecimal.ZERO;
    private UUID accountId;
}

