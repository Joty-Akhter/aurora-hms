package com.easyops.pharma.repository;

import com.easyops.pharma.entity.IncentiveDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncentiveDistributionRepository extends JpaRepository<IncentiveDistribution, UUID> {
    
    List<IncentiveDistribution> findByIncentiveCalculationId(UUID incentiveCalculationId);
    
    List<IncentiveDistribution> findByEmployeeId(UUID employeeId);
    
    List<IncentiveDistribution> findByTerritoryId(UUID territoryId);
    
    @Query("SELECT id FROM IncentiveDistribution id WHERE id.employeeId = :employeeId AND id.status = 'PAID'")
    List<IncentiveDistribution> findPaidIncentivesByEmployee(@Param("employeeId") UUID employeeId);
    
    @Query("SELECT SUM(id.incentiveAmount) FROM IncentiveDistribution id JOIN id.incentiveCalculation ic WHERE id.employeeId = :employeeId AND ic.year = :year AND ic.month = :month")
    java.math.BigDecimal getTotalIncentiveForEmployeeAndMonth(@Param("employeeId") UUID employeeId, @Param("year") Integer year, @Param("month") Integer month);
    
    @Query("SELECT id FROM IncentiveDistribution id WHERE id.incentiveCalculationId = :calculationId AND id.employeeId = :employeeId")
    List<IncentiveDistribution> findByIncentiveCalculationIdAndEmployeeId(@Param("calculationId") UUID calculationId, @Param("employeeId") UUID employeeId);
}

