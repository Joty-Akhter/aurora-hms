package com.easyops.accounting.repository;

import com.easyops.accounting.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, UUID> {
    
    List<FiscalYear> findByOrganizationIdOrderByStartDateDesc(UUID organizationId);
    
    Optional<FiscalYear> findByOrganizationIdAndYearCode(UUID organizationId, String yearCode);
    
    @Query("SELECT f FROM FiscalYear f WHERE f.organizationId = :orgId AND :date BETWEEN f.startDate AND f.endDate")
    Optional<FiscalYear> findByOrganizationIdAndDate(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT f FROM FiscalYear f WHERE f.organizationId = :orgId AND f.isClosed = false ORDER BY f.startDate")
    List<FiscalYear> findOpenYears(@Param("orgId") UUID organizationId);
}

