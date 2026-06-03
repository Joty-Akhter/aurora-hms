package com.easyops.inventory.service;

import com.easyops.inventory.entity.ProductCategory;
import com.easyops.inventory.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryService {
    
    private final ProductCategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#organizationId")
    public List<ProductCategory> getAllCategories(UUID organizationId) {
        log.debug("Fetching all categories for organization: {}", organizationId);
        return categoryRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#organizationId + '_active_' + #activeOnly")
    public List<ProductCategory> getActiveCategories(UUID organizationId, Boolean activeOnly) {
        log.debug("Fetching active categories for organization: {}, activeOnly: {}", organizationId, activeOnly);
        if (Boolean.TRUE.equals(activeOnly)) {
            return categoryRepository.findByOrganizationIdAndIsActive(organizationId, true);
        }
        return categoryRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public ProductCategory getCategoryById(UUID id) {
        log.debug("Fetching category by ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }
    
    @Transactional(readOnly = true)
    public ProductCategory getCategoryByCode(UUID organizationId, String code) {
        log.debug("Fetching category by code: {} for organization: {}", code, organizationId);
        return categoryRepository.findByOrganizationIdAndCode(organizationId, code)
                .orElseThrow(() -> new RuntimeException("Category not found with code: " + code));
    }
    
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategory createCategory(ProductCategory category) {
        log.info("Creating category: {}", category.getCode());
        
        // Check if code already exists
        if (categoryRepository.findByOrganizationIdAndCode(category.getOrganizationId(), category.getCode()).isPresent()) {
            throw new RuntimeException("Category with code already exists: " + category.getCode());
        }
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public ProductCategory updateCategory(UUID id, ProductCategory category) {
        log.info("Updating category: {}", id);
        
        ProductCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        
        // Update fields
        existing.setCode(category.getCode());
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setParentCategoryId(category.getParentCategoryId());
        existing.setImageUrl(category.getImageUrl());
        existing.setIsActive(category.getIsActive());
        existing.setDisplayOrder(category.getDisplayOrder());
        existing.setUpdatedBy(category.getUpdatedBy());
        
        return categoryRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(UUID id) {
        log.info("Deleting category: {}", id);
        categoryRepository.deleteById(id);
    }
}

