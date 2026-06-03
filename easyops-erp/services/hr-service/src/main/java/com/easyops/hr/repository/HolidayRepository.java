package com.easyops.hr.repository;

import com.easyops.hr.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    
    List<Holiday> findByOrganizationId(UUID organizationId);
    
    List<Holiday> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    @Query("SELECT h FROM Holiday h WHERE h.organizationId = :organizationId " +
           "AND h.holidayDate >= :startDate AND h.holidayDate <= :endDate " +
           "AND h.isActive = true " +
           "ORDER BY h.holidayDate")
    List<Holiday> findHolidaysInRange(@Param("organizationId") UUID organizationId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    /** Org-wide holidays only (loan installment shifting / institution-wide non-working days). */
    @Query("SELECT h FROM Holiday h WHERE h.organizationId = :organizationId " +
           "AND h.holidayDate >= :startDate AND h.holidayDate <= :endDate " +
           "AND h.isActive = true AND h.departmentId IS NULL AND h.employeeId IS NULL " +
           "ORDER BY h.holidayDate")
    List<Holiday> findOrgWideActiveHolidaysInRange(@Param("organizationId") UUID organizationId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
}

