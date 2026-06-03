package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.CancellationReportResponse;
import com.easyops.hospitalscheduling.api.dto.NoShowReportResponse;
import com.easyops.hospitalscheduling.api.dto.UtilizationReportResponse;
import com.easyops.hospitalscheduling.domain.reporting.ReportingService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/reports")
@RequiredArgsConstructor
public class SchedulingReportController {

    private final ReportingService reportingService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @GetMapping("/utilization")
    public UtilizationReportResponse getUtilization(
            @RequestParam(value = "resourceId", required = false) UUID resourceId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "groupBy", defaultValue = "DAY") String groupBy,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return reportingService.getUtilizationReport(resourceId, fromDate, toDate, groupBy);
    }

    @GetMapping("/no-show")
    public NoShowReportResponse getNoShow(
            @RequestParam(value = "resourceId", required = false) UUID resourceId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return reportingService.getNoShowReport(resourceId, fromDate, toDate);
    }

    @GetMapping("/cancellations")
    public CancellationReportResponse getCancellations(
            @RequestParam(value = "resourceId", required = false) UUID resourceId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return reportingService.getCancellationReport(resourceId, fromDate, toDate);
    }
}
