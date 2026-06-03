package com.easyops.inventory.repository;

import com.easyops.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {
    
    List<Stock> findByOrganizationId(UUID organizationId);
    
    List<Stock> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    
    List<Stock> findByOrganizationIdAndWarehouseId(UUID organizationId, UUID warehouseId);
    
    Optional<Stock> findByProductIdAndWarehouseIdAndLocationIdAndBatchNumberAndLotNumberAndSerialNumber(
        UUID productId, UUID warehouseId, UUID locationId, String batchNumber, String lotNumber, String serialNumber);
    
    @Query("SELECT s FROM Stock s WHERE s.organizationId = :orgId AND s.productId = :productId AND s.warehouseId = :warehouseId AND s.batchNumber IS NULL AND s.serialNumber IS NULL")
    Optional<Stock> findMainStock(@Param("orgId") UUID organizationId, @Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId);
    
    @Query("SELECT s FROM Stock s WHERE s.organizationId = :orgId AND s.quantityAvailable <= 0 AND s.status = 'AVAILABLE'")
    List<Stock> findOutOfStockItems(@Param("orgId") UUID organizationId);
    
    @Query("SELECT s FROM Stock s JOIN Product p ON s.productId = p.id WHERE s.organizationId = :orgId AND s.quantityAvailable <= p.reorderLevel")
    List<Stock> findItemsAtReorderPoint(@Param("orgId") UUID organizationId);
    
    @Query("SELECT s FROM Stock s WHERE s.organizationId = :orgId AND s.expiryDate IS NOT NULL AND s.expiryDate <= :date")
    List<Stock> findExpiringStock(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT COALESCE(SUM(s.quantityAvailable), 0) FROM Stock s WHERE s.productId = :productId AND s.warehouseId = :warehouseId AND s.status = 'AVAILABLE'")
    BigDecimal getTotalAvailableQuantity(@Param("productId") UUID productId, @Param("warehouseId") UUID warehouseId);
}

