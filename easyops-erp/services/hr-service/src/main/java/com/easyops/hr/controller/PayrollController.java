package com.easyops.hr.controller;

import com.easyops.hr.dto.MyPayslipSummaryDto;
import com.easyops.hr.dto.PayslipDto;
import com.easyops.hr.dto.PayrollAccountingExportDto;
import com.easyops.hr.dto.PayrollDetailWithEmployeeDto;
import com.easyops.hr.dto.PayrollTimeAttendancePolicyDto;
import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.entity.PayrollDetail;
import com.easyops.hr.entity.PayrollRun;
import com.easyops.hr.integration.IntegrationCorrelationIdHolder;
import com.easyops.hr.security.HrEmployeeSelfAccessService;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.AccountingFinanceIntegrationService;
import com.easyops.hr.service.PayrollCalculationService;
import com.easyops.hr.service.PayrollEpfService;
import com.easyops.hr.service.PayrollService;
import com.easyops.hr.service.PayrollTimeAttendancePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/hr/payroll")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PayrollController {
    
    private final PayrollService payrollService;
    private final PayrollCalculationService payrollCalculationService;
    private final AccountingFinanceIntegrationService accountingFinanceIntegrationService;
    private final PayrollEpfService payrollEpfService;
    private final PayrollTimeAttendancePolicyService payrollTimeAttendancePolicyService;
    private final HrRbacService hrRbac;
    private final HrEmployeeSelfAccessService hrEmployeeSelfAccessService;
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPayrollStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /payroll/stats - organizationId: {}", organizationId);
        Map<String, Object> stats = payrollService.getPayrollStats(organizationId);
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/runs")
    public ResponseEntity<List<PayrollRun>> getAllPayrollRuns(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "status", required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /payroll/runs - organizationId: {}, status: {}", organizationId, status);
        
        List<PayrollRun> runs;
        
        if (status != null) {
            runs = payrollService.getPayrollRunsByStatus(organizationId, status);
        } else {
            runs = payrollService.getAllPayrollRuns(organizationId);
        }
        
        return ResponseEntity.ok(runs);
    }
    
    @GetMapping("/runs/{id}")
    public ResponseEntity<PayrollRun> getPayrollRunById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        log.info("GET /payroll/runs/{}", id);
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, run.getOrganizationId());
        return ResponseEntity.ok(run);
    }
    
    @PostMapping("/runs")
    public ResponseEntity<PayrollRun> createPayrollRun(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody PayrollRun payrollRun) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, payrollRun.getOrganizationId());
        log.info("POST /payroll/runs - Creating payroll run: {}", payrollRun.getRunName());
        PayrollRun created = payrollService.createPayrollRun(payrollRun);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/runs/{id}")
    public ResponseEntity<PayrollRun> updatePayrollRun(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @RequestBody PayrollRun payrollRun) {
        PayrollRun existing = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        log.info("PUT /payroll/runs/{}", id);
        PayrollRun updated = payrollService.updatePayrollRun(id, payrollRun);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/runs/{id}/process")
    public ResponseEntity<PayrollRun> processPayrollRun(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request) {
        PayrollRun existing = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        String processedByStr = request != null ? request.get("processedBy") : null;
        UUID processedBy = (processedByStr != null && !processedByStr.isBlank())
                ? UUID.fromString(processedByStr)
                : null;
        log.info("POST /payroll/runs/{}/process - processedBy: {}", id, processedBy);
        PayrollRun processed = payrollService.processPayrollRun(id, processedBy);
        return ResponseEntity.ok(processed);
    }
    
    @PostMapping("/runs/{id}/approve")
    public ResponseEntity<PayrollRun> approvePayrollRun(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request) {
        PayrollRun existing = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        String approvedByStr = request != null ? request.get("approvedBy") : null;
        UUID approvedBy = (approvedByStr != null && !approvedByStr.isBlank())
                ? UUID.fromString(approvedByStr)
                : null;
        log.info("POST /payroll/runs/{}/approve - approvedBy: {}", id, approvedBy);
        PayrollRun approved = payrollService.approvePayrollRun(id, approvedBy);
        return ResponseEntity.ok(approved);
    }
    
    @GetMapping("/runs/{id}/details")
    public ResponseEntity<List<PayrollDetailWithEmployeeDto>> getPayrollDetails(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, run.getOrganizationId());
        log.info("GET /payroll/runs/{}/details", id);
        List<PayrollDetailWithEmployeeDto> details = payrollService.getPayrollDetailsWithEmployees(id);
        return ResponseEntity.ok(details);
    }
    
    @PostMapping("/details")
    public ResponseEntity<PayrollDetail> createPayrollDetail(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody PayrollDetail detail) {
        UUID orgId = detail.getOrganizationId();
        if (orgId == null && detail.getPayrollRunId() != null) {
            orgId = payrollService.getPayrollRunById(detail.getPayrollRunId()).getOrganizationId();
        }
        if (orgId == null) {
            throw new IllegalArgumentException("organizationId or payrollRunId is required on payroll detail");
        }
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, orgId);
        log.info("POST /payroll/details - Creating payroll detail");
        PayrollDetail created = payrollService.createPayrollDetail(detail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/details/employee/{employeeId}")
    public ResponseEntity<List<PayrollDetail>> getEmployeePayrollHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("employeeId") UUID employeeId,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /payroll/details/employee/{}", employeeId);
        List<PayrollDetail> history = payrollService.getEmployeePayrollHistory(employeeId, organizationId);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/details/{id}/mark-paid")
    public ResponseEntity<PayrollDetail> markAsPaid(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @RequestBody Map<String, String> request) {
        PayrollDetail detail = payrollService.getPayrollDetailById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, detail.getOrganizationId());
        String paymentReference = request.get("paymentReference");
        log.info("POST /payroll/details/{}/mark-paid", id);
        PayrollDetail updated = payrollService.markAsPaid(id, paymentReference);
        return ResponseEntity.ok(updated);
    }

    /** ES-22–ES-27: Populate payroll from employee salary (assignment + components). Only when run is DRAFT. */
    @PostMapping("/runs/{id}/populate-from-salary")
    public ResponseEntity<Map<String, Object>> populatePayrollFromSalary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, run.getOrganizationId());
        log.info("POST /payroll/runs/{}/populate-from-salary", id);
        var summary = payrollCalculationService.populatePayrollFromSalary(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payrollRunId", summary.getPayrollRunId());
        body.put("employeesPopulated", summary.getEmployeesPopulated());
        body.put("employeesWithoutAssignment", summary.getEmployeesWithoutAssignment());
        body.put("employeesMissingBasic", summary.getEmployeesMissingBasic());
        String cid = IntegrationCorrelationIdHolder.get();
        if (cid != null) {
            body.put("correlationId", cid);
        }
        return ResponseEntity.ok(body);
    }

    /**
     * INT-19–INT-23: Export payroll results for accounting (detail + summary by component).
     * Accounting or finance module can call this API to generate journal entries.
     */
    @GetMapping("/runs/{id}/accounting-export")
    public ResponseEntity<PayrollAccountingExportDto> getPayrollAccountingExport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        log.info("GET /payroll/runs/{}/accounting-export", id);
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, run.getOrganizationId());
        PayrollAccountingExportDto dto = payrollCalculationService.getPayrollAccountingExport(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Create EPF contributions from payroll run PF components.
     * Extracts PF_EMPLOYEE and PF_EMPLOYER amounts from payroll and creates/updates EpfContribution records.
     */
    @PostMapping("/runs/{id}/process-epf")
    public ResponseEntity<Map<String, Object>> processEpfFromPayroll(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, run.getOrganizationId());
        log.info("POST /payroll/runs/{}/process-epf", id);
        Map<String, Object> result = payrollEpfService.createEpfContributionsFromPayroll(id);
        return ResponseEntity.ok(result);
    }

    /**
     * Post EPF contributions to accounting for the payroll run period.
     * Run Process EPF first to create contributions. Requires EPF_PAYABLE and CASH in CoA.
     */
    @PostMapping("/runs/{id}/post-epf-to-accounting")
    public ResponseEntity<Map<String, Object>> postEpfToAccounting(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        log.info("POST /payroll/runs/{}/post-epf-to-accounting", id);
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requirePfRemittanceManage(actor, run.getOrganizationId());
        java.time.LocalDate periodEnd = run.getPayPeriodEnd() != null ? run.getPayPeriodEnd() : run.getPayPeriodStart();
        int month = periodEnd.getMonthValue();
        int year = periodEnd.getYear();
        Map<String, Object> result = accountingFinanceIntegrationService.postEpfContributionsToAccounting(
                run.getOrganizationId(), month, year, actor.toString());
        return ResponseEntity.ok(result);
    }

    /**
     * Post payroll run to accounting (create and post journal entry).
     * Only for PROCESSED or APPROVED runs. Requires standard CoA accounts (6110, 2020, 1030).
     */
    @PostMapping("/runs/{id}/post-to-accounting")
    public ResponseEntity<Map<String, Object>> postPayrollToAccounting(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        log.info("POST /payroll/runs/{}/post-to-accounting", id);
        PayrollRun run = payrollService.getPayrollRunById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, run.getOrganizationId());
        String status = run.getStatus();
        if (!"PROCESSED".equals(status) && !"APPROVED".equals(status)) {
            throw new IllegalArgumentException("Payroll run must be PROCESSED or APPROVED before posting to accounting. Current status: " + status);
        }
        PayrollAccountingExportDto export = payrollCalculationService.getPayrollAccountingExport(id);
        Map<String, Object> result = accountingFinanceIntegrationService.postPayrollToAccounting(export);
        return ResponseEntity.ok(result);
    }

    /** ES-26: Payslip with component order for an employee in a payroll run. ES-54: Download uses this with responseType for PDF. */
    @GetMapping("/runs/{runId}/payslip")
    public ResponseEntity<PayslipDto> getPayslip(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("runId") UUID runId,
            @RequestParam("employeeId") UUID employeeId) {
        PayrollRun run = payrollService.getPayrollRunById(runId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, run.getOrganizationId(), employeeId);
        Optional<PayslipDto> payslip = payrollCalculationService.getPayslip(runId, employeeId);
        return payslip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** ES-54: Employee self-service – list payroll runs (payslips) for this employee. */
    @GetMapping("/self/payslips")
    public ResponseEntity<List<MyPayslipSummaryDto>> getMyPayslips(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrEmployeeSelfAccessService.requireOwnEmployeeOrHrView(actor, organizationId, employeeId);
        List<MyPayslipSummaryDto> list = payrollService.getMyPayslips(employeeId, organizationId);
        return ResponseEntity.ok(list);
    }

    /** Organization time & attendance rules for payroll (OT multiplier, missing-day LOP, standard hours). */
    @GetMapping("/time-attendance-policy")
    public ResponseEntity<PayrollTimeAttendancePolicyDto> getTimeAttendancePolicy(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        PayrollTimeAttendancePolicy p = payrollTimeAttendancePolicyService.getOrCreate(organizationId);
        return ResponseEntity.ok(toTimeAttendancePolicyDto(p));
    }

    @PutMapping("/time-attendance-policy")
    public ResponseEntity<PayrollTimeAttendancePolicyDto> updateTimeAttendancePolicy(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestBody PayrollTimeAttendancePolicyDto body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        PayrollTimeAttendancePolicy p = payrollTimeAttendancePolicyService.update(
                organizationId,
                body != null ? body.getOvertimeRateMultiplier() : null,
                body != null ? body.getInferMissingWeekdayLop() : null,
                body != null ? body.getStandardHoursPerDay() : null,
                body != null ? body.getLeavePayrollBridgeEnabled() : null,
                body != null ? body.getUnpaidApprovedLeaveCountsAsLop() : null,
                body != null ? body.getExcludeActiveHolidaysFromWorkingDays() : null,
                body != null ? body.getExcludeActiveHolidaysFromLopInference() : null);
        return ResponseEntity.ok(toTimeAttendancePolicyDto(p));
    }

    private static PayrollTimeAttendancePolicyDto toTimeAttendancePolicyDto(PayrollTimeAttendancePolicy p) {
        return PayrollTimeAttendancePolicyDto.builder()
                .organizationId(p.getOrganizationId())
                .overtimeRateMultiplier(p.getOvertimeRateMultiplier())
                .inferMissingWeekdayLop(p.getInferMissingWeekdayLop())
                .standardHoursPerDay(p.getStandardHoursPerDay())
                .leavePayrollBridgeEnabled(p.getLeavePayrollBridgeEnabled())
                .unpaidApprovedLeaveCountsAsLop(p.getUnpaidApprovedLeaveCountsAsLop())
                .excludeActiveHolidaysFromWorkingDays(p.getExcludeActiveHolidaysFromWorkingDays())
                .excludeActiveHolidaysFromLopInference(p.getExcludeActiveHolidaysFromLopInference())
                .build();
    }
}

