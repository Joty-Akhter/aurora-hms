package com.easyops.sales.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "ar-service")
public interface ARClient {
    
    @PostMapping("/api/ar/invoices")
    Map<String, Object> createInvoice(@RequestBody Map<String, Object> invoiceRequest);
    
    @GetMapping("/api/ar/customers/{id}")
    Map<String, Object> getCustomer(@PathVariable("id") UUID id);
    
    @GetMapping("/api/ar/customers")
    java.util.List<Map<String, Object>> getCustomers(@RequestParam("organizationId") UUID organizationId);
    
    @PostMapping("/api/ar/customers")
    Map<String, Object> createCustomer(@RequestBody Map<String, Object> customerRequest);
}

