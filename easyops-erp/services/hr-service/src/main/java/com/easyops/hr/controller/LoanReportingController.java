package com.easyops.hr.controller;

import com.easyops.hr.dto.LoanAccountingExportDto;
import com.easyops.hr.dto.LoanArrearsRowDto;
import com.easyops.hr.dto.LoanRegisterRowDto;
import com.easyops.hr.dto.LoanReportSummaryDto;
import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Phase 6 (RE-01): loan reporting and CSV export.
 * Phase 7 (PI-05): optional accounting-period export (disbursements + repayments).
 */
@RestController
@RequestMapping("/api/hr/loans/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanReportingController {

    private final LoanReportingService loanReportingService;
    private final LoanRbacService loanRbac;

    @GetMapping("/summary")
    public ResponseEntity<LoanReportSummaryDto> summary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanReportingService.getSummary(organizationId));
    }

    @GetMapping("/register")
    public ResponseEntity<List<LoanRegisterRowDto>> register(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) EmployeeLoanStatus status,
            @RequestParam(required = false) LoanCategoryType categoryType) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanReportingService.getRegister(organizationId, categoryId, employeeId, status, categoryType));
    }

    @GetMapping("/arrears")
    public ResponseEntity<List<LoanArrearsRowDto>> arrears(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanReportingService.getArrears(organizationId, asOf));
    }

    @GetMapping("/settlement-exit")
    public ResponseEntity<List<LoanRegisterRowDto>> settlementExit(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanReportingService.getSettlementExitRegister(organizationId));
    }

    @GetMapping(value = "/register/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportRegister(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) EmployeeLoanStatus status,
            @RequestParam(required = false) LoanCategoryType categoryType) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        List<LoanRegisterRowDto> rows = loanReportingService.getRegister(organizationId, categoryId, employeeId, status, categoryType);
        String csv = loanReportingService.registerRowsToCsv(rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan-register.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/arrears/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportArrears(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        List<LoanArrearsRowDto> rows = loanReportingService.getArrears(organizationId, asOf);
        String csv = loanReportingService.arrearsRowsToCsv(rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan-arrears.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /** PI-05: JSON bundle for integrations (journal staging). */
    @GetMapping("/accounting-export")
    public ResponseEntity<LoanAccountingExportDto> accountingExport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanReportingService.getAccountingExport(organizationId, periodFrom, periodTo));
    }

    @GetMapping(value = "/accounting/disbursements/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAccountingDisbursements(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        LoanAccountingExportDto exp = loanReportingService.getAccountingExport(organizationId, periodFrom, periodTo);
        String csv = loanReportingService.accountingDisbursementsToCsv(exp);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan-accounting-disbursements.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping(value = "/accounting/repayments/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAccountingRepayments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodTo) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        LoanAccountingExportDto exp = loanReportingService.getAccountingExport(organizationId, periodFrom, periodTo);
        String csv = loanReportingService.accountingRepaymentsToCsv(exp);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"loan-accounting-repayments.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
