package com.easyops.hospital.repository;

import com.easyops.hospital.entity.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface VitalSignsRepository extends JpaRepository<VitalSigns, UUID> {
    
    List<VitalSigns> findByPatientPatientId(UUID patientId);
    
    List<VitalSigns> findByPatientPatientIdOrderByMeasurementDateDescMeasurementTimeDesc(UUID patientId);
    
    List<VitalSigns> findByPatientPatientIdAndMeasurementDate(UUID patientId, LocalDate measurementDate);
    
    List<VitalSigns> findByPatientPatientIdAndMeasurementDateBetween(
        UUID patientId, LocalDate startDate, LocalDate endDate);
    
    List<VitalSigns> findByEncounterId(UUID encounterId);
    
    @Query("SELECT v FROM VitalSigns v WHERE v.patient.patientId = :patientId " +
           "AND v.isAbnormal = true ORDER BY v.measurementDate DESC, v.measurementTime DESC")
    List<VitalSigns> findAbnormalVitalSignsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT v FROM VitalSigns v WHERE v.patient.patientId = :patientId " +
           "AND v.isCritical = true ORDER BY v.measurementDate DESC, v.measurementTime DESC")
    List<VitalSigns> findCriticalVitalSignsByPatient(@Param("patientId") UUID patientId);
    
    @Query(value = "SELECT * FROM ehr.vital_signs WHERE patient_id = CAST(:patientId AS UUID) " +
           "ORDER BY measurement_date DESC, measurement_time DESC LIMIT 1", nativeQuery = true)
    VitalSigns findLatestVitalSignsByPatient(@Param("patientId") UUID patientId);
    
    @Query(value = "SELECT patient_id, measurement_date, avg_systolic_bp, avg_diastolic_bp, " +
           "avg_heart_rate, avg_respiratory_rate, avg_temperature, avg_oxygen_saturation, " +
           "avg_weight, avg_bmi, measurement_count " +
           "FROM ehr.v_vital_signs_trends WHERE patient_id = CAST(:patientId AS UUID) " +
           "AND measurement_date >= :startDate ORDER BY measurement_date DESC", nativeQuery = true)
    List<Object[]> findVitalSignsTrends(@Param("patientId") UUID patientId, 
                                        @Param("startDate") LocalDate startDate);
    
    void deleteByPatientPatientId(UUID patientId);
}
