package com.easyops.pharma.service;

import com.easyops.pharma.entity.*;
import com.easyops.pharma.repository.*;
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
public class ProductDisbursementService {
    
    private final ProductDisbursementRepository disbursementRepository;
    private final ProductDisbursementLineRepository disbursementLineRepository;
    private final TerritoryRepository territoryRepository;
    private final EmployeeTerritoryAssignmentRepository assignmentRepository;
    
    @Transactional(readOnly = true)
    public List<ProductDisbursement> getAllDisbursements(UUID organizationId) {
        log.debug("Fetching all product disbursements for organization: {}", organizationId);
        return disbursementRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<ProductDisbursement> getDisbursementsByTerritory(UUID territoryId) {
        log.debug("Fetching disbursements for territory: {}", territoryId);
        return disbursementRepository.findByTerritoryId(territoryId);
    }
    
    @Transactional(readOnly = true)
    public List<ProductDisbursement> getDisbursementsByTerritoryAndPeriod(UUID territoryId, Integer year, Integer month) {
        log.debug("Fetching disbursements for territory: {}, year: {}, month: {}", territoryId, year, month);
        return disbursementRepository.findByTerritoryAndPeriod(territoryId, year, month);
    }
    
    @Transactional(readOnly = true)
    public ProductDisbursement getDisbursementById(UUID id) {
        log.debug("Fetching disbursement by ID: {}", id);
        ProductDisbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product disbursement not found with ID: " + id));
        
        // Load disbursement lines
        disbursement.setDisbursementLines(disbursementLineRepository.findByProductDisbursementId(id));
        
        return disbursement;
    }
    
    @Transactional
    @CacheEvict(value = "productDisbursements", allEntries = true)
    public ProductDisbursement createDisbursement(ProductDisbursement disbursement) {
        log.info("Creating new product disbursement for territory: {}", disbursement.getTerritoryId());
        
        // Validate territory exists
        Territory territory = territoryRepository.findById(disbursement.getTerritoryId())
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + disbursement.getTerritoryId()));
        
        if (!Boolean.TRUE.equals(territory.getIsActive())) {
            throw new RuntimeException("Cannot disburse to inactive territory");
        }
        
        // Validate employee belongs to territory
        List<EmployeeTerritoryAssignment> assignments = assignmentRepository.findActiveAssignmentsByTerritory(
                disbursement.getTerritoryId(), LocalDate.now());
        boolean employeeBelongsToTerritory = assignments.stream()
                .anyMatch(a -> a.getEmployeeId().equals(disbursement.getEmployeeId()));
        
        if (!employeeBelongsToTerritory) {
            throw new RuntimeException("Employee does not belong to selected territory");
        }
        
        // Extract year and month from date
        LocalDate date = disbursement.getDisbursementDate();
        disbursement.setYear(date.getYear());
        disbursement.setMonth(date.getMonthValue());
        
        // Calculate totals from lines
        if (disbursement.getDisbursementLines() != null && !disbursement.getDisbursementLines().isEmpty()) {
            BigDecimal previousMonthOpeningTotalDue = BigDecimal.ZERO;
            BigDecimal totalSupplyAmount = BigDecimal.ZERO;
            BigDecimal totalBalanceAmount = BigDecimal.ZERO;
            
            for (ProductDisbursementLine line : disbursement.getDisbursementLines()) {
                // Calculate line amounts
                if (line.getPreviousMonthOpeningQuantity() != null && line.getTpWithVat() != null) {
                    BigDecimal openingValue = line.getPreviousMonthOpeningQuantity().multiply(line.getTpWithVat());
                    previousMonthOpeningTotalDue = previousMonthOpeningTotalDue.add(openingValue);
                }
                
                if (line.getCurrentMonthQuantity() != null && line.getTpWithVat() != null) {
                    line.setProductAmount(line.getCurrentMonthQuantity().multiply(line.getTpWithVat()));
                    totalSupplyAmount = totalSupplyAmount.add(line.getProductAmount());
                    
                    // Total quantity = Opening + Current
                    if (line.getPreviousMonthOpeningQuantity() != null) {
                        line.setTotalQuantity(line.getPreviousMonthOpeningQuantity().add(line.getCurrentMonthQuantity()));
                    } else {
                        line.setTotalQuantity(line.getCurrentMonthQuantity());
                    }
                    
                    // Total balance amount = Total Quantity × TP
                    if (line.getTotalQuantity() != null && line.getTpWithVat() != null) {
                        totalBalanceAmount = totalBalanceAmount.add(line.getTotalQuantity().multiply(line.getTpWithVat()));
                    }
                }
            }
            
            disbursement.setPreviousMonthOpeningTotalDue(previousMonthOpeningTotalDue);
            disbursement.setTotalSupplyAmount(totalSupplyAmount);
            disbursement.setTotalBalanceAmount(totalBalanceAmount);
        }
        
