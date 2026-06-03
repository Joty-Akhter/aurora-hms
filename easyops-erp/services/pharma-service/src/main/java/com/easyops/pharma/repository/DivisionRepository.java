package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Division;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DivisionRepository extends JpaRepository<Division, UUID> {
    
    List<Division> findByOrganizationId(UUID organizationId);
    
    List<Division> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<Division> findByOrganizationIdAndCode(UUID organizationId, String code);
    
    Optional<Division> findByOrganizationIdAndName(UUID organizationId, String name);
    
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
    
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);
}

