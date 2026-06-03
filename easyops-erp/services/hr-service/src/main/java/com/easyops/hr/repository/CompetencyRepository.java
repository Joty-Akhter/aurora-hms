package com.easyops.hr.repository;

import com.easyops.hr.entity.Competency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetencyRepository extends JpaRepository<Competency, UUID> {
    
    List<Competency> findByOrganizationId(UUID organizationId);
    
    List<Competency> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Competency> findByOrganizationIdAndCompetencyCategory(UUID organizationId, String category);
}

