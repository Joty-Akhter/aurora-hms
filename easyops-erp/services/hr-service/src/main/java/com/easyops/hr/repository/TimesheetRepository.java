package com.easyops.hr.repository;

import com.easyops.hr.entity.Timesheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {
    
    List<Timesheet> findByOrganizationId(UUID organizationId);
    
    List<Timesheet> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    Optional<Timesheet> findByEmployeeIdAndWeekStartDate(UUID employeeId, LocalDate weekStartDate);
    
    List<Timesheet> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Timesheet> findByEmployeeIdAndStatus(UUID employeeId, String status);
    
    @Query("SELECT t FROM Timesheet t WHERE t.organizationId = :organizationId " +
           "AND t.weekStartDate >= :startDate AND t.weekStartDate <= :endDate " +
           "ORDER BY t.weekStartDate DESC")
    List<Timesheet> findTimesheetsInRange(@Param("organizationId") UUID organizationId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM Timesheet t WHERE t.employeeId = :employeeId " +
           "AND t.weekStartDate >= :startDate AND t.weekStartDate <= :endDate " +
           "ORDER BY t.weekStartDate DESC")
    List<Timesheet> findEmployeeTimesheetsInRange(@Param("employeeId") UUID employeeId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /** Weeks overlapping the pay period (inclusive) for overtime roll-up from timesheets. */
    @Query("SELECT t FROM Timesheet t WHERE t.employeeId = :employeeId " +
           "AND t.weekStartDate <= :periodEnd AND t.weekEndDate >= :periodStart " +
           "ORDER BY t.weekStartDate ASC")
    List<Timesheet> findByEmployeeIdAndWeekOverlappingPeriod(@Param("employeeId") UUID employeeId,
                                                             @Param("periodStart") LocalDate periodStart,
                                                             @Param("periodEnd") LocalDate periodEnd);
}

