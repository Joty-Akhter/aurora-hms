package com.easyops.manufacturing.service;

import com.easyops.manufacturing.entity.ProductRouting;
import com.easyops.manufacturing.repository.ProductRoutingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductRoutingService {

    private final ProductRoutingRepository productRoutingRepository;

    @Transactional(readOnly = true)
    public List<ProductRouting> getAllRoutingsByOrganization(UUID organizationId) {
        return productRoutingRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductRouting> getRoutingById(UUID routingId) {
        return productRoutingRepository.findById(routingId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductRouting> getRoutingByNumber(UUID organizationId, String routingNumber) {
        return productRoutingRepository.findByOrganizationIdAndRoutingNumber(organizationId, routingNumber);
    }

    @Transactional(readOnly = true)
    public List<ProductRouting> getRoutingsByProduct(UUID productId) {
        return productRoutingRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductRouting> getActiveRoutingsByProduct(UUID productId) {
        return productRoutingRepository.findActiveRoutingsByProductId(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductRouting> getActiveRoutingsByBom(UUID bomId) {
        return productRoutingRepository.findActiveRoutingsByBomId(bomId);
    }

    @Transactional(readOnly = true)
    public List<ProductRouting> getRoutingsByWorkCenter(String workCenterCode) {
        return productRoutingRepository.findByWorkCenterCode(workCenterCode);
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForProductRoutings(UUID productId) {
        List<ProductRouting> list = productRoutingRepository.findByProductId(productId);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No routings for product");
        }
        return list.get(0).getOrganizationId();
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForActiveProductRoutings(UUID productId) {
        List<ProductRouting> list = productRoutingRepository.findActiveRoutingsByProductId(productId);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active routings for product");
        }
        return list.get(0).getOrganizationId();
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForRoutingsByWorkCenter(String workCenterCode) {
        List<ProductRouting> list = productRoutingRepository.findByWorkCenterCode(workCenterCode);
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No routings for work center");
        }
        return list.get(0).getOrganizationId();
    }

    @Transactional
    public ProductRouting createRouting(ProductRouting routing) {
        // Auto-generate routing number if not provided
        if (routing.getRoutingNumber() == null || routing.getRoutingNumber().isEmpty()) {
            routing.setRoutingNumber(generateRoutingNumber(routing.getOrganizationId()));
        }
        
        // Set defaults
        if (routing.getIsActive() == null) {
            routing.setIsActive(true);
        }
        if (routing.getSetupTime() == null) {
            routing.setSetupTime(BigDecimal.ZERO);
        }
        if (routing.getTeardownTime() == null) {
            routing.setTeardownTime(BigDecimal.ZERO);
        }
        if (routing.getQualityCheckRequired() == null) {
            routing.setQualityCheckRequired(false);
        }
        
        ProductRouting saved = productRoutingRepository.save(routing);
        log.info("Created routing: {} for product: {}", saved.getRoutingNumber(), saved.getProductId());
        return saved;
    }

    @Transactional
    public ProductRouting updateRouting(UUID routingId, ProductRouting routing) {
        ProductRouting existing = productRoutingRepository.findById(routingId)
                .orElseThrow(() -> new RuntimeException("Routing not found: " + routingId));
        
        existing.setRoutingName(routing.getRoutingName());
        existing.setOperationSequence(routing.getOperationSequence());
        existing.setOperationCode(routing.getOperationCode());
        existing.setOperationName(routing.getOperationName());
        existing.setWorkCenterCode(routing.getWorkCenterCode());
        existing.setWorkCenterName(routing.getWorkCenterName());
        existing.setSetupTime(routing.getSetupTime());
        existing.setRunTimePerUnit(routing.getRunTimePerUnit());
        existing.setTeardownTime(routing.getTeardownTime());
        existing.setCostPerHour(routing.getCostPerHour());
        existing.setSetupCost(routing.getSetupCost());
        existing.setDescription(routing.getDescription());
        existing.setInstructions(routing.getInstructions());
        existing.setQualityCheckRequired(routing.getQualityCheckRequired());
        existing.setIsActive(routing.getIsActive());
        existing.setEffectiveFrom(routing.getEffectiveFrom());
        existing.setEffectiveTo(routing.getEffectiveTo());
        existing.setUpdatedBy(routing.getUpdatedBy());
        
        ProductRouting updated = productRoutingRepository.save(existing);
        log.info("Updated routing: {}", updated.getRoutingNumber());
        return updated;
    }

    @Transactional
    public void deleteRouting(UUID routingId) {
        productRoutingRepository.deleteById(routingId);
        log.info("Deleted routing: {}", routingId);
    }

    // Calculate total production time for a product
    @Transactional(readOnly = true)
    public Map<String, Object> calculateProductionTime(UUID productId, BigDecimal quantity) {
        List<ProductRouting> routings = productRoutingRepository.findActiveRoutingsByProductId(productId);
        
        BigDecimal totalSetupTime = BigDecimal.ZERO;
        BigDecimal totalRunTime = BigDecimal.ZERO;
        BigDecimal totalTeardownTime = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        
        for (ProductRouting routing : routings) {
            totalSetupTime = totalSetupTime.add(routing.getSetupTime() != null ? routing.getSetupTime() : BigDecimal.ZERO);
            totalRunTime = totalRunTime.add((routing.getRunTimePerUnit() != null ? routing.getRunTimePerUnit() : BigDecimal.ZERO).multiply(quantity));
            totalTeardownTime = totalTeardownTime.add(routing.getTeardownTime() != null ? routing.getTeardownTime() : BigDecimal.ZERO);
            
            if (routing.getCostPerHour() != null) {
                BigDecimal opTime = totalSetupTime.add(totalRunTime).add(totalTeardownTime);
                BigDecimal opCost = opTime.divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP)
                        .multiply(routing.getCostPerHour());
                totalCost = totalCost.add(opCost);
            }
            if (routing.getSetupCost() != null) {
                totalCost = totalCost.add(routing.getSetupCost());
            }
        }
        
        BigDecimal totalTime = totalSetupTime.add(totalRunTime).add(totalTeardownTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("quantity", quantity);
        result.put("operationCount", routings.size());
        result.put("setupTime", totalSetupTime);
        result.put("runTime", totalRunTime);
        result.put("teardownTime", totalTeardownTime);
        result.put("totalTime", totalTime);
        result.put("totalTimeHours", totalTime.divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP));
        result.put("totalCost", totalCost);
        result.put("operations", routings);
        
        return result;
    }

    // Utility Methods
    private String generateRoutingNumber(UUID organizationId) {
        List<ProductRouting> routings = productRoutingRepository.findByOrganizationId(organizationId);
        return String.format("RTG-%06d", routings.size() + 1);
    }
}

