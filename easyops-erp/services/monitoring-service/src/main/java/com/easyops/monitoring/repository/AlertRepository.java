package com.easyops.monitoring.repository;

import com.easyops.monitoring.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    
    List<Alert> findByStatus(String status);
    
    Page<Alert> findByServiceNameOrderByCreatedAtDesc(String serviceName, Pageable pageable);
    
    @Query("SELECT a FROM Alert a WHERE a.status = 'ACTIVE' ORDER BY a.createdAt DESC")
    List<Alert> findActiveAlerts();
    
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = 'ACTIVE' AND a.alertType = 'CRITICAL'")
    Long countCriticalAlerts();
    
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}

