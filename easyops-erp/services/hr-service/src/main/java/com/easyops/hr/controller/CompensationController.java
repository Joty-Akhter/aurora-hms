package com.easyops.hr.controller;

import com.easyops.hr.entity.Bonus;
import com.easyops.hr.entity.Reimbursement;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.CompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/compensation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CompensationController {
    
    private final CompensationService compensationService;
    private final HrRbacService hrRbac;
    
    // Reimbursements
    @GetMapping("/reimbursements")
    public ResponseEntity<List<Reimbursement>> getAllReimbursements(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status) {
        
        log.info("GET /compensation/reimbursements - organizationId: {}, employeeId: {}, status: {}", 
                organizationId, employeeId, status);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<Reimbursement> reimbursements;
        
        if (status != null && "pending".equals(status)) {
            reimbursements = compensationService.getPendingReimbursements(organizationId);
        } else if (employeeId != null) {
            reimbursements = compensationService.getEmployeeReimbursements(employeeId, organizationId);
        } else {
            reimbursements = compensationService.getAllReimbursements(organizationId);
        }
        
        return ResponseEntity.ok(reimbursements);
    }
    
    @PostMapping("/reimbursements")
    public ResponseEntity<Reimbursement> createReimbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Reimbursement reimbursement) {
        log.info("POST /compensation/reimbursements - Creating reimbursement");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, reimbursement.getOrganizationId());
        Reimbursement created = compensationService.createReimbursement(reimbursement);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PostMapping("/reimbursements/{id}/approve")
    public ResponseEntity<Reimbursement> approveReimbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID approvedBy = UUID.fromString(request.get("approvedBy"));
        log.info("POST /compensation/reimbursements/{}/approve - approvedBy: {}", id, approvedBy);
        Reimbursement existing = compensationService.getReimbursementById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Reimbursement approved = compensationService.approveReimbursement(id, approvedBy);
        return ResponseEntity.ok(approved);
    }
    
    @PostMapping("/reimbursements/{id}/reject")
    public ResponseEntity<Reimbursement> rejectReimbursement(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID rejectedBy = UUID.fromString(request.get("rejectedBy"));
        String rejectionReason = request.get("rejectionReason");
        
        log.info("POST /compensation/reimbursements/{}/reject - rejectedBy: {}", id, rejectedBy);
        Reimbursement existing = compensationService.getReimbursementById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Reimbursement rejected = compensationService.rejectReimbursement(id, rejectedBy, rejectionReason);
        return ResponseEntity.ok(rejected);
    }
    
    // Bonuses
    @GetMapping("/bonuses")
    public ResponseEntity<List<Bonus>> getAllBonuses(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID payrollRunId) {

        log.info("GET /compensation/bonuses - organizationId: {}, employeeId: {}, status: {}, payrollRunId: {}",
                organizationId, employeeId, status, payrollRunId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);

        List<Bonus> bonuses;

        if (payrollRunId != null) {
            bonuses = compensationService.getBonusesByPayrollRun(payrollRunId);
        } else if (status != null && "pending".equals(status)) {
            bonuses = compensationService.getPendingBonuses(organizationId);
        } else if (employeeId != null) {
            bonuses = compensationService.getEmployeeBonuses(employeeId, organizationId);
        } else {
            bonuses = compensationService.getAllBonuses(organizationId);
        }

        return ResponseEntity.ok(bonuses);
    }
    
    @PostMapping("/bonuses")
    public ResponseEntity<Bonus> createBonus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Bonus bonus) {
        log.info("POST /compensation/bonuses - Creating bonus");
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, bonus.getOrganizationId());
        Bonus created = compensationService.createBonus(bonus);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PostMapping("/bonuses/{id}/approve")
    public ResponseEntity<Bonus> approveBonus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        UUID approvedBy = UUID.fromString(request.get("approvedBy"));
        log.info("POST /compensation/bonuses/{}/approve - approvedBy: {}", id, approvedBy);
        Bonus existing = compensationService.getBonusById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Bonus approved = compensationService.approveBonus(id, approvedBy);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/bonuses/{id}/reject")
    public ResponseEntity<Bonus> rejectBonus(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        UUID rejectedBy = UUID.fromString(request.get("rejectedBy"));
        String rejectionReason = request.get("rejectionReason");
        log.info("POST /compensation/bonuses/{}/reject - rejectedBy: {}", id, rejectedBy);
        Bonus existing = compensationService.getBonusById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Bonus rejected = compensationService.rejectBonus(id, rejectedBy, rejectionReason);
        return ResponseEntity.ok(rejected);
    }
}
