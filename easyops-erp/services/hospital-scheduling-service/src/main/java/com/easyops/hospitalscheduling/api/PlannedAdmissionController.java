package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.plannedadmission.PlannedAdmissionService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/planned-admissions")
@RequiredArgsConstructor
public class PlannedAdmissionController {

    private final PlannedAdmissionService plannedAdmissionService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping
    public ResponseEntity<PlannedAdmissionResponse> create(
            @Valid @RequestBody CreatePlannedAdmissionRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        PlannedAdmissionResponse created = plannedAdmissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/expected")
    public ExpectedAdmissionsResponse getExpected(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "wardOrBedClass", required = false) String wardOrBedClass,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return plannedAdmissionService.getExpectedAdmissions(fromDate, toDate, wardOrBedClass);
    }

    @GetMapping("/{id}")
    public PlannedAdmissionResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return plannedAdmissionService.getById(id);
    }

    @GetMapping
    public PagedResponse<PlannedAdmissionResponse> list(
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @RequestParam(value = "preferredDateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDateFrom,
            @RequestParam(value = "preferredDateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDateTo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return plannedAdmissionService.list(patientId, preferredDateFrom, preferredDateTo, status, page, size);
    }

    @PatchMapping("/{id}/status")
    public PlannedAdmissionResponse updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdatePlannedAdmissionStatusRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        return plannedAdmissionService.updateStatus(id, request);
    }
}
