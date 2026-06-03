package com.easyops.crm.controller;

import com.easyops.crm.entity.Case;
import com.easyops.crm.entity.CaseComment;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    @Autowired
    private CaseService caseService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<Case>> getAllCases(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) String search) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        List<Case> cases;

        if (search != null && !search.isEmpty()) {
            cases = caseService.searchCases(organizationId, search);
        } else if (status != null) {
            cases = caseService.getCasesByStatus(organizationId, status);
        } else if (priority != null) {
            cases = caseService.getCasesByPriority(organizationId, priority);
        } else if (assignedTo != null) {
            cases = caseService.getCasesByAssignedUser(organizationId, assignedTo);
        } else {
            cases = caseService.getAllCases(organizationId);
        }

        return ResponseEntity.ok(cases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Case> getCaseById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return caseService.getCaseById(id)
                .map(c -> {
                    crmRbac.requireCrmView(actor, c.getOrganizationId());
                    return ResponseEntity.ok(c);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{number}")
    public ResponseEntity<Case> getCaseByCaseNumber(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable String number) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        return caseService.getCaseByCaseNumber(organizationId, number)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Case> createCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Case caseEntity) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseEntity.getOrganizationId());
        Case created = caseService.createCase(caseEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Case> updateCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Case caseEntity) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        try {
            Case updated = caseService.updateCase(id, caseEntity);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        caseService.deleteCase(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sla-breached")
    public ResponseEntity<List<Case>> getSlaBreachedCases(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        List<Case> cases = caseService.getSlaBreachedCases(organizationId);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Case>> getOverdueCases(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        List<Case> cases = caseService.getOverdueCases(organizationId);
        return ResponseEntity.ok(cases);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<Case> assignCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        try {
            UUID assignedTo = request.get("assignedTo");
            Case updated = caseService.assignCase(id, assignedTo);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Case> resolveCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        try {
            UUID resolvedBy = UUID.fromString(request.get("resolvedBy").toString());
            String resolution = request.get("resolution").toString();
            Case updated = caseService.resolveCase(id, resolvedBy, resolution);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Case> closeCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        try {
            Case updated = caseService.closeCase(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Case> rateCase(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        try {
            Integer rating = Integer.parseInt(request.get("rating").toString());
            String feedback = request.get("feedback") != null ? request.get("feedback").toString() : null;
            Case updated = caseService.rateCase(id, rating, feedback);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCaseStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> stats = caseService.getCaseStats(organizationId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CaseComment>> getCaseComments(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, caseService.getOrganizationIdForCase(id));
        List<CaseComment> comments = caseService.getCaseComments(id);
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CaseComment> addComment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody CaseComment comment) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCase(id));
        comment.setCaseId(id);
        CaseComment created = caseService.addComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID commentId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, caseService.getOrganizationIdForCaseComment(commentId));
        caseService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
