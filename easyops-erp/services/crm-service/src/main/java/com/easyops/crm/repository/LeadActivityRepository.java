package com.easyops.crm.repository;

import com.easyops.crm.entity.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadActivityRepository extends JpaRepository<LeadActivity, UUID> {
    
    List<LeadActivity> findByLeadIdOrderByActivityDateDesc(UUID leadId);
    
    List<LeadActivity> findByOrganizationIdOrderByActivityDateDesc(UUID organizationId);
    
    List<LeadActivity> findByLeadIdAndActivityType(UUID leadId, String activityType);
    
    List<LeadActivity> findByLeadIdAndStatus(UUID leadId, String status);
}


