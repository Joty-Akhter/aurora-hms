package com.easyops.hr.controller;

import com.easyops.hr.dto.DepartmentLeaveApproverRowDto;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LeaveApprovalMatrixService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HMS Phase A — per-department ordered leave approvers (draft §9 matrix persistence).
 */
@RestController
@RequestMapping("/api/hr/departments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DepartmentLeaveApprovalController {

    private final LeaveApprovalMatrixService leaveApprovalMatrixService;
    private final HrRbacService hrRbac;

    @GetMapping("/{departmentId}/leave-approvers")
    public ResponseEntity<List<DepartmentLeaveApproverRowDto>> getLeaveApprovers(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID departmentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<DepartmentLeaveApproverRowDto> rows =
                leaveApprovalMatrixService.getRows(organizationId, departmentId);
        return ResponseEntity.ok(rows);
    }

    @PutMapping("/{departmentId}/leave-approvers")
    public ResponseEntity<List<DepartmentLeaveApproverRowDto>> replaceLeaveApprovers(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID departmentId,
            @RequestBody List<DepartmentLeaveApproverRowDto> body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        log.info("PUT leave-approvers org={} departmentId={} rows={}", organizationId, departmentId,
                body != null ? body.size() : 0);
        List<DepartmentLeaveApproverRowDto> saved =
                leaveApprovalMatrixService.replaceDepartmentApprovers(organizationId, departmentId, body);
        return ResponseEntity.ok(saved);
    }
}
