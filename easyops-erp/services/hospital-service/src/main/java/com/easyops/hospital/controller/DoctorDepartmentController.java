package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.DoctorDepartmentRequest;
import com.easyops.hospital.dto.response.DoctorDepartmentResponse;
import com.easyops.hospital.service.DoctorDepartmentService;
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
@RequestMapping("/api/doctor-departments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctor Department Management", description = "APIs for managing doctor departments/specialties")
public class DoctorDepartmentController {
    
    private final DoctorDepartmentService doctorDepartmentService;
    
    @GetMapping
    @Operation(summary = "Get all departments", description = "Retrieve a list of all doctor departments")
    public ResponseEntity<List<DoctorDepartmentResponse>> getAllDepartments() {
        List<DoctorDepartmentResponse> responses = doctorDepartmentService.getAllDepartments();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active departments", description = "Retrieve a list of active doctor departments")
    public ResponseEntity<List<DoctorDepartmentResponse>> getActiveDepartments() {
        List<DoctorDepartmentResponse> responses = doctorDepartmentService.getActiveDepartments();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{departmentId}")
    @Operation(summary = "Get department by ID", description = "Retrieve department information by department ID")
    public ResponseEntity<DoctorDepartmentResponse> getDepartmentById(@PathVariable UUID departmentId) {
        DoctorDepartmentResponse response = doctorDepartmentService.getDepartmentById(departmentId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search departments", description = "Search departments by name")
    public ResponseEntity<List<DoctorDepartmentResponse>> searchDepartments(
            @RequestParam String searchTerm) {
        List<DoctorDepartmentResponse> responses = doctorDepartmentService.searchDepartments(searchTerm);
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    @Operation(summary = "Create a new department", description = "Create a new doctor department")
    public ResponseEntity<DoctorDepartmentResponse> createDepartment(
            @Valid @RequestBody DoctorDepartmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("Creating new department: {}", request.getDepartmentName());
        
        // Use default user ID if not provided (for testing)
        if (userId == null) {
            userId = "system";
        }
        
        DoctorDepartmentResponse response = doctorDepartmentService.createDepartment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/{departmentId}")
    @Operation(summary = "Update department", description = "Update department information")
    public ResponseEntity<DoctorDepartmentResponse> updateDepartment(
            @PathVariable UUID departmentId,
            @Valid @RequestBody DoctorDepartmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("Updating department: {}", departmentId);
        
        if (userId == null) {
            userId = "system";
        }
        
        DoctorDepartmentResponse response = doctorDepartmentService.updateDepartment(departmentId, request, userId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{departmentId}")
    @Operation(summary = "Delete department", description = "Soft delete department by setting status to INACTIVE")
    public ResponseEntity<Void> deleteDepartment(
            @PathVariable UUID departmentId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("Deleting department: {}", departmentId);
        
        if (userId == null) {
            userId = "system";
        }
        
        doctorDepartmentService.deleteDepartment(departmentId, userId);
        return ResponseEntity.noContent().build();
    }
}
