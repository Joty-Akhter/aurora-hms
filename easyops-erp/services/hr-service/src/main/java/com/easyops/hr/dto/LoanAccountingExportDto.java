package com.easyops.hr.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** PI-05: combined disbursement + repayment export for a period (JSON for integrations; CSV also available). */
@Value
@Builder
public class LoanAccountingExportDto {
    UUID organizationId;
    LocalDate periodFrom;
    LocalDate periodTo;
    String currency;
    List<LoanAccountingDisbursementLineDto> disbursements;
    List<LoanAccountingRepaymentLineDto> repayments;
    BigDecimal totalDisbursements;
    BigDecimal totalRepayments;
}
