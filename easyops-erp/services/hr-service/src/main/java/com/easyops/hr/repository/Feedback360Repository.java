package com.easyops.hr.repository;

import com.easyops.hr.entity.Feedback360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface Feedback360Repository extends JpaRepository<Feedback360, UUID> {
    
    List<Feedback360> findByOrganizationId(UUID organizationId);
    
    List<Feedback360> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<Feedback360> findByReviewerId(UUID reviewerId);
    
    List<Feedback360> findByCycleId(UUID cycleId);
    
    List<Feedback360> findByEmployeeIdAndCycleId(UUID employeeId, UUID cycleId);
}

