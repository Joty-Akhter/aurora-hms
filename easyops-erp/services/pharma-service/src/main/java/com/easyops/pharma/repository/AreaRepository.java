package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AreaRepository extends JpaRepository<Area, UUID> {
    
    List<Area> findByOrganizationId(UUID organizationId);
    
    List<Area> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Area> findByRegionId(UUID regionId);
    
    List<Area> findByRegionIdAndIsActive(UUID regionId, Boolean isActive);
    
    List<Area> findByDivisionId(UUID divisionId);
    
    Optional<Area> findByRegionIdAndName(UUID regionId, String name);
    
    boolean existsByRegionIdAndName(UUID regionId, String name);
    
    @Query("SELECT a FROM Area a WHERE a.organizationId = :orgId AND a.isActive = true ORDER BY a.divisionId, a.regionId, a.name")
    List<Area> findAllActiveAreasByOrganization(@Param("orgId") UUID organizationId);
}

