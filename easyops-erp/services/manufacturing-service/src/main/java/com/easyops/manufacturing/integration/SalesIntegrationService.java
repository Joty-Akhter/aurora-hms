package com.easyops.manufacturing.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Integration service for Manufacturing <-> Sales communication
 * Handles make-to-order work orders and sales order integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${integration.sales-service.url:http://SALES-SERVICE}")
    private String salesServiceUrl;

    /**
     * Get sales order details
     */
    public Map<String, Object> getSalesOrderDetails(UUID salesOrderId) {
        try {
            String url = String.format("%s/api/sales/orders/%s", salesServiceUrl, salesOrderId);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            log.info("Retrieved sales order details for: {}", salesOrderId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get sales order details: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Update sales order status (e.g., when production starts/completes)
     */
    public void updateSalesOrderStatus(UUID salesOrderId, String status) {
        try {
            String url = String.format("%s/api/sales/orders/%s/status?status=%s", 
                    salesServiceUrl, salesOrderId, status);
            
            restTemplate.postForEntity(url, null, Void.class);
            
            log.info("Updated sales order {} status to: {}", salesOrderId, status);
        } catch (Exception e) {
            log.error("Failed to update sales order status: {}", e.getMessage());
        }
    }

    /**
     * Update sales order line production status
     */
    public void updateSalesOrderLineProduction(UUID salesOrderLineId, String productionStatus, String workOrderNumber) {
        try {
            String url = String.format("%s/api/sales/orders/lines/%s/production", salesServiceUrl, salesOrderLineId);
            
            Map<String, Object> request = Map.of(
                "productionStatus", productionStatus,
                "workOrderNumber", workOrderNumber
            );
            
            restTemplate.postForEntity(url, request, Void.class);
            
            log.info("Updated sales order line {} production status", salesOrderLineId);
        } catch (Exception e) {
            log.error("Failed to update sales order line production: {}", e.getMessage());
        }
    }

    /**
     * Notify sales module of production completion
     */
    public void notifyProductionComplete(String workOrderNumber, UUID productId, double quantityProduced) {
        try {
            String url = salesServiceUrl + "/api/sales/production/complete";
            
            Map<String, Object> request = Map.of(
                "workOrderNumber", workOrderNumber,
                "productId", productId,
                "quantityProduced", quantityProduced
            );
            
            restTemplate.postForEntity(url, request, Void.class);
            
            log.info("Notified sales of production completion for WO: {}", workOrderNumber);
        } catch (Exception e) {
            log.error("Failed to notify sales of production completion: {}", e.getMessage());
        }
    }
}

