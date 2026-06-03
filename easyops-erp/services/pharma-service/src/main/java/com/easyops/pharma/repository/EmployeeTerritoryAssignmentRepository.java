package com.easyops.pharma.repository;

import com.easyops.pharma.entity.EmployeeTerritoryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeTerritoryAssignmentRepository extends JpaRepository<EmployeeTerritoryAssignment, UUID> {

    List<EmployeeTerritoryAssignment> findByOrganizationId(UUID organizationId);

    List<EmployeeTerritoryAssignment> findByEmployeeId(UUID employeeId);

    List<EmployeeTerritoryAssignment> findByTerritoryId(UUID territoryId);

    List<EmployeeTerritoryAssignment> findByTerritoryIdAndStatus(UUID territoryId, String status);

    List<EmployeeTerritoryAssignment> findByEmployeeIdAndStatus(UUID employeeId, String status);

    @Query("SELECT e FROM EmployeeTerritoryAssignment e WHERE e.employeeId = :employeeId AND e.status = 'ACTIVE' AND (e.endDate IS NULL OR e.endDate >= :currentDate)")
    List<EmployeeTerritoryAssignment> findActiveAssignmentsByEmployee(@Param("employeeId") UUID employeeId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT e FROM EmployeeTerritoryAssignment e WHERE e.territoryId = :territoryId AND e.status = 'ACTIVE' AND (e.endDate IS NULL OR e.endDate >= :currentDate)")
    List<EmployeeTerritoryAssignment> findActiveAssignmentsByTerritory(@Param("territoryId") UUID territoryId, @Param("currentDate") LocalDate currentDate);

    boolean existsByEmployeeIdAndTerritoryIdAndStatus(UUID employeeId, UUID territoryId, String status);

    @Query("SELECT e FROM EmployeeTerritoryAssignment e WHERE e.territoryId = :territoryId AND e.employeeId = :employeeId")
    Optional<EmployeeTerritoryAssignment> findByTerritoryIdAndEmployeeId(@Param("territoryId") UUID territoryId, @Param("employeeId") UUID employeeId);
}
