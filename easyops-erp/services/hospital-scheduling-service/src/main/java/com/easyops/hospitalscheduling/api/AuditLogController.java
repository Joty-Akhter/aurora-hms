package com.easyops.hospitalscheduling.api;

import com.easyops.hospitalscheduling.api.dto.AuditLogResponse;
import com.easyops.hospitalscheduling.api.dto.PagedResponse;
import com.easyops.hospitalscheduling.domain.audit.AuditLogService;
import com.easyops.hospitalscheduling.security.HospitalSchedulingRbacService;
import com.easyops.hospitalscheduling.security.RbacRequestHeaders;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-scheduling/audit-log")
public class AuditLogController {

    private final AuditLogService service;
    private final HospitalSchedulingRbacService rbac;

    public AuditLogController(AuditLogService service, HospitalSchedulingRbacService rbac) {
        this.service = service;
        this.rbac = rbac;
    }

    @GetMapping
    public PagedResponse<AuditLogResponse> search(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireHospitalView(actor, orgId);
        return service.search(entityType, entityId, actorId, action, fromDate, toDate, correlationId, page, size);
    }

    @GetMapping("/{id}")
    public AuditLogResponse getById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID orgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        rbac.requireHospitalView(actor, orgId);
        return service.getById(id);
    }
}
