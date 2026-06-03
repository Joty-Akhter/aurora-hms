package com.easyops.inventory.repository;

import com.easyops.inventory.entity.ReorderRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReorderRuleRepository extends JpaRepository<ReorderRule, UUID> {
    
    List<ReorderRule> findByOrganizationId(UUID organizationId);
    
    List<ReorderRule> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<ReorderRule> findByOrganizationIdAndProductIdAndWarehouseId(
        UUID organizationId, UUID productId, UUID warehouseId);
    
    List<ReorderRule> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    
    List<ReorderRule> findByOrganizationIdAndWarehouseId(UUID organizationId, UUID warehouseId);
    
    @Query("SELECT r FROM ReorderRule r WHERE r.organizationId = :orgId AND r.isActive = true")
    List<ReorderRule> findActiveRules(@Param("orgId") UUID organizationId);
    
    boolean existsByOrganizationIdAndProductIdAndWarehouseId(
        UUID organizationId, UUID productId, UUID warehouseId);
}

