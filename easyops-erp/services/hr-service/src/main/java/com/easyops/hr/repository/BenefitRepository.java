package com.easyops.hr.repository;

import com.easyops.hr.entity.Benefit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BenefitRepository extends JpaRepository<Benefit, UUID> {
    
    List<Benefit> findByOrganizationId(UUID organizationId);
    
    List<Benefit> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Benefit> findByOrganizationIdAndBenefitType(UUID organizationId, String benefitType);
}

