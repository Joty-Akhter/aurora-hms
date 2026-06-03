package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, UUID> {
    
    List<Deposit> findByOrganizationId(UUID organizationId);
    
    List<Deposit> findByTerritoryId(UUID territoryId);
    
    List<Deposit> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);
    
    List<Deposit> findByOrganizationIdAndDepositDateBetween(UUID organizationId, LocalDate startDate, LocalDate endDate);
    
    List<Deposit> findByEmployeeIdAndDepositDateBetween(UUID employeeId, LocalDate startDate, LocalDate endDate);
    
    List<Deposit> findByOrganizationIdAndEmployeeIdAndDepositDateBetween(UUID organizationId, UUID employeeId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT SUM(d.depositAmount) FROM Deposit d WHERE d.territoryId = :territoryId AND d.year = :year AND d.month = :month AND d.status IN ('COMPLETED', 'SUBMITTED')")
    java.math.BigDecimal getTotalCoveredAmountForTerritoryAndMonth(@Param("territoryId") UUID territoryId, @Param("year") Integer year, @Param("month") Integer month);
}

