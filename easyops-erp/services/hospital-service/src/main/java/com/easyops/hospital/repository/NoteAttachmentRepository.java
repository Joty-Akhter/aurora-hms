package com.easyops.hospital.repository;

import com.easyops.hospital.entity.NoteAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteAttachmentRepository extends JpaRepository<NoteAttachment, UUID> {
    
    List<NoteAttachment> findByNoteNoteId(UUID noteId);
    
    List<NoteAttachment> findByNoteNoteIdAndIsActiveTrue(UUID noteId);
    
    List<NoteAttachment> findByNoteNoteIdAndAttachmentType(
        UUID noteId, NoteAttachment.AttachmentType attachmentType);
    
    @Query("SELECT a FROM NoteAttachment a WHERE a.note.noteId = :noteId " +
           "AND a.isActive = true ORDER BY a.uploadedDate DESC")
    List<NoteAttachment> findActiveAttachmentsByNote(@Param("noteId") UUID noteId);
    
    void deleteByNoteNoteId(UUID noteId);
}
