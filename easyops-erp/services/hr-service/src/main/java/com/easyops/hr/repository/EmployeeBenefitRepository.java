package com.easyops.hr.repository;

import com.easyops.hr.entity.EmployeeBenefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeBenefitRepository extends JpaRepository<EmployeeBenefit, UUID> {
    
    List<EmployeeBenefit> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<EmployeeBenefit> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<EmployeeBenefit> findByBenefitId(UUID benefitId);
}

