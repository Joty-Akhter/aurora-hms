package com.easyops.hr.service;

import com.easyops.hr.dto.EpfPayrollContributionResult;
import com.easyops.hr.entity.*;
import com.easyops.hr.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Creates EPF contributions from payroll run PF components.
 * Extracts PF_EMPLOYEE and PF_EMPLOYER amounts from payroll_components and creates/updates EpfContribution records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollEpfService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final EpfAccountRepository epfAccountRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EmployeeRepository employeeRepository;
    private final ProvidentFundService providentFundService;
    private final SalaryService salaryService;
    private final EpfPayrollCalculationService epfPayrollCalculationService;

    private static final Set<String> PF_EMPLOYEE_KEYS = Set.of("PF_EMPLOYEE", "PF_EMP");
    private static final Set<String> PF_EMPLOYER_KEYS = Set.of("PF_EMPLOYER", "PF_EMPR");

    /**
     * Create or update EPF contributions from payroll run PF components.
     * For each employee with PF deduction in payroll: get/create EPF account, create or update contribution.
     * @return summary: contributionsCreated, contributionsUpdated, employeesSkipped (no EPF account), errors
     */
    public Map<String, Object> createEpfContributionsFromPayroll(UUID payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found"));
        if (!"PROCESSED".equals(run.getStatus()) && !"APPROVED".equals(run.getStatus())) {
            throw new IllegalStateException("Payroll run must be PROCESSED or APPROVED before creating EPF contributions");
        }
        UUID orgId = run.getOrganizationId();
        LocalDate periodEnd = run.getPayPeriodEnd() != null ? run.getPayPeriodEnd() : run.getPayPeriodStart();
        int month = periodEnd.getMonthValue();
        int year = periodEnd.getYear();
        LocalDate periodStart = run.getPayPeriodStart() != null ? run.getPayPeriodStart() : periodEnd;

        Map<UUID, SalaryComponent> componentById = new HashMap<>();
        for (SalaryComponent sc : salaryComponentRepository.findByOrganizationId(orgId)) {
            componentById.put(sc.getComponentId(), sc);
        }

        Map<UUID, PayrollPfBundle> pfByEmployee = new HashMap<>();
        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(payrollRunId);
        for (PayrollDetail pd : details) {
            List<PayrollComponent> pcs = payrollComponentRepository.findByPayrollDetailId(pd.getPayrollDetailId());
            BigDecimal empAmount = BigDecimal.ZERO;
            BigDecimal emprAmount = BigDecimal.ZERO;
            BigDecimal basicSalary = pd.getBasicSalary() != null ? pd.getBasicSalary() : BigDecimal.ZERO;

            for (PayrollComponent pc : pcs) {
                SalaryComponent sc = componentById.get(pc.getComponentId());
                if (sc == null) continue;
                String code = sc.getCode() != null ? sc.getCode().toUpperCase() : "";
                String statutoryType = sc.getStatutoryType() != null ? sc.getStatutoryType().toUpperCase() : "";
                BigDecimal amt = pc.getAmount() != null ? pc.getAmount() : BigDecimal.ZERO;

                if (PF_EMPLOYEE_KEYS.contains(code) || "PF_EMPLOYEE".equals(statutoryType)) {
                    empAmount = empAmount.add(amt);
                } else if (PF_EMPLOYER_KEYS.contains(code) || "PF_EMPLOYER".equals(statutoryType)) {
                    emprAmount = emprAmount.add(amt);
                }
            }

            if (empAmount.compareTo(BigDecimal.ZERO) > 0 || emprAmount.compareTo(BigDecimal.ZERO) > 0) {
                pfByEmployee.put(pd.getEmployeeId(), new PayrollPfBundle(new PayrollPfAmounts(empAmount, emprAmount, basicSalary), pcs));
            }
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Map.Entry<UUID, PayrollPfBundle> e : pfByEmployee.entrySet()) {
            UUID employeeId = e.getKey();
            PayrollPfAmounts pf = e.getValue().amounts();
            List<PayrollComponent> pcs = e.getValue().components();
            try {
                EpfAccount account = getOrCreateEpfAccount(employeeId, orgId);
                if (account == null) {
                    skipped++;
                    errors.add("Employee " + employeeId + ": Could not create EPF account");
                    continue;
                }

                EpfPayrollContributionResult policyResult = epfResultForEmployee(employeeId, orgId, periodEnd, pcs, componentById);

                Optional<EpfContribution> existing = epfContributionRepository
                        .findByEpfAccountIdAndContributionMonthAndContributionYear(account.getEpfAccountId(), month, year);

                if (existing.isPresent()) {
                    EpfContribution c = existing.get();
                    c.setEmployeeContributionAmount(pf.employeeAmount);
                    c.setEmployerContributionAmount(pf.employerAmount);
                    c.setTotalContribution(pf.employeeAmount.add(pf.employerAmount));
                    c.setEmployeeBasicSalary(pf.basicSalary);
                    applyPolicyRatesAndPfWage(c, policyResult);
                    c.setPayrollRunId(payrollRunId);
                    c.setOrganizationId(orgId);
                    c.setContributionPeriodStart(periodStart);
                    c.setContributionPeriodEnd(periodEnd);
                    c.setContributionMonth(month);
                    c.setContributionYear(year);
                    c.setStatus("processed");
                    epfContributionRepository.save(c);
                    providentFundService.updateEpfAccountBalanceFromContributions(account.getEpfAccountId());
                    updated++;
                } else {
                    EpfContribution contribution = EpfContribution.builder()
                            .epfAccountId(account.getEpfAccountId())
                            .employeeId(employeeId)
                            .organizationId(orgId)
                            .contributionPeriodStart(periodStart)
                            .contributionPeriodEnd(periodEnd)
                            .contributionMonth(month)
                            .contributionYear(year)
                            .employeeBasicSalary(pf.basicSalary)
                            .employeeContributionRate(policyResult != null ? policyResult.getEmployeeRatePercent() : new BigDecimal("12.00"))
                            .employeeContributionAmount(pf.employeeAmount)
                            .employerContributionRate(policyResult != null ? policyResult.getEmployerRatePercent() : new BigDecimal("12.00"))
                            .employerContributionAmount(pf.employerAmount)
                            .employerEpfAmount(policyResult != null ? policyResult.getEmployerEpfAmount() : BigDecimal.ZERO)
                            .employerPensionAmount(policyResult != null ? policyResult.getEmployerPensionAmount() : BigDecimal.ZERO)
                            .employerEdliAmount(policyResult != null ? policyResult.getEmployerEdliAmount() : BigDecimal.ZERO)
                            .employerAdminChargeAmount(policyResult != null ? policyResult.getEmployerAdminChargeAmount() : BigDecimal.ZERO)
                            .totalContribution(pf.employeeAmount.add(pf.employerAmount))
                            .payrollRunId(payrollRunId)
                            .status("processed")
                            .processedDate(LocalDate.now())
                            .build();
                    applyPolicyRatesAndPfWage(contribution, policyResult);
                    epfContributionRepository.save(contribution);
                    providentFundService.updateEpfAccountBalanceFromContributions(account.getEpfAccountId());
                    created++;
                }
            } catch (Exception ex) {
                errors.add("Employee " + employeeId + ": " + ex.getMessage());
                log.warn("Failed to create EPF contribution for employee {}", employeeId, ex);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("contributionsCreated", created);
        result.put("contributionsUpdated", updated);
        result.put("employeesSkipped", skipped);
        result.put("errors", errors);
        result.put("month", month);
        result.put("year", year);
        log.info("EPF from payroll: run={}, created={}, updated={}, skipped={}", payrollRunId, created, updated, skipped);
        return result;
    }

    private EpfAccount getOrCreateEpfAccount(UUID employeeId, UUID organizationId) {
        List<EpfAccount> accounts = epfAccountRepository.findByEmployeeId(employeeId).stream()
                .filter(a -> organizationId.equals(a.getOrganizationId()))
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .toList();
        if (!accounts.isEmpty()) {
            return accounts.get(0);
        }
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        if (emp == null) return null;
        String epfNumber = "EPF-" + emp.getEmployeeNumber();
        if (epfAccountRepository.findByOrganizationIdAndEpfAccountNumber(organizationId, epfNumber).isPresent()) {
            epfNumber = "EPF-" + emp.getEmployeeNumber() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        EpfAccount account = EpfAccount.builder()
                .employeeId(employeeId)
                .organizationId(organizationId)
                .epfAccountNumber(epfNumber)
                .accountStatus("active")
                .openingDate(LocalDate.now())
                .isActive(true)
                .currentBalance(BigDecimal.ZERO)
                .employeeContributionBalance(BigDecimal.ZERO)
                .employerContributionBalance(BigDecimal.ZERO)
                .interestBalance(BigDecimal.ZERO)
                .build();
        return epfAccountRepository.save(account);
    }

    private EpfPayrollContributionResult epfResultForEmployee(
            UUID employeeId,
            UUID orgId,
            LocalDate periodEnd,
            List<PayrollComponent> pcs,
            Map<UUID, SalaryComponent> componentById) {
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        if (emp == null) {
            return null;
        }
        Map<String, BigDecimal> computedByCode = new HashMap<>();
        for (PayrollComponent pc : pcs) {
            SalaryComponent sc = componentById.get(pc.getComponentId());
            if (sc != null && sc.getCode() != null) {
                computedByCode.put(sc.getCode(), pc.getAmount() != null ? pc.getAmount() : BigDecimal.ZERO);
            }
        }
        List<SalaryComponent> order = salaryService.getComponentsInDependencyOrder(orgId, periodEnd);
        return epfPayrollCalculationService.computeContributionsForPayroll(emp, orgId, order, computedByCode);
    }

    private void applyPolicyRatesAndPfWage(EpfContribution c, EpfPayrollContributionResult policyResult) {
        if (policyResult == null) {
            return;
        }
        if (policyResult.getEmployeeRatePercent() != null) {
            c.setEmployeeContributionRate(policyResult.getEmployeeRatePercent());
        }
        if (policyResult.getEmployerRatePercent() != null) {
            c.setEmployerContributionRate(policyResult.getEmployerRatePercent());
        }
        if (policyResult.getPfWageAfterCeiling() != null) {
            c.setPfWageBase(policyResult.getPfWageAfterCeiling());
        }
        c.setEmployerEpfAmount(policyResult.getEmployerEpfAmount() != null ? policyResult.getEmployerEpfAmount() : BigDecimal.ZERO);
        c.setEmployerPensionAmount(policyResult.getEmployerPensionAmount() != null ? policyResult.getEmployerPensionAmount() : BigDecimal.ZERO);
        c.setEmployerEdliAmount(policyResult.getEmployerEdliAmount() != null ? policyResult.getEmployerEdliAmount() : BigDecimal.ZERO);
        c.setEmployerAdminChargeAmount(policyResult.getEmployerAdminChargeAmount() != null ? policyResult.getEmployerAdminChargeAmount() : BigDecimal.ZERO);
    }

    private record PayrollPfAmounts(BigDecimal employeeAmount, BigDecimal employerAmount, BigDecimal basicSalary) {}

    private record PayrollPfBundle(PayrollPfAmounts amounts, List<PayrollComponent> components) {}
}
