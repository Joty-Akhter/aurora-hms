package com.easyops.crm.repository;

import com.easyops.crm.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    
    List<EmailTemplate> findByOrganizationId(UUID organizationId);
    
    List<EmailTemplate> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<EmailTemplate> findByOrganizationIdAndTemplateType(UUID organizationId, String templateType);
    
    Optional<EmailTemplate> findByOrganizationIdAndTemplateCode(UUID organizationId, String templateCode);
    
    boolean existsByOrganizationIdAndTemplateCode(UUID organizationId, String templateCode);
}

