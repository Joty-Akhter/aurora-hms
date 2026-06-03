package com.easyops.inventory.repository;

import com.easyops.inventory.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    
    List<ProductCategory> findByOrganizationId(UUID organizationId);
    
    List<ProductCategory> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<ProductCategory> findByOrganizationIdAndCode(UUID organizationId, String code);
    
    List<ProductCategory> findByOrganizationIdAndParentCategoryId(UUID organizationId, UUID parentCategoryId);
    
    List<ProductCategory> findByOrganizationIdAndParentCategoryIdIsNull(UUID organizationId);
}

