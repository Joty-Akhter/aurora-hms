package com.easyops.hr.security;

import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.LeaveRequest;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.service.LeaveApprovalMatrixService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * HMS Phase A: linked employees use narrow APIs without {@code HR_VIEW}/{@code HR_MANAGE};
 * department leave matrix + staged workflow for approvers ({@link LeaveApprovalMatrixService}).
 */
@Service
@RequiredArgsConstructor
public class HrEmployeeSelfAccessService {

    private final EmployeeRepository employeeRepository;
    private final HrRbacService hrRbac;
    private final LeaveApprovalMatrixService leaveApprovalMatrixService;

    public void requireLeaveTypesReadable(UUID actorUserId, UUID organizationId) {
        if (hrRbac.hasHrView(actorUserId, organizationId)) {
            return;
        }
        if (employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId).isPresent()) {
            return;
        }
        if (hrRbac.hasLeaveSelfSubmit(actorUserId, organizationId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "hr_view_or_employee_profile_required");
    }

    public void requireLeaveBalanceReadable(UUID actorUserId, UUID organizationId, UUID employeeId) {
        if (hrRbac.hasHrView(actorUserId, organizationId)) {
            return;
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        hrRbac.hasLeaveSelfSubmit(actorUserId, organizationId)
                                ? "leave_balance_requires_linked_profile"
                                : "hr_view_or_own_leave_balance"));
        if (!self.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_balance_scope_denied");
        }
    }

    public void requireLeaveRequestSubmit(UUID actorUserId, UUID organizationId, UUID targetEmployeeId) {
        if (hrRbac.hasHrManage(actorUserId, organizationId)) {
            return;
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "hr_manage_or_own_employee_required"));
        if (self.getEmployeeId().equals(targetEmployeeId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_submit_scope_denied");
    }

    /**
     * Org-wide queue requires HR visibility; filtered list for own employeeId allowed for self-service.
     */
    public void requireLeaveRequestList(UUID actorUserId, UUID organizationId, UUID employeeIdFilter) {
        if (employeeIdFilter == null) {
            hrRbac.requireHrView(actorUserId, organizationId);
            return;
        }
        if (hrRbac.hasHrView(actorUserId, organizationId)) {
            return;
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "hr_view_or_employee_filter_required"));
        if (self.getEmployeeId().equals(employeeIdFilter)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_list_scope_denied");
    }

    /** Pending queue for a named approver employee (matrix current step). */
    public void requirePendingApproverQueue(UUID actorUserId, UUID organizationId, UUID approverEmployeeIdFilter) {
        if (hrRbac.hasHrView(actorUserId, organizationId)) {
            return;
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "pending_queue_requires_hr_or_linked_employee"));
        if (!self.getEmployeeId().equals(approverEmployeeIdFilter)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "pending_queue_scope_denied");
        }
    }

    public void requireLeaveRequestReadable(UUID actorUserId, UUID organizationId, LeaveRequest request) {
        if (hrRbac.hasHrView(actorUserId, organizationId)) {
            return;
        }
        Employee subject = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "employee_not_found"));
        if (!subject.getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_request_org_mismatch");
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "hr_view_or_leave_owner"));
        if (self.getEmployeeId().equals(subject.getEmployeeId())) {
            return;
        }
        UUID mgrId = subject.getManagerId();
        if (mgrId != null && mgrId.equals(self.getEmployeeId())) {
            return;
        }
        if (leaveApprovalMatrixService.isInApproverChain(organizationId, subject.getEmployeeId(), self.getEmployeeId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_request_read_denied");
    }

    /**
     * HR manage always allowed; otherwise the {@code approvedBy} employee must be the actor and the expected approver
     * for the current {@link LeaveRequest#getPendingStepIndex()} (department matrix or manager fallback).
     */
    public void requireLeaveApprove(UUID actorUserId, UUID organizationId, LeaveRequest request,
                                    UUID approvedByEmployeeId, boolean actorHasHrManage) {
        if (actorHasHrManage) {
            return;
        }
        Employee actorEmp = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_approve_actor_not_linked"));
        if (!actorEmp.getEmployeeId().equals(approvedByEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_approve_actor_must_match_approved_by");
        }
        List<UUID> chain = leaveApprovalMatrixService.resolveApproverChain(organizationId, request.getEmployeeId());
        int step = request.getPendingStepIndex() != null ? request.getPendingStepIndex() : 1;
        if (chain.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_approve_no_chain");
        }
        if (step < 1 || step > chain.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leave_approve_invalid_step");
        }
        UUID expected = chain.get(step - 1);
        if (!expected.equals(approvedByEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "leave_wrong_approver_for_step");
        }
    }

    public void requireLeaveReject(UUID actorUserId, UUID organizationId, LeaveRequest request,
                                   UUID rejectedByEmployeeId, boolean actorHasHrManage) {
        requireLeaveApprove(actorUserId, organizationId, request, rejectedByEmployeeId, actorHasHrManage);
    }

    /**
     * Own payslip / salary summary (AC-10): {@code HR_VIEW} for HR staff; otherwise linked profile must match
     * {@code employeeId}. Database seeds {@code HR_SELF_PAYSLIP_VIEW} / {@code HR_SELF_SALARY_SUMMARY_VIEW} document
     * least-privilege roles for gateways and audits; access here is driven by the linked {@code Employee.userId}.
     */
    public void requireOwnEmployeeOrHrView(UUID actorUserId, UUID organizationId, UUID employeeId) {
        if (hrRbac.hasHrView(actorUserId, organizationId)) {
            return;
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "hr_view_or_own_employee_required"));
        if (!self.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "self_service_scope_denied");
        }
    }

    /** Own EPF withdrawal/nomination mutations: HR manage, or linked employee matching {@code employeeId}. */
    public void requireOwnEmployeeOrHrManage(UUID actorUserId, UUID organizationId, UUID employeeId) {
        if (hrRbac.hasHrManage(actorUserId, organizationId)) {
            return;
        }
        Employee self = employeeRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "hr_manage_or_own_employee_required"));
        if (!self.getEmployeeId().equals(employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "self_service_scope_denied");
        }
    }
}
