package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientMedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientMedicalHistoryRepository extends JpaRepository<PatientMedicalHistory, UUID> {
    
    List<PatientMedicalHistory> findByPatientPatientId(UUID patientId);
    
    List<PatientMedicalHistory> findByPatientPatientIdAndHistoryType(
        UUID patientId, PatientMedicalHistory.HistoryType historyType);
    
    List<PatientMedicalHistory> findByPatientPatientIdAndStatus(
        UUID patientId, PatientMedicalHistory.Status status);
    
    @Query("SELECT h FROM PatientMedicalHistory h WHERE h.patient.patientId = :patientId " +
           "AND h.historyType = 'PAST_MEDICAL' ORDER BY h.onsetDate DESC")
    List<PatientMedicalHistory> findPastMedicalHistoryByPatient(@Param("patientId") UUID patientId);
    
    void deleteByPatientPatientId(UUID patientId);
}
