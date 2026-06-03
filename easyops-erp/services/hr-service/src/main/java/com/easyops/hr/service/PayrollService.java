package com.easyops.hr.service;

import com.easyops.hr.dto.MyPayslipSummaryDto;
import com.easyops.hr.dto.PayrollDetailWithEmployeeDto;
import com.easyops.hr.entity.PayrollDetail;
import com.easyops.hr.entity.PayrollRun;
import com.easyops.hr.repository.PayrollDetailRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.entity.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final LoanPayrollRecoveryService loanPayrollRecoveryService;
    
    public List<PayrollRun> getAllPayrollRuns(UUID organizationId) {
        return payrollRunRepository.findByOrganizationIdOrderByPayPeriodStartDesc(organizationId);
    }
    
    public PayrollRun getPayrollRunById(UUID payrollRunId) {
        return payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new RuntimeException("Payroll run not found"));
    }
    
    public List<PayrollRun> getPayrollRunsByStatus(UUID organizationId, String status) {
        return payrollRunRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public Map<String, Object> getPayrollStats(UUID organizationId) {
        List<PayrollRun> allRuns = payrollRunRepository.findByOrganizationIdOrderByPayPeriodStartDesc(organizationId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("currentMonthTotal", BigDecimal.ZERO);
        stats.put("employeeCount", 0);
        stats.put("pendingRuns", allRuns.stream().filter(r -> "DRAFT".equals(r.getStatus())).count());
        stats.put("processedRuns", allRuns.stream().filter(r -> "PROCESSED".equals(r.getStatus())).count());
        stats.put("ytdTotal", allRuns.stream()
                .map(r -> r.getTotalNetPay() != null ? r.getTotalNetPay() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        return stats;
    }
    
    public PayrollRun createPayrollRun(PayrollRun payrollRun) {
        log.info("Creating payroll run: {}", payrollRun.getRunName());
        
        if (payrollRun.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "organizationId is required.");
        }
        if (payrollRun.getPayPeriodStart() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payPeriodStart is required.");
        }
        if (payrollRun.getPayPeriodEnd() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payPeriodEnd is required.");
        }
        if (payrollRun.getPaymentDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentDate is required.");
        }
        if (payrollRun.getPayPeriodEnd().isBefore(payrollRun.getPayPeriodStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payPeriodEnd cannot be before payPeriodStart.");
        }

        if (!StringUtils.hasText(payrollRun.getRunName())) {
            payrollRun.setRunName(String.format("Payroll %s - %s",
                    payrollRun.getPayPeriodStart(),
                    payrollRun.getPayPeriodEnd()));
        }

        payrollRun.setStatus("DRAFT");
        payrollRun.setEmployeeCount(0);
        payrollRun.setTotalGrossPay(BigDecimal.ZERO);
        payrollRun.setTotalDeductions(BigDecimal.ZERO);
        payrollRun.setTotalNetPay(BigDecimal.ZERO);
        return payrollRunRepository.save(payrollRun);
    }
    
    public PayrollRun updatePayrollRun(UUID payrollRunId, PayrollRun payrollRun) {
        PayrollRun existing = getPayrollRunById(payrollRunId);
        
        if (payrollRun.getRunName() != null) existing.setRunName(payrollRun.getRunName());
        if (payrollRun.getPaymentDate() != null) existing.setPaymentDate(payrollRun.getPaymentDate());
        if (payrollRun.getStatus() != null) existing.setStatus(payrollRun.getStatus().toUpperCase());
        if (payrollRun.getNotes() != null) existing.setNotes(payrollRun.getNotes());
        
        return payrollRunRepository.save(existing);
    }
    
    /**
     * Finalizes payroll totals and status. Posts idempotent PAYROLL loan repayments for the run
     * (same as {@link LoanPayrollRecoveryService#confirmPayrollDeductions}).
     */
    public PayrollRun processPayrollRun(UUID payrollRunId, UUID processedBy) {
        PayrollRun payrollRun = getPayrollRunById(payrollRunId);
        var processor = resolveProcessor(payrollRun.getOrganizationId(), processedBy);
        
        // Get all payroll details
        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(payrollRunId);
        
        // Calculate totals
        BigDecimal totalGross = details.stream()
                .map(PayrollDetail::getGrossSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDeductions = details.stream()
                .map(PayrollDetail::getTotalDeductions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalNet = details.stream()
                .map(PayrollDetail::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        payrollRun.setEmployeeCount(details.size());
        payrollRun.setTotalGrossPay(totalGross);
        payrollRun.setTotalDeductions(totalDeductions);
        payrollRun.setTotalNetPay(totalNet);
        payrollRun.setStatus("PROCESSED");
        payrollRun.setProcessedBy(processor.map(Employee::getEmployeeId).orElse(null));
        payrollRun.setProcessedAt(LocalDateTime.now());
        
        PayrollRun saved = payrollRunRepository.save(payrollRun);
        confirmLoanPayrollRecoveries(payrollRunId, saved.getOrganizationId(), processedBy);
        return saved;
    }

    /**
     * Marks the run approved. Re-invokes idempotent loan PAYROLL posting so approve-only workflows
     * (or process-then-approve) still record recoveries when not already posted.
     */
    public PayrollRun approvePayrollRun(UUID payrollRunId, UUID approvedBy) {
        PayrollRun payrollRun = getPayrollRunById(payrollRunId);
        payrollRun.setStatus("APPROVED");
        payrollRun.setApprovedBy(approvedBy);
        payrollRun.setApprovedAt(LocalDateTime.now());
        PayrollRun saved = payrollRunRepository.save(payrollRun);
        confirmLoanPayrollRecoveries(payrollRunId, saved.getOrganizationId(), approvedBy);
        return saved;
    }

    /** RP-04 / PI-01: align loan ledger with payroll run; safe to call repeatedly (idempotent per loan+run). */
    private void confirmLoanPayrollRecoveries(UUID payrollRunId, UUID organizationId, UUID actorUserId) {
        loanPayrollRecoveryService.confirmPayrollDeductions(payrollRunId, organizationId, actorUserId);
    }
    
    // Payroll Detail Methods
    public List<PayrollDetail> getPayrollDetails(UUID payrollRunId) {
        return payrollDetailRepository.findByPayrollRunId(payrollRunId);
    }

    /** Get payroll details with employee names for display. */
    public List<PayrollDetailWithEmployeeDto> getPayrollDetailsWithEmployees(UUID payrollRunId) {
        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(payrollRunId);
        return details.stream()
                .map(d -> {
                    var empOpt = d.getEmployeeId() != null ? employeeRepository.findById(d.getEmployeeId()) : Optional.<Employee>empty();
                    return PayrollDetailWithEmployeeDto.builder()
                            .payrollDetailId(d.getPayrollDetailId())
                            .payrollRunId(d.getPayrollRunId())
                            .employeeId(d.getEmployeeId())
                            .employeeName(empOpt.map(Employee::getName).orElse(null))
                            .employeeNumber(empOpt.map(Employee::getEmployeeNumber).orElse(null))
                            .basicSalary(d.getBasicSalary())
                            .grossSalary(d.getGrossSalary())
                            .totalDeductions(d.getTotalDeductions())
                            .totalReimbursements(d.getTotalReimbursements())
                            .netSalary(d.getNetSalary())
                            .workingDays(d.getWorkingDays())
                            .presentDays(d.getPresentDays())
                            .leaveDays(d.getLeaveDays())
                            .overtimeHours(d.getOvertimeHours())
                            .overtimeAmount(d.getOvertimeAmount())
                            .lopDays(d.getLopDays())
                            .lopAmount(d.getLopAmount())
                            .bonusAmount(d.getBonusAmount())
                            .status(d.getStatus())
                            .paymentReference(d.getPaymentReference())
                            .paidAt(d.getPaidAt())
                            .build();
                })
                .toList();
    }
    
    public List<PayrollDetail> getEmployeePayrollHistory(UUID employeeId, UUID organizationId) {
        return payrollDetailRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }

    /** ES-54: List payroll runs for which this employee has a payslip (for self-service view/download). */
    public List<MyPayslipSummaryDto> getMyPayslips(UUID employeeId, UUID organizationId) {
        List<PayrollDetail> details = payrollDetailRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
        List<UUID> runIds = details.stream().map(PayrollDetail::getPayrollRunId).distinct().toList();
        return runIds.stream()
                .map(payrollRunRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(r -> organizationId.equals(r.getOrganizationId()))
                .sorted((a, b) -> (b.getPayPeriodStart() != null && a.getPayPeriodStart() != null)
                        ? b.getPayPeriodStart().compareTo(a.getPayPeriodStart()) : 0)
                .map(r -> MyPayslipSummaryDto.builder()
                        .payrollRunId(r.getPayrollRunId())
                        .runName(r.getRunName())
                        .payPeriodStart(r.getPayPeriodStart())
                        .payPeriodEnd(r.getPayPeriodEnd())
                        .paymentDate(r.getPaymentDate())
                        .status(r.getStatus())
                        .build())
                .toList();
    }
    
    public PayrollDetail createPayrollDetail(PayrollDetail detail) {
        log.info("Creating payroll detail for employee: {}", detail.getEmployeeId());
        detail.setStatus("pending");
        return payrollDetailRepository.save(detail);
    }

    public PayrollDetail getPayrollDetailById(UUID payrollDetailId) {
        return payrollDetailRepository.findById(payrollDetailId)
                .orElseThrow(() -> new RuntimeException("Payroll detail not found"));
    }
    
    public PayrollDetail markAsPaid(UUID payrollDetailId, String paymentReference) {
        PayrollDetail detail = payrollDetailRepository.findById(payrollDetailId)
                .orElseThrow(() -> new RuntimeException("Payroll detail not found"));
        
        detail.setStatus("paid");
        detail.setPaymentReference(paymentReference);
        detail.setPaidAt(LocalDateTime.now());
        
        return payrollDetailRepository.save(detail);
    }

    private Optional<Employee> resolveProcessor(UUID organizationId, UUID identifier) {
        if (identifier == null) {
            return Optional.empty();
        }

        Optional<Employee> byEmployeeId = employeeRepository.findById(identifier)
                .filter(emp -> organizationId.equals(emp.getOrganizationId()));
        if (byEmployeeId.isPresent()) {
            return byEmployeeId;
        }

        return employeeRepository.findByOrganizationIdAndUserId(organizationId, identifier);
    }
}

