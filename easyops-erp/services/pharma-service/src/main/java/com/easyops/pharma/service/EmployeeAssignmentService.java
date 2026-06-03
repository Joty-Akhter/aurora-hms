package com.easyops.pharma.service;

import com.easyops.pharma.entity.EmployeeTerritoryAssignment;
import com.easyops.pharma.repository.EmployeeTerritoryAssignmentRepository;
import com.easyops.pharma.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeAssignmentService {
    
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;
    private final TerritoryRepository territoryRepository;
    
    @Transactional(readOnly = true)
    public List<EmployeeTerritoryAssignment> getAllAssignments(UUID organizationId) {
        log.debug("Fetching all employee assignments for organization: {}", organizationId);
        return assignmentRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<EmployeeTerritoryAssignment> getAssignmentsByEmployee(UUID employeeId) {
        log.debug("Fetching assignments for employee: {}", employeeId);
        return assignmentRepository.findByEmployeeId(employeeId);
    }
    
    @Transactional(readOnly = true)
    public List<EmployeeTerritoryAssignment> getActiveAssignmentsByEmployee(UUID employeeId) {
        log.debug("Fetching active assignments for employee: {}", employeeId);
        return assignmentRepository.findActiveAssignmentsByEmployee(employeeId, LocalDate.now());
    }
    
    @Transactional(readOnly = true)
    public List<EmployeeTerritoryAssignment> getAssignmentsByTerritory(UUID territoryId) {
        log.debug("Fetching assignments for territory: {}", territoryId);
        return assignmentRepository.findByTerritoryId(territoryId);
    }
    
    @Transactional(readOnly = true)
    public List<EmployeeTerritoryAssignment> getActiveAssignmentsByTerritory(UUID territoryId) {
        log.debug("Fetching active assignments for territory: {}", territoryId);
        return assignmentRepository.findActiveAssignmentsByTerritory(territoryId, LocalDate.now());
    }
    
    @Transactional(readOnly = true)
    public EmployeeTerritoryAssignment getAssignmentById(UUID id) {
        log.debug("Fetching assignment by ID: {}", id);
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee assignment not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"territories", "employeeAssignments"}, allEntries = true)
    public EmployeeTerritoryAssignment createAssignment(EmployeeTerritoryAssignment assignment) {
        log.info("Creating new employee assignment: employee={}, territory={}", assignment.getEmployeeId(), assignment.getTerritoryId());
        
        // Validate territory exists and is active
        var territory = territoryRepository.findById(assignment.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + assignment.getTerritoryId()));
        
        if (!Boolean.TRUE.equals(territory.getIsActive())) {
            throw new RuntimeException("Cannot assign employee to inactive territory");
        }
        
        // Check for overlapping active assignments (optional business rule)
        if (assignment.getStatus() == null || "ACTIVE".equals(assignment.getStatus())) {
            List<EmployeeTerritoryAssignment> existing = assignmentRepository.findActiveAssignmentsByEmployee(
                    assignment.getEmployeeId(), LocalDate.now());
            for (EmployeeTerritoryAssignment existingAssignment : existing) {
                if (existingAssignment.getTerritoryId().equals(assignment.getTerritoryId())) {
                    // Check for date overlap
                    if (assignment.getEndDate() == null || existingAssignment.getEndDate() == null ||
                            !assignment.getAssignmentDate().isAfter(existingAssignment.getEndDate()) &&
                            !(existingAssignment.getEndDate() != null && existingAssignment.getEndDate().isBefore(assignment.getAssignmentDate()))) {
                        throw new RuntimeException("Employee already has an active assignment to this territory");
                    }
                }
            }
        }
        
        if (assignment.getAssignmentDate() == null) {
            assignment.setAssignmentDate(LocalDate.now());
        }
        
        if (assignment.getStatus() == null) {
            assignment.setStatus("ACTIVE");
        }
        
        return assignmentRepository.save(assignment);
    }
    
    @Transactional
    @CacheEvict(value = {"territories", "employeeAssignments"}, allEntries = true)
    public EmployeeTerritoryAssignment updateAssignment(UUID id, EmployeeTerritoryAssignment assignment) {
        log.info("Updating employee assignment: {}", id);
        EmployeeTerritoryAssignment existing = getAssignmentById(id);

        // Validate territory exists and is active
        var territory = territoryRepository.findById(assignment.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + assignment.getTerritoryId()));

        if (!Boolean.TRUE.equals(territory.getIsActive())) {
            throw new RuntimeException("Cannot assign employee to inactive territory");
        }

        // Check for overlapping active assignments for the (possibly changed) employee
        if (assignment.getStatus() == null || "ACTIVE".equals(assignment.getStatus())) {
            List<EmployeeTerritoryAssignment> existingAssignments = assignmentRepository.findActiveAssignmentsByEmployee(
                    assignment.getEmployeeId(), LocalDate.now());
            for (EmployeeTerritoryAssignment other : existingAssignments) {
                if (!other.getId().equals(existing.getId()) && other.getTerritoryId().equals(assignment.getTerritoryId())) {
                    // Check for date overlap between the updated assignment and other active assignments
                    if (assignment.getEndDate() == null || other.getEndDate() == null ||
                            !assignment.getAssignmentDate().isAfter(other.getEndDate()) &&
                                    !(other.getEndDate() != null && other.getEndDate().isBefore(assignment.getAssignmentDate()))) {
                        throw new RuntimeException("Employee already has an active assignment to this territory");
                    }
                }
            }
        }

        existing.setEmployeeId(assignment.getEmployeeId());
        existing.setTerritoryId(assignment.getTerritoryId());
        existing.setAssignmentDate(assignment.getAssignmentDate());
        existing.setEndDate(assignment.getEndDate());
        existing.setRoleInTerritory(assignment.getRoleInTerritory());
        existing.setStatus(assignment.getStatus());
        existing.setNotes(assignment.getNotes());
        existing.setUpdatedBy(assignment.getUpdatedBy());

        return assignmentRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"territories", "employeeAssignments"}, allEntries = true)
    public void deleteAssignment(UUID id) {
        log.info("Deleting employee assignment: {}", id);
        EmployeeTerritoryAssignment assignment = getAssignmentById(id);
        assignmentRepository.delete(assignment);
    }
}
