package com.easyops.pharma.controller;

import com.easyops.pharma.dto.*;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.ReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportingService reportingService;
    private final PharmaRbacService pharmaRbac;

    // Monthly Closing Report - supports areaId (aggregates territories in area) or territoryId
    @GetMapping("/monthly-closing")
    public ResponseEntity<MonthlyClosingReportDTO> getMonthlyClosingReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "areaId", required = false) UUID areaId,
            @RequestParam(name = "territoryId", required = false) UUID territoryId,
            @RequestParam(name = "employeeId", required = false) UUID employeeId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        if (areaId != null) {
            log.info("Generating monthly closing report for area: {}, employee: {}, year: {}, month: {}", areaId, employeeId, year, month);
            MonthlyClosingReportDTO report = reportingService.generateMonthlyClosingReportByArea(organizationId, areaId, employeeId, year, month);
            return ResponseEntity.ok(report);
        } else if (territoryId != null) {
            log.info("Generating monthly closing report for territory: {}, employee: {}, year: {}, month: {}", territoryId, employeeId, year, month);
            MonthlyClosingReportDTO report = reportingService.generateMonthlyClosingReportByTerritory(organizationId, territoryId, employeeId, year, month);
            return ResponseEntity.ok(report);
        } else {
            throw new IllegalArgumentException("Either areaId or territoryId must be provided");
        }
    }

    // Area Performance Report
    @GetMapping("/area-performance")
    public ResponseEntity<AreaPerformanceReportDTO> getAreaPerformanceReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("areaId") UUID areaId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating area performance report for area: {}, year: {}, month: {}",
                areaId, year, month);

        AreaPerformanceReportDTO report = reportingService.generateAreaPerformanceReport(
                organizationId, areaId, year, month);
        return ResponseEntity.ok(report);
    }

    // Inventory Reports - In-Stock Total Amount
    @GetMapping("/inventory/in-stock-total")
    public ResponseEntity<InventoryReportDTO.InStockTotalAmount> getInStockTotalAmountReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating in-stock total amount report from {} to {}", startDate, endDate);

        InventoryReportDTO.InStockTotalAmount report = reportingService.generateInStockTotalAmountReport(
                organizationId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // Inventory Reports - In-Stock Product-Wise
    @GetMapping("/inventory/in-stock-product-wise")
    public ResponseEntity<InventoryReportDTO.InStockProductWise> getInStockProductWiseReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating in-stock product-wise report from {} to {}", startDate, endDate);

        InventoryReportDTO.InStockProductWise report = reportingService.generateInStockProductWiseReport(
                organizationId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // Inventory Reports - Area-Wise Allocation
    @GetMapping("/inventory/area-wise-allocation")
    public ResponseEntity<InventoryReportDTO.AreaWiseAllocation> getAreaWiseAllocationReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "areaId", required = false) UUID areaId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating area-wise allocation report from {} to {}", startDate, endDate);

        InventoryReportDTO.AreaWiseAllocation report = reportingService.generateAreaWiseAllocationReport(
                organizationId, startDate, endDate, areaId);
        return ResponseEntity.ok(report);
    }

    // Inventory Reports - Month-Wise Allocation
    @GetMapping("/inventory/month-wise-allocation")
    public ResponseEntity<InventoryReportDTO.MonthWiseAllocation> getMonthWiseAllocationReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("year") Integer year,
            @RequestParam(name = "areaId", required = false) UUID areaId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating month-wise allocation report for year: {}", year);

        InventoryReportDTO.MonthWiseAllocation report = reportingService.generateMonthWiseAllocationReport(
                organizationId, year, areaId);
        return ResponseEntity.ok(report);
    }

    // Inventory Reports - Annual Allocation
    @GetMapping("/inventory/annual-allocation")
    public ResponseEntity<InventoryReportDTO.AnnualAllocation> getAnnualAllocationReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("year") Integer year,
            @RequestParam(name = "areaId", required = false) UUID areaId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating annual allocation report for year: {}", year);

        InventoryReportDTO.AnnualAllocation report = reportingService.generateAnnualAllocationReport(
                organizationId, year, areaId);
        return ResponseEntity.ok(report);
    }

    // Collection Reports - Area-Wise Collection
    @GetMapping("/collection/area-wise")
    public ResponseEntity<CollectionReportDTO.AreaWiseCollection> getAreaWiseCollectionReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "areaId", required = false) UUID areaId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating area-wise collection report from {} to {}", startDate, endDate);

        CollectionReportDTO.AreaWiseCollection report = reportingService.generateAreaWiseCollectionReport(
                organizationId, startDate, endDate, areaId);
        return ResponseEntity.ok(report);
    }

    // Collection Reports - Employee-Wise Collection
    @GetMapping("/collection/employee-wise")
    public ResponseEntity<CollectionReportDTO.EmployeeWiseCollection> getEmployeeWiseCollectionReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "employeeId", required = false) UUID employeeId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating employee-wise collection report from {} to {}", startDate, endDate);

        CollectionReportDTO.EmployeeWiseCollection report = reportingService.generateEmployeeWiseCollectionReport(
                organizationId, startDate, endDate, employeeId);
        return ResponseEntity.ok(report);
    }

    // Financial Reports - Accounts Balance
    @GetMapping("/financial/accounts-balance")
    public ResponseEntity<FinancialReportDTO.AccountsBalance> getAccountsBalanceReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("asOfDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating accounts balance report as of {}", asOfDate);

        FinancialReportDTO.AccountsBalance report = reportingService.generateAccountsBalanceReport(
                organizationId, asOfDate);
        return ResponseEntity.ok(report);
    }

    // Financial Reports - Income and Expense
    @GetMapping("/financial/income-expense")
    public ResponseEntity<FinancialReportDTO.IncomeExpense> getIncomeExpenseReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating income expense report from {} to {}", startDate, endDate);

        FinancialReportDTO.IncomeExpense report = reportingService.generateIncomeExpenseReport(
                organizationId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    // Financial Reports - Incentive Report
    @GetMapping("/financial/incentive")
    public ResponseEntity<FinancialReportDTO.IncentiveReport> getIncentiveReport(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("Generating incentive report for year: {}, month: {}", year, month);

        FinancialReportDTO.IncentiveReport report = reportingService.generateIncentiveReport(
                organizationId, year, month);
        return ResponseEntity.ok(report);
    }
}
