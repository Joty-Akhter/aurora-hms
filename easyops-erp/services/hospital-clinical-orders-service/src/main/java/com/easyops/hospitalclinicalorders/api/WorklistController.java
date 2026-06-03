package com.easyops.hospitalclinicalorders.api;

import com.easyops.hospitalclinicalorders.api.dto.*;
import com.easyops.hospitalclinicalorders.domain.worklist.WorklistService;
import com.easyops.hospitalclinicalorders.security.HospitalClinicalOrdersRbacService;
import com.easyops.hospitalclinicalorders.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-clinical-orders/worklists")
@RequiredArgsConstructor
public class WorklistController {

    private final WorklistService worklistService;
    private final HospitalClinicalOrdersRbacService hospitalClinicalOrdersRbac;

    /** List worklist items. Sorted by priority (STAT, URGENT, ROUTINE), then scheduled_time, then created_at. departmentId filters by order set ordering_department_id; section by worklist_type; facilityId by order set facility. */
    @GetMapping
    public PagedResponse<WorklistItemDetailResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalView(actor, organizationId);
        return worklistService.list(type, status, assignedTo, departmentId, section, facilityId, from, to, page, size);
    }

    @PostMapping("/{worklistItemId}/assign")
    public WorklistItemResponse assign(
            @PathVariable UUID worklistItemId,
            @RequestBody AssignWorklistRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        return worklistService.assign(worklistItemId, request);
    }

    @PostMapping("/{worklistItemId}/status")
    public WorklistItemResponse updateStatus(
            @PathVariable UUID worklistItemId,
            @Valid @RequestBody UpdateWorklistStatusRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalClinicalOrdersRbac.requireHospitalManage(actor, organizationId);
        return worklistService.updateStatus(worklistItemId, request);
    }
}
