package com.easyops.accounting.repository;

import com.easyops.accounting.entity.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PeriodRepository extends JpaRepository<Period, UUID> {
    
    List<Period> findByOrganizationIdOrderByPeriodNumber(UUID organizationId);
    
    List<Period> findByFiscalYearIdOrderByPeriodNumber(UUID fiscalYearId);
    
    List<Period> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    @Query("SELECT p FROM Period p WHERE p.organizationId = :orgId AND :date BETWEEN p.startDate AND p.endDate")
    Optional<Period> findByOrganizationIdAndDate(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT p FROM Period p WHERE p.organizationId = :orgId AND p.status = 'OPEN' ORDER BY p.periodNumber")
    List<Period> findOpenPeriods(@Param("orgId") UUID organizationId);
}

