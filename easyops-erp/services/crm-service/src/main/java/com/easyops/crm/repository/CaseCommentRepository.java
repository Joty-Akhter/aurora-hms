package com.easyops.crm.repository;

import com.easyops.crm.entity.CaseComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseCommentRepository extends JpaRepository<CaseComment, UUID> {
    
    List<CaseComment> findByCaseIdOrderByCreatedAtAsc(UUID caseId);
    
    List<CaseComment> findByCaseIdAndIsPublic(UUID caseId, Boolean isPublic);
    
    @Query("SELECT COUNT(cc) FROM CaseComment cc WHERE cc.caseId = :caseId")
    long countByCaseId(@Param("caseId") UUID caseId);
    
    @Query("SELECT cc FROM CaseComment cc WHERE cc.caseId = :caseId AND cc.commentType = :type ORDER BY cc.createdAt ASC")
    List<CaseComment> findByCaseIdAndType(@Param("caseId") UUID caseId, @Param("type") String commentType);
}

