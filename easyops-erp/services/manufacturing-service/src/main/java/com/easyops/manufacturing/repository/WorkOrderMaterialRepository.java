package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.WorkOrderMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderMaterialRepository extends JpaRepository<WorkOrderMaterial, UUID> {

    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId")
    List<WorkOrderMaterial> findByWorkOrderId(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId " +
           "AND wom.status = :status")
    List<WorkOrderMaterial> findByWorkOrderIdAndStatus(@Param("workOrderId") UUID workOrderId, 
                                                        @Param("status") String status);

    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId " +
           "AND wom.operationId = :operationId")
    List<WorkOrderMaterial> findByWorkOrderIdAndOperationId(@Param("workOrderId") UUID workOrderId, 
                                                             @Param("operationId") UUID operationId);

    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.componentId = :componentId " +
           "AND wom.status IN ('PLANNED', 'RESERVED')")
    List<WorkOrderMaterial> findPendingMaterialRequirements(@Param("componentId") UUID componentId);

    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId " +
           "AND wom.status = 'PLANNED' AND wom.quantityReserved < wom.quantityRequired")
    List<WorkOrderMaterial> findUnreservedMaterials(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT wom FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId " +
           "AND wom.status = 'RESERVED' AND wom.quantityIssued < wom.quantityReserved")
    List<WorkOrderMaterial> findReservedButNotIssued(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT SUM(wom.totalCost) FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId")
    BigDecimal calculateTotalMaterialCost(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT COUNT(wom) FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId")
    long countByWorkOrderId(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT COUNT(wom) FROM WorkOrderMaterial wom WHERE wom.workOrder.workOrderId = :workOrderId " +
           "AND wom.status = 'CONSUMED'")
    long countConsumedMaterials(@Param("workOrderId") UUID workOrderId);
}

