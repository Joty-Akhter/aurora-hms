package com.easyops.ap.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgingReportResponse {
    
    private UUID billId;
    private String billNumber;
    private LocalDate billDate;
    private LocalDate dueDate;
    private UUID vendorId;
    private String vendorName;
    private BigDecimal totalAmount;
    private BigDecimal balanceDue;
    private Integer daysOverdue;
    private BigDecimal current;
    private BigDecimal days1To30;
    private BigDecimal days31To60;
    private BigDecimal days61To90;
    private BigDecimal days90Plus;
}
