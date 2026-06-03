package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabResultMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultMedicationRepository extends JpaRepository<LabResultMedication, UUID> {
    
    List<LabResultMedication> findByResultResultId(UUID resultId);
    
    List<LabResultMedication> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    @Query("SELECT lrm FROM LabResultMedication lrm WHERE lrm.result.resultId = :resultId AND lrm.prescription.prescriptionId = :prescriptionId")
    LabResultMedication findByResultIdAndPrescriptionId(@Param("resultId") UUID resultId, @Param("prescriptionId") UUID prescriptionId);
    
    @Query("SELECT lrm FROM LabResultMedication lrm WHERE lrm.organizationId = :organizationId")
    List<LabResultMedication> findByOrganizationId(@Param("organizationId") UUID organizationId);
}
