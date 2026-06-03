package com.easyops.pharma.service;

import com.easyops.pharma.entity.Adjustment;
import com.easyops.pharma.entity.AdjustmentLine;
import com.easyops.pharma.repository.AdjustmentRepository;
import com.easyops.pharma.repository.AdjustmentLineRepository;
import com.easyops.pharma.repository.TerritoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdjustmentService {
    
    private final AdjustmentRepository adjustmentRepository;
    private final AdjustmentLineRepository adjustmentLineRepository;
    private final TerritoryRepository territoryRepository;
    
    @Transactional(readOnly = true)
    public List<Adjustment> getAllAdjustments(UUID organizationId) {
        log.debug("Fetching all adjustments for organization: {}", organizationId);
        return adjustmentRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Adjustment> getAdjustmentsByTerritory(UUID territoryId) {
        log.debug("Fetching adjustments for territory: {}", territoryId);
        return adjustmentRepository.findByTerritoryId(territoryId);
    }
    
    @Transactional(readOnly = true)
    public List<Adjustment> getAdjustmentsByTerritoryAndPeriod(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching adjustments for territory: {}, year: {}, month: {}", territoryId, year, month);
        return adjustmentRepository.findByTerritoryIdAndYearAndMonth(territoryId, year, month);
    }
    
    @Transactional(readOnly = true)
    public Adjustment getAdjustmentById(UUID id) {
        log.debug("Fetching adjustment by ID: {}", id);
        Adjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Adjustment not found with ID: " + id));
        
        // Load adjustment lines
        adjustment.setAdjustmentLines(adjustmentLineRepository.findByAdjustmentId(id));
        
        return adjustment;
    }
    
    @Transactional
    @CacheEvict(value = "adjustments", allEntries = true)
    public Adjustment createAdjustment(Adjustment adjustment) {
        log.info("Creating new adjustment for territory: {}", adjustment.getTerritoryId());
        
        // Validate territory exists
        territoryRepository.findById(adjustment.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + adjustment.getTerritoryId()));
        
        // Validate adjustment type
        if (!"DAMAGE".equals(adjustment.getAdjustmentType()) && !"EXPIRY".equals(adjustment.getAdjustmentType())) {
            throw new RuntimeException("Adjustment type must be DAMAGE or EXPIRY");
        }
        
        // Extract year and month from date
        LocalDate date = adjustment.getAdjustmentDate();
        adjustment.setYear(date.getYear());
        adjustment.setMonth(date.getMonthValue());
        
        // Calculate total adjustment amount from lines
        if (adjustment.getAdjustmentLines() != null && !adjustment.getAdjustmentLines().isEmpty()) {
            BigDecimal totalAdjustmentAmount = BigDecimal.ZERO;
            
            for (AdjustmentLine line : adjustment.getAdjustmentLines()) {
                // Calculate line amount: Adjustment Quantity × TP with VAT
                if (line.getAdjustmentQuantity() != null && line.getTpWithVat() != null) {
                    line.setAdjustmentAmount(line.getAdjustmentQuantity().multiply(line.getTpWithVat()));
                    totalAdjustmentAmount = totalAdjustmentAmount.add(line.getAdjustmentAmount());
                }
            }
            
            adjustment.setTotalAdjustmentAmount(totalAdjustmentAmount);
        }
        
        // Set default status
        if (adjustment.getStatus() == null) {
            adjustment.setStatus("DRAFT");
        }
        
        // Save adjustment first
        Adjustment savedAdjustment = adjustmentRepository.save(adjustment);
        
        // Save adjustment lines
        if (adjustment.getAdjustmentLines() != null) {
            for (AdjustmentLine line : adjustment.getAdjustmentLines()) {
                line.setAdjustmentId(savedAdjustment.getId());
                adjustmentLineRepository.save(line);
            }
        }
        
        return savedAdjustment;
    }
    
    @Transactional
    @CacheEvict(value = "adjustments", allEntries = true)
    public Adjustment updateAdjustment(UUID id, Adjustment adjustment) {
        log.info("Updating adjustment: {}", id);
        Adjustment existing = getAdjustmentById(id);
        
        // Don't allow updating submitted/completed adjustments
        if ("SUBMITTED".equals(existing.getStatus()) || "COMPLETED".equals(existing.getStatus())) {
            throw new RuntimeException("Cannot update adjustment with status: " + existing.getStatus());
        }
        
        existing.setAdjustmentType(adjustment.getAdjustmentType());
        existing.setAdjustmentDate(adjustment.getAdjustmentDate());
        existing.setYear(adjustment.getAdjustmentDate().getYear());
        existing.setMonth(adjustment.getAdjustmentDate().getMonthValue());
        existing.setDescription(adjustment.getDescription());
        existing.setUpdatedBy(adjustment.getUpdatedBy());
        
        // Update adjustment lines
        if (adjustment.getAdjustmentLines() != null) {
            // Delete existing lines
            adjustmentLineRepository.findByAdjustmentId(id).forEach(adjustmentLineRepository::delete);
            
            // Recalculate total
            BigDecimal totalAdjustmentAmount = BigDecimal.ZERO;
            
            // Add new lines
            for (AdjustmentLine line : adjustment.getAdjustmentLines()) {
                line.setAdjustmentId(id);
                if (line.getAdjustmentQuantity() != null && line.getTpWithVat() != null) {
                    line.setAdjustmentAmount(line.getAdjustmentQuantity().multiply(line.getTpWithVat()));
                    totalAdjustmentAmount = totalAdjustmentAmount.add(line.getAdjustmentAmount());
                }
                adjustmentLineRepository.save(line);
            }
            
            existing.setTotalAdjustmentAmount(totalAdjustmentAmount);
        }
        
        return adjustmentRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = "adjustments", allEntries = true)
    public Adjustment submitAdjustment(UUID id) {
        log.info("Submitting adjustment: {}", id);
        Adjustment adjustment = getAdjustmentById(id);
        
        if (!"DRAFT".equals(adjustment.getStatus())) {
            throw new RuntimeException("Only DRAFT adjustments can be submitted");
        }
        
        if (adjustment.getAdjustmentLines() == null || adjustment.getAdjustmentLines().isEmpty()) {
            throw new RuntimeException("Cannot submit adjustment without lines");
        }
        
        adjustment.setStatus("SUBMITTED");
        return adjustmentRepository.save(adjustment);
    }
    
    @Transactional
    @CacheEvict(value = "adjustments", allEntries = true)
    public void deleteAdjustment(UUID id) {
        log.info("Deleting adjustment: {}", id);
        Adjustment adjustment = getAdjustmentById(id);
        
        if (!"DRAFT".equals(adjustment.getStatus())) {
            throw new RuntimeException("Only DRAFT adjustments can be deleted");
        }
        
        // Delete adjustment lines first
        adjustmentLineRepository.findByAdjustmentId(id).forEach(adjustmentLineRepository::delete);
        
        adjustmentRepository.delete(adjustment);
    }
}

