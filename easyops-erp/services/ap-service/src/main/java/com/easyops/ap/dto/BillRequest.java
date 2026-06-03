package com.easyops.ap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class BillRequest {
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotBlank(message = "Bill number is required")
    private String billNumber;
    
    @NotNull(message = "Bill date is required")
    private LocalDate billDate;
    
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
    
    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;
    
    @NotNull(message = "Period ID is required")
    private UUID periodId;
    
    private String currency = "USD";
    private String purchaseOrderRef;
    private String notes;
    
    @Valid
    private List<BillLineRequest> lines = new ArrayList<>();
    
    private UUID createdBy;
}

