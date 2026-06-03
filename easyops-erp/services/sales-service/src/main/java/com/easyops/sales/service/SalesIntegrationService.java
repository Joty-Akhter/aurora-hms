package com.easyops.sales.service;

import com.easyops.sales.client.ARClient;
import com.easyops.sales.entity.SalesOrder;
import com.easyops.sales.entity.SalesOrderLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesIntegrationService {
    
    private final SalesOrderService salesOrderService;
    private final ARClient arClient;
    
    @Transactional
    public UUID convertOrderToInvoice(UUID orderId) {
        log.info("Converting sales order {} to AR invoice", orderId);
        
        SalesOrder order = salesOrderService.getOrderById(orderId);
        
        // Validate order status
        if (!"COMPLETED".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus()) && !"IN_PROGRESS".equals(order.getStatus())) {
            throw new RuntimeException("Can only invoice orders in COMPLETED, CONFIRMED, or IN_PROGRESS status. Current status: " + order.getStatus());
        }
        
        // Check if already invoiced
        if (order.getConvertedToInvoiceId() != null) {
            throw new RuntimeException("Order already invoiced: " + order.getConvertedToInvoiceId());
        }
        
        // Find or sync customer in AR service
        UUID arCustomerId = findOrCreateARCustomer(order.getCustomerId(), order.getOrganizationId());
        
        // Build invoice request
        Map<String, Object> invoiceRequest = new HashMap<>();
        invoiceRequest.put("organizationId", order.getOrganizationId().toString());
        invoiceRequest.put("customerId", arCustomerId.toString());
        invoiceRequest.put("invoiceDate", order.getOrderDate().toString());
        invoiceRequest.put("dueDate", order.getOrderDate().plusDays(30).toString()); // Default 30 days
        invoiceRequest.put("periodId", null); // Will be auto-determined by accounting service
        
        // Build line items
        List<Map<String, Object>> lines = new ArrayList<>();
        for (SalesOrderLine orderLine : order.getLines()) {
            Map<String, Object> line = new HashMap<>();
            line.put("lineNumber", orderLine.getLineNumber());
            line.put("description", orderLine.getProductName() + 
                    (orderLine.getDescription() != null ? " - " + orderLine.getDescription() : ""));
            line.put("quantity", orderLine.getQuantity());
            line.put("unitPrice", orderLine.getUnitPrice());
            line.put("discountPercent", orderLine.getDiscountPercent());
            line.put("taxPercent", orderLine.getTaxPercent());
            line.put("accountId", orderLine.getRevenueAccountId() != null ? 
                    orderLine.getRevenueAccountId().toString() : null);
            
            lines.add(line);
        }
        invoiceRequest.put("lines", lines);
        
        // Add discount if applicable
        if (order.getDiscountPercent() != null && order.getDiscountPercent().compareTo(java.math.BigDecimal.ZERO) > 0) {
            invoiceRequest.put("discountPercent", order.getDiscountPercent());
        }
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            invoiceRequest.put("discountAmount", order.getDiscountAmount());
        }
        
        // Add notes
        String notes = "Generated from Sales Order: " + order.getOrderNumber();
        if (order.getNotes() != null) {
            notes += "\n" + order.getNotes();
        }
        invoiceRequest.put("notes", notes);
        
        try {
            // Call AR service to create invoice
            Map<String, Object> invoiceResponse = arClient.createInvoice(invoiceRequest);
            UUID invoiceId = UUID.fromString(invoiceResponse.get("id").toString());
            
            log.info("Successfully created AR invoice {} from sales order {}", invoiceId, orderId);
            
            // Update sales order
            salesOrderService.markAsInvoiced(orderId, invoiceId);
            
            return invoiceId;
            
        } catch (Exception e) {
            log.error("Failed to create invoice from sales order {}", orderId, e);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage(), e);
        }
    }
    
    /**
     * Find AR customer ID by matching customer ID
     * Note: Customer master data is maintained in CRM service
     * Sales orders reference CRM account IDs which should map to AR customer IDs
     */
    private UUID findOrCreateARCustomer(UUID customerId, UUID organizationId) {
        try {
            // Try to get customer from AR service
            arClient.getCustomer(customerId);
            log.debug("Customer {} already exists in AR service", customerId);
            return customerId;
        } catch (Exception e) {
            // Customer not found in AR, should sync from CRM
            log.warn("Customer {} not found in AR service. Customer should be synced from CRM to AR.", customerId);
            // For now, return the customerId and let AR service handle the error
            // TODO: Implement CRM-to-AR customer sync
            return customerId;
        }
    }
}

