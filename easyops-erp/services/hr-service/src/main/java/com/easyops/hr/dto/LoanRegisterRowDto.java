package com.easyops.hr.dto;

import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.LoanCategoryType;
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
public class LoanRegisterRowDto {

    private UUID loanId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private UUID categoryId;
    private String categoryName;
    private LoanCategoryType categoryType;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private String currency;
    private EmployeeLoanStatus status;
    private LocalDate disbursementDate;
    private BigDecimal settlementShortfallAmount;
    private LocalDate separationEffectiveDate;
}
