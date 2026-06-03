package com.easyops.inventory.repository;

import com.easyops.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    
    List<StockMovement> findByOrganizationId(UUID organizationId);
    
    List<StockMovement> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    
    List<StockMovement> findByOrganizationIdAndWarehouseId(UUID organizationId, UUID warehouseId);
    
    List<StockMovement> findByOrganizationIdAndTransactionType(UUID organizationId, String transactionType);
    
    @Query("SELECT m FROM StockMovement m WHERE m.organizationId = :orgId AND m.transactionDate BETWEEN :startDate AND :endDate")
    List<StockMovement> findByDateRange(@Param("orgId") UUID organizationId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT m FROM StockMovement m WHERE m.sourceType = :sourceType AND m.sourceId = :sourceId")
    List<StockMovement> findBySource(@Param("sourceType") String sourceType, @Param("sourceId") UUID sourceId);
    
    boolean existsByTransactionNumber(String transactionNumber);
}

