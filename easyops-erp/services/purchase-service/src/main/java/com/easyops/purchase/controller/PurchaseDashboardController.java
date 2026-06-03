package com.easyops.purchase.controller;

import com.easyops.purchase.repository.PurchaseOrderRepository;
import com.easyops.purchase.security.PurchaseRbacService;
import com.easyops.purchase.security.RbacRequestHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/purchase/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Purchase Dashboard", description = "Dashboard statistics and metrics")
@CrossOrigin(origins = "*")
public class PurchaseDashboardController {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseRbacService purchaseRbac;

    @GetMapping("/stats")
    @Operation(summary = "Get purchase statistics")
    public ResponseEntity<Map<String, Object>> getPurchaseStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        purchaseRbac.requirePurchaseView(actor, organizationId);
        log.info("GET /api/purchase/dashboard/stats - organizationId: {}", organizationId);

        var allOrders = purchaseOrderRepository.findByOrganizationIdOrderByPoDateDesc(organizationId);

        long totalPOs = allOrders.size();
        long pendingApproval = allOrders.stream().filter(po -> "DRAFT".equals(po.getStatus()) || "SUBMITTED".equals(po.getStatus())).count();
        long approvedPOs = allOrders.stream().filter(po -> "APPROVED".equals(po.getStatus())).count();
        long receivedPOs = allOrders.stream().filter(po -> "RECEIVED".equals(po.getStatus())).count();

        BigDecimal totalValue = allOrders.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = allOrders.isEmpty() ? "USD" : allOrders.get(0).getCurrency();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPOs", totalPOs);
        stats.put("pendingApproval", pendingApproval);
        stats.put("approvedPOs", approvedPOs);
        stats.put("receivedPOs", receivedPOs);
        stats.put("totalValue", totalValue);
        stats.put("currency", currency);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/top-vendors")
    @Operation(summary = "Get top vendors by spend")
    public ResponseEntity<List<Map<String, Object>>> getTopVendors(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "5") int limit) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        purchaseRbac.requirePurchaseView(actor, organizationId);
        log.info("GET /api/purchase/dashboard/top-vendors - organizationId: {}, limit: {}", organizationId, limit);

        var allOrders = purchaseOrderRepository.findByOrganizationIdOrderByPoDateDesc(organizationId);

        Map<UUID, Map<String, Object>> vendorMap = new HashMap<>();

        for (var po : allOrders) {
            UUID vendorId = po.getVendorId();
            vendorMap.computeIfAbsent(vendorId, k -> {
                Map<String, Object> vendor = new HashMap<>();
                vendor.put("vendorId", vendorId);
                vendor.put("vendorName", po.getVendorName());
                vendor.put("totalAmount", BigDecimal.ZERO);
                vendor.put("poCount", 0);
                return vendor;
            });

            Map<String, Object> vendor = vendorMap.get(vendorId);
            BigDecimal currentTotal = (BigDecimal) vendor.get("totalAmount");
            vendor.put("totalAmount", currentTotal.add(po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO));
            vendor.put("poCount", (Integer) vendor.get("poCount") + 1);
        }

        List<Map<String, Object>> topVendors = vendorMap.values().stream()
                .sorted((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")))
                .limit(limit)
                .toList();

        return ResponseEntity.ok(topVendors);
    }
}
