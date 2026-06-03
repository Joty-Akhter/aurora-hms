package com.easyops.hr.repository;

import com.easyops.hr.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    
    List<AttendanceRecord> findByOrganizationId(UUID organizationId);
    
    List<AttendanceRecord> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate attendanceDate);
    
    List<AttendanceRecord> findByOrganizationIdAndAttendanceDateBetween(
            UUID organizationId, LocalDate startDate, LocalDate endDate);
    
    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetween(
            UUID employeeId, LocalDate startDate, LocalDate endDate);
    
    List<AttendanceRecord> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.organizationId = :organizationId " +
           "AND ar.attendanceDate = :date")
    List<AttendanceRecord> findTodayAttendance(@Param("organizationId") UUID organizationId, 
                                               @Param("date") LocalDate date);
    
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.employeeId = :employeeId " +
           "AND ar.attendanceDate >= :startDate AND ar.attendanceDate <= :endDate " +
           "ORDER BY ar.attendanceDate DESC")
    List<AttendanceRecord> findEmployeeAttendanceInRange(@Param("employeeId") UUID employeeId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
}

