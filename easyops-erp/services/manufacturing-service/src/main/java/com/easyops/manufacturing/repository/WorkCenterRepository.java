package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.WorkCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkCenterRepository extends JpaRepository<WorkCenter, UUID> {

    List<WorkCenter> findByOrganizationId(UUID organizationId);

    Optional<WorkCenter> findByOrganizationIdAndWorkCenterCode(UUID organizationId, String workCenterCode);

    List<WorkCenter> findByOrganizationIdAndWorkCenterType(UUID organizationId, String workCenterType);

    List<WorkCenter> findByOrganizationIdAndStatus(UUID organizationId, String status);

    @Query("SELECT wc FROM WorkCenter wc WHERE wc.organizationId = :orgId " +
           "AND wc.isActive = true ORDER BY wc.workCenterCode")
    List<WorkCenter> findActiveWorkCenters(@Param("orgId") UUID organizationId);

    @Query("SELECT wc FROM WorkCenter wc WHERE wc.department = :dept " +
           "AND wc.isActive = true ORDER BY wc.workCenterCode")
    List<WorkCenter> findByDepartment(@Param("dept") String department);

    @Query("SELECT wc FROM WorkCenter wc WHERE wc.organizationId = :orgId " +
           "AND wc.nextMaintenanceDate < :date ORDER BY wc.nextMaintenanceDate")
    List<WorkCenter> findMaintenanceDue(@Param("orgId") UUID organizationId, @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(wc) FROM WorkCenter wc WHERE wc.organizationId = :orgId")
    long countByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(wc) FROM WorkCenter wc WHERE wc.organizationId = :orgId " +
           "AND wc.status = 'AVAILABLE'")
    long countAvailable(@Param("orgId") UUID organizationId);
}

