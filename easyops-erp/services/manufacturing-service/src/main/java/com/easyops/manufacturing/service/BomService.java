package com.easyops.manufacturing.service;

import com.easyops.manufacturing.entity.BomHeader;
import com.easyops.manufacturing.entity.BomLine;
import com.easyops.manufacturing.entity.BomVersion;
import com.easyops.manufacturing.repository.BomHeaderRepository;
import com.easyops.manufacturing.repository.BomLineRepository;
import com.easyops.manufacturing.repository.BomVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BomService {

    private final BomHeaderRepository bomHeaderRepository;
    private final BomLineRepository bomLineRepository;
    private final BomVersionRepository bomVersionRepository;

    @Transactional(readOnly = true)
    public List<BomHeader> getAllBomsByOrganization(UUID organizationId) {
        return bomHeaderRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<BomHeader> getBomById(UUID bomId) {
        return bomHeaderRepository.findById(bomId);
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForBomLine(UUID bomLineId) {
        return bomLineRepository.findById(bomLineId)
                .map(BomLine::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM line not found"));
    }

    @Transactional(readOnly = true)
    public Optional<BomHeader> getBomByNumber(UUID organizationId, String bomNumber) {
        return bomHeaderRepository.findByOrganizationIdAndBomNumber(organizationId, bomNumber);
    }

    @Transactional(readOnly = true)
    public List<BomHeader> getBomsByProduct(UUID organizationId, UUID productId) {
        return bomHeaderRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }

    @Transactional(readOnly = true)
    public List<BomHeader> getActiveBoms(UUID organizationId) {
        return bomHeaderRepository.findActiveBoms(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<BomHeader> getLatestActiveBomForProduct(UUID productId) {
        return bomHeaderRepository.findLatestActiveBomForProduct(productId);
    }

    @Transactional
    public BomHeader createBom(BomHeader bomHeader) {
        // Auto-generate BOM number if not provided
        if (bomHeader.getBomNumber() == null || bomHeader.getBomNumber().isEmpty()) {
            bomHeader.setBomNumber(generateBomNumber(bomHeader.getOrganizationId()));
        }
        
        // Set defaults
        if (bomHeader.getStatus() == null) {
            bomHeader.setStatus("DRAFT");
        }
        if (bomHeader.getVersionNumber() == null) {
            bomHeader.setVersionNumber(1);
        }
        if (bomHeader.getBaseQuantity() == null) {
            bomHeader.setBaseQuantity(BigDecimal.ONE);
        }
        
        BomHeader savedBom = bomHeaderRepository.save(bomHeader);
        log.info("Created BOM: {} for product: {}", savedBom.getBomNumber(), savedBom.getProductId());
        return savedBom;
    }

    @Transactional
    public BomHeader updateBom(UUID bomId, BomHeader bomHeader) {
        BomHeader existing = bomHeaderRepository.findById(bomId)
                .orElseThrow(() -> new RuntimeException("BOM not found: " + bomId));
        
        // Update fields
        existing.setProductCode(bomHeader.getProductCode());
        existing.setProductName(bomHeader.getProductName());
        existing.setBomType(bomHeader.getBomType());
        existing.setRevision(bomHeader.getRevision());
        existing.setStatus(bomHeader.getStatus());
        existing.setEffectiveFrom(bomHeader.getEffectiveFrom());
        existing.setEffectiveTo(bomHeader.getEffectiveTo());
        existing.setBaseQuantity(bomHeader.getBaseQuantity());
        existing.setUom(bomHeader.getUom());
        existing.setDescription(bomHeader.getDescription());
        existing.setNotes(bomHeader.getNotes());
        existing.setUpdatedBy(bomHeader.getUpdatedBy());
        
        BomHeader updated = bomHeaderRepository.save(existing);
        log.info("Updated BOM: {}", updated.getBomNumber());
        return updated;
    }

    @Transactional
    public void deleteBom(UUID bomId) {
        bomHeaderRepository.deleteById(bomId);
        log.info("Deleted BOM: {}", bomId);
    }

    @Transactional
    public BomHeader approveBom(UUID bomId, UUID approvedBy) {
        BomHeader bom = bomHeaderRepository.findById(bomId)
                .orElseThrow(() -> new RuntimeException("BOM not found: " + bomId));
        
        bom.setStatus("APPROVED");
        bom.setApprovedBy(approvedBy);
        bom.setApprovedDate(LocalDateTime.now());
        
        BomHeader approved = bomHeaderRepository.save(bom);
        log.info("Approved BOM: {}", approved.getBomNumber());
        return approved;
    }

    // BOM Line Operations
    @Transactional(readOnly = true)
    public List<BomLine> getBomLines(UUID bomId) {
        return bomLineRepository.findByBomId(bomId);
    }

    @Transactional(readOnly = true)
    public List<BomLine> getTopLevelComponents(UUID bomId) {
        return bomLineRepository.findTopLevelComponentsByBomId(bomId);
    }

    @Transactional
    public BomLine addBomLine(BomLine bomLine) {
        // Set defaults
        if (bomLine.getIsActive() == null) {
            bomLine.setIsActive(true);
        }
        if (bomLine.getIsOptional() == null) {
            bomLine.setIsOptional(false);
        }
        if (bomLine.getIsPhantom() == null) {
            bomLine.setIsPhantom(false);
        }
        if (bomLine.getCanSubstitute() == null) {
            bomLine.setCanSubstitute(false);
        }
        if (bomLine.getScrapPercentage() == null) {
            bomLine.setScrapPercentage(BigDecimal.ZERO);
        }
        
        BomLine saved = bomLineRepository.save(bomLine);
        
        // Recalculate BOM costs
        recalculateBomCosts(bomLine.getBomHeader().getBomId());
        
        log.info("Added BOM line for component: {} to BOM: {}", 
                saved.getComponentId(), saved.getBomHeader().getBomId());
        return saved;
    }

    @Transactional
    public BomLine updateBomLine(UUID bomLineId, BomLine bomLine) {
        BomLine existing = bomLineRepository.findById(bomLineId)
                .orElseThrow(() -> new RuntimeException("BOM Line not found: " + bomLineId));
        
        existing.setComponentCode(bomLine.getComponentCode());
        existing.setComponentName(bomLine.getComponentName());
        existing.setQuantityPerUnit(bomLine.getQuantityPerUnit());
        existing.setUom(bomLine.getUom());
        existing.setComponentType(bomLine.getComponentType());
        existing.setIsOptional(bomLine.getIsOptional());
        existing.setIsPhantom(bomLine.getIsPhantom());
        existing.setCanSubstitute(bomLine.getCanSubstitute());
        existing.setScrapPercentage(bomLine.getScrapPercentage());
        existing.setUnitCost(bomLine.getUnitCost());
        existing.setNotes(bomLine.getNotes());
        existing.setSequenceNumber(bomLine.getSequenceNumber());
        existing.setUpdatedBy(bomLine.getUpdatedBy());
        
        BomLine updated = bomLineRepository.save(existing);
        
        // Recalculate BOM costs
        recalculateBomCosts(existing.getBomHeader().getBomId());
        
        log.info("Updated BOM line: {}", updated.getBomLineId());
        return updated;
    }

    @Transactional
    public void deleteBomLine(UUID bomLineId) {
        BomLine bomLine = bomLineRepository.findById(bomLineId)
                .orElseThrow(() -> new RuntimeException("BOM Line not found: " + bomLineId));
        
        UUID bomId = bomLine.getBomHeader().getBomId();
        bomLineRepository.deleteById(bomLineId);
        
        // Recalculate BOM costs
        recalculateBomCosts(bomId);
        
        log.info("Deleted BOM line: {}", bomLineId);
    }

    // BOM Explosion - Calculate full material requirements
    @Transactional(readOnly = true)
    public Map<String, Object> explodeBom(UUID bomId, BigDecimal quantity) {
        BomHeader bom = bomHeaderRepository.findById(bomId)
                .orElseThrow(() -> new RuntimeException("BOM not found: " + bomId));
        
        List<Map<String, Object>> explosion = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        
        List<BomLine> topLevelLines = bomLineRepository.findTopLevelComponentsByBomId(bomId);
        for (BomLine line : topLevelLines) {
            explodeBomRecursive(line, quantity, 1, explosion, visited);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("bomId", bomId);
        result.put("bomNumber", bom.getBomNumber());
        result.put("productId", bom.getProductId());
        result.put("productCode", bom.getProductCode());
        result.put("quantity", quantity);
        result.put("components", explosion);
        result.put("totalComponents", explosion.size());
        
        return result;
    }

    private void explodeBomRecursive(BomLine line, BigDecimal parentQty, int level, 
                                     List<Map<String, Object>> explosion, Set<UUID> visited) {
        if (visited.contains(line.getBomLineId())) {
            return; // Prevent circular references
        }
        visited.add(line.getBomLineId());
        
        BigDecimal requiredQty = parentQty.multiply(line.getQuantityPerUnit());
        
        // Add scrap
        if (line.getScrapPercentage() != null && line.getScrapPercentage().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal scrapFactor = BigDecimal.ONE.add(line.getScrapPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            requiredQty = requiredQty.multiply(scrapFactor);
        }
        
        Map<String, Object> component = new HashMap<>();
        component.put("componentId", line.getComponentId());
        component.put("componentCode", line.getComponentCode());
        component.put("componentName", line.getComponentName());
        component.put("componentType", line.getComponentType());
        component.put("level", level);
        component.put("quantityPerUnit", line.getQuantityPerUnit());
        component.put("requiredQuantity", requiredQty);
        component.put("uom", line.getUom());
        component.put("unitCost", line.getUnitCost());
        component.put("extendedCost", requiredQty.multiply(line.getUnitCost() != null ? line.getUnitCost() : BigDecimal.ZERO));
        component.put("isOptional", line.getIsOptional());
        component.put("isPhantom", line.getIsPhantom());
        
        explosion.add(component);
        
        // Recursively process child components
        List<BomLine> childLines = bomLineRepository.findByParentLineId(line.getBomLineId());
        for (BomLine childLine : childLines) {
            explodeBomRecursive(childLine, requiredQty, level + 1, explosion, visited);
        }
    }

    // Cost Roll-up Calculation
    @Transactional
    public BomHeader recalculateBomCosts(UUID bomId) {
        BomHeader bom = bomHeaderRepository.findById(bomId)
                .orElseThrow(() -> new RuntimeException("BOM not found: " + bomId));
        
        Map<String, Object> explosion = explodeBom(bomId, bom.getBaseQuantity());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) explosion.get("components");
        
        BigDecimal totalMaterialCost = BigDecimal.ZERO;
        for (Map<String, Object> component : components) {
            BigDecimal extCost = (BigDecimal) component.get("extendedCost");
            if (extCost != null) {
                totalMaterialCost = totalMaterialCost.add(extCost);
            }
        }
        
        bom.setMaterialCost(totalMaterialCost);
        
        // Total cost = material + labor + overhead
        BigDecimal totalCost = totalMaterialCost;
        if (bom.getLaborCost() != null) {
            totalCost = totalCost.add(bom.getLaborCost());
        }
        if (bom.getOverheadCost() != null) {
            totalCost = totalCost.add(bom.getOverheadCost());
        }
        bom.setTotalCost(totalCost);
        
        BomHeader updated = bomHeaderRepository.save(bom);
        log.info("Recalculated costs for BOM: {} - Material: {}, Total: {}", 
                updated.getBomNumber(), totalMaterialCost, totalCost);
        
        return updated;
    }

    // BOM Version Operations
    @Transactional(readOnly = true)
    public List<BomVersion> getBomVersions(UUID bomId) {
        return bomVersionRepository.findByBomId(bomId);
    }

    @Transactional
    public BomVersion createBomVersion(BomVersion bomVersion) {
        BomVersion saved = bomVersionRepository.save(bomVersion);
        log.info("Created BOM version: {} for BOM: {}", 
                saved.getVersionNumber(), saved.getBomHeader().getBomId());
        return saved;
    }

    // Utility Methods
    private String generateBomNumber(UUID organizationId) {
        long count = bomHeaderRepository.countByOrganization(organizationId);
        return String.format("BOM-%06d", count + 1);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBomDashboardStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalBoms", bomHeaderRepository.countByOrganization(organizationId));
        stats.put("activeBoms", bomHeaderRepository.countByOrganizationAndStatus(organizationId, "ACTIVE"));
        stats.put("draftBoms", bomHeaderRepository.countByOrganizationAndStatus(organizationId, "DRAFT"));
        stats.put("approvedBoms", bomHeaderRepository.countByOrganizationAndStatus(organizationId, "APPROVED"));
        
        return stats;
    }
}

