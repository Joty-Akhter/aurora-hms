package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.doctormapping.DoctorResourceMappingService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/doctor-resource-mappings")
public class DoctorResourceMappingController {

    private final DoctorResourceMappingService service;
    private final HospitalSchedulingRbacService rbac;

    public DoctorResourceMappingController(DoctorResourceMappingService service, HospitalSchedulingRbacService rbac) {
        this.service = service;
        this.rbac = rbac;
    }

    @PostMapping
    public ResponseEntity<DoctorResourceMappingResponse> create(
            @Valid @RequestBody CreateDoctorResourceMappingRequest req,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireHospitalManage(actor, orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, actor));
    }

    @GetMapping
    public PagedResponse<DoctorResourceMappingResponse> list(
            @RequestParam(required = false) UUID doctorUserId,
            @RequestParam(required = false) UUID resourceId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireHospitalView(actor, orgId);
        return service.list(doctorUserId, resourceId, branchId, status, page, size);
    }

    @GetMapping("/resolve")
    public DoctorResourceMappingResponse resolve(
            @RequestParam UUID doctorUserId,
            @RequestParam(required = false) UUID branchId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireHospitalView(actor, orgId);
        return service.resolve(doctorUserId, branchId);
    }

    @PatchMapping("/{id}")
    public DoctorResourceMappingResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateDoctorResourceMappingRequest req,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireHospitalManage(actor, orgId);
        return service.update(id, req);
    }
}
