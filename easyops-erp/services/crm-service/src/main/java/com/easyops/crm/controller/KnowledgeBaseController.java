package com.easyops.crm.controller;

import com.easyops.crm.entity.KnowledgeBase;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/knowledge-base")
@CrossOrigin(origins = "*")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService kbService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> getAllArticles(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean publicOnly,
            @RequestParam(required = false) Boolean featuredOnly,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        List<KnowledgeBase> articles;

        if (search != null && !search.isEmpty()) {
            articles = kbService.searchArticles(organizationId, search);
        } else if (publicOnly != null && publicOnly) {
            articles = kbService.getPublicArticles(organizationId);
        } else if (featuredOnly != null && featuredOnly) {
            articles = kbService.getFeaturedArticles(organizationId);
        } else if (category != null) {
            articles = kbService.getArticlesByCategory(organizationId, category);
        } else {
            articles = kbService.getAllArticles(organizationId);
        }

        return ResponseEntity.ok(articles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBase> getArticleById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Optional<KnowledgeBase> found = kbService.getArticleById(id);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        KnowledgeBase article = found.get();
        crmRbac.requireCrmView(actor, article.getOrganizationId());
        kbService.incrementViewCount(id);
        return ResponseEntity.ok(article);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<KnowledgeBase> getArticleBySlug(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable String slug) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Optional<KnowledgeBase> article = kbService.getArticleBySlug(organizationId, slug);
        if (article.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        KnowledgeBase kb = article.get();
        kbService.incrementViewCount(kb.getArticleId());
        return ResponseEntity.ok(kb);
    }

    @PostMapping
    public ResponseEntity<KnowledgeBase> createArticle(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody KnowledgeBase article) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, article.getOrganizationId());
        KnowledgeBase created = kbService.createArticle(article);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> updateArticle(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody KnowledgeBase article) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, kbService.getOrganizationIdForArticle(id));
        try {
            KnowledgeBase updated = kbService.updateArticle(id, article);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, kbService.getOrganizationIdForArticle(id));
        kbService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<KnowledgeBase> publishArticle(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, kbService.getOrganizationIdForArticle(id));
        try {
            UUID publishedBy = request.get("publishedBy");
            KnowledgeBase updated = kbService.publishArticle(id, publishedBy);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/helpful")
    public ResponseEntity<Void> markHelpful(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, kbService.getOrganizationIdForArticle(id));
        kbService.markHelpful(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/not-helpful")
    public ResponseEntity<Void> markNotHelpful(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, kbService.getOrganizationIdForArticle(id));
        kbService.markNotHelpful(id);
        return ResponseEntity.ok().build();
    }
}
