package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingStudyMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImagingStudyMedicationRepository extends JpaRepository<ImagingStudyMedication, UUID> {
    
    List<ImagingStudyMedication> findByStudyStudyId(UUID studyId);
    
    List<ImagingStudyMedication> findByPrescriptionPrescriptionId(UUID prescriptionId);
    
    @Query("SELECT l FROM ImagingStudyMedication l WHERE l.study.studyId = :studyId " +
           "ORDER BY l.linkedDate DESC")
    List<ImagingStudyMedication> findByStudyIdOrdered(@Param("studyId") UUID studyId);
    
    @Query("SELECT l FROM ImagingStudyMedication l WHERE l.prescription.prescriptionId = :prescriptionId " +
           "ORDER BY l.linkedDate DESC")
    List<ImagingStudyMedication> findByPrescriptionIdOrdered(@Param("prescriptionId") UUID prescriptionId);
    
    void deleteByStudyStudyId(UUID studyId);
    
    void deleteByPrescriptionPrescriptionId(UUID prescriptionId);
}
