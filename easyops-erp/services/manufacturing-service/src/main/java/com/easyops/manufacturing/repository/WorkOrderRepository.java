package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    List<WorkOrder> findByOrganizationId(UUID organizationId);

    List<WorkOrder> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<WorkOrder> findByOrganizationIdAndWorkOrderNumber(UUID organizationId, String workOrderNumber);

    List<WorkOrder> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);

    List<WorkOrder> findByOrganizationIdAndBomId(UUID organizationId, UUID bomId);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.organizationId = :orgId " +
           "AND wo.status IN ('RELEASED', 'IN_PROGRESS', 'PAUSED') " +
           "ORDER BY wo.priority DESC, wo.plannedEndDate ASC")
    List<WorkOrder> findActiveWorkOrders(@Param("orgId") UUID organizationId);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.organizationId = :orgId " +
           "AND wo.status = 'IN_PROGRESS' " +
           "ORDER BY wo.plannedEndDate ASC")
    List<WorkOrder> findInProgressWorkOrders(@Param("orgId") UUID organizationId);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.organizationId = :orgId " +
           "AND wo.plannedEndDate < :date AND wo.status IN ('RELEASED', 'IN_PROGRESS')")
    List<WorkOrder> findOverdueWorkOrders(@Param("orgId") UUID organizationId, @Param("date") LocalDateTime date);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.organizationId = :orgId " +
           "AND wo.plannedEndDate BETWEEN :startDate AND :endDate " +
           "ORDER BY wo.plannedEndDate ASC")
    List<WorkOrder> findByPlannedDateRange(@Param("orgId") UUID organizationId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.priority = :priority " +
           "AND wo.status IN ('RELEASED', 'IN_PROGRESS') " +
           "ORDER BY wo.plannedEndDate ASC")
    List<WorkOrder> findByPriority(@Param("priority") String priority);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.sourceType = :sourceType " +
           "AND wo.sourceReference = :sourceRef")
    Optional<WorkOrder> findBySourceReference(@Param("sourceType") String sourceType, 
                                               @Param("sourceRef") String sourceReference);

    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.organizationId = :orgId")
    long countByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(wo) FROM WorkOrder wo WHERE wo.organizationId = :orgId AND wo.status = :status")
    long countByOrganizationAndStatus(@Param("orgId") UUID organizationId, @Param("status") String status);

    @Query("SELECT wo FROM WorkOrder wo WHERE wo.workCenterId = :workCenterId " +
           "AND wo.status IN ('RELEASED', 'IN_PROGRESS') " +
           "ORDER BY wo.priority DESC")
    List<WorkOrder> findByWorkCenter(@Param("workCenterId") UUID workCenterId);
}

