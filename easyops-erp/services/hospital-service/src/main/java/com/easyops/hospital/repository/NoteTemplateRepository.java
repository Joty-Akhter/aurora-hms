package com.easyops.hospital.repository;

import com.easyops.hospital.entity.NoteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteTemplateRepository extends JpaRepository<NoteTemplate, UUID> {
    
    List<NoteTemplate> findByTemplateType(NoteTemplate.TemplateType templateType);
    
    List<NoteTemplate> findByTemplateTypeAndIsActiveTrue(NoteTemplate.TemplateType templateType);
    
    List<NoteTemplate> findBySpecialty(String specialty);
    
    List<NoteTemplate> findBySpecialtyAndIsActiveTrue(String specialty);
    
    List<NoteTemplate> findByIsSystemTemplateTrue();
    
    List<NoteTemplate> findByIsPublicTrue();
    
    List<NoteTemplate> findByIsPublicTrueAndIsActiveTrue();
    
    List<NoteTemplate> findByCreatedBy(UUID createdBy);
    
    List<NoteTemplate> findByCreatedByAndIsActiveTrue(UUID createdBy);
    
    @Query("SELECT t FROM NoteTemplate t WHERE t.isActive = true " +
           "AND (t.isPublic = true OR t.createdBy = :userId) " +
           "ORDER BY t.usageCount DESC, t.templateName ASC")
    List<NoteTemplate> findAvailableTemplates(@Param("userId") UUID userId);
    
    @Query("SELECT t FROM NoteTemplate t WHERE t.templateType = :templateType " +
           "AND t.isActive = true " +
           "AND (t.isPublic = true OR t.createdBy = :userId) " +
           "ORDER BY t.usageCount DESC, t.templateName ASC")
    List<NoteTemplate> findAvailableTemplatesByType(
        @Param("templateType") NoteTemplate.TemplateType templateType,
        @Param("userId") UUID userId);
    
    @Query("SELECT t FROM NoteTemplate t WHERE LOWER(t.templateName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND t.isActive = true " +
           "AND (t.isPublic = true OR t.createdBy = :userId)")
    List<NoteTemplate> searchTemplates(@Param("searchTerm") String searchTerm, 
                                        @Param("userId") UUID userId);
}
