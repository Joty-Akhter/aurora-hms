package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID> {
    
    List<ClinicalNote> findByPatientPatientId(UUID patientId);
    
    List<ClinicalNote> findByPatientPatientIdOrderByNoteDateDescNoteTimeDesc(UUID patientId);
    
    List<ClinicalNote> findByPatientPatientIdAndNoteType(UUID patientId, ClinicalNote.NoteType noteType);
    
    List<ClinicalNote> findByPatientPatientIdAndNoteStatus(UUID patientId, ClinicalNote.NoteStatus noteStatus);
    
    List<ClinicalNote> findByPatientPatientIdAndNoteDateBetween(
        UUID patientId, LocalDate startDate, LocalDate endDate);
    
    List<ClinicalNote> findByEncounterId(UUID encounterId);
    
    List<ClinicalNote> findByCreatedBy(UUID createdBy);
    
    List<ClinicalNote> findBySignedBy(UUID signedBy);
    
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.patientId = :patientId " +
           "AND n.isCurrentVersion = true ORDER BY n.noteDate DESC, n.noteTime DESC")
    List<ClinicalNote> findCurrentVersionsByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT n FROM ClinicalNote n WHERE n.originalNote.noteId = :originalNoteId " +
           "ORDER BY n.versionNumber DESC")
    List<ClinicalNote> findAmendmentsByOriginalNote(@Param("originalNoteId") UUID originalNoteId);
    
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.patientId = :patientId " +
           "AND n.noteStatus = 'SIGNED' ORDER BY n.noteDate DESC, n.noteTime DESC")
    List<ClinicalNote> findSignedNotesByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.patientId = :patientId " +
           "AND n.noteStatus = 'DRAFT' ORDER BY n.noteDate DESC, n.noteTime DESC")
    List<ClinicalNote> findDraftNotesByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.patientId = :patientId " +
           "AND (LOWER(n.subjective) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.objective) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.assessment) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.plan) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.chiefComplaint) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY n.noteDate DESC, n.noteTime DESC")
    List<ClinicalNote> searchNotesByContent(@Param("patientId") UUID patientId, 
                                             @Param("searchTerm") String searchTerm);
    
    @Query(value = "SELECT * FROM ehr.clinical_notes WHERE patient_id = CAST(:patientId AS UUID) " +
           "AND is_current_version = true ORDER BY note_date DESC, note_time DESC LIMIT 1", 
           nativeQuery = true)
    Optional<ClinicalNote> findLatestNoteByPatient(@Param("patientId") UUID patientId);
    
    @Query("SELECT n FROM ClinicalNote n WHERE n.patient.patientId = :patientId " +
           "AND n.noteType = :noteType AND n.isCurrentVersion = true " +
           "ORDER BY n.noteDate DESC, n.noteTime DESC")
    List<ClinicalNote> findCurrentVersionsByPatientAndType(
        @Param("patientId") UUID patientId, 
        @Param("noteType") ClinicalNote.NoteType noteType);
    
    void deleteByPatientPatientId(UUID patientId);
}
