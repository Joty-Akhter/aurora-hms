package com.easyops.ap.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailClient {
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${notification.service.url:http://localhost:8086}")
    private String notificationServiceUrl;
    
    /**
     * Send email via notification service
     */
    public void sendEmail(String toEmail, String subject, String body, UUID organizationId) {
        try {
            String url = notificationServiceUrl + "/api/notifications/email";
            
            Map<String, Object> emailRequest = new HashMap<>();
            emailRequest.put("toEmail", toEmail);
            emailRequest.put("subject", subject);
            emailRequest.put("body", body);
            emailRequest.put("organizationId", organizationId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailRequest, headers);
            
            restTemplate.postForEntity(url, request, String.class);
            
            log.info("Email sent successfully to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send email with HTML content
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody, UUID organizationId) {
        sendEmail(toEmail, subject, htmlBody, organizationId);
    }
}

