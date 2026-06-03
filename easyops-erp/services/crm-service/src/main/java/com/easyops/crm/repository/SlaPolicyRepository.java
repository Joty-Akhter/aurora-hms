package com.easyops.crm.repository;

import com.easyops.crm.entity.SlaPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    
    List<SlaPolicy> findByOrganizationId(UUID organizationId);
    
    List<SlaPolicy> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<SlaPolicy> findByOrganizationIdAndPriority(UUID organizationId, String priority);
    
    Optional<SlaPolicy> findByOrganizationIdAndPolicyCode(UUID organizationId, String policyCode);
    
    boolean existsByOrganizationIdAndPolicyCode(UUID organizationId, String policyCode);
}

