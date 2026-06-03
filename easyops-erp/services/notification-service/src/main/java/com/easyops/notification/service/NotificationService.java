package com.easyops.notification.service;

import com.easyops.notification.dto.NotificationRequest;
import com.easyops.notification.dto.NotificationResponse;
import com.easyops.notification.entity.Notification;
import com.easyops.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setOrganizationId(request.getOrganizationId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setActionUrl(request.getActionUrl());
        notification.setActionLabel(request.getActionLabel());
        notification.setPriority(request.getPriority());
        notification.setExpiresAt(request.getExpiresAt());
        notification.setMetadata(request.getMetadata());
        notification.setIsRead(false);
        
        notification = notificationRepository.save(notification);
        log.info("Created notification for user: {}", request.getUserId());
        
        // Send real-time notification via WebSocket
        NotificationResponse response = mapToResponse(notification);
        sendRealtimeNotification(request.getUserId(), response);
        
        return response;
    }
    
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::mapToResponse);
    }
    
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public Long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        
        log.info("Marked notification as read: {}", notificationId);
    }
    
    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = 
            notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });
        
        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked all notifications as read for user: {}", userId);
    }
    
    @Transactional
    public void deleteNotification(UUID notificationId) {
        notificationRepository.deleteById(notificationId);
        log.info("Deleted notification: {}", notificationId);
    }
    
    private void sendRealtimeNotification(UUID userId, NotificationResponse notification) {
        try {
            // Send to user-specific topic
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                notification
            );
            log.debug("Sent real-time notification to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send real-time notification", e);
        }
    }
    
    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getUserId(),
            notification.getOrganizationId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getActionUrl(),
            notification.getActionLabel(),
            notification.getIsRead(),
            notification.getReadAt(),
            notification.getPriority(),
            notification.getExpiresAt(),
            notification.getCreatedAt(),
            notification.getMetadata()
        );
    }
}

