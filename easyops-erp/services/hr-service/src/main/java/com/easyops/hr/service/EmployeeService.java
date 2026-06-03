package com.easyops.hr.service;

import com.easyops.hr.dto.PagedResponse;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    
    public List<Employee> getAllEmployees(UUID organizationId) {
        log.debug("Fetching all employees for organization: {}", organizationId);
        return employeeRepository.findByOrganizationId(organizationId);
    }
    
    public List<Employee> getEmployeesByStatus(UUID organizationId, String status) {
        log.debug("Fetching employees for organization: {} with status: {}", organizationId, status);
        return employeeRepository.findByOrganizationIdAndEmploymentStatus(organizationId, status);
    }
    
    public List<Employee> getEmployeesByDepartment(UUID organizationId, UUID departmentId) {
        log.debug("Fetching employees for organization: {} in department: {}", organizationId, departmentId);
        return employeeRepository.findByOrganizationIdAndDepartmentId(organizationId, departmentId);
    }
    
    public List<Employee> searchEmployees(UUID organizationId, String search) {
        log.debug("Searching employees for organization: {} with term: {}", organizationId, search);
        return employeeRepository.searchEmployees(organizationId, search);
    }

    public PagedResponse<Employee> getEmployeesPaged(
            UUID organizationId,
            String status,
            UUID departmentId,
            String search,
            int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name"));
        String normalizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim() : null;

        Page<Employee> result = employeeRepository.findEmployeesFiltered(
                organizationId,
                normalizedStatus,
                departmentId,
                normalizedSearch,
                pageable);

        return new PagedResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                result.isFirst(),
                result.isLast());
    }
    
    public Employee getEmployeeById(UUID employeeId) {
        log.debug("Fetching employee by ID: {}", employeeId);
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));
    }
    
    public Employee getEmployeeByNumber(UUID organizationId, String employeeNumber) {
        log.debug("Fetching employee by number: {} for organization: {}", employeeNumber, organizationId);
        return employeeRepository.findByOrganizationIdAndEmployeeNumber(organizationId, employeeNumber)
                .orElseThrow(() -> new RuntimeException("Employee not found with number: " + employeeNumber));
    }
    
    public Employee createEmployee(Employee employee) {
        log.info("Creating new employee: {} for organization: {}", 
                employee.getName(), employee.getOrganizationId());
        
        // Check for duplicate employee number
        employeeRepository.findByOrganizationIdAndEmployeeNumber(
                employee.getOrganizationId(), employee.getEmployeeNumber())
                .ifPresent(e -> {
                    throw new RuntimeException("Employee number already exists: " + employee.getEmployeeNumber());
                });
        
        // Check for duplicate email (only if email is provided)
        if (employee.getEmail() != null && !employee.getEmail().trim().isEmpty()) {
            employeeRepository.findByOrganizationIdAndEmail(
                    employee.getOrganizationId(), employee.getEmail())
                    .ifPresent(e -> {
                        throw new RuntimeException("Email already exists: " + employee.getEmail());
                    });
        }
        
        return employeeRepository.save(employee);
    }
    
    public Employee updateEmployee(UUID employeeId, Employee employeeData) {
        log.info("Updating employee: {}", employeeId);

        Employee existingEmployee = getEmployeeById(employeeId);

        // Partial update: only overwrite when employeeData has non-null value (avoids null overwrites from partial payloads)
        if (employeeData.getUserId() != null) {
            existingEmployee.setUserId(employeeData.getUserId());
        }
        if (employeeData.getName() != null) {
            existingEmployee.setName(employeeData.getName());
        }
        if (employeeData.getEmail() != null) {
            existingEmployee.setEmail(employeeData.getEmail());
        }
        if (employeeData.getPhone() != null) {
            existingEmployee.setPhone(employeeData.getPhone());
        }
        if (employeeData.getDateOfBirth() != null) {
            existingEmployee.setDateOfBirth(employeeData.getDateOfBirth());
        }
        if (employeeData.getHireDate() != null) {
            existingEmployee.setHireDate(employeeData.getHireDate());
        }
        if (employeeData.getTerminationDate() != null) {
            existingEmployee.setTerminationDate(employeeData.getTerminationDate());
        }
        if (employeeData.getGender() != null) {
            existingEmployee.setGender(employeeData.getGender());
        }

        UUID deptId = resolveDepartmentId(employeeData.getDepartmentId(), existingEmployee.getDepartmentId());
        existingEmployee.setDepartmentId(deptId);

        UUID posId = resolvePositionId(employeeData.getPositionId(), existingEmployee.getPositionId());
        existingEmployee.setPositionId(posId);

        UUID mgrId = resolveManagerId(employeeData.getManagerId(), existingEmployee.getManagerId());
        existingEmployee.setManagerId(mgrId);

        if (employeeData.getEmploymentType() != null) {
            existingEmployee.setEmploymentType(employeeData.getEmploymentType());
        }
        if (employeeData.getEmploymentStatus() != null) {
            existingEmployee.setEmploymentStatus(employeeData.getEmploymentStatus());
        }

        if (employeeData.getAddressLine1() != null) {
            existingEmployee.setAddressLine1(employeeData.getAddressLine1());
        }
        if (employeeData.getAddressLine2() != null) {
            existingEmployee.setAddressLine2(employeeData.getAddressLine2());
        }
        if (employeeData.getCity() != null) {
            existingEmployee.setCity(employeeData.getCity());
        }
        if (employeeData.getStateProvince() != null) {
            existingEmployee.setStateProvince(employeeData.getStateProvince());
        }
        if (employeeData.getPostalCode() != null) {
            existingEmployee.setPostalCode(employeeData.getPostalCode());
        }
        if (employeeData.getCountry() != null) {
            existingEmployee.setCountry(employeeData.getCountry());
        }

        if (employeeData.getEmergencyContactName() != null) {
            existingEmployee.setEmergencyContactName(employeeData.getEmergencyContactName());
        }
        if (employeeData.getEmergencyContactPhone() != null) {
            existingEmployee.setEmergencyContactPhone(employeeData.getEmergencyContactPhone());
        }
        if (employeeData.getEmergencyContactRelationship() != null) {
            existingEmployee.setEmergencyContactRelationship(employeeData.getEmergencyContactRelationship());
        }

        if (employeeData.getBankName() != null) {
            existingEmployee.setBankName(blankToNull(employeeData.getBankName()));
        }
        if (employeeData.getBankBranch() != null) {
            existingEmployee.setBankBranch(blankToNull(employeeData.getBankBranch()));
        }
        if (employeeData.getBankAccountNumber() != null) {
            existingEmployee.setBankAccountNumber(blankToNull(employeeData.getBankAccountNumber()));
        }
        if (employeeData.getBankRoutingOrIban() != null) {
            existingEmployee.setBankRoutingOrIban(blankToNull(employeeData.getBankRoutingOrIban()));
        }
        if (employeeData.getPayrollOvertimeRateMultiplier() != null) {
            BigDecimal m = employeeData.getPayrollOvertimeRateMultiplier();
            existingEmployee.setPayrollOvertimeRateMultiplier(
                    m.compareTo(BigDecimal.ZERO) <= 0 ? null : m);
        }
        if (employeeData.getPayrollStandardHoursPerDay() != null) {
            BigDecimal h = employeeData.getPayrollStandardHoursPerDay();
            existingEmployee.setPayrollStandardHoursPerDay(h.compareTo(BigDecimal.ZERO) <= 0 ? null : h);
        }

        if (employeeData.getUpdatedBy() != null) {
            existingEmployee.setUpdatedBy(employeeData.getUpdatedBy());
        }

        return employeeRepository.save(existingEmployee);
    }

    /** Department ids are admin.departments (organization-service); validated by DB foreign key. */
    private UUID resolveDepartmentId(UUID provided, UUID existing) {
        return provided != null ? provided : existing;
    }

    /**
     * Use provided positionId only if it exists in hr.positions; otherwise keep existing.
     */
    private UUID resolvePositionId(UUID provided, UUID existing) {
        if (provided == null) {
            return existing;
        }
        if (positionRepository.findById(provided).isPresent()) {
            return provided;
        }
        log.warn("Position id {} not found in HR; keeping existing position for employee", provided);
        return existing;
    }

    /**
     * Use provided managerId only if it exists in hr.employees; otherwise keep existing.
     */
    private UUID resolveManagerId(UUID provided, UUID existing) {
        if (provided == null) {
            return existing;
        }
        if (employeeRepository.findById(provided).isPresent()) {
            return provided;
        }
        log.warn("Manager id {} not found in HR; keeping existing manager for employee", provided);
        return existing;
    }
    
    public void deleteEmployee(UUID employeeId) {
        log.info("Deactivating employee: {}", employeeId);
        Employee employee = getEmployeeById(employeeId);
        employee.setIsActive(false);
        employee.setEmploymentStatus("TERMINATED");
        employeeRepository.save(employee);
    }
    
    public long countEmployeesByStatus(UUID organizationId, String status) {
        return employeeRepository.countByOrganizationIdAndEmploymentStatus(organizationId, status);
    }

    /** Actor-linked HR profile for self-service (no {@code HR_VIEW} required). */
    public Optional<Employee> findLinkedEmployee(UUID organizationId, UUID userId) {
        return employeeRepository.findByOrganizationIdAndUserId(organizationId, userId);
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

