package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ClinicalNoteMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClinicalNoteMedicationRepository extends JpaRepository<ClinicalNoteMedication, UUID> {
    
    List<ClinicalNoteMedication> findByNoteNoteId(UUID noteId);
    
    List<ClinicalNoteMedication> findByMedicationMedicationId(UUID medicationId);
    
    @Query("SELECT cnm FROM ClinicalNoteMedication cnm WHERE cnm.note.noteId = :noteId " +
           "ORDER BY cnm.linkedDate DESC")
    List<ClinicalNoteMedication> findByNoteIdOrdered(@Param("noteId") UUID noteId);
    
    @Query("SELECT cnm FROM ClinicalNoteMedication cnm WHERE cnm.medication.medicationId = :medicationId " +
           "ORDER BY cnm.linkedDate DESC")
    List<ClinicalNoteMedication> findByMedicationIdOrdered(@Param("medicationId") UUID medicationId);
    
    @Query("SELECT cnm FROM ClinicalNoteMedication cnm WHERE cnm.note.noteId = :noteId AND cnm.medication.medicationId = :medicationId")
    ClinicalNoteMedication findByNoteIdAndMedicationId(@Param("noteId") UUID noteId, @Param("medicationId") UUID medicationId);
    
    void deleteByNoteNoteId(UUID noteId);
    
    void deleteByMedicationMedicationId(UUID medicationId);
}
