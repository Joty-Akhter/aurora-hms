package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanInstallmentStatus;
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
public class LoanInstallmentDto {

    private UUID installmentId;
    private Integer sequenceNumber;
    private LocalDate dueDate;
    private BigDecimal scheduledAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private LoanInstallmentStatus status;
    /** RP-01: populated when status is SKIPPED. */
    private String skipReason;
    /** True if due date is before today and not fully paid (arrears / PI-03 visibility). */
    private boolean overdue;
}
