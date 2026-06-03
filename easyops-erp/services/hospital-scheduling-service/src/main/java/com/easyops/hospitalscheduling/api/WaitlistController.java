package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.waitlist.WaitlistService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping
    public ResponseEntity<WaitlistEntryResponse> addEntry(
            @Valid @RequestBody CreateWaitlistEntryRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        WaitlistEntryResponse created = waitlistService.addEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public PagedResponse<WaitlistEntryResponse> list(
            @RequestParam(value = "resourceId", required = false) UUID resourceId,
            @RequestParam(value = "patientId", required = false) UUID patientId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return waitlistService.list(resourceId, patientId, status, page, size);
    }

    @PatchMapping("/{id}/status")
    public WaitlistEntryResponse updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateWaitlistStatusRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        return waitlistService.updateStatus(id, request);
    }

    @PostMapping("/promote")
    public PromoteWaitlistResponse promote(
            @Valid @RequestBody PromoteWaitlistRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        return waitlistService.promote(request);
    }
}
