package com.easyops.notification.repository;

import com.easyops.notification.entity.WebhookDelivery;
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
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {
    
    Page<WebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(UUID webhookId, Pageable pageable);
    
    @Query("SELECT wd FROM WebhookDelivery wd WHERE wd.success = false AND wd.retryAttempt < 3 ORDER BY wd.createdAt")
    List<WebhookDelivery> findFailedDeliveriesForRetry();
    
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}

