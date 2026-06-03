package com.easyops.notification.repository;

import com.easyops.notification.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    
    Optional<EmailTemplate> findByName(String name);
    
    @Query("SELECT t FROM EmailTemplate t WHERE t.name = :name AND (t.organizationId = :orgId OR t.organizationId IS NULL) AND t.isActive = true ORDER BY t.organizationId DESC NULLS LAST")
    Optional<EmailTemplate> findByNameAndOrganizationId(@Param("name") String name, @Param("orgId") UUID organizationId);
    
    List<EmailTemplate> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<EmailTemplate> findByOrganizationIdIsNullAndIsActive(Boolean isActive);
}

