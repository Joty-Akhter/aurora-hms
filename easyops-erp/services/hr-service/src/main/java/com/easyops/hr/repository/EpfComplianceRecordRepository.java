package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfComplianceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EpfComplianceRecordRepository extends JpaRepository<EpfComplianceRecord, UUID> {
    
    List<EpfComplianceRecord> findByOrganizationId(UUID organizationId);
    
    List<EpfComplianceRecord> findByComplianceType(String complianceType);
    
    List<EpfComplianceRecord> findByStatus(String status);
    
    List<EpfComplianceRecord> findByOrganizationIdAndComplianceType(UUID organizationId, String complianceType);
    
    List<EpfComplianceRecord> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    @Query("SELECT cr FROM EpfComplianceRecord cr WHERE cr.organizationId = :organizationId " +
           "AND cr.dueDate < :date " +
           "AND LOWER(COALESCE(cr.status, 'pending')) NOT IN ('submitted', 'verified', 'filed')")
    List<EpfComplianceRecord> findOverdueComplianceRecords(
            @Param("organizationId") UUID organizationId,
            @Param("date") LocalDate date);
    
    @Query("SELECT cr FROM EpfComplianceRecord cr WHERE cr.organizationId = :organizationId " +
           "AND MONTH(cr.compliancePeriodStart) = :month AND YEAR(cr.compliancePeriodStart) = :year")
    List<EpfComplianceRecord> findByOrganizationIdAndComplianceMonthAndComplianceYear(
            @Param("organizationId") UUID organizationId,
            @Param("month") Integer month,
            @Param("year") Integer year);
}

