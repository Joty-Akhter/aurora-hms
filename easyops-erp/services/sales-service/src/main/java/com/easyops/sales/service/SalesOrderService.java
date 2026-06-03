package com.easyops.sales.service;

import com.easyops.sales.client.InventoryClient;
import com.easyops.sales.dto.SalesOrderRequest;
import com.easyops.sales.entity.*;
import com.easyops.sales.repository.QuotationRepository;
import com.easyops.sales.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesOrderService {
    
    private final SalesOrderRepository salesOrderRepository;
    private final QuotationRepository quotationRepository;
    private final InventoryClient inventoryClient;
    
    @Value("${sales.order.auto-number-prefix:SO}")
    private String orderPrefix;
    
    @Value("${sales.order.require-approval:false}")
    private boolean requireApproval;
    
    @Value("${sales.order.approval-amount-threshold:10000}")
    private double approvalThreshold;
    
    @Transactional(readOnly = true)
    public List<SalesOrder> getAllOrders(UUID organizationId) {
        return salesOrderRepository.findByOrganizationIdOrderByOrderDateDesc(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<SalesOrder> getOrdersByStatus(UUID organizationId, String status) {
        return salesOrderRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<SalesOrder> getOrdersByCustomer(UUID organizationId, UUID customerId) {
        return salesOrderRepository.findByOrganizationIdAndCustomerId(organizationId, customerId);
    }
    
    @Transactional(readOnly = true)
    public List<SalesOrder> getOrdersReadyForInvoicing(UUID organizationId) {
        return salesOrderRepository.findOrdersReadyForInvoicing(organizationId);
    }
    
    @Transactional(readOnly = true)
    public SalesOrder getOrderById(UUID id) {
        return salesOrderRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Sales order not found with id: " + id));
    }
    
    @Transactional
    public SalesOrder createOrder(SalesOrderRequest request) {
        log.info("Creating sales order for organization: {}", request.getOrganizationId());
        
        // Customer details should be provided in the request
        // Customer master data is managed in CRM service, not in Sales service
        if (request.getCustomerId() == null) {
            throw new RuntimeException("Customer ID is required");
        }
        
        // Create sales order
        SalesOrder order = new SalesOrder();
        order.setOrganizationId(request.getOrganizationId());
        order.setOrderNumber(generateOrderNumber(request.getOrganizationId()));
        order.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        
        order.setCustomerId(request.getCustomerId());
        order.setCustomerName(request.getCustomerName()); // From request
        order.setCustomerEmail(request.getCustomerEmail()); // From request
        order.setContactPerson(request.getContactPerson());
        order.setBillingAddress(request.getBillingAddress());
        order.setShippingAddress(request.getShippingAddress());
        
        order.setDiscountPercent(request.getDiscountPercent());
        order.setDiscountAmount(request.getDiscountAmount());
        order.setPriority(request.getPriority());
        order.setNotes(request.getNotes());
        order.setTermsAndConditions(request.getTermsAndConditions());
        order.setSalesPersonId(request.getSalesPersonId());
        order.setSalesPersonName(request.getSalesPersonName());
        order.setQuotationId(request.getQuotationId());
        order.setStatus("DRAFT");
        
        // Add lines
        int lineNumber = 1;
        for (SalesOrderRequest.SalesOrderLineRequest lineReq : request.getLines()) {
            SalesOrderLine line = new SalesOrderLine();
            line.setLineNumber(lineNumber++);
            line.setProductId(lineReq.getProductId());
            line.setProductCode(lineReq.getProductCode());
            line.setProductName(lineReq.getProductName());
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitOfMeasure(lineReq.getUnitOfMeasure());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setDiscountPercent(lineReq.getDiscountPercent());
            line.setTaxPercent(lineReq.getTaxPercent());
            line.setRevenueAccountId(lineReq.getRevenueAccountId());
            
            line.calculateLineTotal();
            order.addLine(line);
        }
        
        // Calculate totals
        order.calculateTotals();
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public SalesOrder createOrderFromQuotation(UUID quotationId) {
        log.info("Creating sales order from quotation: {}", quotationId);
        
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));
        
        if (!"ACCEPTED".equals(quotation.getStatus())) {
            throw new RuntimeException("Can only convert ACCEPTED quotations to orders");
        }
        
        // Check if already converted
        if (quotation.getConvertedToOrderId() != null) {
            throw new RuntimeException("Quotation already converted to order: " + quotation.getConvertedToOrderId());
        }
        
        // Create order from quotation
        SalesOrder order = new SalesOrder();
        order.setOrganizationId(quotation.getOrganizationId());
        order.setOrderNumber(generateOrderNumber(quotation.getOrganizationId()));
        order.setOrderDate(LocalDate.now());
        order.setCustomerId(quotation.getCustomerId());
        order.setCustomerName(quotation.getCustomerName());
        order.setCustomerEmail(quotation.getCustomerEmail());
        order.setContactPerson(quotation.getContactPerson());
        order.setBillingAddress(quotation.getBillingAddress());
        order.setShippingAddress(quotation.getShippingAddress());
        order.setSubtotal(quotation.getSubtotal());
        order.setDiscountAmount(quotation.getDiscountAmount());
        order.setDiscountPercent(quotation.getDiscountPercent());
        order.setTaxAmount(quotation.getTaxAmount());
        order.setTotalAmount(quotation.getTotalAmount());
        order.setNotes(quotation.getNotes());
        order.setTermsAndConditions(quotation.getTermsAndConditions());
        order.setSalesPersonId(quotation.getSalesPersonId());
        order.setSalesPersonName(quotation.getSalesPersonName());
        order.setQuotationId(quotation.getId());
        order.setStatus("DRAFT");
        
        // Copy lines
        int lineNumber = 1;
        for (QuotationLine quoteLine : quotation.getLines()) {
            SalesOrderLine orderLine = new SalesOrderLine();
            orderLine.setLineNumber(lineNumber++);
            orderLine.setProductId(quoteLine.getProductId());
            orderLine.setProductCode(quoteLine.getProductCode());
            orderLine.setProductName(quoteLine.getProductName());
            orderLine.setDescription(quoteLine.getDescription());
            orderLine.setQuantity(quoteLine.getQuantity());
            orderLine.setUnitOfMeasure(quoteLine.getUnitOfMeasure());
            orderLine.setUnitPrice(quoteLine.getUnitPrice());
            orderLine.setDiscountPercent(quoteLine.getDiscountPercent());
            orderLine.setDiscountAmount(quoteLine.getDiscountAmount());
            orderLine.setTaxPercent(quoteLine.getTaxPercent());
            orderLine.setTaxAmount(quoteLine.getTaxAmount());
            orderLine.setLineTotal(quoteLine.getLineTotal());
            
            order.addLine(orderLine);
        }
        
        SalesOrder savedOrder = salesOrderRepository.save(order);
        
        // Mark quotation as converted
        quotation.setStatus("CONVERTED");
        quotation.setConvertedToOrderId(savedOrder.getId());
        quotation.setConvertedDate(LocalDateTime.now());
        quotationRepository.save(quotation);
        
        return savedOrder;
    }
    
    @Transactional
    public SalesOrder updateOrder(UUID id, SalesOrderRequest request) {
        log.info("Updating sales order: {}", id);
        
        SalesOrder order = getOrderById(id);
        
        // Only allow updates if order is in DRAFT status
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("Cannot update order in status: " + order.getStatus());
        }
        
        // Update basic fields
        order.setOrderDate(request.getOrderDate());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setContactPerson(request.getContactPerson());
        order.setBillingAddress(request.getBillingAddress());
        order.setShippingAddress(request.getShippingAddress());
        order.setDiscountPercent(request.getDiscountPercent());
        order.setDiscountAmount(request.getDiscountAmount());
        order.setPriority(request.getPriority());
        order.setNotes(request.getNotes());
        order.setTermsAndConditions(request.getTermsAndConditions());
        order.setSalesPersonId(request.getSalesPersonId());
        order.setSalesPersonName(request.getSalesPersonName());
        
        // Update lines - clear and re-add
        order.getLines().clear();
        
        int lineNumber = 1;
        for (SalesOrderRequest.SalesOrderLineRequest lineReq : request.getLines()) {
            SalesOrderLine line = new SalesOrderLine();
            line.setLineNumber(lineNumber++);
            line.setProductId(lineReq.getProductId());
            line.setProductCode(lineReq.getProductCode());
            line.setProductName(lineReq.getProductName());
            line.setDescription(lineReq.getDescription());
            line.setQuantity(lineReq.getQuantity());
            line.setUnitOfMeasure(lineReq.getUnitOfMeasure());
            line.setUnitPrice(lineReq.getUnitPrice());
            line.setDiscountPercent(lineReq.getDiscountPercent());
            line.setTaxPercent(lineReq.getTaxPercent());
            line.setRevenueAccountId(lineReq.getRevenueAccountId());
            
            line.calculateLineTotal();
            order.addLine(line);
        }
        
        // Recalculate totals
        order.calculateTotals();
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public SalesOrder confirmOrder(UUID id) {
        log.info("Confirming sales order: {}", id);
        
        SalesOrder order = getOrderById(id);
        
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("Can only confirm orders in DRAFT status");
        }
        
        // Check if approval is required
        if (requireApproval && order.getTotalAmount().doubleValue() >= approvalThreshold) {
            throw new RuntimeException("Order requires approval before confirmation");
        }
        
        // Allocate inventory for each line
        for (SalesOrderLine line : order.getLines()) {
            try {
                allocateInventoryForOrderLine(order.getOrganizationId(), line, order.getId());
            } catch (Exception e) {
                log.error("Failed to allocate inventory for product {}: {}", line.getProductId(), e.getMessage());
                throw new RuntimeException("Failed to allocate inventory: " + e.getMessage());
            }
        }
        
        order.setStatus("CONFIRMED");
        
        // TODO: Send confirmation email
        
        return salesOrderRepository.save(order);
    }
    
    /**
     * Allocate inventory for an order line
     */
    private void allocateInventoryForOrderLine(UUID organizationId, SalesOrderLine line, UUID orderId) {
        log.info("Allocating inventory for product: {}, quantity: {}", line.getProductId(), line.getQuantity());
        
        // For now, use first available warehouse (Main Warehouse)
        // TODO: Implement warehouse selection logic
        
        Map<String, Object> allocationRequest = new HashMap<>();
        allocationRequest.put("organizationId", organizationId);
        allocationRequest.put("productId", line.getProductId());
        allocationRequest.put("warehouseId", line.getProductId()); // Will need to get proper warehouse
        allocationRequest.put("quantity", line.getQuantity());
        allocationRequest.put("salesOrderId", orderId);
        
        try {
            inventoryClient.allocateStock(allocationRequest);
            log.info("Successfully allocated {} units of product {}", line.getQuantity(), line.getProductId());
        } catch (Exception e) {
            log.error("Inventory allocation failed: {}", e.getMessage());
            throw e;
        }
    }
    
    @Transactional
    public SalesOrder approveOrder(UUID id, UUID approvedBy) {
        log.info("Approving sales order: {} by user: {}", id, approvedBy);
        
        SalesOrder order = getOrderById(id);
        
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("Can only approve orders in DRAFT status");
        }
        
        order.setApprovedBy(approvedBy);
        order.setApprovedDate(LocalDateTime.now());
        order.setStatus("CONFIRMED");
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public SalesOrder startProcessing(UUID id) {
        log.info("Starting processing for order: {}", id);
        
        SalesOrder order = getOrderById(id);
        
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("Can only start processing confirmed orders");
        }
        
        order.setStatus("IN_PROGRESS");
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public SalesOrder completeOrder(UUID id) {
        log.info("Completing order: {}", id);
        
        SalesOrder order = getOrderById(id);
        
        if (!"IN_PROGRESS".equals(order.getStatus())) {
            throw new RuntimeException("Can only complete orders in IN_PROGRESS status");
        }
        
        order.setStatus("COMPLETED");
        order.setDeliveryStatus("DELIVERED");
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public SalesOrder cancelOrder(UUID id) {
        log.info("Cancelling order: {}", id);
        
        SalesOrder order = getOrderById(id);
        
        if ("INVOICED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot cancel order in status: " + order.getStatus());
        }
        
        order.setStatus("CANCELLED");
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public SalesOrder markAsInvoiced(UUID id, UUID invoiceId) {
        log.info("Marking order {} as invoiced with invoice {}", id, invoiceId);
        
        SalesOrder order = getOrderById(id);
        order.setStatus("INVOICED");
        order.setConvertedToInvoiceId(invoiceId);
        order.setConvertedToInvoiceDate(LocalDateTime.now());
        order.setPaymentStatus("UNPAID");
        
        return salesOrderRepository.save(order);
    }
    
    @Transactional
    public void deleteOrder(UUID id) {
        log.info("Deleting sales order: {}", id);
        
        SalesOrder order = getOrderById(id);
        
        // Only allow deletion of DRAFT or CANCELLED orders
        if (!"DRAFT".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("Cannot delete order in status: " + order.getStatus());
        }
        
        salesOrderRepository.delete(order);
    }
    
    private String generateOrderNumber(UUID organizationId) {
        Integer maxNumber = salesOrderRepository.findMaxOrderNumber(organizationId, orderPrefix);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("%s%06d", orderPrefix, nextNumber);
    }
}

