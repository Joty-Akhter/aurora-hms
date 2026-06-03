package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, UUID> {
    
    List<LabResult> findByPatientPatientId(UUID patientId);
    
    List<LabResult> findByPatientPatientIdOrderByResultDateDesc(UUID patientId);
    
    List<LabResult> findByOrderOrderId(UUID orderId);
    
    List<LabResult> findByEncounterId(UUID encounterId);
    
    Optional<LabResult> findByResultNumber(String resultNumber);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.loincCode = :loincCode ORDER BY r.resultDate DESC")
    List<LabResult> findResultsByLoincCode(
        @Param("patientId") UUID patientId,
        @Param("loincCode") String loincCode);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.testCategory = :category ORDER BY r.resultDate DESC")
    List<LabResult> findResultsByCategory(
        @Param("patientId") UUID patientId,
        @Param("category") String category);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.isCriticalValue = true AND r.isCriticalValueAcknowledged = false " +
           "ORDER BY r.resultDate DESC")
    List<LabResult> findUnacknowledgedCriticalValues(@Param("patientId") UUID patientId);
    
    @Query("SELECT r FROM LabResult r WHERE r.isCriticalValue = true " +
           "AND r.isCriticalValueAcknowledged = false ORDER BY r.resultDate DESC")
    List<LabResult> findAllUnacknowledgedCriticalValues();
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.isReviewed = false ORDER BY r.resultDate DESC")
    List<LabResult> findUnreviewedResults(@Param("patientId") UUID patientId);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.resultDate >= :startDate AND r.resultDate <= :endDate " +
           "ORDER BY r.resultDate DESC")
    List<LabResult> findResultsByDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.abnormalFlag IS NOT NULL AND r.abnormalFlag != 'N' " +
           "ORDER BY r.resultDate DESC")
    List<LabResult> findAbnormalResults(@Param("patientId") UUID patientId);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.resultStatus = :status ORDER BY r.resultDate DESC")
    List<LabResult> findResultsByStatus(
        @Param("patientId") UUID patientId,
        @Param("status") LabResult.ResultStatus status);
    
    @Query("SELECT r FROM LabResult r WHERE r.order.orderId = :orderId " +
           "ORDER BY r.resultDate DESC")
    List<LabResult> findResultsByOrder(@Param("orderId") UUID orderId);
    
    @Query("SELECT r FROM LabResult r WHERE r.patient.patientId = :patientId " +
           "AND r.loincCode = :loincCode AND r.resultDate < :beforeDate " +
           "ORDER BY r.resultDate DESC LIMIT 1")
    Optional<LabResult> findPreviousResult(
        @Param("patientId") UUID patientId,
        @Param("loincCode") String loincCode,
        @Param("beforeDate") LocalDateTime beforeDate);
    
    void deleteByPatientPatientId(UUID patientId);
}
