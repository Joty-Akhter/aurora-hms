package com.easyops.hospital.repository;

import com.easyops.hospital.entity.LabResultClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabResultClinicalNoteRepository extends JpaRepository<LabResultClinicalNote, UUID> {
    
    List<LabResultClinicalNote> findByResultResultId(UUID resultId);
    
    List<LabResultClinicalNote> findByNoteNoteId(UUID noteId);
    
    @Query("SELECT lrcn FROM LabResultClinicalNote lrcn WHERE lrcn.result.resultId = :resultId AND lrcn.note.noteId = :noteId")
    LabResultClinicalNote findByResultIdAndNoteId(@Param("resultId") UUID resultId, @Param("noteId") UUID noteId);
    
    @Query("SELECT lrcn FROM LabResultClinicalNote lrcn WHERE lrcn.organizationId = :organizationId")
    List<LabResultClinicalNote> findByOrganizationId(@Param("organizationId") UUID organizationId);
}
