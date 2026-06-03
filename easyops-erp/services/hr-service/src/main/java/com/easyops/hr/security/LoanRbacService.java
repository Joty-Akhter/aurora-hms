package com.easyops.hr.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Granular RBAC for employee loans (NF-03). Falls back to legacy {@code hr} view/manage so existing roles keep access.
 */
@Service
public class LoanRbacService {

    private static final String[][] HR_VIEW_FALLBACK = {
            {"hr", "view"},
            {"hr", "manage"},
    };

    private static final String[][] HR_MANAGE_FALLBACK = {
            {"hr", "manage"},
    };

    /** hr_loans view OR legacy HR view */
    private static final String[][] LOANS_VIEW = {
            {"hr_loans", "view"},
            {"hr_loans", "manage"},
            {"hr", "view"},
            {"hr", "manage"},
    };

    private static final String[][] LOANS_APPLY = {
            {"hr_loans", "apply"},
            {"hr_loans", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] HR_APPROVE = {
            {"hr_loans", "hr_approve"},
            {"hr_loans", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] FINANCE_APPROVE = {
            {"hr_loans", "finance_approve"},
            {"hr_loans", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] PAYROLL_RECOVERIES = {
            {"hr_loans", "payroll_recoveries"},
            {"hr_loans", "manage"},
            {"hr", "view"},
            {"hr", "manage"},
    };

    private static final String[][] LOANS_MANAGE = {
            {"hr_loans", "manage"},
            {"hr", "manage"},
    };

    private final RbacPermissionClient rbac;

    public LoanRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireHrView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HR_VIEW_FALLBACK, "hr_view");
    }

    public void requireHrManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HR_MANAGE_FALLBACK, "hr_manage");
    }

    public void requireLoansView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, LOANS_VIEW, "hr_loans_view");
    }

    public void requireLoansApply(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, LOANS_APPLY, "hr_loans_apply");
    }

    public void requireHrLoanHrApprove(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HR_APPROVE, "hr_loans_hr_approve");
    }

    public void requireHrLoanFinanceApprove(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, FINANCE_APPROVE, "hr_loans_finance_approve");
    }

    public void requirePayrollRecoveriesRead(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PAYROLL_RECOVERIES, "hr_loans_payroll_recoveries");
    }

    /** Disbursement, settlement, manual repayments, payroll confirm. */
    public void requireLoansManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, LOANS_MANAGE, "hr_loans_manage");
    }

    /** HR or Finance approver for clarification / reject while in workflow. */
    private static final String[][] LOAN_WORKFLOW_ACTOR = {
            {"hr_loans", "hr_approve"},
            {"hr_loans", "finance_approve"},
            {"hr_loans", "manage"},
            {"hr", "manage"},
    };

    public void requireLoanWorkflowParticipant(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, LOAN_WORKFLOW_ACTOR, "hr_loans_workflow");
    }
}
