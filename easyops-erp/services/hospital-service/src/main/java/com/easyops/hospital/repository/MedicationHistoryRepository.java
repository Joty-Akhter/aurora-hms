package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Medication;
import com.easyops.hospital.entity.MedicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationHistoryRepository extends JpaRepository<MedicationHistory, UUID> {
    
    List<MedicationHistory> findByPatientPatientIdOrderByStartDateDesc(UUID patientId);
    
    List<MedicationHistory> findByMedicationMedicationIdOrderByStatusDateDesc(UUID medicationId);
    
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND mh.medicationStatus = :status ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndStatus(@Param("patientId") UUID patientId, @Param("status") Medication.MedicationStatus status);
    
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND mh.startDate <= :date AND (mh.endDate IS NULL OR mh.endDate >= :date) ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndDate(@Param("patientId") UUID patientId, @Param("date") LocalDate date);
    
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND mh.medicationCode = :code ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndMedicationCode(@Param("patientId") UUID patientId, @Param("code") String code);
    
    // Search by medication name (case-insensitive partial match)
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND " +
           "LOWER(mh.medicationName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndMedicationNameContaining(@Param("patientId") UUID patientId, @Param("searchTerm") String searchTerm);
    
    // Search by date range
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND " +
           "mh.startDate >= :startDate AND (mh.endDate IS NULL OR mh.endDate <= :endDate) ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndDateRange(@Param("patientId") UUID patientId, 
                                                         @Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);
    
    // Search by status and date range
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND " +
           "mh.medicationStatus = :status AND mh.startDate >= :startDate AND " +
           "(mh.endDate IS NULL OR mh.endDate <= :endDate) ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndStatusAndDateRange(@Param("patientId") UUID patientId,
                                                                  @Param("status") Medication.MedicationStatus status,
                                                                  @Param("startDate") LocalDate startDate,
                                                                  @Param("endDate") LocalDate endDate);
    
    // Search by generic name
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND " +
           "LOWER(mh.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY mh.startDate DESC")
    List<MedicationHistory> findByPatientIdAndGenericNameContaining(@Param("patientId") UUID patientId, @Param("searchTerm") String searchTerm);
    
    // Get complete medication history from first prescription to current (all records)
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId ORDER BY mh.startDate ASC, mh.statusDate ASC")
    List<MedicationHistory> findCompleteHistoryByPatientOrderByStartDateAsc(@Param("patientId") UUID patientId);
    
    // Get medications with discontinuation reason
    @Query("SELECT mh FROM MedicationHistory mh WHERE mh.patient.patientId = :patientId AND " +
           "mh.medicationStatus = 'DISCONTINUED' AND mh.discontinuationReason IS NOT NULL ORDER BY mh.statusDate DESC")
    List<MedicationHistory> findDiscontinuedMedicationsWithReason(@Param("patientId") UUID patientId);
}
