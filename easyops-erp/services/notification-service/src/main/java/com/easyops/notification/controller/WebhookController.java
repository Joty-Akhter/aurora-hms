package com.easyops.notification.controller;

import com.easyops.notification.dto.WebhookRequest;
import com.easyops.notification.dto.WebhookResponse;
import com.easyops.notification.entity.WebhookDelivery;
import com.easyops.notification.security.NotificationRbacService;
import com.easyops.notification.security.RbacRequestHeaders;
import com.easyops.notification.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook management")
public class WebhookController {
    
    private final WebhookService webhookService;
    private final NotificationRbacService notificationRbac;
    
    @PostMapping
    @Operation(summary = "Create a webhook")
    public ResponseEntity<WebhookResponse> createWebhook(
            @Valid @RequestBody WebhookRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(webhookService.createWebhook(request, actor));
    }
    
    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get organization webhooks")
    public ResponseEntity<List<WebhookResponse>> getOrganizationWebhooks(
            @PathVariable UUID organizationId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID rbacOrgId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, rbacOrgId);
        return ResponseEntity.ok(webhookService.getOrganizationWebhooks(organizationId));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get webhook by ID")
    public ResponseEntity<WebhookResponse> getWebhook(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(webhookService.getWebhook(id));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update webhook")
    public ResponseEntity<WebhookResponse> updateWebhook(
            @PathVariable UUID id,
            @Valid @RequestBody WebhookRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        return ResponseEntity.ok(webhookService.updateWebhook(id, request));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete webhook")
    public ResponseEntity<Void> deleteWebhook(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/test")
    @Operation(summary = "Test webhook with sample payload")
    public ResponseEntity<String> testWebhook(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        WebhookResponse webhook = webhookService.getWebhook(id);
        webhookService.triggerWebhooks(webhook.getOrganizationId(), "test.event", payload);
        return ResponseEntity.ok("Webhook test triggered");
    }
    
    @GetMapping("/{id}/deliveries")
    @Operation(summary = "Get webhook delivery history")
    public ResponseEntity<Page<WebhookDelivery>> getDeliveryHistory(
            @PathVariable UUID id,
            Pageable pageable,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(webhookService.getDeliveryHistory(id, pageable));
    }
}
