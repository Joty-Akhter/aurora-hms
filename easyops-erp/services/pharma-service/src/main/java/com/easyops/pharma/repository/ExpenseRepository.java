package com.easyops.pharma.repository;

import com.easyops.pharma.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    
    List<Expense> findByOrganizationId(UUID organizationId);
    
    List<Expense> findByTerritoryId(UUID territoryId);
    
    List<Expense> findByTerritoryIdAndYearAndMonth(UUID territoryId, Integer year, Integer month);
    
    @Query("SELECT SUM(e.expenseAmount) FROM Expense e WHERE e.territoryId = :territoryId AND e.year = :year AND e.month = :month AND e.status = 'SUBMITTED'")
    java.math.BigDecimal getTotalExpensesForTerritoryAndMonth(@Param("territoryId") UUID territoryId, @Param("year") Integer year, @Param("month") Integer month);
    
    List<Expense> findByOrganizationIdAndExpenseDateBetween(UUID organizationId, LocalDate startDate, LocalDate endDate);
}

