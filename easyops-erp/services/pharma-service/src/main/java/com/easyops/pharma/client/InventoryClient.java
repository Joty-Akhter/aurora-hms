package com.easyops.pharma.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Feign client for Inventory Service
 * Used to create warehouses when creating areas and update stock levels
 */
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    
    /**
     * Create a new warehouse
     * @param warehouse warehouse data
     * @return created warehouse
     */
    @PostMapping("/api/inventory/warehouses")
    Map<String, Object> createWarehouse(@RequestBody Map<String, Object> warehouse);
    
    /**
     * Update a warehouse (e.g. soft-delete: isActive=false, status=INACTIVE)
     * @param id warehouse ID
     * @param warehouse partial warehouse data to update
     * @return updated warehouse
     */
    @PutMapping("/api/inventory/warehouses/{id}")
    Map<String, Object> updateWarehouse(@PathVariable("id") UUID id, @RequestBody Map<String, Object> warehouse);
    
    /**
     * Get warehouse by ID
     * @param id warehouse ID
     * @return warehouse data
     */
    @GetMapping("/api/inventory/warehouses/{id}")
    Map<String, Object> getWarehouse(@PathVariable("id") UUID id);
    
    /**
     * Get warehouses by organization
     * @param organizationId organization ID
     * @return list of warehouses
     */
    @GetMapping("/api/inventory/warehouses")
    java.util.List<Map<String, Object>> getWarehouses(@RequestParam("organizationId") UUID organizationId);
    
    /**
     * Receive stock into warehouse (from factory to central depot)
     * @param request stock receipt request
     * @return updated stock
     */
    @PostMapping("/api/inventory/stock/receive")
    Map<String, Object> receiveStock(@RequestBody Map<String, Object> request);
}

