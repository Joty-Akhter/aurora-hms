package com.easyops.manufacturing.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integration service for Manufacturing <-> Inventory communication
 * Handles material availability, stock reduction, and finished goods receipt
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${integration.inventory-service.url:http://INVENTORY-SERVICE}")
    private String inventoryServiceUrl;

    /**
     * Check material availability in inventory
     */
    public boolean checkMaterialAvailability(UUID componentId, BigDecimal requiredQuantity, UUID warehouseId) {
        try {
            String url = String.format("%s/api/inventory/stock/availability?productId=%s&warehouseId=%s&requiredQuantity=%s",
                    inventoryServiceUrl, componentId, warehouseId, requiredQuantity);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> result = response.getBody();
            
            if (result != null && result.containsKey("available")) {
                boolean available = (Boolean) result.get("available");
                log.info("Material availability check for component {}: {}", componentId, available);
                return available;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to check material availability: {}", e.getMessage());
            return false; // Assume not available on error
        }
    }

    /**
     * Reserve material in inventory
     */
    public Map<String, Object> reserveMaterial(UUID componentId, BigDecimal quantity, UUID warehouseId, 
                                               String referenceType, String referenceNumber) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/stock/reserve";
            
            Map<String, Object> request = new HashMap<>();
            request.put("productId", componentId);
            request.put("quantity", quantity);
            request.put("warehouseId", warehouseId);
            request.put("referenceType", referenceType);
            request.put("referenceNumber", referenceNumber);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            log.info("Reserved material {}: quantity {}", componentId, quantity);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to reserve material: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Issue material from inventory (reduce stock)
     */
    public Map<String, Object> issueMaterial(UUID componentId, BigDecimal quantity, UUID warehouseId,
                                            String referenceType, String referenceNumber, UUID issuedBy) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/stock/issue";
            
            Map<String, Object> request = new HashMap<>();
            request.put("productId", componentId);
            request.put("quantity", quantity);
            request.put("warehouseId", warehouseId);
            request.put("referenceType", referenceType);
            request.put("referenceNumber", referenceNumber);
            request.put("issuedBy", issuedBy);
            request.put("movementType", "ISSUE_TO_PRODUCTION");
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            log.info("Issued material {} from inventory: quantity {}", componentId, quantity);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to issue material: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Receive finished goods to inventory
     */
    public Map<String, Object> receiveFinishedGoods(UUID productId, BigDecimal quantity, UUID warehouseId,
                                                    String workOrderNumber, BigDecimal unitCost, UUID receivedBy) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/stock/receive";
            
            Map<String, Object> request = new HashMap<>();
            request.put("productId", productId);
            request.put("quantity", quantity);
            request.put("warehouseId", warehouseId);
            request.put("referenceType", "WORK_ORDER");
            request.put("referenceNumber", workOrderNumber);
            request.put("unitCost", unitCost);
            request.put("receivedBy", receivedBy);
            request.put("movementType", "RECEIPT_FROM_PRODUCTION");
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            log.info("Received finished goods {} to inventory: quantity {}", productId, quantity);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to receive finished goods: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Return material to inventory (unused or excess)
     */
    public Map<String, Object> returnMaterial(UUID componentId, BigDecimal quantity, UUID warehouseId,
                                             String workOrderNumber, UUID returnedBy) {
        try {
            String url = inventoryServiceUrl + "/api/inventory/stock/return";
            
            Map<String, Object> request = new HashMap<>();
            request.put("productId", componentId);
            request.put("quantity", quantity);
            request.put("warehouseId", warehouseId);
            request.put("referenceType", "WORK_ORDER");
            request.put("referenceNumber", workOrderNumber);
            request.put("returnedBy", returnedBy);
            request.put("movementType", "RETURN_FROM_PRODUCTION");
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            log.info("Returned material {} to inventory: quantity {}", componentId, quantity);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to return material: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get stock level for a product
     */
    public Map<String, Object> getStockLevel(UUID productId, UUID warehouseId) {
        try {
            String url = String.format("%s/api/inventory/stock/level?productId=%s&warehouseId=%s",
                    inventoryServiceUrl, productId, warehouseId);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get stock level: {}", e.getMessage());
            return Map.of("onHandQuantity", 0, "availableQuantity", 0);
        }
    }
}

