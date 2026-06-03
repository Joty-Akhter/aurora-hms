package com.easyops.inventory.repository;

import com.easyops.inventory.entity.ReorderAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReorderAlertRepository extends JpaRepository<ReorderAlert, UUID> {
    
    List<ReorderAlert> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    
    List<ReorderAlert> findByOrganizationIdAndAlertStatus(UUID organizationId, String alertStatus);
    
    List<ReorderAlert> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    
    @Query("SELECT a FROM ReorderAlert a WHERE a.organizationId = :orgId AND a.alertStatus = 'OPEN'")
    List<ReorderAlert> findOpenAlerts(@Param("orgId") UUID organizationId);
    
    @Query("SELECT a FROM ReorderAlert a WHERE a.organizationId = :orgId AND a.priority = :priority AND a.alertStatus = 'OPEN'")
    List<ReorderAlert> findOpenAlertsByPriority(@Param("orgId") UUID organizationId, @Param("priority") String priority);
}

