package com.easyops.hr.controller;

import com.easyops.hr.entity.Benefit;
import com.easyops.hr.entity.EmployeeBenefit;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.BenefitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/benefits")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BenefitController {
    
    private final BenefitService benefitService;
    private final HrRbacService hrRbac;
    
    @GetMapping
    public ResponseEntity<List<Benefit>> getAllBenefits(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /benefits - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<Benefit> benefits = benefitService.getAllBenefits(organizationId);
        return ResponseEntity.ok(benefits);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Benefit> getBenefitById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /benefits/{}", id);
        Benefit benefit = benefitService.getBenefitById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, benefit.getOrganizationId());
        return ResponseEntity.ok(benefit);
    }
    
    @PostMapping
    public ResponseEntity<Benefit> createBenefit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Benefit benefit) {
        log.info("POST /benefits - Creating benefit: {}", benefit.getBenefitName());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, benefit.getOrganizationId());
        Benefit created = benefitService.createBenefit(benefit);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Benefit> updateBenefit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Benefit benefit) {
        log.info("PUT /benefits/{}", id);
        Benefit existing = benefitService.getBenefitById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Benefit updated = benefitService.updateBenefit(id, benefit);
        return ResponseEntity.ok(updated);
    }
    
    @GetMapping("/enrollments")
    public ResponseEntity<List<EmployeeBenefit>> getEmployeeBenefits(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID organizationId) {
        
        log.info("GET /benefits/enrollments - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<EmployeeBenefit> enrollments = benefitService.getEmployeeBenefits(employeeId, organizationId);
        return ResponseEntity.ok(enrollments);
    }
    
    @PostMapping("/enrollments")
    public ResponseEntity<EmployeeBenefit> enrollBenefit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EmployeeBenefit enrollment) {
        log.info("POST /benefits/enrollments - Enrolling employee: {}", enrollment.getEmployeeId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, enrollment.getOrganizationId());
        EmployeeBenefit created = benefitService.enrollBenefit(enrollment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/enrollments/{id}")
    public ResponseEntity<EmployeeBenefit> updateBenefitEnrollment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody EmployeeBenefit enrollment) {
        log.info("PUT /benefits/enrollments/{}", id);
        EmployeeBenefit existing = benefitService.getEmployeeBenefitById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        EmployeeBenefit updated = benefitService.updateBenefitEnrollment(id, enrollment);
        return ResponseEntity.ok(updated);
    }
}
