package com.easyops.manufacturing.controller;

import com.easyops.manufacturing.entity.BomHeader;
import com.easyops.manufacturing.entity.ProductRouting;
import com.easyops.manufacturing.security.ManufacturingRbacService;
import com.easyops.manufacturing.security.RbacRequestHeaders;
import com.easyops.manufacturing.service.BomService;
import com.easyops.manufacturing.service.ProductRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/routings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductRoutingController {

    private final ProductRoutingService routingService;
    private final BomService bomService;
    private final ManufacturingRbacService manufacturingRbac;

    @GetMapping
    public ResponseEntity<List<ProductRouting>> getAllRoutings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        List<ProductRouting> routings = routingService.getAllRoutingsByOrganization(organizationId);
        return ResponseEntity.ok(routings);
    }

    @GetMapping("/{routingId}")
    public ResponseEntity<ProductRouting> getRoutingById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID routingId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return routingService.getRoutingById(routingId)
                .map(r -> {
                    manufacturingRbac.requireManufacturingView(actor, r.getOrganizationId());
                    return ResponseEntity.ok(r);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{routingNumber}")
    public ResponseEntity<ProductRouting> getRoutingByNumber(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String routingNumber,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingView(actor, organizationId);
        return routingService.getRoutingByNumber(organizationId, routingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductRouting>> getRoutingsByProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID productId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = routingService.getOrganizationIdForProductRoutings(productId);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<ProductRouting> routings = routingService.getRoutingsByProduct(productId);
        return ResponseEntity.ok(routings);
    }

    @GetMapping("/product/{productId}/active")
    public ResponseEntity<List<ProductRouting>> getActiveRoutingsByProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID productId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = routingService.getOrganizationIdForActiveProductRoutings(productId);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<ProductRouting> routings = routingService.getActiveRoutingsByProduct(productId);
        return ResponseEntity.ok(routings);
    }

    @GetMapping("/bom/{bomId}/active")
    public ResponseEntity<List<ProductRouting>> getActiveRoutingsByBom(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID bomId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = bomService.getBomById(bomId)
                .map(BomHeader::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BOM not found"));
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<ProductRouting> routings = routingService.getActiveRoutingsByBom(bomId);
        return ResponseEntity.ok(routings);
    }

    @GetMapping("/work-center/{workCenterCode}")
    public ResponseEntity<List<ProductRouting>> getRoutingsByWorkCenter(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable String workCenterCode) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = routingService.getOrganizationIdForRoutingsByWorkCenter(workCenterCode);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        List<ProductRouting> routings = routingService.getRoutingsByWorkCenter(workCenterCode);
        return ResponseEntity.ok(routings);
    }

    @PostMapping
    public ResponseEntity<ProductRouting> createRouting(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody ProductRouting routing) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        manufacturingRbac.requireManufacturingManage(actor, routing.getOrganizationId());
        ProductRouting created = routingService.createRouting(routing);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{routingId}")
    public ResponseEntity<ProductRouting> updateRouting(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID routingId,
            @RequestBody ProductRouting routing) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = routingService.getRoutingById(routingId)
                .map(ProductRouting::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        ProductRouting updated = routingService.updateRouting(routingId, routing);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{routingId}")
    public ResponseEntity<Void> deleteRouting(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID routingId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = routingService.getRoutingById(routingId)
                .map(ProductRouting::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing not found"));
        manufacturingRbac.requireManufacturingManage(actor, orgId);
        routingService.deleteRouting(routingId);
        return ResponseEntity.noContent().build();
    }

    // Production Time Calculation
    @GetMapping("/product/{productId}/production-time")
    public ResponseEntity<Map<String, Object>> calculateProductionTime(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "1") BigDecimal quantity) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID orgId = routingService.getOrganizationIdForProductRoutings(productId);
        manufacturingRbac.requireManufacturingView(actor, orgId);
        Map<String, Object> result = routingService.calculateProductionTime(productId, quantity);
        return ResponseEntity.ok(result);
    }
}
