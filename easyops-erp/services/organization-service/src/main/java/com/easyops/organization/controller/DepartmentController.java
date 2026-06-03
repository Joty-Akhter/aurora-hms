package com.easyops.organization.controller;

import com.easyops.organization.dto.DepartmentRequest;
import com.easyops.organization.dto.DepartmentResponse;
import com.easyops.organization.security.OrganizationRbacService;
import com.easyops.organization.security.RbacRequestHeaders;
import com.easyops.organization.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Department Controller
 */
@RestController
@RequestMapping("/api/organizations/{organizationId}/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;
    private final OrganizationRbacService organizationRbac;

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @PathVariable("organizationId") UUID organizationId,
            @Valid @RequestBody DepartmentRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        DepartmentResponse response = departmentService.createDepartment(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getDepartments(
            @PathVariable("organizationId") UUID organizationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        List<DepartmentResponse> response = departmentService.getDepartmentsByOrganization(organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tree")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentTree(
            @PathVariable("organizationId") UUID organizationId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        List<DepartmentResponse> response = departmentService.getDepartmentTree(organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(
            @PathVariable("organizationId") UUID organizationId,
            @PathVariable("departmentId") UUID departmentId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgRead(actor, organizationId);
        DepartmentResponse response = departmentService.getDepartmentById(departmentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable("organizationId") UUID organizationId,
            @PathVariable("departmentId") UUID departmentId,
            @Valid @RequestBody DepartmentRequest request,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        DepartmentResponse response = departmentService.updateDepartment(departmentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<Void> deleteDepartment(
            @PathVariable("organizationId") UUID organizationId,
            @PathVariable("departmentId") UUID departmentId,
            @RequestHeader("X-User-Id") String userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        organizationRbac.requireOrgWrite(actor, organizationId);
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }
}
