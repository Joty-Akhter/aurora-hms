package com.easyops.hr.repository;

import com.easyops.hr.entity.PerformanceCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceCycleRepository extends JpaRepository<PerformanceCycle, UUID> {
    
    List<PerformanceCycle> findByOrganizationId(UUID organizationId);
    
    List<PerformanceCycle> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<PerformanceCycle> findByOrganizationIdOrderByStartDateDesc(UUID organizationId);
}

