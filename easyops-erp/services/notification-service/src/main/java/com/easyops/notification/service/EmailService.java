package com.easyops.notification.service;

import com.easyops.notification.dto.EmailRequest;
import com.easyops.notification.entity.EmailLog;
import com.easyops.notification.entity.EmailTemplate;
import com.easyops.notification.repository.EmailLogRepository;
import com.easyops.notification.repository.EmailTemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    
    @Value("${notification.email.from-address:noreply@easyops.com}")
    private String defaultFromAddress;
    
    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;
    
    @Async
    @Transactional
    public void sendEmail(EmailRequest request) {
        // Create email log
        EmailLog emailLog = new EmailLog();
        emailLog.setToEmail(request.getToEmail());
        emailLog.setFromEmail(request.getFromEmail() != null ? request.getFromEmail() : defaultFromAddress);
        emailLog.setSubject(request.getSubject());
        emailLog.setTemplateName(request.getTemplateName());
        emailLog.setTemplateVariables(request.getTemplateVariables());
        emailLog.setOrganizationId(request.getOrganizationId());
        emailLog.setUserId(request.getUserId());
        emailLog.setStatus("PENDING");
        
        emailLog = emailLogRepository.save(emailLog);
        
        if (!emailEnabled) {
            log.warn("Email service is disabled. Email not sent to: {}", request.getToEmail());
            emailLog.setStatus("FAILED");
            emailLog.setFailedReason("Email service is disabled");
            emailLogRepository.save(emailLog);
            return;
        }
        
        try {
            String body = request.getBody();
            
            // If template is specified, use template
            if (request.getTemplateName() != null) {
                body = processTemplate(request.getTemplateName(), 
                                      request.getTemplateVariables(), 
                                      request.getOrganizationId());
            }
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailLog.getFromEmail());
            helper.setTo(request.getToEmail());
            helper.setSubject(request.getSubject());
            helper.setText(body, true); // true = HTML
            
            mailSender.send(message);
            
            emailLog.setStatus("SENT");
            emailLog.setSentAt(LocalDateTime.now());
            log.info("Email sent successfully to: {}", request.getToEmail());
            
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", request.getToEmail(), e);
            emailLog.setStatus("FAILED");
            emailLog.setFailedReason(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        }
        
        emailLogRepository.save(emailLog);
    }
    
    private String processTemplate(String templateName, Map<String, Object> variables, UUID organizationId) {
        EmailTemplate template = emailTemplateRepository
            .findByNameAndOrganizationId(templateName, organizationId)
            .orElseGet(() -> emailTemplateRepository.findByName(templateName)
                .orElseThrow(() -> new RuntimeException("Email template not found: " + templateName)));
        
        String body = template.getBodyHtml();
        
        // Simple variable substitution
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                body = body.replace(placeholder, value);
            }
        }
        
        return body;
    }
    
    public EmailLog getEmailLog(UUID id) {
        return emailLogRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Email log not found"));
    }
}

