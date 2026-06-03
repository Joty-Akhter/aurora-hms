package com.easyops.hr.controller;

import com.easyops.hr.entity.LeaveBalance;
import com.easyops.hr.entity.LeaveRequest;
import com.easyops.hr.entity.LeaveType;
import com.easyops.hr.security.HrEmployeeSelfAccessService;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/leave")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;
    private final HrRbacService hrRbac;
    private final HrEmployeeSelfAccessService hrEmployeeSelfAccessService;
    
    // Leave Types
    @GetMapping("/types")
    public ResponseEntity<List<LeaveType>> getAllLeaveTypes(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /leave/types - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireLeaveTypesReadable(actor, organizationId);
        List<LeaveType> leaveTypes = leaveService.getAllLeaveTypes(organizationId);
        return ResponseEntity.ok(leaveTypes);
    }
    
    @PostMapping("/types")
    public ResponseEntity<LeaveType> createLeaveType(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody LeaveType leaveType) {
        log.info("POST /leave/types - Creating leave type: {}", leaveType.getTypeName());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, leaveType.getOrganizationId());
        LeaveType created = leaveService.createLeaveType(leaveType);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/types/{id}")
    public ResponseEntity<LeaveType> updateLeaveType(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody LeaveType leaveType) {
        log.info("PUT /leave/types/{}", id);
        LeaveType existing = leaveService.getLeaveTypeById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        LeaveType updated = leaveService.updateLeaveType(id, leaveType);
        return ResponseEntity.ok(updated);
    }
    
    // Leave Requests
    @GetMapping("/requests")
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID pendingForApproverEmployeeId) {

        log.info("GET /leave/requests - organizationId: {}, employeeId: {}, status: {}, pendingForApproverEmployeeId: {}",
                organizationId, employeeId, status, pendingForApproverEmployeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);

        List<LeaveRequest> requests;

        if (pendingForApproverEmployeeId != null) {
            hrEmployeeSelfAccessService.requirePendingApproverQueue(actor, organizationId, pendingForApproverEmployeeId);
            requests = leaveService.getPendingLeaveRequestsForApprover(organizationId, pendingForApproverEmployeeId);
            return ResponseEntity.ok(requests);
        }

        hrEmployeeSelfAccessService.requireLeaveRequestList(actor, organizationId, employeeId);

        if (employeeId != null) {
            requests = leaveService.getEmployeeLeaveRequests(employeeId, organizationId);
            if (status != null && !status.isBlank() && !requests.isEmpty()) {
                String st = status.trim().toLowerCase();
                requests = requests.stream()
                        .filter(r -> r.getStatus() != null && st.equals(r.getStatus().trim().toLowerCase()))
                        .toList();
            }
        } else if (status != null && "pending".equalsIgnoreCase(status.trim())) {
            requests = leaveService.getPendingLeaveRequests(organizationId);
        } else {
            requests = leaveService.getAllLeaveRequests(organizationId);
        }
        
        return ResponseEntity.ok(requests);
    }
    
    @GetMapping("/requests/{id}")
    public ResponseEntity<LeaveRequest> getLeaveRequestById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /leave/requests/{}", id);
        LeaveRequest request = leaveService.getLeaveRequestById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireLeaveRequestReadable(actor, request.getOrganizationId(), request);
        return ResponseEntity.ok(request);
    }
    
    @PostMapping("/requests")
    public ResponseEntity<LeaveRequest> createLeaveRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody LeaveRequest leaveRequest) {
        log.info("POST /leave/requests - Creating leave request for employee: {}", 
                leaveRequest.getEmployeeId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireLeaveRequestSubmit(actor, leaveRequest.getOrganizationId(),
                leaveRequest.getEmployeeId());
        LeaveRequest created = leaveService.createLeaveRequest(leaveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<LeaveRequest> approveLeaveRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID approvedBy = UUID.fromString(request.get("approvedBy"));
        log.info("POST /leave/requests/{}/approve - approvedBy: {}", id, approvedBy);
        LeaveRequest existing = leaveService.getLeaveRequestById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        boolean hrManage = hrRbac.hasHrManage(actor, existing.getOrganizationId());
        hrEmployeeSelfAccessService.requireLeaveApprove(actor, existing.getOrganizationId(), existing, approvedBy, hrManage);
        LeaveRequest approved = leaveService.approveLeaveRequest(id, approvedBy, hrManage);
        return ResponseEntity.ok(approved);
    }
    
    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<LeaveRequest> rejectLeaveRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID rejectedBy = UUID.fromString(request.get("rejectedBy"));
        String rejectionReason = request.get("rejectionReason");
        
        log.info("POST /leave/requests/{}/reject - rejectedBy: {}", id, rejectedBy);
        LeaveRequest existing = leaveService.getLeaveRequestById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        boolean hrManage = hrRbac.hasHrManage(actor, existing.getOrganizationId());
        hrEmployeeSelfAccessService.requireLeaveReject(actor, existing.getOrganizationId(), existing, rejectedBy, hrManage);
        LeaveRequest rejected = leaveService.rejectLeaveRequest(id, rejectedBy, rejectionReason);
        return ResponseEntity.ok(rejected);
    }
    
    // Leave Balances
    @GetMapping("/balances")
    public ResponseEntity<List<LeaveBalance>> getEmployeeLeaveBalances(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID organizationId) {
        
        log.info("GET /leave/balances - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireLeaveBalanceReadable(actor, organizationId, employeeId);
        List<LeaveBalance> balances = leaveService.getEmployeeLeaveBalances(employeeId, organizationId);
        return ResponseEntity.ok(balances);
    }
    
    @PostMapping("/balances")
    public ResponseEntity<LeaveBalance> createLeaveBalance(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody LeaveBalance balance) {
        log.info("POST /leave/balances - Creating leave balance");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, balance.getOrganizationId());
        LeaveBalance created = leaveService.createLeaveBalance(balance);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
