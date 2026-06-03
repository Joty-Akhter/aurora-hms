package com.easyops.pharma.repository;

import com.easyops.pharma.entity.TerritoryIncentiveRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerritoryIncentiveRuleRepository extends JpaRepository<TerritoryIncentiveRule, UUID> {

    List<TerritoryIncentiveRule> findByOrganizationId(UUID organizationId);

    List<TerritoryIncentiveRule> findByTerritoryId(UUID territoryId);

    Optional<TerritoryIncentiveRule> findByTerritoryIdAndIsActive(UUID territoryId, Boolean isActive);

    @Query("SELECT r FROM TerritoryIncentiveRule r WHERE r.territoryId = :territoryId AND r.isActive = true " +
            "AND (r.effectiveFromDate IS NULL OR r.effectiveFromDate <= :date) " +
            "AND (r.effectiveToDate IS NULL OR r.effectiveToDate >= :date)")
    Optional<TerritoryIncentiveRule> findActiveRuleForTerritoryAndDate(
            @Param("territoryId") UUID territoryId, @Param("date") LocalDate date);

    List<TerritoryIncentiveRule> findByTerritoryIdAndIsActiveOrderByRuleVersionDesc(UUID territoryId, Boolean isActive);

    @Query("SELECT r FROM TerritoryIncentiveRule r WHERE r.territoryId = :territoryId ORDER BY r.ruleVersion DESC")
    List<TerritoryIncentiveRule> findHistoryByTerritoryId(@Param("territoryId") UUID territoryId);
}
