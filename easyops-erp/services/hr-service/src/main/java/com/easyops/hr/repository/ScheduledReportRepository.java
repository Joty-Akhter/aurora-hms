package com.easyops.hr.repository;

import com.easyops.hr.entity.ScheduledReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, UUID> {
    
    List<ScheduledReport> findByOrganizationId(UUID organizationId);
    
    List<ScheduledReport> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    @Query("SELECT sr FROM ScheduledReport sr WHERE sr.isActive = true " +
           "AND sr.nextRunDate <= :date")
    List<ScheduledReport> findReportsDueForExecution(@Param("date") LocalDate date);
}

