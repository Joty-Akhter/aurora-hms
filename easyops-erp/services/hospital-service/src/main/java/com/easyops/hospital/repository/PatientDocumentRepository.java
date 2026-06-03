package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PatientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientDocumentRepository extends JpaRepository<PatientDocument, UUID> {

    @Query("SELECT d FROM PatientDocument d WHERE d.patientId = :patientId AND d.isActive = true ORDER BY d.uploadedDate DESC")
    List<PatientDocument> findActiveByPatientId(@Param("patientId") UUID patientId);

    @Query("SELECT d FROM PatientDocument d WHERE d.patientId = :patientId AND d.documentType = :type AND d.isActive = true ORDER BY d.uploadedDate DESC")
    List<PatientDocument> findActiveByPatientIdAndType(@Param("patientId") UUID patientId,
                                                        @Param("type") PatientDocument.DocumentType type);

    @Query("SELECT d FROM PatientDocument d WHERE d.clinicalNoteId = :noteId AND d.isActive = true ORDER BY d.uploadedDate DESC")
    List<PatientDocument> findActiveByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT d FROM PatientDocument d WHERE d.labResultId = :labResultId AND d.isActive = true ORDER BY d.uploadedDate DESC")
    List<PatientDocument> findActiveByLabResultId(@Param("labResultId") UUID labResultId);

    @Query("SELECT d FROM PatientDocument d WHERE d.prescriptionId = :prescriptionId AND d.isActive = true ORDER BY d.uploadedDate DESC")
    List<PatientDocument> findActiveByPrescriptionId(@Param("prescriptionId") UUID prescriptionId);
}
