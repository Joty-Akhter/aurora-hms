package com.easyops.hr.service;

import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HrDashboardService {
    
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final JdbcTemplate jdbcTemplate;
    
    public Map<String, Object> getDashboardStats(UUID organizationId) {
        log.debug("Fetching dashboard statistics for organization: {}", organizationId);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Get stats from view
        String sql = "SELECT * FROM hr.v_hr_dashboard_stats WHERE organization_id = ?";
        
        try {
            Map<String, Object> viewStats = jdbcTemplate.queryForMap(sql, organizationId);
            stats.putAll(viewStats);
        } catch (Exception e) {
            log.warn("Could not fetch from view, falling back to manual calculation", e);
            
            // Fallback to manual calculation
            long totalEmployees = employeeRepository.findByOrganizationId(organizationId).size();
            long activeEmployees = employeeRepository.countByOrganizationIdAndEmploymentStatus(organizationId, "ACTIVE");
            long positionCount = positionRepository.findByOrganizationId(organizationId).size();
            
            stats.put("total_employees", totalEmployees);
            stats.put("active_employees", activeEmployees);
            // Note: department_count should be fetched from Organization Service API
            stats.put("department_count", 0); // TODO: Call Organization Service API
            stats.put("position_count", positionCount);
            stats.put("full_time_employees", 0);
            stats.put("part_time_employees", 0);
            stats.put("contract_employees", 0);
        }
        
        return stats;
    }
    
    public List<Map<String, Object>> getHeadcountByDepartment(UUID organizationId) {
        log.debug("Fetching headcount by department for organization: {}", organizationId);
        
        String sql = "SELECT * FROM hr.v_headcount_by_department WHERE organization_id = ? ORDER BY total_employees DESC";
        
        try {
            return jdbcTemplate.queryForList(sql, organizationId);
        } catch (Exception e) {
            log.error("Error fetching headcount by department", e);
            return new ArrayList<>();
        }
    }
    
    public List<Map<String, Object>> getRecentHires(UUID organizationId, int limit) {
        log.debug("Fetching recent hires for organization: {}", organizationId);
        
        String sql = """
            SELECT 
                employee_id,
                employee_number,
                name,
                email,
                hire_date,
                position_title,
                department_name
            FROM hr.v_employee_summary 
            WHERE organization_id = ? 
            AND employment_status = 'ACTIVE'
            ORDER BY hire_date DESC 
            LIMIT ?
            """;
        
        try {
            return jdbcTemplate.queryForList(sql, organizationId, limit);
        } catch (Exception e) {
            log.error("Error fetching recent hires", e);
            return new ArrayList<>();
        }
    }
    
    public Map<String, Object> getEmployeeSummary(UUID employeeId) {
        log.debug("Fetching employee summary for ID: {}", employeeId);
        
        String sql = "SELECT * FROM hr.v_employee_summary WHERE employee_id = ?";
        
        try {
            return jdbcTemplate.queryForMap(sql, employeeId);
        } catch (Exception e) {
            log.error("Error fetching employee summary", e);
            return new HashMap<>();
        }
    }
}

