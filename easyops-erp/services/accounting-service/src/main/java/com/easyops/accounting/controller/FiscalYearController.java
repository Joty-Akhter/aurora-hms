package com.easyops.accounting.controller;

import com.easyops.accounting.entity.FiscalYear;
import com.easyops.accounting.entity.Period;
import com.easyops.accounting.exception.PeriodNotFoundException;
import com.easyops.accounting.security.AccountingRbacService;
import com.easyops.accounting.security.RbacRequestHeaders;
import com.easyops.accounting.service.FiscalYearService;
import com.easyops.accounting.service.PeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounting/fiscal-years")
@RequiredArgsConstructor
@Tag(name = "Fiscal Years", description = "Fiscal year and period management")
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;
    private final PeriodService periodService;
    private final AccountingRbacService accountingRbac;

    @PostMapping("/organization/{organizationId}/setup-current-year")
    @Operation(summary = "Quick setup - Create current fiscal year with 12 monthly periods")
    public ResponseEntity<FiscalYear> setupCurrentFiscalYear(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, organizationId);
        try {
            FiscalYear fiscalYear = fiscalYearService.createCurrentFiscalYearWithPeriods(organizationId);
            return ResponseEntity.status(HttpStatus.CREATED).body(fiscalYear);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/organization/{organizationId}")
    @Operation(summary = "Create a fiscal year")
    public ResponseEntity<FiscalYear> createFiscalYear(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId,
            @RequestParam String yearCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, organizationId);
        try {
            FiscalYear fiscalYear = fiscalYearService.createFiscalYear(
                    organizationId,
                    yearCode,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate)
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(fiscalYear);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{fiscalYearId}/generate-periods")
    @Operation(summary = "Generate monthly periods for a fiscal year")
    public ResponseEntity<List<Period>> generatePeriods(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID fiscalYearId,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingManage(actor, organizationId);
        List<Period> periods = fiscalYearService.generateMonthlyPeriods(fiscalYearId, organizationId);
        return ResponseEntity.ok(periods);
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get all fiscal years for organization")
    public ResponseEntity<List<FiscalYear>> getFiscalYears(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(fiscalYearService.getOrganizationFiscalYears(organizationId));
    }

    @GetMapping("/organization/{organizationId}/open")
    @Operation(summary = "Get open fiscal years")
    public ResponseEntity<List<FiscalYear>> getOpenFiscalYears(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(fiscalYearService.getOpenFiscalYears(organizationId));
    }

    @GetMapping("/{fiscalYearId}")
    @Operation(summary = "Get fiscal year by ID")
    public ResponseEntity<FiscalYear> getFiscalYear(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID fiscalYearId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        FiscalYear fy = fiscalYearService.getFiscalYearById(fiscalYearId);
        accountingRbac.requireAccountingView(actor, fy.getOrganizationId());
        return ResponseEntity.ok(fy);
    }

    @GetMapping("/organization/{organizationId}/periods")
    @Operation(summary = "Get all periods for organization")
    public ResponseEntity<List<Period>> getPeriods(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(periodService.getOrganizationPeriods(organizationId));
    }

    @GetMapping("/organization/{organizationId}/periods/open")
    @Operation(summary = "Get open periods")
    public ResponseEntity<List<Period>> getOpenPeriods(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        return ResponseEntity.ok(periodService.getOpenPeriods(organizationId));
    }

    @GetMapping("/organization/{organizationId}/periods/for-date")
    @Operation(summary = "Get accounting period that contains a date")
    public ResponseEntity<Period> getPeriodForDate(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID organizationId,
            @RequestParam String date) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        accountingRbac.requireAccountingView(actor, organizationId);
        final LocalDate targetDate;
        try {
            targetDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(periodService.getPeriodForDate(organizationId, targetDate));
        } catch (PeriodNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/periods/{periodId}/close")
    @Operation(summary = "Close a period")
    public ResponseEntity<Period> closePeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID periodId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Period period = periodService.getPeriodById(periodId);
        accountingRbac.requireAccountingManage(actor, period.getOrganizationId());
        return ResponseEntity.ok(periodService.closePeriod(periodId, actor));
    }

    @PatchMapping("/periods/{periodId}/reopen")
    @Operation(summary = "Reopen a closed period")
    public ResponseEntity<Period> reopenPeriod(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID periodId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Period period = periodService.getPeriodById(periodId);
        accountingRbac.requireAccountingManage(actor, period.getOrganizationId());
        return ResponseEntity.ok(periodService.reopenPeriod(periodId));
    }

    @PatchMapping("/{fiscalYearId}/close")
    @Operation(summary = "Close a fiscal year")
    public ResponseEntity<FiscalYear> closeFiscalYear(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID fiscalYearId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        FiscalYear fy = fiscalYearService.getFiscalYearById(fiscalYearId);
        accountingRbac.requireAccountingManage(actor, fy.getOrganizationId());
        return ResponseEntity.ok(fiscalYearService.closeFiscalYear(fiscalYearId, actor));
    }
}
