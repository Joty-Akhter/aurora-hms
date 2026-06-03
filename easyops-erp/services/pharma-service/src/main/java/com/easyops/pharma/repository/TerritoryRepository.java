package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Territory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerritoryRepository extends JpaRepository<Territory, UUID> {
    
    List<Territory> findByOrganizationId(UUID organizationId);
    
    List<Territory> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Territory> findByRegionId(UUID regionId);
    
    List<Territory> findByRegionIdAndIsActive(UUID regionId, Boolean isActive);
    
    List<Territory> findByAreaId(UUID areaId);
    
    List<Territory> findByAreaIdAndIsActive(UUID areaId, Boolean isActive);
    
    List<Territory> findByDivisionId(UUID divisionId);
    
    Optional<Territory> findByRegionIdAndName(UUID regionId, String name);
    
    Optional<Territory> findByAreaIdAndName(UUID areaId, String name);
    
    boolean existsByRegionIdAndName(UUID regionId, String name);
    
    boolean existsByAreaIdAndName(UUID areaId, String name);
}

