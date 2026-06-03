package com.easyops.hr.repository;

import com.easyops.hr.entity.PayrollDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollDetailRepository extends JpaRepository<PayrollDetail, UUID> {
    
    List<PayrollDetail> findByPayrollRunId(UUID payrollRunId);
    
    List<PayrollDetail> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<PayrollDetail> findByPayrollRunIdAndStatus(UUID payrollRunId, String status);

    /**
     * INT-14 / payslip YTD: sums gross, deductions, net for calendar year through {@code asOfDate},
     * including closed runs and the current run (even if DRAFT).
     */
    @Query("""
            SELECT COALESCE(SUM(pd.grossSalary), 0), COALESCE(SUM(pd.totalDeductions), 0), COALESCE(SUM(pd.netSalary), 0)
            FROM PayrollDetail pd, PayrollRun pr
            WHERE pr.payrollRunId = pd.payrollRunId
            AND pd.employeeId = :employeeId
            AND pd.organizationId = :organizationId
            AND pr.payPeriodEnd >= :yearStart
            AND pr.payPeriodEnd <= :asOfDate
            AND (UPPER(pr.status) IN ('PROCESSED', 'APPROVED', 'FINALIZED') OR pr.payrollRunId = :currentRunId)
            """)
    Object[] sumYearToDatePayrollTotals(@Param("employeeId") UUID employeeId,
                                        @Param("organizationId") UUID organizationId,
                                        @Param("yearStart") LocalDate yearStart,
                                        @Param("asOfDate") LocalDate asOfDate,
                                        @Param("currentRunId") UUID currentRunId);

    /**
     * YTD sum of payroll lines that represent income tax (statutory type or known codes).
     */
    @Query("""
            SELECT COALESCE(SUM(pc.amount), 0)
            FROM PayrollComponent pc
            JOIN PayrollDetail pd ON pd.payrollDetailId = pc.payrollDetailId
            JOIN PayrollRun pr ON pr.payrollRunId = pd.payrollRunId
            JOIN SalaryComponent sc ON sc.componentId = pc.componentId
            WHERE pd.employeeId = :employeeId
            AND pd.organizationId = :organizationId
            AND pr.payPeriodEnd >= :yearStart
            AND pr.payPeriodEnd <= :asOfDate
            AND (UPPER(pr.status) IN ('PROCESSED', 'APPROVED', 'FINALIZED') OR pr.payrollRunId = :currentRunId)
            AND (
                UPPER(COALESCE(sc.statutoryType, '')) = 'INCOME_TAX'
                OR UPPER(COALESCE(sc.code, '')) IN ('INCOME_TAX', 'TAX', 'IT')
            )
            """)
    Object sumYearToDateIncomeTaxWithheld(@Param("employeeId") UUID employeeId,
                                         @Param("organizationId") UUID organizationId,
                                         @Param("yearStart") LocalDate yearStart,
                                         @Param("asOfDate") LocalDate asOfDate,
                                         @Param("currentRunId") UUID currentRunId);
}

