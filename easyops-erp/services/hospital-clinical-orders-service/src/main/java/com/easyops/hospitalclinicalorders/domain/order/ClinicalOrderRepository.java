package com.easyops.hospitalclinicalorders.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ClinicalOrderRepository extends JpaRepository<ClinicalOrder, UUID>, JpaSpecificationExecutor<ClinicalOrder> {

    List<ClinicalOrder> findByOrderSetId(UUID orderSetId);

    Page<ClinicalOrder> findByOrderSetId(UUID orderSetId, Pageable pageable);

    /** TAT report: [orderType, count, avgTatHours]. Completed = result_available_at or (COMPLETED and performed_at). */
    @Query(value = ""
        + "SELECT o.order_type, COUNT(*), AVG(EXTRACT(EPOCH FROM (COALESCE(o.result_available_at, o.performed_at) - o.created_at)) / 3600.0) "
        + "FROM hospital_clinical_orders.clinical_orders o "
        + "WHERE o.created_at >= :from AND o.created_at < :to "
        + "  AND (o.result_available_at IS NOT NULL OR (o.status = 'COMPLETED' AND o.performed_at IS NOT NULL)) "
        + "  AND (CAST(:orderType AS text) IS NULL OR o.order_type = :orderType) "
        + "GROUP BY o.order_type",
        nativeQuery = true)
    List<Object[]> findTatAggregates(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to, @Param("orderType") String orderType);

    /** Volume by order type: [orderType, count]. */
    @Query(value = ""
        + "SELECT o.order_type, COUNT(*) "
        + "FROM hospital_clinical_orders.clinical_orders o "
        + "WHERE o.created_at >= :from AND o.created_at < :to "
        + "GROUP BY o.order_type",
        nativeQuery = true)
    List<Object[]> findVolumesByOrderType(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /** Volume by department: [ordering_department_id, count]. */
    @Query(value = ""
        + "SELECT CAST(os.ordering_department_id AS text), COUNT(*) "
        + "FROM hospital_clinical_orders.clinical_orders o "
        + "JOIN hospital_clinical_orders.order_sets os ON os.id = o.order_set_id "
        + "WHERE o.created_at >= :from AND o.created_at < :to "
        + "GROUP BY os.ordering_department_id",
        nativeQuery = true)
    List<Object[]> findVolumesByDepartment(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
