package com.easyops.hr.controller;

import com.easyops.hr.entity.ShiftDefinition;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.ShiftDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/shift-definitions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShiftDefinitionController {

    private final ShiftDefinitionService shiftDefinitionService;
    private final HrRbacService hrRbac;

    @GetMapping
    public ResponseEntity<List<ShiftDefinition>> list(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        return ResponseEntity.ok(shiftDefinitionService.list(organizationId, activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftDefinition> get(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        ShiftDefinition def = shiftDefinitionService.get(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, def.getOrganizationId());
        return ResponseEntity.ok(def);
    }

    @PostMapping
    public ResponseEntity<ShiftDefinition> create(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody ShiftDefinition body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, body.getOrganizationId());
        ShiftDefinition created = shiftDefinitionService.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftDefinition> update(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody ShiftDefinition body) {
        ShiftDefinition existing = shiftDefinitionService.get(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        return ResponseEntity.ok(shiftDefinitionService.update(id, body));
    }
}
