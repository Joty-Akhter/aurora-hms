package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.DoctorDepartmentRequest;
import com.easyops.hospital.dto.response.DoctorDepartmentResponse;
import com.easyops.hospital.entity.DoctorDepartment;
import com.easyops.hospital.repository.DoctorDepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorDepartmentService {
    
    private final DoctorDepartmentRepository doctorDepartmentRepository;
    
    /**
     * Get all departments
     */
    public List<DoctorDepartmentResponse> getAllDepartments() {
        List<DoctorDepartment> departments = doctorDepartmentRepository.findAll();
        return departments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active departments
     */
    public List<DoctorDepartmentResponse> getActiveDepartments() {
        List<DoctorDepartment> departments = doctorDepartmentRepository.findByStatus(DoctorDepartment.DepartmentStatus.ACTIVE);
        return departments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get department by ID
     */
    public DoctorDepartmentResponse getDepartmentById(UUID departmentId) {
        DoctorDepartment department = doctorDepartmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + departmentId));
        return mapToResponse(department);
    }
    
    /**
     * Search departments by name
     */
    public List<DoctorDepartmentResponse> searchDepartments(String searchTerm) {
        List<DoctorDepartment> departments = doctorDepartmentRepository.searchDepartments(
                searchTerm,
                DoctorDepartment.DepartmentStatus.ACTIVE);
        return departments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Create a new department
     */
    @Transactional
    public DoctorDepartmentResponse createDepartment(DoctorDepartmentRequest request, String userId) {
        // Check if department name already exists
        if (doctorDepartmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Same department already added in the list. Please use a different name.");
        }
        
        DoctorDepartment department = DoctorDepartment.builder()
                .departmentName(request.getDepartmentName())
                .generalVisitAmount(request.getGeneralVisitAmount())
                .status(request.getStatus() != null ? request.getStatus() : DoctorDepartment.DepartmentStatus.ACTIVE)
                .createdBy(userId)
                .build();
        
        department = doctorDepartmentRepository.save(department);
        log.info("Created department: {} with ID: {}", department.getDepartmentName(), department.getDepartmentId());
        
        return mapToResponse(department);
    }
    
    /**
     * Update department
     */
    @Transactional
    public DoctorDepartmentResponse updateDepartment(UUID departmentId, DoctorDepartmentRequest request, String userId) {
        DoctorDepartment department = doctorDepartmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + departmentId));
        
        // Check if new name conflicts with existing department
        if (!department.getDepartmentName().equals(request.getDepartmentName()) &&
            doctorDepartmentRepository.existsByDepartmentName(request.getDepartmentName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Same department already added in the list. Please use a different name.");
        }
        
        department.setDepartmentName(request.getDepartmentName());
        department.setGeneralVisitAmount(request.getGeneralVisitAmount());
        if (request.getStatus() != null) {
            department.setStatus(request.getStatus());
        }
        department.setUpdatedBy(userId);
        
        department = doctorDepartmentRepository.save(department);
        log.info("Updated department: {} with ID: {}", department.getDepartmentName(), department.getDepartmentId());
        
        return mapToResponse(department);
    }
    
    /**
     * Delete department (soft delete by setting status to INACTIVE)
     */
    @Transactional
    public void deleteDepartment(UUID departmentId, String userId) {
        DoctorDepartment department = doctorDepartmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + departmentId));
        
        department.setStatus(DoctorDepartment.DepartmentStatus.INACTIVE);
        department.setUpdatedBy(userId);
        doctorDepartmentRepository.save(department);
        log.info("Deleted (deactivated) department: {} with ID: {}", department.getDepartmentName(), department.getDepartmentId());
    }
    
    /**
     * Map entity to response DTO
     */
    private DoctorDepartmentResponse mapToResponse(DoctorDepartment department) {
        return DoctorDepartmentResponse.builder()
                .departmentId(department.getDepartmentId())
                .departmentName(department.getDepartmentName())
                .generalVisitAmount(department.getGeneralVisitAmount())
                .status(department.getStatus())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .createdBy(department.getCreatedBy())
                .updatedBy(department.getUpdatedBy())
                .build();
    }
}
