package com.easyops.organization.service;

import com.easyops.organization.dto.DepartmentRequest;
import com.easyops.organization.dto.DepartmentResponse;
import com.easyops.organization.entity.Department;
import com.easyops.organization.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Department Service
 * Handles business logic for department management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Create a new department
     */
    @Transactional
    public DepartmentResponse createDepartment(UUID organizationId, DepartmentRequest request) {
        log.info("Creating department for organization: {}", organizationId);

        // Check if code already exists
        if (departmentRepository.existsByOrganizationIdAndCode(organizationId, request.getCode())) {
            throw new IllegalArgumentException("Department code already exists: " + request.getCode());
        }

        Department department = Department.builder()
                .organizationId(organizationId)
                .parentDepartmentId(request.getParentDepartmentId())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : "DEPARTMENT")
                .managerId(request.getManagerId())
                .costCenter(request.getCostCenter())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created successfully: {}", saved.getId());

        return DepartmentResponse.fromEntity(saved);
    }

    /**
     * Get department by ID
     */
    public DepartmentResponse getDepartmentById(UUID id) {
        log.debug("Fetching department by ID: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));
        return DepartmentResponse.fromEntity(department);
    }

    /**
     * Get all departments for organization
     */
    public List<DepartmentResponse> getDepartmentsByOrganization(UUID organizationId) {
        log.debug("Fetching departments for organization: {}", organizationId);
        return departmentRepository.findByOrganizationId(organizationId).stream()
                .map(DepartmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get department hierarchy tree
     */
    public List<DepartmentResponse> getDepartmentTree(UUID organizationId) {
        log.debug("Building department tree for organization: {}", organizationId);
        
        List<Department> allDepartments = departmentRepository.findByOrganizationId(organizationId);
        Map<UUID, List<Department>> childrenMap = allDepartments.stream()
                .filter(d -> d.getParentDepartmentId() != null)
                .collect(Collectors.groupingBy(Department::getParentDepartmentId));

        // Get root departments (those without parents)
        List<Department> roots = allDepartments.stream()
                .filter(d -> d.getParentDepartmentId() == null)
                .collect(Collectors.toList());

        return roots.stream()
                .map(root -> buildDepartmentTree(root, childrenMap))
                .collect(Collectors.toList());
    }

    private DepartmentResponse buildDepartmentTree(Department department, Map<UUID, List<Department>> childrenMap) {
        DepartmentResponse response = DepartmentResponse.fromEntity(department);
        
        List<Department> children = childrenMap.get(department.getId());
        if (children != null && !children.isEmpty()) {
            response.setChildren(
                children.stream()
                    .map(child -> buildDepartmentTree(child, childrenMap))
                    .collect(Collectors.toList())
            );
        } else {
            response.setChildren(new ArrayList<>());
        }
        
        return response;
    }

    /**
     * Update department
     */
    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        log.info("Updating department: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        if (request.getName() != null) department.setName(request.getName());
        if (request.getDescription() != null) department.setDescription(request.getDescription());
        if (request.getType() != null) department.setType(request.getType());
        if (request.getManagerId() != null) department.setManagerId(request.getManagerId());
        if (request.getCostCenter() != null) department.setCostCenter(request.getCostCenter());
        if (request.getStatus() != null) department.setStatus(request.getStatus());
        if (request.getParentDepartmentId() != null) {
            // Prevent circular reference
            if (request.getParentDepartmentId().equals(id)) {
                throw new IllegalArgumentException("Department cannot be its own parent");
            }
            department.setParentDepartmentId(request.getParentDepartmentId());
        }

        Department updated = departmentRepository.save(department);
        log.info("Department updated successfully: {}", id);

        return DepartmentResponse.fromEntity(updated);
    }

    /**
     * Delete department
     */
    @Transactional
    public void deleteDepartment(UUID id) {
        log.info("Deleting department: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + id));

        // Check if department has children
        List<Department> children = departmentRepository.findByParentDepartmentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot delete department with child departments");
        }

        departmentRepository.delete(department);
        log.info("Department deleted successfully: {}", id);
    }
}

