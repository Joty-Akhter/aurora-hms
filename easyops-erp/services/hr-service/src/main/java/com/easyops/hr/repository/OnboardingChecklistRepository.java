package com.easyops.hr.repository;

import com.easyops.hr.entity.OnboardingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingChecklistRepository extends JpaRepository<OnboardingChecklist, UUID> {
    
    List<OnboardingChecklist> findByEmployeeId(UUID employeeId);
    
    List<OnboardingChecklist> findByEmployeeIdAndStatus(UUID employeeId, String status);
    
    List<OnboardingChecklist> findByOrganizationId(UUID organizationId);
    
    long countByEmployeeIdAndStatus(UUID employeeId, String status);
}

