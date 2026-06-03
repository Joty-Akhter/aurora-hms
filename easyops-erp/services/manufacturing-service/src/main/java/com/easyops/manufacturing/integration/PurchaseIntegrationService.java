package com.easyops.manufacturing.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Integration service for Manufacturing <-> Purchase communication
 * Handles MRP-driven purchase requisitions and material procurement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${integration.purchase-service.url:http://PURCHASE-SERVICE}")
    private String purchaseServiceUrl;

    /**
     * Create purchase requisition for material shortage (MRP)
     */
    public Map<String, Object> createPurchaseRequisition(UUID componentId, String componentCode,
                                                        BigDecimal shortageQuantity, String uom,
                                                        LocalDateTime requiredDate, String workOrderNumber,
                                                        UUID organizationId, UUID requestedBy) {
        try {
            String url = purchaseServiceUrl + "/api/purchase/requisitions/mrp";
            
            Map<String, Object> request = new HashMap<>();
            request.put("organizationId", organizationId);
            request.put("productId", componentId);
            request.put("productCode", componentCode);
            request.put("quantity", shortageQuantity);
            request.put("uom", uom);
            request.put("requiredDate", requiredDate);
            request.put("sourceType", "MRP");
            request.put("sourceReference", workOrderNumber);
            request.put("requestedBy", requestedBy);
            request.put("priority", "HIGH");
            request.put("description", "MRP-driven requisition for Work Order " + workOrderNumber);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            log.info("Created purchase requisition for component: {} quantity: {}", componentCode, shortageQuantity);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to create purchase requisition: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Check if purchase order exists for a material
     */
    public Map<String, Object> checkPurchaseOrderStatus(UUID componentId, LocalDateTime requiredDate) {
        try {
            String url = String.format("%s/api/purchase/orders/material-status?productId=%s&requiredDate=%s",
                    purchaseServiceUrl, componentId, requiredDate);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to check purchase order status: {}", e.getMessage());
            return Map.of("hasPurchaseOrder", false);
        }
    }

    /**
     * Get expected delivery date for a material
     */
    public LocalDateTime getExpectedDeliveryDate(UUID componentId) {
        try {
            String url = String.format("%s/api/purchase/orders/expected-delivery?productId=%s",
                    purchaseServiceUrl, componentId);
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> result = response.getBody();
            
            if (result != null && result.containsKey("expectedDate")) {
                return LocalDateTime.parse((String) result.get("expectedDate"));
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get expected delivery date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Notify purchase module of material consumption (for cost tracking)
     */
    public void notifyMaterialConsumption(UUID componentId, BigDecimal quantity, BigDecimal cost,
                                         String workOrderNumber, UUID organizationId) {
        try {
            String url = purchaseServiceUrl + "/api/purchase/material-consumption";
            
            Map<String, Object> request = new HashMap<>();
            request.put("organizationId", organizationId);
            request.put("productId", componentId);
            request.put("quantity", quantity);
            request.put("cost", cost);
            request.put("referenceType", "WORK_ORDER");
            request.put("referenceNumber", workOrderNumber);
            request.put("consumptionDate", LocalDateTime.now());
            
            restTemplate.postForEntity(url, request, Void.class);
            
            log.info("Notified purchase of material consumption for component: {}", componentId);
        } catch (Exception e) {
            log.error("Failed to notify material consumption: {}", e.getMessage());
        }
    }
}

