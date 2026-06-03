package com.easyops.hr.repository;

import com.easyops.hr.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    
    List<Employee> findByOrganizationId(UUID organizationId);
    
    List<Employee> findByOrganizationIdAndEmploymentStatus(UUID organizationId, String employmentStatus);
    
    List<Employee> findByOrganizationIdAndDepartmentId(UUID organizationId, UUID departmentId);
    
    Optional<Employee> findByOrganizationIdAndEmployeeNumber(UUID organizationId, String employeeNumber);
    
    Optional<Employee> findByOrganizationIdAndEmail(UUID organizationId, String email);

    Optional<Employee> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    
    @Query("SELECT e FROM Employee e WHERE e.organizationId = :organizationId " +
           "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Employee> searchEmployees(@Param("organizationId") UUID organizationId, 
                                  @Param("search") String search);

    @Query("SELECT e FROM Employee e WHERE e.organizationId = :organizationId " +
           "AND (:status IS NULL OR e.employmentStatus = :status) " +
           "AND (:departmentId IS NULL OR e.departmentId = :departmentId) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Employee> findEmployeesFiltered(
            @Param("organizationId") UUID organizationId,
            @Param("status") String status,
            @Param("departmentId") UUID departmentId,
            @Param("search") String search,
            Pageable pageable);
    
    long countByOrganizationIdAndEmploymentStatus(UUID organizationId, String employmentStatus);

    /** SS-48: Count employees in positions with this default grade, employed as of date. */
    @Query(value = """
        SELECT COUNT(*) FROM hr.employees e
        INNER JOIN hr.positions p ON e.position_id = p.position_id
        WHERE e.organization_id = :orgId AND p.default_salary_grade_id = :gradeId
        AND e.hire_date <= :asOfDate AND (e.termination_date IS NULL OR e.termination_date >= :asOfDate)
        """, nativeQuery = true)
    long countByOrganizationAndDefaultGradeAsOfDate(
        @Param("orgId") UUID orgId,
        @Param("gradeId") UUID gradeId,
        @Param("asOfDate") java.time.LocalDate asOfDate
    );

    /** SS-48: Count employees in positions with this default band, employed as of date. */
    @Query(value = """
        SELECT COUNT(*) FROM hr.employees e
        INNER JOIN hr.positions p ON e.position_id = p.position_id
        WHERE e.organization_id = :orgId AND p.default_salary_band_id = :bandId
        AND e.hire_date <= :asOfDate AND (e.termination_date IS NULL OR e.termination_date >= :asOfDate)
        """, nativeQuery = true)
    long countByOrganizationAndDefaultBandAsOfDate(
        @Param("orgId") UUID orgId,
        @Param("bandId") UUID bandId,
        @Param("asOfDate") java.time.LocalDate asOfDate
    );
}

