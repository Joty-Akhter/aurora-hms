package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PDMPQueryResult;
import com.easyops.hospital.entity.Prescription;
import com.easyops.hospital.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PDMPQueryResultRepository extends JpaRepository<PDMPQueryResult, UUID> {
    
    /**
     * Find all PDMP query results for a prescription
     */
    List<PDMPQueryResult> findByPrescriptionPrescriptionIdOrderByQueryDateDesc(UUID prescriptionId);
    
    /**
     * Find the most recent PDMP query result for a prescription
     */
    Optional<PDMPQueryResult> findFirstByPrescriptionPrescriptionIdOrderByQueryDateDesc(UUID prescriptionId);
    
    /**
     * Find all PDMP query results for a patient
     */
    List<PDMPQueryResult> findByPatientPatientIdOrderByQueryDateDesc(UUID patientId);
    
    /**
     * Find PDMP query results for a patient within a date range
     */
    @Query("SELECT p FROM PDMPQueryResult p WHERE p.patient.patientId = :patientId " +
           "AND p.queryDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.queryDate DESC")
    List<PDMPQueryResult> findByPatientAndDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find PDMP query results by querying provider
     */
    List<PDMPQueryResult> findByQueryingProviderIdOrderByQueryDateDesc(UUID providerId);
    
    /**
     * Find PDMP query results by state
     */
    List<PDMPQueryResult> findByQueryStateOrderByQueryDateDesc(String state);
    
    /**
     * Find successful PDMP query results for a prescription
     */
    @Query("SELECT p FROM PDMPQueryResult p WHERE p.prescription.prescriptionId = :prescriptionId " +
           "AND p.querySuccess = true ORDER BY p.queryDate DESC")
    List<PDMPQueryResult> findSuccessfulQueriesByPrescription(@Param("prescriptionId") UUID prescriptionId);
    
    /**
     * Find PDMP query results with high risk level
     */
    @Query("SELECT p FROM PDMPQueryResult p WHERE p.riskLevel = 'HIGH' OR p.riskLevel = 'CRITICAL' " +
           "ORDER BY p.queryDate DESC")
    List<PDMPQueryResult> findHighRiskQueries();
    
    /**
     * Find PDMP query results for a patient with high risk
     */
    @Query("SELECT p FROM PDMPQueryResult p WHERE p.patient.patientId = :patientId " +
           "AND (p.riskLevel = 'HIGH' OR p.riskLevel = 'CRITICAL') " +
           "ORDER BY p.queryDate DESC")
    List<PDMPQueryResult> findHighRiskQueriesByPatient(@Param("patientId") UUID patientId);
    
    /**
     * Count queries for a patient in the last N days
     */
    @Query("SELECT COUNT(p) FROM PDMPQueryResult p WHERE p.patient.patientId = :patientId " +
           "AND p.queryDate >= :since")
    Long countQueriesSince(@Param("patientId") UUID patientId, @Param("since") LocalDateTime since);
}
