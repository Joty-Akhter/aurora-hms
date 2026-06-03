package com.easyops.hr.repository;

import com.easyops.hr.entity.EmployeeSalaryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeSalaryAssignmentRepository extends JpaRepository<EmployeeSalaryAssignment, UUID> {

    /** All assignments for an employee, newest first (for history). */
    List<EmployeeSalaryAssignment> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    /** ES-01: The single active assignment for an employee on a given date (one active per employee per date). */
    @Query("""
        SELECT a FROM EmployeeSalaryAssignment a
        WHERE a.employeeId = :employeeId
        AND a.effectiveFrom <= :asOfDate
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :asOfDate)
        """)
    List<EmployeeSalaryAssignment> findActiveByEmployeeIdAndDate(
        @Param("employeeId") UUID employeeId,
        @Param("asOfDate") LocalDate asOfDate
    );

    /**
     * NF-03: Batch fetch active assignments for many employees in an organization
     * on a given date. Used by payroll batch processing to avoid N+1 queries.
     */
    @Query("""
        SELECT a FROM EmployeeSalaryAssignment a
        WHERE a.organizationId = :organizationId
          AND a.employeeId IN :employeeIds
          AND a.effectiveFrom <= :asOfDate
          AND (a.effectiveTo IS NULL OR a.effectiveTo >= :asOfDate)
        """)
    List<EmployeeSalaryAssignment> findActiveByOrganizationAndEmployeeIdsAndDate(
        @Param("organizationId") UUID organizationId,
        @Param("employeeIds") List<UUID> employeeIds,
        @Param("asOfDate") LocalDate asOfDate
    );

    /** Count assignments overlapping the given period for same employee (exclude optional assignmentId for update). Periods overlap when newFrom <= existingTo AND existingFrom <= newTo (null end = open). */
    @Query("""
        SELECT COUNT(a) FROM EmployeeSalaryAssignment a
        WHERE a.employeeId = :employeeId
        AND (:excludeAssignmentId IS NULL OR a.assignmentId <> :excludeAssignmentId)
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :newEffectiveFrom)
        AND (:newEffectiveTo IS NULL OR :newEffectiveTo >= a.effectiveFrom)
        """)
    long countOverlappingPeriod(
        @Param("employeeId") UUID employeeId,
        @Param("newEffectiveFrom") LocalDate newEffectiveFrom,
        @Param("newEffectiveTo") LocalDate newEffectiveTo,
        @Param("excludeAssignmentId") UUID excludeAssignmentId
    );

    /** RPT-05: All assignments active in the organization on the given date (one row per assignment; dedupe by employeeId in service for one-per-employee). */
    @Query("""
        SELECT a FROM EmployeeSalaryAssignment a
        WHERE a.organizationId = :organizationId
        AND a.effectiveFrom <= :asOfDate
        AND (a.effectiveTo IS NULL OR a.effectiveTo >= :asOfDate)
        """)
    List<EmployeeSalaryAssignment> findActiveByOrganizationAndDate(
        @Param("organizationId") UUID organizationId,
        @Param("asOfDate") LocalDate asOfDate
    );

    /** List assignments by org with optional filters; for asOfDate only the active row per employee is desired, use findActiveByEmployeeIdAndDate per employee or add a native query. */
    List<EmployeeSalaryAssignment> findByOrganizationIdOrderByEffectiveFromDesc(UUID organizationId);

    @Query("""
        SELECT a FROM EmployeeSalaryAssignment a
        WHERE a.organizationId = :organizationId
        AND (:structureId IS NULL OR a.salaryStructureId = :structureId)
        AND (:gradeId IS NULL OR a.salaryGradeId = :gradeId)
        AND (:asOfDate IS NULL OR (a.effectiveFrom <= :asOfDate AND (a.effectiveTo IS NULL OR a.effectiveTo >= :asOfDate)))
        ORDER BY a.effectiveFrom DESC
        """)
    List<EmployeeSalaryAssignment> findByOrganizationIdAndFilters(
        @Param("organizationId") UUID organizationId,
        @Param("structureId") UUID structureId,
        @Param("gradeId") UUID gradeId,
        @Param("asOfDate") LocalDate asOfDate
    );
}
