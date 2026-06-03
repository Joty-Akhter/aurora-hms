package com.easyops.pharma.repository;

import com.easyops.pharma.entity.TerritoryIncentiveAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TerritoryIncentiveAllocationRepository extends JpaRepository<TerritoryIncentiveAllocation, UUID> {

    List<TerritoryIncentiveAllocation> findByTerritoryIncentiveRuleId(UUID territoryIncentiveRuleId);

    @Modifying
    @Query("DELETE FROM TerritoryIncentiveAllocation t WHERE t.territoryIncentiveRuleId = :ruleId")
    void deleteByTerritoryIncentiveRuleId(@Param("ruleId") UUID territoryIncentiveRuleId);

    boolean existsByTerritoryIncentiveRuleIdAndEmployeeId(UUID territoryIncentiveRuleId, UUID employeeId);
}
