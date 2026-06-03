package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegionRepository extends JpaRepository<Region, UUID> {
    
    List<Region> findByOrganizationId(UUID organizationId);
    
    List<Region> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Region> findByDivisionId(UUID divisionId);
    
    List<Region> findByDivisionIdAndIsActive(UUID divisionId, Boolean isActive);
    
    Optional<Region> findByDivisionIdAndName(UUID divisionId, String name);
    
    boolean existsByDivisionIdAndName(UUID divisionId, String name);
}

