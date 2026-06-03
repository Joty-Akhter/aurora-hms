package com.easyops.hr.repository;

import com.easyops.hr.entity.DevelopmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DevelopmentPlanRepository extends JpaRepository<DevelopmentPlan, UUID> {
    
    List<DevelopmentPlan> findByOrganizationId(UUID organizationId);
    
    List<DevelopmentPlan> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<DevelopmentPlan> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<DevelopmentPlan> findByEmployeeIdAndStatus(UUID employeeId, String status);
}

