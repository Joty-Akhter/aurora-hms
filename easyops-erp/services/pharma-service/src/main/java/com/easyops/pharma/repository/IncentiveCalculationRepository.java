package com.easyops.pharma.repository;

import com.easyops.pharma.entity.IncentiveCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncentiveCalculationRepository extends JpaRepository<IncentiveCalculation, UUID> {
    
    List<IncentiveCalculation> findByOrganizationId(UUID organizationId);
    
    List<IncentiveCalculation> findByTerritoryId(UUID territoryId);
    
    Optional<IncentiveCalculation> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);
    
    @Query("SELECT ic FROM IncentiveCalculation ic WHERE ic.territoryId = :territoryId AND ic.year = :year ORDER BY ic.month")
    List<IncentiveCalculation> findByTerritoryAndYear(@Param("territoryId") UUID territoryId, @Param("year") Integer year);
}

