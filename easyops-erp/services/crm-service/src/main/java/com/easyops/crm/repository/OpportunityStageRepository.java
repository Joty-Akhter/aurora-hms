package com.easyops.crm.repository;

import com.easyops.crm.entity.OpportunityStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityStageRepository extends JpaRepository<OpportunityStage, UUID> {
    
    List<OpportunityStage> findByOrganizationIdOrderByStageOrder(UUID organizationId);
    
    List<OpportunityStage> findByOrganizationIdAndIsActiveOrderByStageOrder(UUID organizationId, Boolean isActive);
    
    Optional<OpportunityStage> findByOrganizationIdAndStageCode(UUID organizationId, String stageCode);
    
    boolean existsByOrganizationIdAndStageCode(UUID organizationId, String stageCode);
}

