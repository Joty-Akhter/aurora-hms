package com.easyops.pharma.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Notification Service
 * Used for sending notifications for key pharma events
 */
@FeignClient(name = "notification-service")
public interface NotificationClient {
    
    /**
     * Create a notification
     */
    @PostMapping("/api/notifications")
    Map<String, Object> createNotification(@RequestBody Map<String, Object> notificationRequest);
    
    /**
     * Send an email notification
     */
    @PostMapping("/api/notifications/email/send")
    String sendEmail(@RequestBody Map<String, Object> emailRequest);
}
