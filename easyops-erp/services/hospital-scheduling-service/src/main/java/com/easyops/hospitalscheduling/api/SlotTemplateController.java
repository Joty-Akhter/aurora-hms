package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.resource.SlotTemplateService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/slot-templates")
@RequiredArgsConstructor
public class SlotTemplateController {

    private final SlotTemplateService slotTemplateService;
    private final HospitalSchedulingRbacService hospitalSchedulingRbac;

    @PostMapping
    public ResponseEntity<SlotTemplateResponse> create(
            @Valid @RequestBody CreateSlotTemplateRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        SlotTemplateResponse created = slotTemplateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public SlotTemplateResponse getById(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return slotTemplateService.getById(id);
    }

    @GetMapping
    public PagedResponse<SlotTemplateResponse> list(
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "branchId", required = false) UUID branchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalView(actor, organizationId);
        return slotTemplateService.list(resourceType, branchId, status, page, size);
    }

    @PatchMapping("/{id}")
    public SlotTemplateResponse update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateSlotTemplateRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalSchedulingRbac.requireHospitalManage(actor, organizationId);
        return slotTemplateService.update(id, request);
    }
}
