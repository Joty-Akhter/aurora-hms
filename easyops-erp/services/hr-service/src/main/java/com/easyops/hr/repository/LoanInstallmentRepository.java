package com.easyops.hr.repository;

import com.easyops.hr.entity.EmployeeLoanStatus;
import com.easyops.hr.entity.LoanInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {

    List<LoanInstallment> findByLoanIdOrderBySequenceNumberAsc(UUID loanId);

    @Query("SELECT i FROM LoanInstallment i, EmployeeLoan l WHERE i.loanId = l.loanId"
            + " AND l.organizationId = :orgId AND i.dueDate < :asOf AND i.paidAmount < i.scheduledAmount"
            + " AND l.status IN :statuses")
    List<LoanInstallment> findArrearInstallments(
            @Param("orgId") UUID organizationId,
            @Param("asOf") LocalDate asOf,
            @Param("statuses") Collection<EmployeeLoanStatus> statuses);

    /** RE-03: unpaid installments with due date in [fromInclusive, toInclusive] (reminder window). */
    @Query(
            "SELECT i FROM LoanInstallment i JOIN EmployeeLoan l ON i.loanId = l.loanId "
                    + "WHERE l.status IN ('ACTIVE', 'SETTLEMENT_PENDING') "
                    + "AND i.status IN ('DUE', 'PARTIAL') "
                    + "AND i.paidAmount < i.scheduledAmount "
                    + "AND i.dueDate >= :fromInclusive AND i.dueDate <= :toInclusive")
    List<LoanInstallment> findUnpaidInstallmentsDueBetween(
            @Param("fromInclusive") LocalDate fromInclusive, @Param("toInclusive") LocalDate toInclusive);
}
