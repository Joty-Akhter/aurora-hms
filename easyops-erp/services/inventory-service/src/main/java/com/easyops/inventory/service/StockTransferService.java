package com.easyops.inventory.service;

import com.easyops.inventory.entity.StockTransfer;
import com.easyops.inventory.entity.StockTransferLine;
import com.easyops.inventory.repository.StockTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTransferService {
    
    private final StockTransferRepository transferRepository;
    private final StockService stockService;
    
    @Transactional(readOnly = true)
    public List<StockTransfer> getAllTransfers(UUID organizationId) {
        log.debug("Fetching all transfers for organization: {}", organizationId);
        return transferRepository.findByOrganizationIdOrderByTransferDateDesc(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<StockTransfer> getTransfersByStatus(UUID organizationId, String status) {
        log.debug("Fetching transfers with status: {} for organization: {}", status, organizationId);
        return transferRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    @Transactional(readOnly = true)
    public List<StockTransfer> getPendingTransfers(UUID organizationId) {
        return transferRepository.findPendingTransfers(organizationId);
    }
    
    @Transactional(readOnly = true)
    public StockTransfer getTransferById(UUID id) {
        log.debug("Fetching transfer by ID: {}", id);
        return transferRepository.findByIdWithLines(id)
                .orElseThrow(() -> new RuntimeException("Transfer not found with ID: " + id));
    }
    
    @Transactional
    public StockTransfer createTransfer(StockTransfer transfer) {
        log.info("Creating stock transfer from warehouse: {} to warehouse: {}", 
                 transfer.getFromWarehouseId(), transfer.getToWarehouseId());
        
        if (transfer.getFromWarehouseId().equals(transfer.getToWarehouseId())) {
            throw new RuntimeException("Cannot transfer to the same warehouse");
        }
        
        // Generate transfer number if not provided
        if (transfer.getTransferNumber() == null || transfer.getTransferNumber().isEmpty()) {
            transfer.setTransferNumber(generateTransferNumber());
        }
        
        if (transferRepository.existsByTransferNumber(transfer.getTransferNumber())) {
            throw new RuntimeException("Transfer number already exists: " + transfer.getTransferNumber());
        }
        
        transfer.setStatus("DRAFT");
        transfer.setTransferDate(LocalDate.now());
        
        // Set transferId on all lines before saving (using addLine method which sets the transferId)
        // But first we need to save to get an ID, so we'll handle it differently
        List<StockTransferLine> linesToAdd = new ArrayList<>(transfer.getLines());
        transfer.getLines().clear(); // Clear lines temporarily
        
        // Save transfer first to get an ID
        StockTransfer savedTransfer = transferRepository.save(transfer);
        
        // Now add lines with the transfer ID set
        for (StockTransferLine line : linesToAdd) {
            line.setTransferId(savedTransfer.getId());
            savedTransfer.addLine(line);
        }
        
        // Save again to persist the lines
        return transferRepository.save(savedTransfer);
    }
    
    @Transactional
    public StockTransfer submitForApproval(UUID id, UUID userId) {
        log.info("Submitting transfer: {} for approval by user: {}", id, userId);
        
        StockTransfer transfer = getTransferById(id);
        
        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new RuntimeException("Can only submit draft transfers");
        }
        
        if (transfer.getLines().isEmpty()) {
            throw new RuntimeException("Cannot submit transfer with no lines");
        }
        
        // Validate stock availability
        for (StockTransferLine line : transfer.getLines()) {
            validateStockAvailability(transfer, line);
        }
        
        transfer.setStatus("SUBMITTED");
        transfer.setRequestedBy(userId);
        
        return transferRepository.save(transfer);
    }
    
    @Transactional
    public StockTransfer approveTransfer(UUID id, UUID approvedBy) {
        log.info("Approving transfer: {} by user: {}", id, approvedBy);
        
        StockTransfer transfer = getTransferById(id);
        
        if (!"SUBMITTED".equals(transfer.getStatus())) {
            throw new RuntimeException("Can only approve submitted transfers");
        }
        
        transfer.setStatus("APPROVED");
        transfer.setApprovedBy(approvedBy);
        transfer.setApprovedAt(LocalDateTime.now());
        
        return transferRepository.save(transfer);
    }
    
    @Transactional
    public StockTransfer shipTransfer(UUID id, UUID shippedBy, String trackingNumber) {
        log.info("Shipping transfer: {} by user: {}", id, shippedBy);
        
        StockTransfer transfer = getTransferById(id);
        
        if (!"APPROVED".equals(transfer.getStatus())) {
            throw new RuntimeException("Can only ship approved transfers");
        }
        
        // Issue stock from source warehouse
        for (StockTransferLine line : transfer.getLines()) {
            try {
                stockService.issueStock(
                    transfer.getOrganizationId(),
                    line.getProductId(),
                    transfer.getFromWarehouseId(),
                    line.getRequestedQuantity(),
                    "TRANSFER",
                    transfer.getId(),
                    shippedBy
                );
                
                line.setShippedQuantity(line.getRequestedQuantity());
                line.setStatus("SHIPPED");
                
            } catch (Exception e) {
                log.error("Failed to issue stock for product: {}", line.getProductId(), e);
                throw new RuntimeException("Failed to issue stock: " + e.getMessage());
            }
        }
        
        transfer.setStatus("IN_TRANSIT");
        transfer.setShippedBy(shippedBy);
        transfer.setShippedAt(LocalDateTime.now());
        transfer.setTrackingNumber(trackingNumber);
        
        return transferRepository.save(transfer);
    }
    
    @Transactional
    public StockTransfer receiveTransfer(UUID id, UUID receivedBy) {
        log.info("Receiving transfer: {} by user: {}", id, receivedBy);
        
        StockTransfer transfer = getTransferById(id);
        
        if (!"IN_TRANSIT".equals(transfer.getStatus())) {
            throw new RuntimeException("Can only receive in-transit transfers");
        }
        
        // Receive stock at destination warehouse
        for (StockTransferLine line : transfer.getLines()) {
            try {
                BigDecimal qtyToReceive = line.getShippedQuantity();
                
                stockService.receiveStock(
                    transfer.getOrganizationId(),
                    line.getProductId(),
                    transfer.getToWarehouseId(),
                    qtyToReceive,
                    line.getUnitCost(),
                    "TRANSFER",
                    transfer.getId(),
                    receivedBy
                );
                
                line.setReceivedQuantity(qtyToReceive);
                line.setVarianceQuantity(qtyToReceive.subtract(line.getRequestedQuantity()));
                line.setStatus("RECEIVED");
                
            } catch (Exception e) {
                log.error("Failed to receive stock for product: {}", line.getProductId(), e);
                throw new RuntimeException("Failed to receive stock: " + e.getMessage());
            }
        }
        
        transfer.setStatus("RECEIVED");
        transfer.setReceivedBy(receivedBy);
        transfer.setReceivedAt(LocalDateTime.now());
        transfer.setActualDeliveryDate(LocalDate.now());
        
        return transferRepository.save(transfer);
    }
    
    @Transactional
    public StockTransfer cancelTransfer(UUID id, String reason) {
        log.info("Cancelling transfer: {}", id);
        
        StockTransfer transfer = getTransferById(id);
        
        if ("RECEIVED".equals(transfer.getStatus())) {
            throw new RuntimeException("Cannot cancel received transfers");
        }
        
        if ("IN_TRANSIT".equals(transfer.getStatus())) {
            throw new RuntimeException("Cannot cancel in-transit transfers. Receive the transfer and adjust if needed.");
        }
        
        transfer.setStatus("CANCELLED");
        transfer.setNotes(reason);
        
        // Cancel all lines
        transfer.getLines().forEach(line -> line.setStatus("CANCELLED"));
        
        return transferRepository.save(transfer);
    }
    
    private void validateStockAvailability(StockTransfer transfer, StockTransferLine line) {
        // This would check if there's enough stock in the source warehouse
        // For now, we'll rely on the issueStock method to validate
        log.debug("Validating stock availability for product: {}, qty: {}", 
                 line.getProductId(), line.getRequestedQuantity());
    }
    
    private String generateTransferNumber() {
        String prefix = "TR";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp.substring(timestamp.length() - 10);
    }
}

