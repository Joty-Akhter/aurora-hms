package com.easyops.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class SalesOrderRequest {
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    private LocalDate orderDate = LocalDate.now();
    private LocalDate expectedDeliveryDate;
    
    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    private String customerName;
    private String customerEmail;
    private String contactPerson;
    private String billingAddress;
    private String shippingAddress;
    
    private BigDecimal discountPercent = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
    
    private String notes;
    private String termsAndConditions;
    
    private UUID salesPersonId;
    private String salesPersonName;
    
    private UUID quotationId; // If converting from quotation
    
    @NotEmpty(message = "Sales order must have at least one line item")
    @Valid
    private List<SalesOrderLineRequest> lines = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesOrderLineRequest {
        private Integer lineNumber;
        private UUID productId;
        private String productCode;
        
        @NotNull(message = "Product name is required")
        private String productName;
        
        private String description;
        
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        
        private String unitOfMeasure;
        
        @NotNull(message = "Unit price is required")
        private BigDecimal unitPrice;
        
        private BigDecimal discountPercent = BigDecimal.ZERO;
        private BigDecimal taxPercent = BigDecimal.ZERO;
        
        private UUID revenueAccountId;
    }
}

