package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.resource.SchedulingResourceService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final SchedulingResourceService resourceService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody CreateResourceRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireSchedulingResourceCreate(actor, organizationId);
        ResourceResponse created = resourceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResourceResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStaffRead(actor, organizationId);
        return resourceService.getById(id);
    }

    @GetMapping
    public PagedResponse<ResourceResponse> list(
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "departmentId", required = false) UUID departmentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireAppointmentStaffRead(actor, organizationId);
        return resourceService.list(branchId, departmentId, resourceType, status, page, size);
    }

    @PatchMapping("/{id}")
    public ResourceResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateResourceRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireDoctorScheduleManage(actor, organizationId);
        return resourceService.update(id, request);
    }
}
