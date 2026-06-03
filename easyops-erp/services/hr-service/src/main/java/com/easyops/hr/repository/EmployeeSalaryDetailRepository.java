package com.easyops.hr.repository;

import com.easyops.hr.entity.EmployeeSalaryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeSalaryDetailRepository extends JpaRepository<EmployeeSalaryDetail, UUID> {

    List<EmployeeSalaryDetail> findByOrganizationId(UUID organizationId);

    List<EmployeeSalaryDetail> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);

    /** ES-18: Revision history – all details for employee ordered by effectiveFrom descending. */
    List<EmployeeSalaryDetail> findByEmployeeIdAndOrganizationIdOrderByEffectiveFromDesc(UUID employeeId, UUID organizationId);

    List<EmployeeSalaryDetail> findByEmployeeIdAndIsActive(UUID employeeId, Boolean isActive);

    /** ES-46: Details effective on asOfDate (effectiveFrom <= asOfDate and (effectiveTo is null or effectiveTo >= asOfDate)), active only. */
    @Query("SELECT d FROM EmployeeSalaryDetail d WHERE d.employeeId = :employeeId AND d.organizationId = :organizationId AND d.isActive = true "
        + "AND d.effectiveFrom <= :asOfDate AND (d.effectiveTo IS NULL OR d.effectiveTo >= :asOfDate) ORDER BY d.effectiveFrom DESC")
    List<EmployeeSalaryDetail> findByEmployeeIdAndOrganizationIdAndEffectiveOnDate(
        @Param("employeeId") UUID employeeId,
        @Param("organizationId") UUID organizationId,
        @Param("asOfDate") LocalDate asOfDate
    );

    /** ES-11: Find active rows for same employee+component (overlap check done in service when newEffectiveTo may be null). */
    List<EmployeeSalaryDetail> findByEmployeeIdAndComponentIdAndIsActive(
        UUID employeeId, UUID componentId, Boolean isActive);

    /** All active details for employee in org that overlap [periodFrom, periodTo] (for Basic-check). */
    @Query("SELECT d FROM EmployeeSalaryDetail d WHERE d.employeeId = :employeeId AND d.organizationId = :organizationId AND d.isActive = true "
        + "AND (d.effectiveTo IS NULL OR d.effectiveTo >= :periodFrom) AND (:periodTo IS NULL OR d.effectiveFrom <= :periodTo)")
    List<EmployeeSalaryDetail> findByEmployeeIdAndOrganizationIdOverlappingPeriod(
        @Param("employeeId") UUID employeeId,
        @Param("organizationId") UUID organizationId,
        @Param("periodFrom") LocalDate periodFrom,
        @Param("periodTo") LocalDate periodTo
    );

    /**
     * ES-11/ES-33: Find active details for the same employee and component whose effective
     * periods overlap the given range. Used in service layer to enforce
     * "one active per employee per component per date".
     *
     * Overlap rule:
     *   existing.effectiveFrom <= newEffectiveTo (or newEffectiveTo is null)
     *   AND
     *   (existing.effectiveTo is null OR existing.effectiveTo >= newEffectiveFrom)
     */
    @Query("""
        SELECT d FROM EmployeeSalaryDetail d
        WHERE d.employeeId = :employeeId
          AND d.componentId = :componentId
          AND d.isActive = true
          AND (d.effectiveTo IS NULL OR d.effectiveTo >= :newEffectiveFrom)
          AND (:newEffectiveTo IS NULL OR :newEffectiveTo >= d.effectiveFrom)
        """)
    List<EmployeeSalaryDetail> findOverlappingEmployeeComponent(
        @Param("employeeId") UUID employeeId,
        @Param("componentId") UUID componentId,
        @Param("newEffectiveFrom") LocalDate newEffectiveFrom,
        @Param("newEffectiveTo") LocalDate newEffectiveTo
    );

    /**
     * NF-03: Batch fetch active salary details for many employees in an organization
     * effective on a given date. Used by payroll batch processing.
     */
    @Query("""
        SELECT d FROM EmployeeSalaryDetail d
        WHERE d.organizationId = :organizationId
          AND d.employeeId IN :employeeIds
          AND d.isActive = true
          AND d.effectiveFrom <= :asOfDate
          AND (d.effectiveTo IS NULL OR d.effectiveTo >= :asOfDate)
        """)
    List<EmployeeSalaryDetail> findActiveByOrganizationAndEmployeeIdsAndEffectiveOnDate(
        @Param("organizationId") UUID organizationId,
        @Param("employeeIds") List<UUID> employeeIds,
        @Param("asOfDate") LocalDate asOfDate
    );

    /** SS-48: Count distinct employees with this structure effective on date (active detail, period overlaps date). */
    @Query("SELECT COUNT(DISTINCT d.employeeId) FROM EmployeeSalaryDetail d WHERE d.organizationId = :organizationId AND d.salaryStructureId = :structureId "
        + "AND d.isActive = true AND d.effectiveFrom <= :asOfDate AND (d.effectiveTo IS NULL OR d.effectiveTo >= :asOfDate)")
    long countDistinctEmployeesByStructureAndDate(
        @Param("organizationId") UUID organizationId,
        @Param("structureId") UUID structureId,
        @Param("asOfDate") LocalDate asOfDate
    );

    /** SC-41: Count distinct employees using this component (active details with this componentId). */
    @Query("SELECT COUNT(DISTINCT d.employeeId) FROM EmployeeSalaryDetail d WHERE d.componentId = :componentId AND d.isActive = true")
    long countDistinctEmployeesByComponentId(@Param("componentId") UUID componentId);

    /** SC-32: Count employee salary detail records referencing this component (prevent delete if in use). */
    long countByComponentId(UUID componentId);
}

