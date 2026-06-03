package com.easyops.ar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgingReportResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private UUID invoiceId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private UUID customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal balanceDue;
    private Integer daysOverdue;
    private BigDecimal current;
    private BigDecimal days1To30;
    private BigDecimal days31To60;
    private BigDecimal days61To90;
    private BigDecimal days90Plus;
}

