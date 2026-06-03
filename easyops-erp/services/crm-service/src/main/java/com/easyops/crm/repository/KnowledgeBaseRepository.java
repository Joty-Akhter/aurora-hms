package com.easyops.crm.repository;

import com.easyops.crm.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {
    
    List<KnowledgeBase> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    List<KnowledgeBase> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<KnowledgeBase> findByOrganizationIdAndIsPublic(UUID organizationId, Boolean isPublic);
    
    List<KnowledgeBase> findByOrganizationIdAndIsFeatured(UUID organizationId, Boolean isFeatured);
    
    List<KnowledgeBase> findByOrganizationIdAndCategory(UUID organizationId, String category);
    
    Optional<KnowledgeBase> findByOrganizationIdAndSlug(UUID organizationId, String slug);
    
    Optional<KnowledgeBase> findByOrganizationIdAndArticleNumber(UUID organizationId, String articleNumber);
    
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.organizationId = :orgId " +
           "AND (LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(kb.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<KnowledgeBase> searchArticles(@Param("orgId") UUID organizationId, @Param("searchTerm") String searchTerm);
    
    @Modifying
    @Query("UPDATE KnowledgeBase kb SET kb.viewCount = kb.viewCount + 1 WHERE kb.articleId = :articleId")
    void incrementViewCount(@Param("articleId") UUID articleId);
    
    @Modifying
    @Query("UPDATE KnowledgeBase kb SET kb.helpfulCount = kb.helpfulCount + 1 WHERE kb.articleId = :articleId")
    void incrementHelpfulCount(@Param("articleId") UUID articleId);
    
    @Modifying
    @Query("UPDATE KnowledgeBase kb SET kb.notHelpfulCount = kb.notHelpfulCount + 1 WHERE kb.articleId = :articleId")
    void incrementNotHelpfulCount(@Param("articleId") UUID articleId);
}

