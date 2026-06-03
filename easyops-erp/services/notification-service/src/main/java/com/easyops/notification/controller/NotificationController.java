package com.easyops.notification.controller;

import com.easyops.notification.dto.NotificationRequest;
import com.easyops.notification.dto.NotificationResponse;
import com.easyops.notification.security.NotificationRbacService;
import com.easyops.notification.security.RbacRequestHeaders;
import com.easyops.notification.service.NotificationService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final NotificationRbacService notificationRbac;
    
    @PostMapping
    @Operation(summary = "Create a notification")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(notificationService.createNotification(request));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications")
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PathVariable UUID userId,
            Pageable pageable,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId, pageable));
    }
    
    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }
    
    @GetMapping("/user/{userId}/unread/count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }
    
    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemRead(actor, organizationId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        notificationRbac.requireSystemConfigure(actor, organizationId);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
