package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * RE-04: loan account audit rows plus originating application workflow actions (when linked).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanCombinedAuditDto {

    private UUID loanId;
    private UUID loanApplicationId;
    /** Principal, schedule, repayments, settlement (loan_audit_log). */
    private List<LoanAuditLog> loanAuditLogs;
    /** Submit, HR/Finance approval, clarification, etc. (loan_application_action). */
    private List<LoanApplicationActionDto> applicationWorkflowActions;
}
