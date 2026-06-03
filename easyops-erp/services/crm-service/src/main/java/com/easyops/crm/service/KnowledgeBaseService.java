package com.easyops.crm.service;

import com.easyops.crm.entity.KnowledgeBase;
import com.easyops.crm.repository.KnowledgeBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class KnowledgeBaseService {
    
    @Autowired
    private KnowledgeBaseRepository kbRepository;
    
    public List<KnowledgeBase> getAllArticles(UUID organizationId) {
        return kbRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
    
    public List<KnowledgeBase> getPublishedArticles(UUID organizationId) {
        return kbRepository.findByOrganizationIdAndStatus(organizationId, "PUBLISHED");
    }
    
    public List<KnowledgeBase> getPublicArticles(UUID organizationId) {
        return kbRepository.findByOrganizationIdAndIsPublic(organizationId, true);
    }
    
    public List<KnowledgeBase> getFeaturedArticles(UUID organizationId) {
        return kbRepository.findByOrganizationIdAndIsFeatured(organizationId, true);
    }
    
    public List<KnowledgeBase> getArticlesByCategory(UUID organizationId, String category) {
        return kbRepository.findByOrganizationIdAndCategory(organizationId, category);
    }
    
    public Optional<KnowledgeBase> getArticleById(UUID articleId) {
        return kbRepository.findById(articleId);
    }

    public UUID getOrganizationIdForArticle(UUID articleId) {
        return getArticleById(articleId)
                .map(KnowledgeBase::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + articleId));
    }
    
    public Optional<KnowledgeBase> getArticleBySlug(UUID organizationId, String slug) {
        return kbRepository.findByOrganizationIdAndSlug(organizationId, slug);
    }
    
    public List<KnowledgeBase> searchArticles(UUID organizationId, String searchTerm) {
        return kbRepository.searchArticles(organizationId, searchTerm);
    }
    
    public KnowledgeBase createArticle(KnowledgeBase article) {
        if (article.getArticleNumber() == null || article.getArticleNumber().isEmpty()) {
            article.setArticleNumber(generateArticleNumber(article.getOrganizationId()));
        }
        if (article.getSlug() == null || article.getSlug().isEmpty()) {
            article.setSlug(generateSlug(article.getTitle()));
        }
        return kbRepository.save(article);
    }
    
    public KnowledgeBase updateArticle(UUID articleId, KnowledgeBase articleDetails) {
        return kbRepository.findById(articleId)
                .map(article -> {
                    article.setTitle(articleDetails.getTitle());
                    article.setSummary(articleDetails.getSummary());
                    article.setContent(articleDetails.getContent());
                    article.setCategory(articleDetails.getCategory());
                    article.setSubcategory(articleDetails.getSubcategory());
                    article.setTags(articleDetails.getTags());
                    article.setStatus(articleDetails.getStatus());
                    article.setIsPublic(articleDetails.getIsPublic());
                    article.setIsFeatured(articleDetails.getIsFeatured());
                    article.setKeywords(articleDetails.getKeywords());
                    article.setRelatedArticles(articleDetails.getRelatedArticles());
                    article.setUpdatedBy(articleDetails.getUpdatedBy());
                    
                    // Increment version on update
                    article.setVersion(article.getVersion() + 1);
                    
                    return kbRepository.save(article);
                })
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + articleId));
    }
    
    public void deleteArticle(UUID articleId) {
        kbRepository.deleteById(articleId);
    }
    
    public KnowledgeBase publishArticle(UUID articleId, UUID publishedBy) {
        return kbRepository.findById(articleId)
                .map(article -> {
                    article.setStatus("PUBLISHED");
                    article.setPublishedAt(LocalDateTime.now());
                    article.setAuthorId(publishedBy);
                    return kbRepository.save(article);
                })
                .orElseThrow(() -> new RuntimeException("Article not found with id: " + articleId));
    }
    
    public void incrementViewCount(UUID articleId) {
        kbRepository.incrementViewCount(articleId);
    }
    
    public void markHelpful(UUID articleId) {
        kbRepository.incrementHelpfulCount(articleId);
    }
    
    public void markNotHelpful(UUID articleId) {
        kbRepository.incrementNotHelpfulCount(articleId);
    }
    
    private String generateArticleNumber(UUID organizationId) {
        String prefix = "KB";
        long count = kbRepository.count() + 1;
        return String.format("%s-%06d", prefix, count);
    }
    
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .substring(0, Math.min(title.length(), 100));
    }
}

