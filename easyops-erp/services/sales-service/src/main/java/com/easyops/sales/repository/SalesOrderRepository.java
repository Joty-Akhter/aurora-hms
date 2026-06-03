package com.easyops.sales.repository;

import com.easyops.sales.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID> {
    
    @Query("SELECT DISTINCT so FROM SalesOrder so LEFT JOIN FETCH so.lines WHERE so.organizationId = :orgId ORDER BY so.orderDate DESC")
    List<SalesOrder> findByOrganizationIdOrderByOrderDateDesc(@Param("orgId") UUID organizationId);
    
    @Query("SELECT DISTINCT so FROM SalesOrder so LEFT JOIN FETCH so.lines WHERE so.organizationId = :orgId AND so.status = :status")
    List<SalesOrder> findByOrganizationIdAndStatus(@Param("orgId") UUID organizationId, @Param("status") String status);
    
    @Query("SELECT DISTINCT so FROM SalesOrder so LEFT JOIN FETCH so.lines WHERE so.organizationId = :orgId AND so.customerId = :customerId")
    List<SalesOrder> findByOrganizationIdAndCustomerId(@Param("orgId") UUID organizationId, @Param("customerId") UUID customerId);
    
    @Query("SELECT DISTINCT so FROM SalesOrder so LEFT JOIN FETCH so.lines WHERE so.organizationId = :orgId AND so.paymentStatus = :paymentStatus")
    List<SalesOrder> findByOrganizationIdAndPaymentStatus(@Param("orgId") UUID organizationId, @Param("paymentStatus") String paymentStatus);
    
    @Query("SELECT DISTINCT so FROM SalesOrder so LEFT JOIN FETCH so.lines WHERE so.organizationId = :orgId AND so.deliveryStatus = :deliveryStatus")
    List<SalesOrder> findByOrganizationIdAndDeliveryStatus(@Param("orgId") UUID organizationId, @Param("deliveryStatus") String deliveryStatus);
    
    @Query("SELECT so FROM SalesOrder so LEFT JOIN FETCH so.lines WHERE so.id = :id")
    Optional<SalesOrder> findByIdWithLines(@Param("id") UUID id);
    
    Optional<SalesOrder> findByOrganizationIdAndOrderNumber(UUID organizationId, String orderNumber);
    
    Optional<SalesOrder> findByQuotationId(UUID quotationId);
    
    @Query("SELECT so FROM SalesOrder so WHERE so.organizationId = :orgId AND so.orderDate BETWEEN :startDate AND :endDate ORDER BY so.orderDate DESC")
    List<SalesOrder> findByDateRange(@Param("orgId") UUID organizationId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT so FROM SalesOrder so WHERE so.organizationId = :orgId AND so.status = 'CONFIRMED' AND so.deliveryStatus != 'DELIVERED'")
    List<SalesOrder> findPendingDeliveries(@Param("orgId") UUID organizationId);
    
    @Query("SELECT so FROM SalesOrder so WHERE so.organizationId = :orgId AND so.status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED') AND so.convertedToInvoiceId IS NULL")
    List<SalesOrder> findOrdersReadyForInvoicing(@Param("orgId") UUID organizationId);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(so.orderNumber, LENGTH(:prefix) + 1) AS INTEGER)), 0) FROM SalesOrder so WHERE so.organizationId = :orgId AND so.orderNumber LIKE CONCAT(:prefix, '%')")
    Integer findMaxOrderNumber(@Param("orgId") UUID organizationId, @Param("prefix") String prefix);
    
    boolean existsByOrganizationIdAndOrderNumber(UUID organizationId, String orderNumber);
}

