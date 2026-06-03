package com.easyops.notification.repository;

import com.easyops.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.expiresAt < :now")
    List<Notification> findExpiredNotifications(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}

