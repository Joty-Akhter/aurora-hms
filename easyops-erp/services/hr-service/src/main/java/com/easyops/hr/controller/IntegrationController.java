package com.easyops.hr.controller;

import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/integration")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class IntegrationController {
    
    private final PayrollIntegrationService payrollIntegrationService;
    private final EmployeeManagementIntegrationService employeeManagementIntegrationService;
    private final AccountingFinanceIntegrationService accountingFinanceIntegrationService;
    private final DataSynchronizationService dataSynchronizationService;
    private final HrRbacService hrRbac;
    
    // Payroll Integration
    @PostMapping("/payroll/complete")
    public ResponseEntity<Void> completePayrollIntegration(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID payrollRunId,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("POST /integration/payroll/complete - payrollRunId: {}, period: {}/{}", 
                payrollRunId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        payrollIntegrationService.completePayrollIntegration(
                payrollRunId, organizationId, month, year);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/payroll/epf")
    public ResponseEntity<Void> processEpfForPayroll(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID payrollRunId,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("POST /integration/payroll/epf - payrollRunId: {}, period: {}/{}", 
                payrollRunId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        payrollIntegrationService.processEpfContributionsForPayroll(
                payrollRunId, organizationId, month, year);
        return ResponseEntity.ok().build();
    }
    
    // Accounting/Finance Integration
    @PostMapping("/accounting/epf")
    public ResponseEntity<Void> postEpfToAccounting(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("POST /integration/accounting/epf - organizationId: {}, period: {}/{}", 
                organizationId, month, year);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfRemittanceManage(actor, organizationId);
        accountingFinanceIntegrationService.postEpfContributionsToAccounting(
                organizationId, month, year, actor.toString());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/accounting/sync-balances")
    public ResponseEntity<Void> syncProvidentFundBalances(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("POST /integration/accounting/sync-balances - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        accountingFinanceIntegrationService.syncProvidentFundBalances(organizationId);
        return ResponseEntity.ok().build();
    }
    
    // Employee Management Integration
    @PostMapping("/employee/sync-provident-fund")
    public ResponseEntity<Void> syncEmployeeDataForProvidentFund(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("POST /integration/employee/sync-provident-fund - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        employeeManagementIntegrationService.syncEmployeeDataForProvidentFund(organizationId);
        return ResponseEntity.ok().build();
    }
    
    // Data Synchronization
    @PostMapping("/sync/trigger")
    public ResponseEntity<Void> triggerSynchronization(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam String syncType,
            @RequestParam UUID organizationId) {
        log.info("POST /integration/sync/trigger - type: {}, organizationId: {}", syncType, organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        dataSynchronizationService.triggerSynchronization(syncType, organizationId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/sync/validate")
    public ResponseEntity<Map<String, Object>> validateDataIntegrity(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /integration/sync/validate - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        Map<String, Object> validation = dataSynchronizationService.validateDataIntegrity(organizationId);
        return ResponseEntity.ok(validation);
    }
}
