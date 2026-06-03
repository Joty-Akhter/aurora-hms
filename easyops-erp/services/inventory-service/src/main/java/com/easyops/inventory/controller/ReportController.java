package com.easyops.inventory.controller;

import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.InventoryReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Reports", description = "Inventory analytics and reporting APIs")
@CrossOrigin(origins = "*")
public class ReportController {

    private final InventoryReportService reportService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping("/summary")
    @Operation(summary = "Get stock summary report")
    public ResponseEntity<Map<String, Object>> getStockSummary(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/reports/summary - org: {}", organizationId);
        Map<String, Object> report = reportService.getStockSummaryReport(organizationId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/movements")
    @Operation(summary = "Get stock movement report for date range")
    public ResponseEntity<List<Map<String, Object>>> getStockMovements(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/reports/movements - org: {}, from: {}, to: {}",
                organizationId, startDate, endDate);
        List<Map<String, Object>> report = reportService.getStockMovementReport(organizationId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/turnover")
    @Operation(summary = "Get inventory turnover analysis")
    public ResponseEntity<Map<String, Object>> getInventoryTurnover(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "30") int days) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/reports/turnover - org: {}, days: {}", organizationId, days);
        Map<String, Object> report = reportService.getInventoryTurnoverReport(organizationId, days);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/abc-analysis")
    @Operation(summary = "Get ABC analysis of inventory")
    public ResponseEntity<List<Map<String, Object>>> getABCAnalysis(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/reports/abc-analysis - org: {}", organizationId);
        List<Map<String, Object>> report = reportService.getABCAnalysis(organizationId);
        return ResponseEntity.ok(report);
    }
}
