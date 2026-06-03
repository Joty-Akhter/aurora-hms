package com.easyops.hr.controller;

import com.easyops.hr.dto.DepartmentDto;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.DepartmentIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hr/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentIntegrationService departmentService;
    private final HrRbacService hrRbac;

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getDepartments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) UUID parentDepartmentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.debug("Fetching departments for org={}, activeOnly={}, parent={}",
                organizationId, activeOnly, parentDepartmentId);
        List<DepartmentDto> departments = departmentService.getDepartments(actor, organizationId, activeOnly, parentDepartmentId);
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/root")
    public ResponseEntity<List<DepartmentDto>> getRootDepartments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.debug("Fetching root departments for org={}", organizationId);
        List<DepartmentDto> departments = departmentService.getDepartments(actor, organizationId, null, null)
                .stream()
                .filter(dto -> dto.getParentDepartmentId() == null)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentDto> getDepartmentById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID departmentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.debug("Fetching department detail org={}, id={}", organizationId, departmentId);
        DepartmentDto department = departmentService.getDepartmentById(actor, organizationId, departmentId);
        return ResponseEntity.ok(department);
    }

    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(required = false) UUID organizationId,
            @RequestBody DepartmentDto department) {
        UUID orgId = organizationId != null ? organizationId : department.getOrganizationId();
        if (orgId == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, orgId);
        department.setOrganizationId(orgId);
        DepartmentDto created = departmentService.createDepartment(actor, orgId, department);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentDto> updateDepartment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID departmentId,
            @RequestBody DepartmentDto department) {
        UUID organizationId = department.getOrganizationId();
        if (organizationId == null) {
            return ResponseEntity.badRequest().build();
        }
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        DepartmentDto updated = departmentService.updateDepartment(actor, organizationId, departmentId, department);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{departmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable UUID departmentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, organizationId);
        departmentService.deleteDepartment(actor, organizationId, departmentId);
    }
}

