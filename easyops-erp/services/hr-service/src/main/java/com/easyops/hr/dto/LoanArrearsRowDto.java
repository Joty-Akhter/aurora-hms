package com.easyops.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanArrearsRowDto {

    private UUID loanId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private String categoryName;
    private UUID installmentId;
    private Integer sequenceNumber;
    private LocalDate dueDate;
    private BigDecimal scheduledAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private long daysPastDue;
}
