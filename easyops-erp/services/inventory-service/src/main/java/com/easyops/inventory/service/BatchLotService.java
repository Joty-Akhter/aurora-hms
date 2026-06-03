package com.easyops.inventory.service;

import com.easyops.inventory.entity.BatchLot;
import com.easyops.inventory.repository.BatchLotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchLotService {
    
    private final BatchLotRepository batchLotRepository;
    
    @Transactional(readOnly = true)
    public List<BatchLot> getAllBatches(UUID organizationId) {
        log.debug("Fetching all batches for organization: {}", organizationId);
        return batchLotRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<BatchLot> getBatchesByProduct(UUID organizationId, UUID productId) {
        log.debug("Fetching batches for product: {}", productId);
        return batchLotRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }
    
    @Transactional(readOnly = true)
    public List<BatchLot> getAvailableBatches(UUID organizationId, UUID productId) {
        log.debug("Fetching available batches for product: {}", productId);
        return batchLotRepository.findAvailableBatchesByProduct(organizationId, productId);
    }
    
    @Transactional(readOnly = true)
    public List<BatchLot> getExpiringBatches(UUID organizationId, LocalDate beforeDate) {
        log.debug("Fetching batches expiring before: {}", beforeDate);
        return batchLotRepository.findExpiringBatches(organizationId, beforeDate);
    }
    
    @Transactional(readOnly = true)
    public List<BatchLot> getExpiredBatches(UUID organizationId) {
        log.debug("Fetching expired batches for organization: {}", organizationId);
        return batchLotRepository.findExpiredBatches(organizationId);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "batch", key = "#id")
    public BatchLot getBatchById(UUID id) {
        log.debug("Fetching batch by ID: {}", id);
        return batchLotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"batch", "batches"}, allEntries = true)
    public BatchLot createBatch(BatchLot batch) {
        log.info("Creating new batch: {} for product: {}", batch.getBatchNumber(), batch.getProductId());
        
        if (batchLotRepository.existsByOrganizationIdAndProductIdAndBatchNumber(
                batch.getOrganizationId(), batch.getProductId(), batch.getBatchNumber())) {
            throw new RuntimeException("Batch already exists: " + batch.getBatchNumber());
        }
        
        batch.setCurrentQuantity(batch.getInitialQuantity());
        batch.setStatus("ACTIVE");
        
        // Check if batch is already expired
        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
            batch.setStatus("EXPIRED");
            log.warn("Batch {} is already expired", batch.getBatchNumber());
        }
        
        return batchLotRepository.save(batch);
    }
    
    @Transactional
    @CacheEvict(value = {"batch", "batches"}, allEntries = true)
    public BatchLot updateBatchQuantity(UUID id, BigDecimal quantity) {
        log.info("Updating batch quantity: {}, new quantity: {}", id, quantity);
        
        BatchLot batch = getBatchById(id);
        batch.setCurrentQuantity(quantity);
        
        // Update status based on quantity
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            batch.setStatus("DEPLETED");
        }
        
        // Check expiry
        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
            batch.setStatus("EXPIRED");
        }
        
        return batchLotRepository.save(batch);
    }
    
    @Transactional
    @CacheEvict(value = {"batch", "batches"}, allEntries = true)
    public BatchLot updateBatch(UUID id, BatchLot batchData) {
        log.info("Updating batch: {}", id);
        
        BatchLot existingBatch = getBatchById(id);
        
        // Update fields
        existingBatch.setProductId(batchData.getProductId());
        existingBatch.setBatchNumber(batchData.getBatchNumber());
        existingBatch.setLotNumber(batchData.getLotNumber());
        existingBatch.setManufactureDate(batchData.getManufactureDate());
        existingBatch.setExpiryDate(batchData.getExpiryDate());
        existingBatch.setSupplierId(batchData.getSupplierId());
        existingBatch.setPoNumber(batchData.getPoNumber());
        existingBatch.setReceiptDate(batchData.getReceiptDate());
        existingBatch.setInitialQuantity(batchData.getInitialQuantity());
        existingBatch.setStatus(batchData.getStatus());
        existingBatch.setQualityCertificate(batchData.getQualityCertificate());
        existingBatch.setNotes(batchData.getNotes());
        
        // Check if batch is expired
        if (existingBatch.getExpiryDate() != null && existingBatch.getExpiryDate().isBefore(LocalDate.now())) {
            existingBatch.setStatus("EXPIRED");
        }
        
        return batchLotRepository.save(existingBatch);
    }
    
    @Transactional
    @CacheEvict(value = {"batch", "batches"}, allEntries = true)
    public BatchLot updateBatchStatus(UUID id, String status) {
        log.info("Updating batch status: {}, new status: {}", id, status);
        
        BatchLot batch = getBatchById(id);
        batch.setStatus(status);
        
        return batchLotRepository.save(batch);
    }
    
    @Transactional
    @CacheEvict(value = {"batch", "batches"}, allEntries = true)
    public void deleteBatch(UUID id) {
        log.info("Deleting batch: {}", id);
        
        BatchLot batch = getBatchById(id);
        
        // Check if batch can be deleted (no current quantity or already depleted)
        if (batch.getCurrentQuantity().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot delete batch with remaining quantity");
        }
        
        batchLotRepository.delete(batch);
    }
    
    /**
     * Get next available batch using FEFO (First Expiry, First Out)
     */
    @Transactional(readOnly = true)
    public BatchLot getNextAvailableBatch(UUID organizationId, UUID productId) {
        List<BatchLot> batches = batchLotRepository.findAvailableBatchesByProduct(organizationId, productId);
        
        if (batches.isEmpty()) {
            throw new RuntimeException("No available batches for product");
        }
        
        return batches.get(0); // Already sorted by expiry date
    }
}

