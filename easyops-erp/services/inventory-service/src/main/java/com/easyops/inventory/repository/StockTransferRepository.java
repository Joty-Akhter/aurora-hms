package com.easyops.inventory.repository;

import com.easyops.inventory.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {
    
    List<StockTransfer> findByOrganizationIdOrderByTransferDateDesc(UUID organizationId);
    
    List<StockTransfer> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<StockTransfer> findByOrganizationIdAndFromWarehouseId(UUID organizationId, UUID fromWarehouseId);
    
    List<StockTransfer> findByOrganizationIdAndToWarehouseId(UUID organizationId, UUID toWarehouseId);
    
    Optional<StockTransfer> findByTransferNumber(String transferNumber);
    
    @Query("SELECT t FROM StockTransfer t LEFT JOIN FETCH t.lines WHERE t.id = :id")
    Optional<StockTransfer> findByIdWithLines(@Param("id") UUID id);
    
    @Query("SELECT t FROM StockTransfer t WHERE t.organizationId = :orgId AND t.status IN ('SUBMITTED', 'APPROVED', 'IN_TRANSIT')")
    List<StockTransfer> findPendingTransfers(@Param("orgId") UUID organizationId);
    
    boolean existsByTransferNumber(String transferNumber);
}

