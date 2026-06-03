package com.easyops.pharma.repository;

import com.easyops.pharma.entity.TargetCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TargetCoverageRepository extends JpaRepository<TargetCoverage, UUID> {
    
    List<TargetCoverage> findByTargetId(UUID targetId);
    
    List<TargetCoverage> findByTerritoryId(UUID territoryId);
    
    Optional<TargetCoverage> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);
    
    @Query("SELECT tc FROM TargetCoverage tc WHERE tc.territoryId = :territoryId AND tc.year = :year ORDER BY tc.month")
    List<TargetCoverage> findByTerritoryAndYear(@Param("territoryId") UUID territoryId, @Param("year") Integer year);
}

