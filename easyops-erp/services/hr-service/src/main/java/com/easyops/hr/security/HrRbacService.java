package com.easyops.hr.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Aligns with {@code HR_VIEW} / {@code HR_MANAGE} ({@code hr}/{@code view|manage} in
 * {@code 011-rbac-module-permissions.sql}).
 */
@Service
public class HrRbacService {

    private static final String[][] HR_VIEW = {
            {"hr", "view"},
            {"hr", "manage"},
    };

    private static final String[][] HR_MANAGE = {
            {"hr", "manage"},
    };

    /** Optional narrow grants (019-hr-employee-self-service-permissions.sql) — typically paired with linked {@code Employee.userId}. */
    private static final String[][] HR_SELF_PAYSLIP = {
            {"hr_self", "payslip_view"},
    };

    private static final String[][] HR_SELF_SALARY_SUMMARY = {
            {"hr_self", "salary_summary_view"},
    };

    private static final String[][] LEAVE_SELF_SUBMIT = {
            {"leave_self", "submit"},
    };

    private static final String[][] PF_POLICY_MANAGE = {
            {"pf_policy", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] PF_COMPLIANCE_MANAGE = {
            {"pf_compliance", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] PF_APPROVAL_MANAGE = {
            {"pf_approval", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] PF_FILING_MANAGE = {
            {"pf_filing", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] PF_REMITTANCE_MANAGE = {
            {"pf_remittance", "manage"},
            {"hr", "manage"},
    };

    private static final String[][] PF_CORRECTION_MANAGE = {
            {"pf_correction", "manage"},
            {"hr", "manage"},
    };

    private final RbacPermissionClient rbac;

    public HrRbacService(RbacPermissionClient rbac) {
        this.rbac = rbac;
    }

    public void requireHrView(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HR_VIEW, "hr_view");
    }

    public void requireHrManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, HR_MANAGE, "hr_manage");
    }

    /** HR_VIEW seed pairs include {@code hr/manage}, so managers with only manage still match. */
    public boolean hasHrView(UUID actorUserId, UUID organizationId) {
        if (actorUserId == null) {
            return false;
        }
        return rbac.hasAnyResourceAction(actorUserId, organizationId, HR_VIEW);
    }

    public boolean hasHrManage(UUID actorUserId, UUID organizationId) {
        if (actorUserId == null) {
            return false;
        }
        return rbac.hasAnyResourceAction(actorUserId, organizationId, HR_MANAGE);
    }

    public boolean hasHrSelfPayslipView(UUID actorUserId, UUID organizationId) {
        if (actorUserId == null) {
            return false;
        }
        return rbac.hasAnyResourceAction(actorUserId, organizationId, HR_SELF_PAYSLIP);
    }

    public boolean hasHrSelfSalarySummaryView(UUID actorUserId, UUID organizationId) {
        if (actorUserId == null) {
            return false;
        }
        return rbac.hasAnyResourceAction(actorUserId, organizationId, HR_SELF_SALARY_SUMMARY);
    }

    public boolean hasLeaveSelfSubmit(UUID actorUserId, UUID organizationId) {
        if (actorUserId == null) {
            return false;
        }
        return rbac.hasAnyResourceAction(actorUserId, organizationId, LEAVE_SELF_SUBMIT);
    }

    public void requirePfPolicyManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PF_POLICY_MANAGE, "pf_policy_manage");
    }

    public void requirePfComplianceManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PF_COMPLIANCE_MANAGE, "pf_compliance_manage");
    }

    public void requirePfApprovalManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PF_APPROVAL_MANAGE, "pf_approval_manage");
    }

    public void requirePfFilingManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PF_FILING_MANAGE, "pf_filing_manage");
    }

    public void requirePfRemittanceManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PF_REMITTANCE_MANAGE, "pf_remittance_manage");
    }

    public void requirePfCorrectionManage(UUID actorUserId, UUID organizationId) {
        rbac.requireAnyResourceAction(actorUserId, organizationId, PF_CORRECTION_MANAGE, "pf_correction_manage");
    }
}
