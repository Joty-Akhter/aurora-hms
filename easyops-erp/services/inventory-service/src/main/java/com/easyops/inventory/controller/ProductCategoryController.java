package com.easyops.inventory.controller;

import com.easyops.inventory.entity.ProductCategory;
import com.easyops.inventory.security.InventoryRbacService;
import com.easyops.inventory.security.RbacRequestHeaders;
import com.easyops.inventory.service.ProductCategoryService;
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
@RequestMapping("/api/inventory/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Category Management", description = "Product category management APIs")
@CrossOrigin(origins = "*")
public class ProductCategoryController {

    private final ProductCategoryService categoryService;
    private final InventoryRbacService inventoryRbac;

    @GetMapping
    @Operation(summary = "Get all product categories")
    public ResponseEntity<List<ProductCategory>> getAllCategories(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam("organizationId") UUID organizationId,
            @RequestParam(name = "activeOnly", required = false) Boolean activeOnly) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryView(actor, organizationId);
        log.info("GET /api/inventory/categories - organizationId: {}, activeOnly: {}", organizationId, activeOnly);
        List<ProductCategory> categories = categoryService.getActiveCategories(organizationId, activeOnly);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ProductCategory> getCategoryById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductCategory category = categoryService.getCategoryById(id);
        inventoryRbac.requireInventoryView(actor, category.getOrganizationId());
        log.info("GET /api/inventory/categories/{}", id);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @Operation(summary = "Create new category")
    public ResponseEntity<ProductCategory> createCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody ProductCategory category) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        inventoryRbac.requireInventoryManage(actor, category.getOrganizationId());
        log.info("POST /api/inventory/categories - code: {}", category.getCode());
        ProductCategory createdCategory = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category")
    public ResponseEntity<ProductCategory> updateCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id,
            @Valid @RequestBody ProductCategory category) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductCategory existing = categoryService.getCategoryById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("PUT /api/inventory/categories/{}", id);
        ProductCategory updatedCategory = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category")
    public ResponseEntity<Void> deleteCategory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable("id") UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        ProductCategory existing = categoryService.getCategoryById(id);
        inventoryRbac.requireInventoryManage(actor, existing.getOrganizationId());
        log.info("DELETE /api/inventory/categories/{}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
