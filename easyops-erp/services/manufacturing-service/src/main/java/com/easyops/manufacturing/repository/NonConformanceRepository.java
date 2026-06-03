package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.NonConformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NonConformanceRepository extends JpaRepository<NonConformance, UUID> {

    List<NonConformance> findByOrganizationId(UUID organizationId);

    List<NonConformance> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<NonConformance> findByOrganizationIdAndNcNumber(UUID organizationId, String ncNumber);

    List<NonConformance> findByWorkOrderId(UUID workOrderId);

    List<NonConformance> findByProductId(UUID productId);

    @Query("SELECT nc FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.severity = :severity ORDER BY nc.reportedDate DESC")
    List<NonConformance> findBySeverity(@Param("orgId") UUID organizationId, @Param("severity") String severity);

    @Query("SELECT nc FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.ncType = :type ORDER BY nc.reportedDate DESC")
    List<NonConformance> findByNcType(@Param("orgId") UUID organizationId, @Param("type") String ncType);

    @Query("SELECT nc FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.status IN ('OPEN', 'IN_REVIEW') ORDER BY nc.severity DESC, nc.reportedDate ASC")
    List<NonConformance> findOpenNonConformances(@Param("orgId") UUID organizationId);

    @Query("SELECT nc FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.severity = 'CRITICAL' AND nc.status IN ('OPEN', 'IN_REVIEW') " +
           "ORDER BY nc.reportedDate ASC")
    List<NonConformance> findCriticalOpenNCs(@Param("orgId") UUID organizationId);

    @Query("SELECT nc FROM NonConformance nc WHERE nc.assignedTo = :userId " +
           "AND nc.status IN ('OPEN', 'IN_REVIEW') ORDER BY nc.severity DESC")
    List<NonConformance> findAssignedToUser(@Param("userId") UUID userId);

    @Query("SELECT nc FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.reportedDate BETWEEN :startDate AND :endDate ORDER BY nc.reportedDate DESC")
    List<NonConformance> findByDateRange(@Param("orgId") UUID organizationId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(nc) FROM NonConformance nc WHERE nc.organizationId = :orgId")
    long countByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(nc) FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.status IN ('OPEN', 'IN_REVIEW')")
    long countOpenByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(nc) FROM NonConformance nc WHERE nc.organizationId = :orgId " +
           "AND nc.severity = :severity")
    long countBySeverity(@Param("orgId") UUID organizationId, @Param("severity") String severity);
}

