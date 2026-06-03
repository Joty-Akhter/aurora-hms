package com.easyops.crm.controller;

import com.easyops.crm.entity.LeadSource;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.LeadSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lead-sources")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LeadSourceController {

    private final LeadSourceService leadSourceService;
    private final CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<LeadSource>> getAllLeadSources(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        List<LeadSource> sources = leadSourceService.getAllLeadSources(organizationId);
        return ResponseEntity.ok(sources);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadSource> getLeadSourceById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return leadSourceService.findLeadSourceById(id)
                .map(source -> {
                    crmRbac.requireCrmView(actor, source.getOrganizationId());
                    return ResponseEntity.ok(source);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LeadSource> createLeadSource(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody LeadSource leadSource) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, leadSource.getOrganizationId());
        LeadSource created = leadSourceService.createLeadSource(leadSource);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadSource> updateLeadSource(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody LeadSource leadSourceDetails) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, leadSourceService.getOrganizationIdForLeadSource(id));
        LeadSource updated = leadSourceService.updateLeadSource(id, leadSourceDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeadSource(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, leadSourceService.getOrganizationIdForLeadSource(id));
        leadSourceService.deleteLeadSource(id);
        return ResponseEntity.noContent().build();
    }
}
