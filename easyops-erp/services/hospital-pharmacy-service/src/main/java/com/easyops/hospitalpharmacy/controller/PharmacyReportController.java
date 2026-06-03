package com.easyops.hospitalpharmacy.controller;

import com.easyops.hospitalpharmacy.dto.response.ConsumptionReportItemResponse;
import com.easyops.hospitalpharmacy.dto.response.ControlledSubstanceDispenseRowResponse;
import com.easyops.hospitalpharmacy.dto.response.PharmacyStockItemResponse;
import com.easyops.hospitalpharmacy.dto.response.SalesSummaryResponse;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentApprovalResponse;
import com.easyops.hospitalpharmacy.dto.response.StockAdjustmentMovementResponse;
import com.easyops.hospitalpharmacy.dto.response.StockOverrideLineReportResponse;
import com.easyops.hospitalpharmacy.dto.response.StockTransferMovementResponse;
import com.easyops.hospitalpharmacy.security.HospitalPharmacyRbacService;
import com.easyops.hospitalpharmacy.security.RbacRequestHeaders;
import com.easyops.hospitalpharmacy.service.PharmacyReportService;
import com.easyops.hospitalpharmacy.service.StockAdjustmentApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-pharmacy/reports")
@RequiredArgsConstructor
@Slf4j
public class PharmacyReportController {

    private final PharmacyReportService pharmacyReportService;
    private final StockAdjustmentApprovalService adjustmentApprovalService;
    private final HospitalPharmacyRbacService hospitalPharmacyRbac;

    @GetMapping("/near-expiry")
    public ResponseEntity<List<PharmacyStockItemResponse>> getNearExpiryStock(
            @RequestParam(value = "pharmacyId", required = false) UUID pharmacyId,
            @RequestParam(value = "days", required = false, defaultValue = "30") int days,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "companyCode", required = false) String companyCode,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        log.info("Fetching near-expiry stock for pharmacy {} within {} days", pharmacyId, days);
        return ResponseEntity.ok(pharmacyReportService.getNearExpiryStock(pharmacyId, days, productCode, companyCode));
    }

    @GetMapping("/consumption")
    public ResponseEntity<List<ConsumptionReportItemResponse>> getConsumptionReport(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "companyCode", required = false) String companyCode,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        log.info("Fetching consumption report for pharmacy {} from {} to {}", pharmacyId, from, to);
        return ResponseEntity.ok(pharmacyReportService.getConsumptionReport(pharmacyId, from, to, productCode, companyCode));
    }

    @GetMapping(value = "/consumption/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportConsumptionCsv(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "companyCode", required = false) String companyCode,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        List<ConsumptionReportItemResponse> rows = pharmacyReportService.getConsumptionReport(
                pharmacyId, from, to, productCode, companyCode);
        String csv = PharmacyReportService.formatConsumptionCsv(rows);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("consumption-report.csv", StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    @GetMapping("/sales-summary")
    public ResponseEntity<SalesSummaryResponse> getSalesSummary(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "companyCode", required = false) String companyCode,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyReportService.getSalesSummary(pharmacyId, from, to, productCode, companyCode));
    }

    @GetMapping("/controlled-substance-register")
    public ResponseEntity<List<ControlledSubstanceDispenseRowResponse>> getControlledSubstanceRegister(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "companyCode", required = false) String companyCode,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyReportService.getControlledSubstanceRegister(
                pharmacyId, from, to, productCode, companyCode));
    }

    @GetMapping("/stock-adjustments")
    public ResponseEntity<List<StockAdjustmentMovementResponse>> getStockAdjustmentsReport(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyReportService.getStockAdjustmentsReport(pharmacyId, from, to));
    }

    @GetMapping("/transfer-history")
    public ResponseEntity<List<StockTransferMovementResponse>> getTransferHistory(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyReportService.getTransferHistory(pharmacyId, from, to));
    }

    @GetMapping("/pending-stock-adjustment-approvals")
    public ResponseEntity<List<StockAdjustmentApprovalResponse>> getPendingStockAdjustmentApprovals(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requireStockAdjustmentApprove(actor, organizationId);
        return ResponseEntity.ok(adjustmentApprovalService.listPending());
    }

    /** Phase P3 WS-L1 — also exposed as {@code /stock-override-lines} for backward compatibility. */
    @GetMapping({"/stock-overrides", "/stock-override-lines"})
    public ResponseEntity<List<StockOverrideLineReportResponse>> getStockOverrideLines(
            @RequestParam("pharmacyId") UUID pharmacyId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "productCode", required = false) String productCode,
            @RequestParam(value = "companyCode", required = false) String companyCode,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalPharmacyRbac.requirePharmacyRead(actor, organizationId);
        return ResponseEntity.ok(pharmacyReportService.getStockOverrideLines(
                pharmacyId, from, to, productCode, companyCode));
    }
}
