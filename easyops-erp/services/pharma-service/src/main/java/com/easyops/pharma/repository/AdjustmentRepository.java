package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Adjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdjustmentRepository extends JpaRepository<Adjustment, UUID> {
    
    List<Adjustment> findByOrganizationId(UUID organizationId);
    
    List<Adjustment> findByTerritoryId(UUID territoryId);
    
    List<Adjustment> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);
    
    List<Adjustment> findByAdjustmentType(String adjustmentType); // DAMAGE, EXPIRY
}

