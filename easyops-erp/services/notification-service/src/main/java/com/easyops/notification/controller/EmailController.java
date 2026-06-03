package com.easyops.notification.controller;

import com.easyops.notification.dto.EmailRequest;
import com.easyops.notification.entity.EmailLog;
import com.easyops.notification.security.NotificationRbacService;
import com.easyops.notification.security.RbacRequestHeaders;
import com.easyops.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Email notification management")
public class EmailController {
    
    private final EmailService emailService;
    private final NotificationRbacService notificationRbac;
    
    @PostMapping("/send")
    @Operation(summary = "Send an email")
    public ResponseEntity<String> sendEmail(
            @Valid @RequestBody EmailRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        emailService.sendEmail(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body("Email queued for delivery");
    }
    
    @GetMapping("/logs/{id}")
    @Operation(summary = "Get email log by ID")
    public ResponseEntity<EmailLog> getEmailLog(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(emailService.getEmailLog(id));
    }
}
