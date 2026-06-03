package com.easyops.notification.repository;

import com.easyops.notification.entity.EmailLog;
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
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    
    Page<EmailLog> findByToEmailOrderByCreatedAtDesc(String toEmail, Pageable pageable);
    
    Page<EmailLog> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    @Query("SELECT e FROM EmailLog e WHERE e.status = 'PENDING' OR (e.status = 'FAILED' AND e.retryCount < 3)")
    List<EmailLog> findPendingOrRetriableEmails();
    
    @Query("SELECT e FROM EmailLog e WHERE e.organizationId = :orgId ORDER BY e.createdAt DESC")
    Page<EmailLog> findByOrganizationId(@Param("orgId") UUID organizationId, Pageable pageable);
    
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}

