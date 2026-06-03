package com.easyops.hr.repository;

import com.easyops.hr.entity.EmployeeLoan;
import com.easyops.hr.entity.EmployeeLoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, UUID> {

    Optional<EmployeeLoan> findByLoanIdAndOrganizationId(UUID loanId, UUID organizationId);

    Optional<EmployeeLoan> findByLoanApplicationId(UUID loanApplicationId);

    List<EmployeeLoan> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<EmployeeLoan> findByOrganizationIdAndStatusIn(UUID organizationId, Collection<EmployeeLoanStatus> statuses);

    List<EmployeeLoan> findByOrganizationIdAndEmployeeIdOrderByCreatedAtDesc(UUID organizationId, UUID employeeId);

    List<EmployeeLoan> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, EmployeeLoanStatus status);

    Optional<EmployeeLoan> findByOrganizationIdAndEmployeeIdAndStatus(
            UUID organizationId, UUID employeeId, EmployeeLoanStatus status);

    boolean existsByOrganizationIdAndEmployeeIdAndStatusIn(
            UUID organizationId, UUID employeeId, Collection<EmployeeLoanStatus> statuses);

    /** Phase 7 (PI-05): loans disbursed in [from, to] inclusive. */
    @Query("SELECT l FROM EmployeeLoan l WHERE l.organizationId = :orgId AND l.disbursementDate IS NOT NULL "
            + "AND l.disbursementDate >= :from AND l.disbursementDate <= :to ORDER BY l.disbursementDate, l.loanId")
    List<EmployeeLoan> findDisbursementsInPeriod(
            @Param("orgId") UUID organizationId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
