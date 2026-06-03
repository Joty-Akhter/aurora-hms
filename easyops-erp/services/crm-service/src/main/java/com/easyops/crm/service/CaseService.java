package com.easyops.crm.service;

import com.easyops.crm.entity.Case;
import com.easyops.crm.entity.CaseComment;
import com.easyops.crm.repository.CaseCommentRepository;
import com.easyops.crm.repository.CaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CaseService {
    
    @Autowired
    private CaseRepository caseRepository;
    
    @Autowired
    private CaseCommentRepository commentRepository;
    
    public List<Case> getAllCases(UUID organizationId) {
        return caseRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }
    
    public Optional<Case> getCaseById(UUID caseId) {
        return caseRepository.findById(caseId);
    }

    public UUID getOrganizationIdForCase(UUID caseId) {
        return getCaseById(caseId)
                .map(Case::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
    }

    public UUID getOrganizationIdForCaseComment(UUID commentId) {
        return commentRepository.findById(commentId)
                .map(CaseComment::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Case comment not found with id: " + commentId));
    }
    
    public Optional<Case> getCaseByCaseNumber(UUID organizationId, String caseNumber) {
        return caseRepository.findByOrganizationIdAndCaseNumber(organizationId, caseNumber);
    }
    
    public Case createCase(Case caseEntity) {
        if (caseEntity.getCaseNumber() == null || caseEntity.getCaseNumber().isEmpty()) {
            caseEntity.setCaseNumber(generateCaseNumber(caseEntity.getOrganizationId()));
        }
        return caseRepository.save(caseEntity);
    }
    
    public Case updateCase(UUID caseId, Case caseDetails) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setSubject(caseDetails.getSubject());
                    caseEntity.setDescription(caseDetails.getDescription());
                    caseEntity.setCaseType(caseDetails.getCaseType());
                    caseEntity.setStatus(caseDetails.getStatus());
                    caseEntity.setPriority(caseDetails.getPriority());
                    caseEntity.setContactId(caseDetails.getContactId());
                    caseEntity.setAccountId(caseDetails.getAccountId());
                    caseEntity.setAssignedTo(caseDetails.getAssignedTo());
                    caseEntity.setOrigin(caseDetails.getOrigin());
                    caseEntity.setResolution(caseDetails.getResolution());
                    caseEntity.setSlaId(caseDetails.getSlaId());
                    caseEntity.setCategory(caseDetails.getCategory());
                    caseEntity.setSubcategory(caseDetails.getSubcategory());
                    caseEntity.setTags(caseDetails.getTags());
                    caseEntity.setUpdatedBy(caseDetails.getUpdatedBy());
                    return caseRepository.save(caseEntity);
                })
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
    }
    
    public void deleteCase(UUID caseId) {
        caseRepository.deleteById(caseId);
    }
    
    public List<Case> getCasesByStatus(UUID organizationId, String status) {
        return caseRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Case> getCasesByPriority(UUID organizationId, String priority) {
        return caseRepository.findByOrganizationIdAndPriority(organizationId, priority);
    }
    
    public List<Case> getCasesByAssignedUser(UUID organizationId, UUID assignedTo) {
        return caseRepository.findByOrganizationIdAndAssignedTo(organizationId, assignedTo);
    }
    
    public List<Case> searchCases(UUID organizationId, String searchTerm) {
        return caseRepository.searchCases(organizationId, searchTerm);
    }
    
    public List<Case> getSlaBreachedCases(UUID organizationId) {
        return caseRepository.findSlaBreachedCases(organizationId);
    }
    
    public List<Case> getOverdueCases(UUID organizationId) {
        return caseRepository.findOverdueCases(organizationId, LocalDateTime.now());
    }
    
    public Case assignCase(UUID caseId, UUID assignedTo) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setAssignedTo(assignedTo);
                    caseEntity.setAssignedAt(LocalDateTime.now());
                    if ("NEW".equals(caseEntity.getStatus())) {
                        caseEntity.setStatus("OPEN");
                    }
                    return caseRepository.save(caseEntity);
                })
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
    }
    
    public Case resolveCase(UUID caseId, UUID resolvedBy, String resolution) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setStatus("RESOLVED");
                    caseEntity.setResolvedBy(resolvedBy);
                    caseEntity.setResolvedAt(LocalDateTime.now());
                    caseEntity.setResolution(resolution);
                    return caseRepository.save(caseEntity);
                })
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
    }
    
    public Case closeCase(UUID caseId) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setStatus("CLOSED");
                    caseEntity.setClosedAt(LocalDateTime.now());
                    return caseRepository.save(caseEntity);
                })
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
    }
    
    public Case rateCase(UUID caseId, Integer rating, String feedback) {
        return caseRepository.findById(caseId)
                .map(caseEntity -> {
                    caseEntity.setCustomerRating(rating);
                    caseEntity.setCustomerFeedback(feedback);
                    return caseRepository.save(caseEntity);
                })
                .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
    }
    
    // Case Comments
    public List<CaseComment> getCaseComments(UUID caseId) {
        return commentRepository.findByCaseIdOrderByCreatedAtAsc(caseId);
    }
    
    public CaseComment addComment(CaseComment comment) {
        // Mark first response
        Case caseEntity = caseRepository.findById(comment.getCaseId()).orElse(null);
        if (caseEntity != null && caseEntity.getFirstResponseAt() == null && "EXTERNAL".equals(comment.getCommentType())) {
            caseEntity.setFirstResponseAt(LocalDateTime.now());
            caseRepository.save(caseEntity);
        }
        return commentRepository.save(comment);
    }
    
    public void deleteComment(UUID commentId) {
        commentRepository.deleteById(commentId);
    }
    
    public Map<String, Object> getCaseStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalCases = caseRepository.count();
        long newCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "NEW");
        long openCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "OPEN");
        long resolvedCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "RESOLVED");
        long closedCases = caseRepository.countByOrganizationIdAndStatus(organizationId, "CLOSED");
        
        stats.put("totalCases", totalCases);
        stats.put("newCases", newCases);
        stats.put("openCases", openCases);
        stats.put("resolvedCases", resolvedCases);
        stats.put("closedCases", closedCases);
        stats.put("activeCases", newCases + openCases);
        
        return stats;
    }
    
    private String generateCaseNumber(UUID organizationId) {
        String prefix = "CASE";
        long count = caseRepository.count() + 1;
        return String.format("%s-%06d", prefix, count);
    }
}

