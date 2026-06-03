package com.easyops.crm.repository;

import com.easyops.crm.entity.LeadSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadSourceRepository extends JpaRepository<LeadSource, UUID> {
    
    List<LeadSource> findByOrganizationId(UUID organizationId);
    
    List<LeadSource> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<LeadSource> findByOrganizationIdAndSourceType(UUID organizationId, String sourceType);
}


