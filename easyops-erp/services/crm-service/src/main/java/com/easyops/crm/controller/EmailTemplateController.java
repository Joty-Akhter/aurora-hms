package com.easyops.crm.controller;

import com.easyops.crm.entity.EmailTemplate;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/email-templates")
@CrossOrigin(origins = "*")
public class EmailTemplateController {

    @Autowired
    private EmailTemplateService templateService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<EmailTemplate>> getAllTemplates(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) String templateType) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        List<EmailTemplate> templates;

        if (activeOnly != null && activeOnly) {
            templates = templateService.getActiveTemplates(organizationId);
        } else if (templateType != null) {
            templates = templateService.getTemplatesByType(organizationId, templateType);
        } else {
            templates = templateService.getAllTemplates(organizationId);
        }

        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplate> getTemplateById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return templateService.getTemplateById(id)
                .map(t -> {
                    crmRbac.requireCrmView(actor, t.getOrganizationId());
                    return ResponseEntity.ok(t);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<EmailTemplate> getTemplateByCode(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable String code) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        return templateService.getTemplateByCode(organizationId, code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmailTemplate> createTemplate(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EmailTemplate template) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, template.getOrganizationId());
        EmailTemplate created = templateService.createTemplate(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> updateTemplate(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody EmailTemplate template) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, templateService.getOrganizationIdForTemplate(id));
        try {
            EmailTemplate updated = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, templateService.getOrganizationIdForTemplate(id));
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
