package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TargetRepository extends JpaRepository<Target, UUID> {
    
    List<Target> findByOrganizationId(UUID organizationId);
    
    List<Target> findByTerritoryId(UUID territoryId);
    
    List<Target> findByEmployeeId(UUID employeeId);
    
    List<Target> findByTerritoryIdAndYear(UUID territoryId, Integer year);
    
    @Query("SELECT t FROM Target t WHERE t.territoryId = :territoryId AND t.year = :year AND t.startMonth <= :month AND t.endMonth >= :month AND t.status = 'ACTIVE'")
    Optional<Target> findActiveTargetForTerritoryAndMonth(@Param("territoryId") UUID territoryId, @Param("year") Integer year, @Param("month") Integer month);
    
    List<Target> findByTerritoryIdAndYearAndStartMonthLessThanEqualAndEndMonthGreaterThanEqual(UUID territoryId, Integer year, Integer startMonth, Integer endMonth);
}

