package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionAllergyCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionAllergyCheckRepository extends JpaRepository<PrescriptionAllergyCheck, UUID> {
    
    List<PrescriptionAllergyCheck> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    List<PrescriptionAllergyCheck> findByPrescriptionPrescriptionIdAndSeverity(
        UUID prescriptionId, PrescriptionAllergyCheck.AllergySeverity severity);
    
    @Query("SELECT a FROM PrescriptionAllergyCheck a WHERE a.prescription.prescriptionId = :prescriptionId " +
           "AND a.isAcknowledged = false ORDER BY " +
           "CASE a.severity " +
           "WHEN 'LIFE_THREATENING' THEN 1 " +
           "WHEN 'SEVERE' THEN 2 " +
           "WHEN 'MODERATE' THEN 3 " +
           "WHEN 'MILD' THEN 4 " +
           "ELSE 5 END")
    List<PrescriptionAllergyCheck> findUnacknowledgedAllergyChecks(@Param("prescriptionId") UUID prescriptionId);
    
    void deleteByPrescriptionPrescriptionId(UUID prescriptionId);
}
