package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.WorkOrderOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkOrderOperationRepository extends JpaRepository<WorkOrderOperation, UUID> {

    @Query("SELECT woo FROM WorkOrderOperation woo WHERE woo.workOrder.workOrderId = :workOrderId " +
           "ORDER BY woo.operationSequence")
    List<WorkOrderOperation> findByWorkOrderId(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT woo FROM WorkOrderOperation woo WHERE woo.workOrder.workOrderId = :workOrderId " +
           "AND woo.status = :status ORDER BY woo.operationSequence")
    List<WorkOrderOperation> findByWorkOrderIdAndStatus(@Param("workOrderId") UUID workOrderId, 
                                                         @Param("status") String status);

    @Query("SELECT woo FROM WorkOrderOperation woo WHERE woo.workOrder.workOrderId = :workOrderId " +
           "AND woo.operationSequence = :sequence")
    Optional<WorkOrderOperation> findByWorkOrderIdAndSequence(@Param("workOrderId") UUID workOrderId, 
                                                               @Param("sequence") Integer sequence);

    @Query("SELECT woo FROM WorkOrderOperation woo WHERE woo.workCenterId = :workCenterId " +
           "AND woo.status IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY woo.plannedStart")
    List<WorkOrderOperation> findPendingOperationsByWorkCenter(@Param("workCenterId") UUID workCenterId);

    @Query("SELECT woo FROM WorkOrderOperation woo WHERE woo.assignedTo = :userId " +
           "AND woo.status IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY woo.plannedStart")
    List<WorkOrderOperation> findOperationsAssignedToUser(@Param("userId") UUID userId);

    @Query("SELECT woo FROM WorkOrderOperation woo WHERE woo.workOrder.workOrderId = :workOrderId " +
           "AND woo.qualityCheckRequired = true AND woo.qualityCheckCompleted = false")
    List<WorkOrderOperation> findPendingQualityChecks(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT COUNT(woo) FROM WorkOrderOperation woo WHERE woo.workOrder.workOrderId = :workOrderId")
    long countByWorkOrderId(@Param("workOrderId") UUID workOrderId);

    @Query("SELECT COUNT(woo) FROM WorkOrderOperation woo WHERE woo.workOrder.workOrderId = :workOrderId " +
           "AND woo.status = 'COMPLETED'")
    long countCompletedOperations(@Param("workOrderId") UUID workOrderId);
}

