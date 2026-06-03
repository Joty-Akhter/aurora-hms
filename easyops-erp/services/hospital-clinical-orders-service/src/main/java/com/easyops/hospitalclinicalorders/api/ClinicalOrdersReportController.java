package com.easyops.hospitalclinicalorders.api;

import com.easyops.hospitalclinicalorders.api.dto.TatReportItem;
import com.easyops.hospitalclinicalorders.api.dto.VolumeReportItem;
import com.easyops.hospitalclinicalorders.domain.report.ClinicalOrdersReportService;
import com.easyops.hospitalclinicalorders.security.HospitalClinicalOrdersRbacService;
import com.easyops.hospitalclinicalorders.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-clinical-orders/reports")
@RequiredArgsConstructor
public class ClinicalOrdersReportController {

    private final ClinicalOrdersReportService reportService;
    private final HospitalClinicalOrdersRbacService hospitalClinicalOrdersRbac;

    @GetMapping("/tat")
    public List<TatReportItem> getTat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String orderType,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return reportService.getTatReport(from, to, orderType);
    }

    @GetMapping("/volumes")
    public List<VolumeReportItem> getVolumes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "orderType") String groupBy,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return reportService.getVolumeReport(from, to, groupBy);
    }
}
