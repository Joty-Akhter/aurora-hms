package com.easyops.inventory.controller;

import com.easyops.inventory.entity.Product;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "Product catalog management APIs")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/products - organizationId: {}, activeOnly: {}", organizationId, activeOnly);
        List<Product> products = productService.getActiveProducts(organizationId, activeOnly);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<Product> getProductById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Product product = productService.getProductById(id);
        inventoryRbac.requireInventoryView(actor, product.getOrganizationId());
        log.info("GET /api/inventory/products/{}", id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ResponseEntity<Product> getProductBySku(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @PathVariable("sku") String sku) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/products/sku/{} - org: {}", sku, organizationId);
        Product product = productService.getProductBySku(organizationId, sku);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<Product>> getProductsByCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @PathVariable("categoryId") UUID categoryId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/products/category/{} - org: {}", categoryId, organizationId);
        List<Product> products = productService.getProductsByCategory(organizationId, categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam("keyword") String keyword) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/products/search - keyword: {}, org: {}", keyword, organizationId);
        List<Product> products = productService.searchProducts(organizationId, keyword);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @Operation(summary = "Create new product")
    public ResponseEntity<Product> createProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody Product product) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, product.getOrganizationId());
        log.info("POST /api/inventory/products - SKU: {}", product.getSku());
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    public ResponseEntity<Product> updateProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody Product product) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Product existing = productService.getProductById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("PUT /api/inventory/products/{}", id);
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<Void> deleteProduct(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        Product existing = productService.getProductById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/inventory/products/{}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