        // Set default status
        if (disbursement.getStatus() == null) {
            disbursement.setStatus("DRAFT");
        }
        
        // Save disbursement first
        ProductDisbursement savedDisbursement = disbursementRepository.save(disbursement);
        
        // Save disbursement lines
        if (disbursement.getDisbursementLines() != null) {
            for (ProductDisbursementLine line : disbursement.getDisbursementLines()) {
                line.setProductDisbursementId(savedDisbursement.getId());
                disbursementLineRepository.save(line);
            }
        }
        
        return savedDisbursement;
    }
    
    @Transactional
    @CacheEvict(value = "productDisbursements", allEntries = true)
    public ProductDisbursement updateDisbursement(UUID id, ProductDisbursement disbursement) {
        log.info("Updating product disbursement: {}", id);
        ProductDisbursement existing = getDisbursementById(id);
        
        // Don't allow updating submitted/completed disbursements
        if ("SUBMITTED".equals(existing.getStatus()) || "COMPLETED".equals(existing.getStatus())) {
            throw new RuntimeException("Cannot update disbursement with status: " + existing.getStatus());
        }
        
        existing.setDisbursementDate(disbursement.getDisbursementDate());
        existing.setYear(disbursement.getDisbursementDate().getYear());
        existing.setMonth(disbursement.getDisbursementDate().getMonthValue());
        existing.setNotes(disbursement.getNotes());
        existing.setUpdatedBy(disbursement.getUpdatedBy());
        
        // Update disbursement lines
        if (disbursement.getDisbursementLines() != null) {
            // Delete existing lines
            disbursementLineRepository.findByProductDisbursementId(id).forEach(disbursementLineRepository::delete);
            
            // Recalculate totals
            BigDecimal previousMonthOpeningTotalDue = BigDecimal.ZERO;
            BigDecimal totalSupplyAmount = BigDecimal.ZERO;
            BigDecimal totalBalanceAmount = BigDecimal.ZERO;
            
            // Add new lines
            for (ProductDisbursementLine line : disbursement.getDisbursementLines()) {
                line.setProductDisbursementId(id);
                if (line.getCurrentMonthQuantity() != null && line.getTpWithVat() != null) {
                    line.setProductAmount(line.getCurrentMonthQuantity().multiply(line.getTpWithVat()));
                    totalSupplyAmount = totalSupplyAmount.add(line.getProductAmount());
                    
                    if (line.getPreviousMonthOpeningQuantity() != null) {
                        line.setTotalQuantity(line.getPreviousMonthOpeningQuantity().add(line.getCurrentMonthQuantity()));
                        BigDecimal openingValue = line.getPreviousMonthOpeningQuantity().multiply(line.getTpWithVat());
                        previousMonthOpeningTotalDue = previousMonthOpeningTotalDue.add(openingValue);
                    } else {
                        line.setTotalQuantity(line.getCurrentMonthQuantity());
                    }
                    
                    if (line.getTotalQuantity() != null && line.getTpWithVat() != null) {
                        totalBalanceAmount = totalBalanceAmount.add(line.getTotalQuantity().multiply(line.getTpWithVat()));
                    }
                }
                disbursementLineRepository.save(line);
            }
            
            existing.setPreviousMonthOpeningTotalDue(previousMonthOpeningTotalDue);
            existing.setTotalSupplyAmount(totalSupplyAmount);
            existing.setTotalBalanceAmount(totalBalanceAmount);
        }
        
        return disbursementRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = "productDisbursements", allEntries = true)
    public ProductDisbursement submitDisbursement(UUID id) {
        log.info("Submitting product disbursement: {}", id);
        ProductDisbursement disbursement = getDisbursementById(id);
        
        if (!"DRAFT".equals(disbursement.getStatus())) {
            throw new RuntimeException("Only DRAFT disbursements can be submitted");
        }
        
        if (disbursement.getDisbursementLines() == null || disbursement.getDisbursementLines().isEmpty()) {
            throw new RuntimeException("Cannot submit disbursement without lines");
        }
        
        disbursement.setStatus("SUBMITTED");
        return disbursementRepository.save(disbursement);
    }
    
    @Transactional
    @CacheEvict(value = "productDisbursements", allEntries = true)
    public void deleteDisbursement(UUID id) {
        log.info("Deleting product disbursement: {}", id);
        ProductDisbursement disbursement = getDisbursementById(id);
        
        if (!"DRAFT".equals(disbursement.getStatus())) {
            throw new RuntimeException("Only DRAFT disbursements can be deleted");
        }
        
        // Delete disbursement lines first
        disbursementLineRepository.findByProductDisbursementId(id).forEach(disbursementLineRepository::delete);
        
        disbursementRepository.delete(disbursement);
    }
}

