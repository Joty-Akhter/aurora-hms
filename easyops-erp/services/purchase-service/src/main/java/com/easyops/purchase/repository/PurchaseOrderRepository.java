package com.easyops.purchase.repository;

import com.easyops.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    
    List<PurchaseOrder> findByOrganizationIdOrderByPoDateDesc(UUID organizationId);
    
    List<PurchaseOrder> findByOrganizationIdAndStatusOrderByPoDateDesc(UUID organizationId, String status);
    
    Optional<PurchaseOrder> findByOrganizationIdAndPoNumber(UUID organizationId, String poNumber);
    
    List<PurchaseOrder> findByVendorIdOrderByPoDateDesc(UUID vendorId);
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.organizationId = :orgId " +
           "AND po.status = 'APPROVED' ORDER BY po.poDate DESC")
    List<PurchaseOrder> findApprovedOrders(@Param("orgId") UUID organizationId);
    
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.organizationId = :orgId " +
           "AND po.status = :status")
    Long countByOrganizationIdAndStatus(
        @Param("orgId") UUID organizationId,
        @Param("status") String status
    );
}

