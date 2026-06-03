package com.easyops.hospital.repository;

import com.easyops.hospital.entity.MedicationReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationReconciliationRepository extends JpaRepository<MedicationReconciliation, UUID> {
    
    List<MedicationReconciliation> findByPatientPatientIdOrderByReconciliationDateDesc(UUID patientId);
    
    @Query("SELECT mr FROM MedicationReconciliation mr WHERE mr.patient.patientId = :patientId AND mr.reconciliationStatus = :status ORDER BY mr.reconciliationDate DESC")
    List<MedicationReconciliation> findByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") MedicationReconciliation.ReconciliationStatus status);
    
    @Query("SELECT mr FROM MedicationReconciliation mr WHERE mr.patient.patientId = :patientId AND mr.reconciliationType = :type ORDER BY mr.reconciliationDate DESC")
    List<MedicationReconciliation> findByPatientIdAndType(@Param("patientId") UUID patientId, @Param("type") MedicationReconciliation.ReconciliationType type);
    
    List<MedicationReconciliation> findByEncounterId(UUID encounterId);
    
    @Query("SELECT mr FROM MedicationReconciliation mr WHERE mr.patient.patientId = :patientId AND mr.reconciliationDate >= :startDate AND mr.reconciliationDate <= :endDate ORDER BY mr.reconciliationDate DESC")
    List<MedicationReconciliation> findByPatientIdAndDateRange(@Param("patientId") UUID patientId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT mr FROM MedicationReconciliation mr WHERE mr.patient.patientId = :patientId AND mr.reconciliationStatus = 'COMPLETED' ORDER BY mr.reconciliationDate DESC")
    Optional<MedicationReconciliation> findLatestCompletedReconciliation(@Param("patientId") UUID patientId);
}
