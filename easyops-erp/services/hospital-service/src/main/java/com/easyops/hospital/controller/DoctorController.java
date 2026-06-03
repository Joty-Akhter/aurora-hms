package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.DoctorRequest;
import com.easyops.hospital.dto.response.DoctorResponse;
import com.easyops.hospital.service.DoctorService;

import java.util.Map;
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
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctor Management", description = "APIs for managing doctors/physicians")
public class DoctorController {
    
    private final DoctorService doctorService;
    
    @GetMapping
    @Operation(summary = "Get all doctors", description = "Retrieve doctors; by default excludes inactive (soft-deleted). Set includeInactive=true to include them.")
    public ResponseEntity<List<DoctorResponse>> getAllDoctors(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        List<DoctorResponse> responses = doctorService.getAllDoctors(includeInactive);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get active doctors for prescription", description = "Retrieve a list of active doctors available for prescription")
    public ResponseEntity<List<DoctorResponse>> getActiveDoctorsForPrescription() {
        List<DoctorResponse> responses = doctorService.getActiveDoctorsForPrescription();
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/{doctorId}")
    @Operation(summary = "Get doctor by ID", description = "Retrieve doctor information by doctor ID")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable UUID doctorId) {
        DoctorResponse response = doctorService.getDoctorById(doctorId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/code/{doctorCode}")
    @Operation(summary = "Get doctor by code", description = "Retrieve doctor information by doctor code")
    public ResponseEntity<DoctorResponse> getDoctorByCode(@PathVariable String doctorCode) {
        DoctorResponse response = doctorService.getDoctorByCode(doctorCode);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search doctors", description = "Search doctors by name, code, department, or speciality; excludes inactive unless includeInactive=true")
    public ResponseEntity<List<DoctorResponse>> searchDoctors(
            @RequestParam String searchTerm,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        List<DoctorResponse> responses = doctorService.searchDoctors(searchTerm, includeInactive);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get doctors by department", description = "Retrieve all doctors in a specific department")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByDepartment(@PathVariable UUID departmentId) {
        List<DoctorResponse> responses = doctorService.getDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/department/{departmentId}/active")
    @Operation(summary = "Get active doctors by department", description = "Retrieve active doctors in a specific department")
    public ResponseEntity<List<DoctorResponse>> getActiveDoctorsByDepartment(@PathVariable UUID departmentId) {
        List<DoctorResponse> responses = doctorService.getActiveDoctorsByDepartment(departmentId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/speciality/{speciality}")
    @Operation(summary = "Get doctors by speciality", description = "Retrieve doctors by speciality")
    public ResponseEntity<List<DoctorResponse>> getDoctorsBySpeciality(@PathVariable String speciality) {
        List<DoctorResponse> responses = doctorService.getDoctorsBySpeciality(speciality);
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping
    @Operation(summary = "Create a new doctor", description = "Register a new doctor with auto-generated doctor code")
    public ResponseEntity<DoctorResponse> createDoctor(
            @Valid @RequestBody DoctorRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        log.info("Creating new doctor: {}", request.getDoctorName());

        // Use default user ID if not provided (for testing)
        if (userId == null) {
            userId = "system";
        }

        DoctorResponse response = doctorService.createDoctor(request, userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{doctorId}")
    @Operation(summary = "Update doctor", description = "Update doctor information")
    public ResponseEntity<DoctorResponse> updateDoctor(
            @PathVariable UUID doctorId,
            @Valid @RequestBody DoctorRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        log.info("Updating doctor: {}", doctorId);

        if (userId == null) {
            userId = "system";
        }

        DoctorResponse response = doctorService.updateDoctor(doctorId, request, userId, organizationId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{doctorId}/scheduling-resource")
    @Operation(summary = "Find or create scheduling resource for doctor",
               description = "Proxies findOrCreate to hospital-scheduling-service so the frontend never needs to call it directly.")
    public ResponseEntity<Map<String, String>> getOrCreateSchedulingResource(
            @PathVariable UUID doctorId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        if (userId == null) userId = "system";
        String resourceId = doctorService.findOrCreateSchedulingResource(doctorId, userId, organizationId);
        if (resourceId == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(Map.of("resourceId", resourceId));
    }

    @DeleteMapping("/{doctorId}")
    @Operation(summary = "Delete doctor", description = "Soft delete doctor by setting isActive to false")
    public ResponseEntity<Void> deleteDoctor(
            @PathVariable UUID doctorId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        log.info("Deleting doctor: {}", doctorId);
        
        if (userId == null) {
            userId = "system";
        }
        
        doctorService.deleteDoctor(doctorId, userId, organizationId);
        return ResponseEntity.noContent().build();
    }
}
