package com.easyops.hr.service;

import com.easyops.hr.dto.DepartmentLeaveApproverRowDto;
import com.easyops.hr.entity.DepartmentLeaveApprover;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.repository.DepartmentLeaveApproverRepository;
import com.easyops.hr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveApprovalMatrixService {

    private static final String ACTIVE = "ACTIVE";

    private final DepartmentLeaveApproverRepository departmentLeaveApproverRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Ordered approvers for an employee's department, or [direct manager] when no matrix rows exist.
     */
    public List<UUID> resolveApproverChain(UUID organizationId, UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("employee_not_found"));
        if (!employee.getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("employee_org_mismatch");
        }
        UUID deptId = employee.getDepartmentId();
        if (deptId == null) {
            return managerFallbackChain(employee);
        }
        List<DepartmentLeaveApprover> rows =
                departmentLeaveApproverRepository.findByDepartmentIdOrderByStepOrderAsc(deptId);
        if (rows.isEmpty()) {
            return managerFallbackChain(employee);
        }
        return rows.stream().map(DepartmentLeaveApprover::getApproverEmployeeId).toList();
    }

    public boolean isInApproverChain(UUID organizationId, UUID leaveSubjectEmployeeId, UUID actorEmployeeId) {
        return resolveApproverChain(organizationId, leaveSubjectEmployeeId).contains(actorEmployeeId);
    }

    public List<DepartmentLeaveApproverRowDto> getRows(UUID organizationId, UUID departmentId) {
        validateDepartmentAccess(organizationId, departmentId);
        return departmentLeaveApproverRepository.findByDepartmentIdOrderByStepOrderAsc(departmentId).stream()
                .map(r -> DepartmentLeaveApproverRowDto.builder()
                        .stepOrder(r.getStepOrder())
                        .approverEmployeeId(r.getApproverEmployeeId())
                        .build())
                .toList();
    }

    @Transactional
    public List<DepartmentLeaveApproverRowDto> replaceDepartmentApprovers(
            UUID organizationId,
            UUID departmentId,
            List<DepartmentLeaveApproverRowDto> rows) {

        validateDepartmentAccess(organizationId, departmentId);

        if (rows == null || rows.isEmpty()) {
            departmentLeaveApproverRepository.deleteByDepartmentId(departmentId);
            return List.of();
        }

        List<DepartmentLeaveApproverRowDto> sorted = rows.stream()
                .sorted(Comparator.comparingInt(r -> r.getStepOrder() != null ? r.getStepOrder() : Integer.MAX_VALUE))
                .toList();

        Set<UUID> seenEmployees = new LinkedHashSet<>();
        List<DepartmentLeaveApprover> entities = new ArrayList<>();
        int expectedStep = 1;

        for (DepartmentLeaveApproverRowDto row : sorted) {
            if (row.getApproverEmployeeId() == null || row.getStepOrder() == null) {
                throw new IllegalArgumentException("department_leave_approver_row_invalid");
            }
            if (!Objects.equals(row.getStepOrder(), expectedStep)) {
                throw new IllegalArgumentException("department_leave_approver_steps_must_be_contiguous_from_one");
            }
            Employee approver = employeeRepository.findById(row.getApproverEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("approver_employee_not_found"));
            if (!approver.getOrganizationId().equals(organizationId)) {
                throw new IllegalArgumentException("approver_org_mismatch");
            }
            String status = approver.getEmploymentStatus();
            if (status != null && !ACTIVE.equalsIgnoreCase(status)) {
                throw new IllegalArgumentException("approver_must_be_active");
            }
            if (!seenEmployees.add(row.getApproverEmployeeId())) {
                throw new IllegalArgumentException("department_leave_approver_duplicate_employee");
            }
            entities.add(DepartmentLeaveApprover.builder()
                    .organizationId(organizationId)
                    .departmentId(departmentId)
                    .stepOrder(row.getStepOrder())
                    .approverEmployeeId(row.getApproverEmployeeId())
                    .createdAt(LocalDateTime.now())
                    .build());
            expectedStep++;
        }

        departmentLeaveApproverRepository.deleteByDepartmentId(departmentId);
        departmentLeaveApproverRepository.saveAll(entities);

        return entities.stream()
                .map(e -> DepartmentLeaveApproverRowDto.builder()
                        .stepOrder(e.getStepOrder())
                        .approverEmployeeId(e.getApproverEmployeeId())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateDepartmentAccess(UUID organizationId, UUID departmentId) {
        List<Employee> sample = employeeRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId);
        if (sample.isEmpty()) {
            // department may still be valid org-wise; allow configure before hires — skip strict FK check
            return;
        }
        if (!sample.get(0).getOrganizationId().equals(organizationId)) {
            throw new IllegalArgumentException("department_org_mismatch");
        }
    }

    private static List<UUID> managerFallbackChain(Employee employee) {
        UUID mgr = employee.getManagerId();
        if (mgr == null) {
            return List.of();
        }
        return List.of(mgr);
    }
}
