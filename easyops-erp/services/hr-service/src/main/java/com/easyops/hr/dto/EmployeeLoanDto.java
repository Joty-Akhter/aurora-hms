package com.easyops.hr.dto;

import com.easyops.hr.entity.EmployeeLoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLoanDto {

    private UUID loanId;
    private UUID organizationId;
    private UUID employeeId;
    private UUID loanApplicationId;
    private UUID categoryId;
    private BigDecimal principalAmount;
    private String currency;
    private BigDecimal outstandingBalance;
    private Integer tenureMonths;
    private EmployeeLoanStatus status;
    private LocalDate disbursementDate;
    private BigDecimal disbursedAmount;
    private BigDecimal settlementShortfallAmount;
    private LocalDateTime settlementStartedAt;
    private LocalDate separationEffectiveDate;
    private String settlementWriteOffPath;
    private String legalCaseReference;
    private String writeOffNotes;
    private String legalWorkflowStatus;
    private LocalDateTime legalWorkflowUpdatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LoanInstallmentDto> installments;
}
