package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.EquipmentMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipmentMaintenanceRepository extends JpaRepository<EquipmentMaintenance, UUID> {

    List<EquipmentMaintenance> findByOrganizationId(UUID organizationId);

    List<EquipmentMaintenance> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<EquipmentMaintenance> findByOrganizationIdAndMaintenanceNumber(UUID organizationId, String maintenanceNumber);

    List<EquipmentMaintenance> findByWorkCenterId(UUID workCenterId);

    @Query("SELECT em FROM EquipmentMaintenance em WHERE em.organizationId = :orgId " +
           "AND em.maintenanceType = :type ORDER BY em.scheduledDate DESC")
    List<EquipmentMaintenance> findByMaintenanceType(@Param("orgId") UUID organizationId, 
                                                     @Param("type") String maintenanceType);

    @Query("SELECT em FROM EquipmentMaintenance em WHERE em.organizationId = :orgId " +
           "AND em.scheduledDate BETWEEN :startDate AND :endDate ORDER BY em.scheduledDate")
    List<EquipmentMaintenance> findByScheduledDateRange(@Param("orgId") UUID organizationId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    @Query("SELECT em FROM EquipmentMaintenance em WHERE em.status = 'SCHEDULED' " +
           "AND em.scheduledDate < :date ORDER BY em.scheduledDate")
    List<EquipmentMaintenance> findOverdueMaintenance(@Param("date") LocalDateTime date);

    @Query("SELECT em FROM EquipmentMaintenance em WHERE em.organizationId = :orgId AND em.status = 'SCHEDULED' " +
           "AND em.scheduledDate < :date ORDER BY em.scheduledDate")
    List<EquipmentMaintenance> findOverdueMaintenanceByOrganization(
            @Param("orgId") UUID organizationId,
            @Param("date") LocalDateTime date);

    @Query("SELECT em FROM EquipmentMaintenance em WHERE em.assignedTo = :userId " +
           "AND em.status IN ('SCHEDULED', 'IN_PROGRESS') ORDER BY em.scheduledDate")
    List<EquipmentMaintenance> findAssignedToUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(em) FROM EquipmentMaintenance em WHERE em.organizationId = :orgId")
    long countByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(em) FROM EquipmentMaintenance em WHERE em.workCenterId = :workCenterId " +
           "AND em.status = 'COMPLETED'")
    long countCompletedByWorkCenter(@Param("workCenterId") UUID workCenterId);
}

