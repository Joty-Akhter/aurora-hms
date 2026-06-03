package com.easyops.accounting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class JournalEntryRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    @NotNull(message = "Journal date is required")
    private LocalDate journalDate;
    
    private String journalType = "MANUAL";
    private String referenceNumber;
    private String sourceModule;
    private UUID sourceDocumentId;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotEmpty(message = "At least one journal line is required")
    private List<JournalLineRequest> lines;
}

