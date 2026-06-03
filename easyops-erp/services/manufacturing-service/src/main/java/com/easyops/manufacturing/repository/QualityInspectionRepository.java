package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.QualityInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityInspectionRepository extends JpaRepository<QualityInspection, UUID> {

    List<QualityInspection> findByOrganizationId(UUID organizationId);

    List<QualityInspection> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<QualityInspection> findByOrganizationIdAndInspectionNumber(UUID organizationId, String inspectionNumber);

    List<QualityInspection> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);

    List<QualityInspection> findByWorkOrderId(UUID workOrderId);

    @Query("SELECT qi FROM QualityInspection qi WHERE qi.organizationId = :orgId " +
           "AND qi.inspectionType = :type ORDER BY qi.inspectionDate DESC")
    List<QualityInspection> findByInspectionType(@Param("orgId") UUID organizationId, @Param("type") String inspectionType);

    @Query("SELECT qi FROM QualityInspection qi WHERE qi.organizationId = :orgId " +
           "AND qi.overallResult = :result ORDER BY qi.inspectionDate DESC")
    List<QualityInspection> findByOverallResult(@Param("orgId") UUID organizationId, @Param("result") String overallResult);

    @Query("SELECT qi FROM QualityInspection qi WHERE qi.organizationId = :orgId " +
           "AND qi.inspectionDate BETWEEN :startDate AND :endDate ORDER BY qi.inspectionDate DESC")
    List<QualityInspection> findByDateRange(@Param("orgId") UUID organizationId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT qi FROM QualityInspection qi WHERE qi.organizationId = :orgId " +
           "AND qi.status = 'PENDING' ORDER BY qi.inspectionDate ASC")
    List<QualityInspection> findPendingInspections(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(qi) FROM QualityInspection qi WHERE qi.organizationId = :orgId")
    long countByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(qi) FROM QualityInspection qi WHERE qi.organizationId = :orgId " +
           "AND qi.overallResult = :result")
    long countByResult(@Param("orgId") UUID organizationId, @Param("result") String result);
}

