package com.easyops.inventory.repository;

import com.easyops.inventory.entity.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, UUID> {
    
    List<StockCount> findByOrganizationIdOrderByCountDateDesc(UUID organizationId);
    
    List<StockCount> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<StockCount> findByOrganizationIdAndWarehouseId(UUID organizationId, UUID warehouseId);
    
    Optional<StockCount> findByCountNumber(String countNumber);
    
    @Query("SELECT c FROM StockCount c LEFT JOIN FETCH c.lines WHERE c.id = :id")
    Optional<StockCount> findByIdWithLines(@Param("id") UUID id);
    
    boolean existsByCountNumber(String countNumber);
}

