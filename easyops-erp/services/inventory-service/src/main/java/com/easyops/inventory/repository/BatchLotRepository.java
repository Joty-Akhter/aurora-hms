package com.easyops.inventory.repository;

import com.easyops.inventory.entity.BatchLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchLotRepository extends JpaRepository<BatchLot, UUID> {
    
    List<BatchLot> findByOrganizationId(UUID organizationId);
    
    List<BatchLot> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    
    List<BatchLot> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    Optional<BatchLot> findByOrganizationIdAndProductIdAndBatchNumber(UUID organizationId, UUID productId, String batchNumber);
    
    @Query("SELECT b FROM BatchLot b WHERE b.organizationId = :orgId AND b.expiryDate IS NOT NULL AND b.expiryDate <= :date AND b.currentQuantity > 0 ORDER BY b.expiryDate")
    List<BatchLot> findExpiringBatches(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM BatchLot b WHERE b.organizationId = :orgId AND b.expiryDate < CURRENT_DATE AND b.currentQuantity > 0")
    List<BatchLot> findExpiredBatches(@Param("orgId") UUID organizationId);
    
    @Query("SELECT b FROM BatchLot b WHERE b.organizationId = :orgId AND b.productId = :productId AND b.currentQuantity > 0 AND b.status = 'ACTIVE' ORDER BY b.expiryDate NULLS LAST, b.manufactureDate")
    List<BatchLot> findAvailableBatchesByProduct(@Param("orgId") UUID organizationId, @Param("productId") UUID productId);
    
    boolean existsByOrganizationIdAndProductIdAndBatchNumber(UUID organizationId, UUID productId, String batchNumber);
}

