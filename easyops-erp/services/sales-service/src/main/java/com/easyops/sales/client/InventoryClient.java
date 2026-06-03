package com.easyops.sales.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "inventory-service", configuration = InventoryFeignConfig.class)
public interface InventoryClient {
    
    @GetMapping("/api/inventory/stock/available")
    Map<String, BigDecimal> getAvailableQuantity(
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("productId") UUID productId,
            @RequestParam("warehouseId") UUID warehouseId);
    
    @PostMapping("/api/inventory/stock/allocate")
    Map<String, Object> allocateStock(@RequestBody Map<String, Object> allocationRequest);
    
    @PostMapping("/api/inventory/stock/deallocate")
    Map<String, Object> deallocateStock(@RequestBody Map<String, Object> allocationRequest);
    
    @PostMapping("/api/inventory/stock/issue")
    Map<String, Object> issueStock(@RequestBody Map<String, Object> issueRequest);
    
    @GetMapping("/api/inventory/products")
    java.util.List<Map<String, Object>> getProducts(@RequestParam("organizationId") UUID organizationId);
    
    @GetMapping("/api/inventory/products/{id}")
    Map<String, Object> getProduct(@PathVariable("id") UUID id);
}

