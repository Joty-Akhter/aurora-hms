package com.easyops.inventory.repository;

import com.easyops.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    List<Product> findByOrganizationId(UUID organizationId);
    
    List<Product> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Product> findByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId);
    
    Optional<Product> findByOrganizationIdAndSku(UUID organizationId, String sku);
    
    Optional<Product> findByOrganizationIdAndBarcode(UUID organizationId, String barcode);
    
    List<Product> findByOrganizationIdAndProductType(UUID organizationId, String productType);
    
    @Query("SELECT p FROM Product p WHERE p.organizationId = :orgId AND p.trackInventory = true")
    List<Product> findStockableProducts(@Param("orgId") UUID organizationId);
    
    @Query("SELECT p FROM Product p WHERE p.organizationId = :orgId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByName(@Param("orgId") UUID organizationId, @Param("keyword") String keyword);
    
    boolean existsByOrganizationIdAndSku(UUID organizationId, String sku);
}

