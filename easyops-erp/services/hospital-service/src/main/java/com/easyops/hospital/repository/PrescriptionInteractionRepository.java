package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PrescriptionInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionInteractionRepository extends JpaRepository<PrescriptionInteraction, UUID> {
    
    List<PrescriptionInteraction> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    List<PrescriptionInteraction> findByPrescriptionPrescriptionIdAndSeverity(
        UUID prescriptionId, PrescriptionInteraction.InteractionSeverity severity);
    
    @Query("SELECT i FROM PrescriptionInteraction i WHERE i.prescription.prescriptionId = :prescriptionId " +
           "AND i.isAcknowledged = false ORDER BY " +
           "CASE i.severity " +
           "WHEN 'CONTRAINDICATED' THEN 1 " +
           "WHEN 'MAJOR' THEN 2 " +
           "WHEN 'MODERATE' THEN 3 " +
           "WHEN 'MINOR' THEN 4 " +
           "ELSE 5 END")
    List<PrescriptionInteraction> findUnacknowledgedInteractions(@Param("prescriptionId") UUID prescriptionId);
    
    void deleteByPrescriptionPrescriptionId(UUID prescriptionId);
}
