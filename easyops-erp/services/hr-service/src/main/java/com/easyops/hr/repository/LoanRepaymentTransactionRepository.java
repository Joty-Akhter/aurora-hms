package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanRepaymentSource;
import com.easyops.hr.entity.LoanRepaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepaymentTransactionRepository extends JpaRepository<LoanRepaymentTransaction, UUID> {

    List<LoanRepaymentTransaction> findByLoanIdOrderByCreatedAtDesc(UUID loanId);

    boolean existsByLoanIdAndPayrollRunId(UUID loanId, UUID payrollRunId);

    Optional<LoanRepaymentTransaction> findByLoanIdAndPayrollRunIdAndSource(
            UUID loanId, UUID payrollRunId, LoanRepaymentSource source);

    boolean existsByReversesTransactionId(UUID reversesTransactionId);

    @Query("SELECT t FROM LoanRepaymentTransaction t JOIN EmployeeLoan l ON t.loanId = l.loanId "
            + "WHERE l.organizationId = :orgId AND t.source = :source AND t.createdAt >= :since "
            + "ORDER BY t.createdAt DESC")
    List<LoanRepaymentTransaction> findPayrollReversalsSince(
            @Param("orgId") UUID orgId,
            @Param("source") LoanRepaymentSource source,
            @Param("since") LocalDateTime since);

    /** Phase 7 (PI-05): repayments in [from, to] inclusive for this organization. */
    @Query("SELECT t FROM LoanRepaymentTransaction t JOIN EmployeeLoan l ON t.loanId = l.loanId "
            + "WHERE l.organizationId = :orgId AND t.paymentDate >= :from AND t.paymentDate <= :to "
            + "ORDER BY t.paymentDate, t.transactionId")
    List<LoanRepaymentTransaction> findByOrganizationAndPaymentDateBetween(
            @Param("orgId") UUID organizationId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM LoanRepaymentTransaction t WHERE t.loanId = :loanId AND t.source = :source")
    BigDecimal sumAmountByLoanIdAndSource(
            @Param("loanId") UUID loanId, @Param("source") LoanRepaymentSource source);
}
