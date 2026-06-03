package com.easyops.inventory.service;

import com.easyops.inventory.entity.Product;
import com.easyops.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#organizationId")
    public List<Product> getAllProducts(UUID organizationId) {
        log.debug("Fetching all products for organization: {}", organizationId);
        return productRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#organizationId + '_active_' + #activeOnly")
    public List<Product> getActiveProducts(UUID organizationId, Boolean activeOnly) {
        log.debug("Fetching active products for organization: {}, activeOnly: {}", organizationId, activeOnly);
        if (Boolean.TRUE.equals(activeOnly)) {
            return productRepository.findByOrganizationIdAndIsActive(organizationId, true);
        }
        return productRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(UUID organizationId, UUID categoryId) {
        log.debug("Fetching products by category: {} for organization: {}", categoryId, organizationId);
        return productRepository.findByOrganizationIdAndCategoryId(organizationId, categoryId);
    }
    
    @Transactional(readOnly = true)
    public List<Product> searchProducts(UUID organizationId, String keyword) {
        log.debug("Searching products with keyword: {} for organization: {}", keyword, organizationId);
        return productRepository.searchByName(organizationId, keyword);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public Product getProductById(UUID id) {
        log.debug("Fetching product by ID: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));
    }
    
    @Transactional(readOnly = true)
    public Product getProductBySku(UUID organizationId, String sku) {
        log.debug("Fetching product by SKU: {} for organization: {}", sku, organizationId);
        return productRepository.findByOrganizationIdAndSku(organizationId, sku)
                .orElseThrow(() -> new RuntimeException("Product not found with SKU: " + sku));
    }
    
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product createProduct(Product product) {
        log.info("Creating new product: {}", product.getSku());
        
        // Check if SKU already exists
        if (productRepository.existsByOrganizationIdAndSku(product.getOrganizationId(), product.getSku())) {
            throw new RuntimeException("Product with SKU already exists: " + product.getSku());
        }
        
        return productRepository.save(product);
    }
    
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public Product updateProduct(UUID id, Product productDetails) {
        log.info("Updating product: {}", id);
        
        Product product = getProductById(id);
        
        // Check if SKU is being changed and if new SKU already exists for another product
        if (productDetails.getSku() != null && !productDetails.getSku().equals(product.getSku())) {
            Optional<Product> existingProduct = productRepository.findByOrganizationIdAndSku(product.getOrganizationId(), productDetails.getSku());
            if (existingProduct.isPresent() && !existingProduct.get().getId().equals(id)) {
                throw new RuntimeException("Product with SKU already exists: " + productDetails.getSku());
            }
        }
        
        // Update fields
        if (productDetails.getSku() != null) {
            product.setSku(productDetails.getSku());
        }
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setShortDescription(productDetails.getShortDescription());
        product.setCategoryId(productDetails.getCategoryId());
        product.setBrand(productDetails.getBrand());
        product.setManufacturer(productDetails.getManufacturer());
        product.setCostPrice(productDetails.getCostPrice());
        product.setSellingPrice(productDetails.getSellingPrice());
        product.setWholesalePrice(productDetails.getWholesalePrice());
        product.setRetailPrice(productDetails.getRetailPrice());
        // Ensure packSize has default value if not provided
        product.setPackSize(productDetails.getPackSize() != null ? productDetails.getPackSize() : java.math.BigDecimal.ONE);
        product.setUom(productDetails.getUom());
        product.setProductType(productDetails.getProductType());
        product.setItemType(productDetails.getItemType());
        product.setReorderLevel(productDetails.getReorderLevel());
        product.setMinStockLevel(productDetails.getMinStockLevel());
        product.setMaxStockLevel(productDetails.getMaxStockLevel());
        product.setIsActive(productDetails.getIsActive());
        product.setStatus(productDetails.getStatus());
        product.setUpdatedBy(productDetails.getUpdatedBy());
        
        return productRepository.save(product);
    }
    
    @Transactional
    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void deleteProduct(UUID id) {
        log.info("Deleting product: {}", id);
        productRepository.deleteById(id);
    }
}

