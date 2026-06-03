package com.easyops.hr.controller;

import com.easyops.hr.dto.PagedResponse;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/employees")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmployeeController {
    
    private final EmployeeService employeeService;
    private final HrRbacService hrRbac;
    
    @GetMapping
    public ResponseEntity<?> getAllEmployees(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "departmentId", required = false) UUID departmentId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /employees - organizationId: {}, status: {}, departmentId: {}, search: {}, page: {}, size: {}",
                organizationId, status, departmentId, search, page, size);

        if (page != null && size != null) {
            PagedResponse<Employee> paged = employeeService.getEmployeesPaged(
                    organizationId, status, departmentId, search, page, size);
            return ResponseEntity.ok(paged);
        }

        List<Employee> employees;

        if (search != null && !search.isEmpty()) {
            employees = employeeService.searchEmployees(organizationId, search);
        } else if (departmentId != null) {
            employees = employeeService.getEmployeesByDepartment(organizationId, departmentId);
        } else if (status != null) {
            employees = employeeService.getEmployeesByStatus(organizationId, status);
        } else {
            employees = employeeService.getAllEmployees(organizationId);
        }

        return ResponseEntity.ok(employees);
    }

    /**
     * HMS Phase A / AC-10: resolve the authenticated user's linked employee row without {@code HR_VIEW}.
     * Declared before {@code /{id}} so {@code "me"} is not parsed as a UUID.
     */
    @GetMapping("/me")
    public ResponseEntity<Employee> getMyEmployeeProfile(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Employee self = employeeService.findLinkedEmployee(organizationId, actor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no_linked_employee_profile"));
        return ResponseEntity.ok(self);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        log.info("GET /employees/{}", id);
        Employee employee = employeeService.getEmployeeById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, employee.getOrganizationId());
        return ResponseEntity.ok(employee);
    }
    
    @GetMapping("/number/{employeeNumber}")
    public ResponseEntity<Employee> getEmployeeByNumber(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("employeeNumber") String employeeNumber,
            @RequestParam("organizationId") UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /employees/number/{} - organizationId: {}", employeeNumber, organizationId);
        Employee employee = employeeService.getEmployeeByNumber(organizationId, employeeNumber);
        return ResponseEntity.ok(employee);
    }
    
    @PostMapping
    public ResponseEntity<Employee> createEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Employee employee) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, employee.getOrganizationId());
        log.info("POST /employees - Creating employee: {}", employee.getName());
        Employee createdEmployee = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @RequestBody Employee employee) {
        Employee existing = employeeService.getEmployeeById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        log.info("PUT /employees/{}", id);
        Employee updatedEmployee = employeeService.updateEmployee(id, employee);
        return ResponseEntity.ok(updatedEmployee);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        Employee existing = employeeService.getEmployeeById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        log.info("DELETE /employees/{}", id);
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> countEmployees(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "status", required = false) String status) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        log.info("GET /employees/count - organizationId: {}, status: {}", organizationId, status);
        
        long count;
        if (status != null) {
            count = employeeService.countEmployeesByStatus(organizationId, status);
        } else {
            count = employeeService.getAllEmployees(organizationId).size();
        }
        
        return ResponseEntity.ok(count);
    }
}

