package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {
    
    List<Medication> findByPatientPatientIdOrderByStartDateDesc(UUID patientId);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.medicationStatus = :status ORDER BY m.startDate DESC")
    List<Medication> findByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") Medication.MedicationStatus status);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.medicationStatus = 'ACTIVE' ORDER BY m.startDate DESC")
    List<Medication> findActiveMedicationsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.medicationStatus = 'DISCONTINUED' ORDER BY m.startDate DESC")
    List<Medication> findDiscontinuedMedicationsByPatient(@Param("patientId") UUID patientId);
    
    List<Medication> findByPrescriptionId(UUID prescriptionId);
    
    List<Medication> findByEncounterId(UUID encounterId);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.medicationSource = :source ORDER BY m.startDate DESC")
    List<Medication> findByPatientIdAndSource(@Param("patientId") UUID patientId, @Param("source") Medication.MedicationSource source);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.medicationCode = :code")
    List<Medication> findByPatientIdAndMedicationCode(@Param("patientId") UUID patientId, @Param("code") String code);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.rxnormCode = :rxnormCode")
    List<Medication> findByPatientIdAndRxnormCode(@Param("patientId") UUID patientId, @Param("rxnormCode") String rxnormCode);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.indication IS NOT NULL AND LOWER(m.indication) LIKE LOWER(CONCAT('%', :indication, '%')) ORDER BY m.startDate DESC")
    List<Medication> findByPatientIdAndIndicationContaining(@Param("patientId") UUID patientId, @Param("indication") String indication);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.startDate BETWEEN :startDate AND :endDate ORDER BY m.startDate DESC")
    List<Medication> findByPatientIdAndDateRange(@Param("patientId") UUID patientId, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
    
    @Query("SELECT m FROM Medication m WHERE m.patient.patientId = :patientId AND m.medicationStatus = :status AND m.startDate BETWEEN :startDate AND :endDate ORDER BY m.startDate DESC")
    List<Medication> findByPatientIdAndStatusAndDateRange(@Param("patientId") UUID patientId, @Param("status") Medication.MedicationStatus status, @Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);
    
    @Query("SELECT DISTINCT m.indication FROM Medication m WHERE m.patient.patientId = :patientId AND m.indication IS NOT NULL AND m.indication != '' ORDER BY m.indication")
    List<String> findDistinctIndicationsByPatientId(@Param("patientId") UUID patientId);
}
