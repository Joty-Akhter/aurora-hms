package com.easyops.ar.dto;

import com.easyops.ar.entity.ARCreditNote;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditNoteRequest {
    
    @NotNull
    private UUID organizationId;
    
    @NotNull
    @Size(max = 50)
    private String creditNoteNumber;
    
    @NotNull
    private LocalDate creditNoteDate;
    
    @NotNull
    private UUID customerId;
    
    private UUID invoiceId;  // Optional: if credit note is for a specific invoice
    
    private ARCreditNote.CreditNoteReason reason;
    
    @Size(max = 1000)
    private String notes;
    
    @Valid
    @NotNull
    @Size(min = 1)
    private List<CreditNoteLineRequest> lines = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditNoteLineRequest {
        
        @NotNull
        private Integer lineNumber;
        
        @Size(max = 500)
        private String description;
        
        @NotNull
        private BigDecimal quantity;
        
        @NotNull
        private BigDecimal unitPrice;
        
        private BigDecimal discountPercent;
        
        private BigDecimal taxPercent;
        
        @NotNull
        private UUID accountId;
    }
}

