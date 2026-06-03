package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.ProductRouting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRoutingRepository extends JpaRepository<ProductRouting, UUID> {

    List<ProductRouting> findByOrganizationId(UUID organizationId);

    @Query("SELECT pr FROM ProductRouting pr WHERE pr.productId = :productId AND pr.isActive = true " +
           "ORDER BY pr.operationSequence")
    List<ProductRouting> findActiveRoutingsByProductId(@Param("productId") UUID productId);

    @Query("SELECT pr FROM ProductRouting pr WHERE pr.productId = :productId ORDER BY pr.operationSequence")
    List<ProductRouting> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT pr FROM ProductRouting pr WHERE pr.bomId = :bomId AND pr.isActive = true " +
           "ORDER BY pr.operationSequence")
    List<ProductRouting> findActiveRoutingsByBomId(@Param("bomId") UUID bomId);

    Optional<ProductRouting> findByOrganizationIdAndRoutingNumber(UUID organizationId, String routingNumber);

    @Query("SELECT pr FROM ProductRouting pr WHERE pr.productId = :productId " +
           "AND pr.operationSequence = :sequence")
    Optional<ProductRouting> findByProductIdAndSequence(@Param("productId") UUID productId, 
                                                        @Param("sequence") Integer sequence);

    @Query("SELECT pr FROM ProductRouting pr WHERE pr.workCenterCode = :workCenterCode AND pr.isActive = true " +
           "ORDER BY pr.operationSequence")
    List<ProductRouting> findByWorkCenterCode(@Param("workCenterCode") String workCenterCode);

    @Query("SELECT COUNT(pr) FROM ProductRouting pr WHERE pr.productId = :productId AND pr.isActive = true")
    long countActiveRoutingsByProductId(@Param("productId") UUID productId);

    @Query("SELECT SUM(pr.setupTime + pr.runTimePerUnit) FROM ProductRouting pr " +
           "WHERE pr.productId = :productId AND pr.isActive = true")
    Double calculateTotalProductionTime(@Param("productId") UUID productId);
}

