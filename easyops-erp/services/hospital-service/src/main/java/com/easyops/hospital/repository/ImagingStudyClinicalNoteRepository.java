package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ImagingStudyClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImagingStudyClinicalNoteRepository extends JpaRepository<ImagingStudyClinicalNote, UUID> {
    
    List<ImagingStudyClinicalNote> findByStudyStudyId(UUID studyId);
    
    List<ImagingStudyClinicalNote> findByNoteNoteId(UUID noteId);
    
    @Query("SELECT l FROM ImagingStudyClinicalNote l WHERE l.study.studyId = :studyId " +
           "ORDER BY l.linkedDate DESC")
    List<ImagingStudyClinicalNote> findByStudyIdOrdered(@Param("studyId") UUID studyId);
    
    @Query("SELECT l FROM ImagingStudyClinicalNote l WHERE l.note.noteId = :noteId " +
           "ORDER BY l.linkedDate DESC")
    List<ImagingStudyClinicalNote> findByNoteIdOrdered(@Param("noteId") UUID noteId);
    
    void deleteByStudyStudyId(UUID studyId);
    
    void deleteByNoteNoteId(UUID noteId);
}
