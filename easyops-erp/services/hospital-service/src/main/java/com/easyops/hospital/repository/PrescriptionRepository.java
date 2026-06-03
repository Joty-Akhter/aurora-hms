package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    
    List<Prescription> findByPatientPatientId(UUID patientId);

    @Query("SELECT DISTINCT p FROM Prescription p LEFT JOIN FETCH p.medications WHERE p.patient.patientId = :patientId ORDER BY p.createdDate DESC")
    List<Prescription> findByPatientPatientIdOrderByCreatedDateDesc(@Param("patientId") UUID patientId);
    
    List<Prescription> findByPatientPatientIdAndPrescriptionStatus(
        UUID patientId, Prescription.PrescriptionStatus status);
    
    List<Prescription> findByEncounterId(UUID encounterId);
    
    List<Prescription> findByPrescribingProviderId(UUID providerId);
    
    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);

    @Query("SELECT DISTINCT p FROM Prescription p LEFT JOIN FETCH p.medications WHERE p.prescriptionId = :id")
    Optional<Prescription> findByIdWithMedications(@Param("id") UUID id);

    @Query("SELECT DISTINCT p FROM Prescription p LEFT JOIN FETCH p.medications WHERE p.patient.patientId = :patientId " +
           "AND p.prescriptionStatus IN ('SENT', 'FILLED', 'PARTIALLY_FILLED') " +
           "ORDER BY p.createdDate DESC")
    List<Prescription> findActivePrescriptionsByPatient(@Param("patientId") UUID patientId);

    @Query("SELECT DISTINCT p FROM Prescription p LEFT JOIN FETCH p.medications WHERE p.patient.patientId = :patientId " +
           "AND p.prescriptionStatus = 'DRAFT' ORDER BY p.createdDate DESC")
    List<Prescription> findDraftPrescriptionsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.patientId = :patientId " +
           "AND p.isControlledSubstance = true ORDER BY p.createdDate DESC")
    List<Prescription> findControlledSubstancePrescriptionsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.patientId = :patientId " +
           "AND p.hasInteractions = true ORDER BY p.createdDate DESC")
    List<Prescription> findPrescriptionsWithInteractions(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.patientId = :patientId " +
           "AND p.hasAllergyWarnings = true ORDER BY p.createdDate DESC")
    List<Prescription> findPrescriptionsWithAllergyWarnings(@Param("patientId") UUID patientId);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.patientId = :patientId " +
           "AND p.startDate >= :startDate AND (p.endDate IS NULL OR p.endDate <= :endDate) " +
           "ORDER BY p.startDate DESC")
    List<Prescription> findPrescriptionsByDateRange(
        @Param("patientId") UUID patientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM Prescription p WHERE p.medicationCode = :medicationCode " +
           "AND p.patient.patientId = :patientId")
    List<Prescription> findPrescriptionsByMedicationCode(
        @Param("patientId") UUID patientId,
        @Param("medicationCode") String medicationCode);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.patientId = :patientId " +
           "AND p.refillsRemaining > 0 ORDER BY p.createdDate DESC")
    List<Prescription> findPrescriptionsWithRefillsRemaining(@Param("patientId") UUID patientId);
    
    void deleteByPatientPatientId(UUID patientId);
}
