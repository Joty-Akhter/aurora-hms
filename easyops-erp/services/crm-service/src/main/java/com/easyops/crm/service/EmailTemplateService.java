package com.easyops.crm.service;

import com.easyops.crm.entity.EmailTemplate;
import com.easyops.crm.repository.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmailTemplateService {
    
    @Autowired
    private EmailTemplateRepository templateRepository;
    
    public List<EmailTemplate> getAllTemplates(UUID organizationId) {
        return templateRepository.findByOrganizationId(organizationId);
    }
    
    public List<EmailTemplate> getActiveTemplates(UUID organizationId) {
        return templateRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    public List<EmailTemplate> getTemplatesByType(UUID organizationId, String templateType) {
        return templateRepository.findByOrganizationIdAndTemplateType(organizationId, templateType);
    }
    
    public Optional<EmailTemplate> getTemplateById(UUID templateId) {
        return templateRepository.findById(templateId);
    }

    public UUID getOrganizationIdForTemplate(UUID templateId) {
        return getTemplateById(templateId)
                .map(EmailTemplate::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + templateId));
    }
    
    public Optional<EmailTemplate> getTemplateByCode(UUID organizationId, String templateCode) {
        return templateRepository.findByOrganizationIdAndTemplateCode(organizationId, templateCode);
    }
    
    public EmailTemplate createTemplate(EmailTemplate template) {
        return templateRepository.save(template);
    }
    
    public EmailTemplate updateTemplate(UUID templateId, EmailTemplate templateDetails) {
        return templateRepository.findById(templateId)
                .map(template -> {
                    template.setTemplateName(templateDetails.getTemplateName());
                    template.setTemplateCode(templateDetails.getTemplateCode());
                    template.setSubject(templateDetails.getSubject());
                    template.setBodyHtml(templateDetails.getBodyHtml());
                    template.setBodyText(templateDetails.getBodyText());
                    template.setTemplateType(templateDetails.getTemplateType());
                    template.setIsActive(templateDetails.getIsActive());
                    template.setCategory(templateDetails.getCategory());
                    template.setPlaceholders(templateDetails.getPlaceholders());
                    template.setUpdatedBy(templateDetails.getUpdatedBy());
                    return templateRepository.save(template);
                })
                .orElseThrow(() -> new RuntimeException("Email template not found with id: " + templateId));
    }
    
    public void deleteTemplate(UUID templateId) {
        templateRepository.deleteById(templateId);
    }
}

