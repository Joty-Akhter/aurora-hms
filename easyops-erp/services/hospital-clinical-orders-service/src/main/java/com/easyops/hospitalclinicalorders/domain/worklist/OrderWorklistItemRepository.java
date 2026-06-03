package com.easyops.hospitalclinicalorders.domain.worklist;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderWorklistItemRepository extends JpaRepository<OrderWorklistItem, UUID> {

    List<OrderWorklistItem> findByOrderId(UUID orderId);

    Page<OrderWorklistItem> findByWorklistType(String worklistType, Pageable pageable);
    Page<OrderWorklistItem> findByStatus(String status, Pageable pageable);
    Page<OrderWorklistItem> findByWorklistTypeAndStatus(String worklistType, String status, Pageable pageable);
    Page<OrderWorklistItem> findByAssignedToUserId(UUID assignedToUserId, Pageable pageable);
    Page<OrderWorklistItem> findByAssignedToUserIdAndStatus(UUID assignedToUserId, String status, Pageable pageable);

    /**
     * Find worklist items with optional filters and sort by priority (STAT first, URGENT, ROUTINE),
     * then scheduled_time (nulls last), then created_at.
     * departmentId filters by order set's ordering_department_id; section filters by worklist_type; facilityId by order set facility.
     * from/to filter by worklist item created_at (inclusive).
     */
    @Query(value = "SELECT w FROM OrderWorklistItem w LEFT JOIN w.order o LEFT JOIN o.orderSet os " +
            "WHERE (:type IS NULL OR w.worklistType = :type) " +
            "AND (:status IS NULL OR w.status = :status) " +
            "AND (:assignedTo IS NULL OR w.assignedToUserId = :assignedTo) " +
            "AND (:departmentId IS NULL OR os.orderingDepartmentId = :departmentId) " +
            "AND (:section IS NULL OR w.worklistType = :section) " +
            "AND (:facilityId IS NULL OR os.facilityId = :facilityId) " +
            "AND (:from IS NULL OR w.createdAt >= :from) " +
            "AND (:to IS NULL OR w.createdAt <= :to) " +
            "ORDER BY CASE WHEN o.priority = 'STAT' THEN 0 WHEN o.priority = 'URGENT' THEN 1 ELSE 2 END, w.scheduledTime ASC NULLS LAST, w.createdAt ASC",
            countQuery = "SELECT COUNT(w) FROM OrderWorklistItem w LEFT JOIN w.order o LEFT JOIN o.orderSet os " +
                    "WHERE (:type IS NULL OR w.worklistType = :type) " +
                    "AND (:status IS NULL OR w.status = :status) " +
                    "AND (:assignedTo IS NULL OR w.assignedToUserId = :assignedTo) " +
                    "AND (:departmentId IS NULL OR os.orderingDepartmentId = :departmentId) " +
                    "AND (:section IS NULL OR w.worklistType = :section) " +
                    "AND (:facilityId IS NULL OR os.facilityId = :facilityId) " +
                    "AND (:from IS NULL OR w.createdAt >= :from) " +
                    "AND (:to IS NULL OR w.createdAt <= :to)")
    Page<OrderWorklistItem> findWithFiltersAndPrioritySort(
            @Param("type") String type,
            @Param("status") String status,
            @Param("assignedTo") UUID assignedTo,
            @Param("departmentId") UUID departmentId,
            @Param("section") String section,
            @Param("facilityId") UUID facilityId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);
}
