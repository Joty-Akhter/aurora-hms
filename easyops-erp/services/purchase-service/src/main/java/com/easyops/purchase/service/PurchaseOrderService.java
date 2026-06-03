package com.easyops.purchase.service;

import com.easyops.purchase.entity.PurchaseOrder;
import com.easyops.purchase.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseOrderService {
    
    private final PurchaseOrderRepository purchaseOrderRepository;
    
    @Cacheable(value = "purchase_orders", key = "#organizationId + '-' + #status")
    public List<PurchaseOrder> getAllPurchaseOrders(UUID organizationId, String status) {
        log.info("Getting purchase orders for organization: {}, status: {}", organizationId, status);
        if (status != null && !status.isEmpty()) {
            return purchaseOrderRepository.findByOrganizationIdAndStatusOrderByPoDateDesc(organizationId, status);
        }
        return purchaseOrderRepository.findByOrganizationIdOrderByPoDateDesc(organizationId);
    }
    
    @Cacheable(value = "purchase_order", key = "#id")
    public PurchaseOrder getPurchaseOrderById(UUID id) {
        log.info("Getting purchase order by id: {}", id);
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase order not found: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder) {
        log.info("Creating purchase order: {}", purchaseOrder.getPoNumber());
        purchaseOrder.setStatus("DRAFT");
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        log.info("Purchase order created successfully: {}", saved.getId());
        return saved;
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public PurchaseOrder updatePurchaseOrder(UUID id, PurchaseOrder purchaseOrderData) {
        log.info("Updating purchase order: {}", id);
        PurchaseOrder existing = getPurchaseOrderById(id);
        
        // Update fields
        existing.setVendorId(purchaseOrderData.getVendorId());
        existing.setVendorName(purchaseOrderData.getVendorName());
        existing.setPoDate(purchaseOrderData.getPoDate());
        existing.setExpectedDeliveryDate(purchaseOrderData.getExpectedDeliveryDate());
        existing.setCurrency(purchaseOrderData.getCurrency());
        existing.setNotes(purchaseOrderData.getNotes());
        existing.setPaymentTerms(purchaseOrderData.getPaymentTerms());
        
        return purchaseOrderRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public PurchaseOrder approvePurchaseOrder(UUID id, String approvedBy) {
        log.info("Approving purchase order: {}", id);
        PurchaseOrder order = getPurchaseOrderById(id);
        
        if (!"DRAFT".equals(order.getStatus()) && !"SUBMITTED".equals(order.getStatus())) {
            throw new IllegalStateException("Can only approve DRAFT or SUBMITTED purchase orders");
        }
        
        order.setStatus("APPROVED");
        order.setApprovedBy(approvedBy);
        order.setApprovedAt(LocalDateTime.now());
        
        PurchaseOrder updated = purchaseOrderRepository.save(order);
        log.info("Purchase order approved successfully: {}", id);
        return updated;
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public void cancelPurchaseOrder(UUID id, String cancelledBy, String reason) {
        log.info("Cancelling purchase order: {}", id);
        PurchaseOrder order = getPurchaseOrderById(id);
        
        order.setStatus("CANCELLED");
        order.setCancelledBy(cancelledBy);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        
        purchaseOrderRepository.save(order);
        log.info("Purchase order cancelled successfully: {}", id);
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public void deletePurchaseOrder(UUID id) {
        log.info("Deleting purchase order: {}", id);
        purchaseOrderRepository.deleteById(id);
    }
}

