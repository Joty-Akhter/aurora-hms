package com.easyops.pharma.controller;

import com.easyops.pharma.entity.EmployeeTerritoryAssignment;
import com.easyops.pharma.security.PharmaRbacService;
import com.easyops.pharma.security.RbacRequestHeaders;
import com.easyops.pharma.service.EmployeeAssignmentService;
import com.easyops.pharma.service.TerritoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pharma/employee-assignments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee Assignment", description = "Employee territory assignment management APIs")
@CrossOrigin(origins = "*")
public class EmployeeAssignmentController {

    private final EmployeeAssignmentService assignmentService;
    private final TerritoryService territoryService;
    private final PharmaRbacService pharmaRbac;

    @GetMapping
    @Operation(summary = "Get all employee assignments")
    public ResponseEntity<List<EmployeeTerritoryAssignment>> getAllAssignments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaView(actor, organizationId);
        log.info("GET /api/pharma/employee-assignments - organizationId: {}", organizationId);
        List<EmployeeTerritoryAssignment> assignments = assignmentService.getAllAssignments(organizationId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get assignments by employee")
    public ResponseEntity<List<EmployeeTerritoryAssignment>> getAssignmentsByEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("employeeId") UUID employeeId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        List<EmployeeTerritoryAssignment> assignments = assignmentService.getAssignmentsByEmployee(employeeId);
        UUID orgId = assignments.stream().findFirst().map(EmployeeTerritoryAssignment::getOrganizationId).orElse(null);
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/employee-assignments/employee/{}", employeeId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/employee/{employeeId}/active")
    @Operation(summary = "Get active assignments by employee")
    public ResponseEntity<List<EmployeeTerritoryAssignment>> getActiveAssignmentsByEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("employeeId") UUID employeeId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        List<EmployeeTerritoryAssignment> assignments = assignmentService.getActiveAssignmentsByEmployee(employeeId);
        UUID orgId = assignments.stream().findFirst().map(EmployeeTerritoryAssignment::getOrganizationId).orElse(null);
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/employee-assignments/employee/{}/active", employeeId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/territory/{territoryId}")
    @Operation(summary = "Get assignments by territory")
    public ResponseEntity<List<EmployeeTerritoryAssignment>> getAssignmentsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/employee-assignments/territory/{}", territoryId);
        List<EmployeeTerritoryAssignment> assignments = assignmentService.getAssignmentsByTerritory(territoryId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/territory/{territoryId}/active")
    @Operation(summary = "Get active assignments by territory")
    public ResponseEntity<List<EmployeeTerritoryAssignment>> getActiveAssignmentsByTerritory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("territoryId") UUID territoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = territoryService.getTerritoryById(territoryId).getOrganizationId();
        pharmaRbac.requirePharmaView(actor, orgId);
        log.info("GET /api/pharma/employee-assignments/territory/{}/active", territoryId);
        List<EmployeeTerritoryAssignment> assignments = assignmentService.getActiveAssignmentsByTerritory(territoryId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assignment by ID")
    public ResponseEntity<EmployeeTerritoryAssignment> getAssignmentById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        EmployeeTerritoryAssignment assignment = assignmentService.getAssignmentById(id);
        pharmaRbac.requirePharmaView(actor, assignment.getOrganizationId());
        log.info("GET /api/pharma/employee-assignments/{}", id);
        return ResponseEntity.ok(assignment);
    }

    @PostMapping
    @Operation(summary = "Create new employee assignment")
    public ResponseEntity<EmployeeTerritoryAssignment> createAssignment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody EmployeeTerritoryAssignment assignment) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, assignment.getOrganizationId());
        log.info("POST /api/pharma/employee-assignments");
        EmployeeTerritoryAssignment created = assignmentService.createAssignment(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee assignment")
    public ResponseEntity<EmployeeTerritoryAssignment> updateAssignment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody EmployeeTerritoryAssignment assignment) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        pharmaRbac.requirePharmaManage(actor, assignment.getOrganizationId());
        log.info("PUT /api/pharma/employee-assignments/{}", id);
        EmployeeTerritoryAssignment updated = assignmentService.updateAssignment(id, assignment);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee assignment")
    public ResponseEntity<Void> deleteAssignment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        EmployeeTerritoryAssignment existing = assignmentService.getAssignmentById(id);
        pharmaRbac.requirePharmaManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/pharma/employee-assignments/{}", id);
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
