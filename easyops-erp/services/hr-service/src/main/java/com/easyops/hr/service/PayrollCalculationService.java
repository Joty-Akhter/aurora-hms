package com.easyops.hr.service;

import com.easyops.hr.dto.PayrollAttendanceRollup;
import com.easyops.hr.entity.Bonus;
import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.dto.PayslipDto;
import com.easyops.hr.dto.PayrollAccountingExportDto;
import com.easyops.hr.dto.PayrollAccountingLineDto;
import com.easyops.hr.integration.IntegrationCorrelationIdHolder;
import com.easyops.hr.dto.EpfPayrollContributionResult;
import com.easyops.hr.dto.PayrollPopulationSummaryDto;
import com.easyops.hr.entity.*;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.EmployeeSalaryAssignmentRepository;
import com.easyops.hr.repository.EmployeeSalaryDetailRepository;
import com.easyops.hr.repository.PayrollComponentRepository;
import com.easyops.hr.repository.PayrollDetailRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import com.easyops.hr.repository.SalaryStructureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ES-22–ES-27: Payroll calculation from employee salary (assignment + component values).
 * Resolves components in dependency order; computes total earnings, deductions, net; stores in payroll result.
 * Recalculation only for open period (DRAFT).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollCalculationService {

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryService salaryService;
    private final EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    private final EmployeeSalaryAssignmentRepository employeeSalaryAssignmentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final LoanPayrollRecoveryService loanPayrollRecoveryService;
    private final PayrollAttendanceRollupService payrollAttendanceRollupService;
    private final EpfPayrollCalculationService epfPayrollCalculationService;
    private final StatutoryPayrollCalculationService statutoryPayrollCalculationService;
    private final PayrollTimeAttendancePolicyService payrollTimeAttendancePolicyService;
    private final OvertimePayrollRuleResolver overtimePayrollRuleResolver;
    private final CompensationService compensationService;

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    /** Default OT multiplier when policy is absent. */
    private static final BigDecimal DEFAULT_OVERTIME_RATE_MULTIPLIER = new BigDecimal("1.5");
    /** Optional salary component codes for payslip/accounting lines when OT/LOP/bonus amounts are applied. */
    private static final String OPTIONAL_OT_COMPONENT_CODE = "OT_PAY";
    private static final String OPTIONAL_LOP_COMPONENT_CODE = "LOP_DED";
    private static final String OPTIONAL_BONUS_COMPONENT_CODE = "BONUS_PAY";

    /**
     * ES-27 / INT-01–INT-08: Populate payroll from salary (assignment + component values) and component master.
     * Only when run status is DRAFT (recalculation only for open period). Returns summary with error handling info.
     */
    public PayrollPopulationSummaryDto populatePayrollFromSalary(UUID payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found"));
        String status = run.getStatus();
        if (status != null && !"DRAFT".equalsIgnoreCase(status.trim())) {
            throw new IllegalStateException("ES-27: Recalculation only for open period. Payroll run must be in DRAFT status. Current: " + status);
        }
        UUID orgId = run.getOrganizationId();
        LocalDate periodStart = run.getPayPeriodStart();
        LocalDate tmpPeriodEnd = run.getPayPeriodEnd();
        if (tmpPeriodEnd == null) {
            tmpPeriodEnd = periodStart;
        }
        final LocalDate periodEnd = tmpPeriodEnd;
        final LocalDate periodStartFinal = periodStart;

        List<PayrollDetail> existing = payrollDetailRepository.findByPayrollRunId(payrollRunId);
        for (PayrollDetail pd : existing) {
            payrollComponentRepository.deleteByPayrollDetailId(pd.getPayrollDetailId());
        }
        payrollDetailRepository.deleteAll(existing);

        List<Employee> allEmployees = employeeRepository.findByOrganizationId(orgId);
        List<Employee> inPeriod = allEmployees.stream()
                .filter(e -> e.getEmploymentStatus() != null && "ACTIVE".equalsIgnoreCase(e.getEmploymentStatus().trim()))
                .filter(e -> e.getHireDate() != null && !e.getHireDate().isAfter(periodEnd))
                .filter(e -> e.getTerminationDate() == null || !e.getTerminationDate().isBefore(periodStartFinal))
                .toList();

        if (inPeriod.isEmpty()) {
            log.info("No active employees in organization {} for payroll period {} to {}", orgId, periodStartFinal, periodEnd);
            run.setEmployeeCount(0);
            run.setTotalGrossPay(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            run.setTotalDeductions(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            run.setTotalNetPay(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            payrollRunRepository.save(run);
            return PayrollPopulationSummaryDto.builder()
                    .payrollRunId(payrollRunId)
                    .employeesPopulated(0)
                    .employeesWithoutAssignment(List.of())
                    .employeesMissingBasic(List.of())
                    .warnings(List.of())
                    .build();
        }

        // NF-03: Batch-load assignments and salary details for all in-period employees to avoid N+1 queries.
        List<UUID> employeeIds = inPeriod.stream().map(Employee::getEmployeeId).toList();

        List<EmployeeSalaryAssignment> assignments = employeeSalaryAssignmentRepository
                .findActiveByOrganizationAndEmployeeIdsAndDate(orgId, employeeIds, periodEnd);
        Map<UUID, EmployeeSalaryAssignment> assignmentByEmployeeId = assignments.stream()
                .collect(Collectors.toMap(EmployeeSalaryAssignment::getEmployeeId, a -> a, (a, b) ->
                        a.getEffectiveFrom() != null && b.getEffectiveFrom() != null && a.getEffectiveFrom().isAfter(b.getEffectiveFrom()) ? a : b));

        List<EmployeeSalaryDetail> allDetails = employeeSalaryDetailRepository
                .findActiveByOrganizationAndEmployeeIdsAndEffectiveOnDate(orgId, employeeIds, periodEnd);
        Map<UUID, List<EmployeeSalaryDetail>> detailsByEmployeeId = allDetails.stream()
                .collect(Collectors.groupingBy(EmployeeSalaryDetail::getEmployeeId));

        List<SalaryComponent> componentsInOrder = salaryService.getComponentsInDependencyOrder(orgId, periodEnd);
        if (componentsInOrder.isEmpty()) {
            log.warn("INT-01–INT-08: No salary components configured for organization {} on {}", orgId, periodEnd);
            run.setEmployeeCount(0);
            run.setTotalGrossPay(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            run.setTotalDeductions(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            run.setTotalNetPay(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            payrollRunRepository.save(run);
            return PayrollPopulationSummaryDto.builder()
                    .payrollRunId(payrollRunId)
                    .employeesPopulated(0)
                    .employeesWithoutAssignment(List.of())
                    .employeesMissingBasic(List.of())
                    .warnings(List.of())
                    .build();
        }

        epfPayrollCalculationService.ensureEpfPolicyRowExistsIfRequired(orgId);
        statutoryPayrollCalculationService.ensureTaxSlabsExistIfRequired(orgId, periodEnd, componentsInOrder);
        PayrollTimeAttendancePolicy taPolicy = payrollTimeAttendancePolicyService.getOrCreate(orgId);
        salaryService.ensureAttendancePayrollLineComponents(orgId);

        // HR-PY-03: Pre-load approved bonuses for the period (paymentDate in range, not yet in a run).
        Map<UUID, List<Bonus>> approvedBonusesByEmployee = compensationService
                .getApprovedBonusesForPeriod(orgId, periodStartFinal, periodEnd)
                .stream()
                .collect(Collectors.groupingBy(Bonus::getEmployeeId));
        List<UUID> processedBonusIds = new ArrayList<>();

        int count = 0;
        List<UUID> employeesWithoutAssignment = new ArrayList<>();
        List<UUID> employeesMissingBasic = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (Boolean.TRUE.equals(taPolicy.getLeavePayrollBridgeEnabled())) {
            warnings.add(
                    "Leave–payroll bridge is enabled (Phase B / HR-LV-03): LOP from attendance is reconciled with approved paid leave; "
                            + "review payslip working-day breakdown where attendance was dual-entered.");
        }
        boolean orgHasStatutoryPf = componentsInOrder.stream().anyMatch(EpfPayrollCalculationService::isStatutoryPfLine);

        for (Employee emp : inPeriod) {
            EmployeeSalaryAssignment assignment = assignmentByEmployeeId.get(emp.getEmployeeId());
            if (assignment == null) {
                log.warn("INT-01–INT-08: Skipping employee {} - no salary assignment as of {}", emp.getEmployeeId(), periodEnd);
                employeesWithoutAssignment.add(emp.getEmployeeId());
                continue;
            }
            List<EmployeeSalaryDetail> details = detailsByEmployeeId.getOrDefault(emp.getEmployeeId(), List.of());
            Map<UUID, EmployeeSalaryDetail> detailByComponentId = details.stream().collect(Collectors.toMap(EmployeeSalaryDetail::getComponentId, d -> d, (a, b) -> a));
            Map<String, BigDecimal> computedByCode = new LinkedHashMap<>();
            BigDecimal grossSoFar = BigDecimal.ZERO;
            BigDecimal totalEarnings = BigDecimal.ZERO;
            BigDecimal totalDeductions = BigDecimal.ZERO;
            List<PayrollComponentLine> lines = new ArrayList<>();
            boolean basicComponentPresent = false;

            for (SalaryComponent comp : componentsInOrder) {
                // Phase 4: loan recovery is computed from loan installments, not from salary detail rows (avoid double count).
                if (SalaryComponentCategory.LOAN_REPAYMENT.equals(comp.getCategory())) {
                    continue;
                }
                // INT-16/INT-17: Other statutory (tax, ESI, …) deferred until OT/LOP merged unless explicit AMOUNT.
                if (StatutoryPayrollCalculationService.isDeferredStatutoryNonPfLine(comp)) {
                    EmployeeSalaryDetail stDetail = detailByComponentId.get(comp.getComponentId());
                    if (stDetail != null && ComponentValueType.AMOUNT.equals(stDetail.getValueType()) && stDetail.getAmount() != null) {
                        BigDecimal amount = stDetail.getAmount().setScale(SCALE, ROUNDING);
                        amount = applyCeilingFloor(amount, comp);
                        amount = applyPayPeriodProration(emp, periodStartFinal, periodEnd, comp, amount);
                        boolean isEarning = "EARNING".equalsIgnoreCase(comp.getComponentType());
                        if (isEarning) {
                            totalEarnings = totalEarnings.add(amount);
                            grossSoFar = totalEarnings;
                        } else if (StatutoryPayrollCalculationService.reducesEmployeeNet(comp)) {
                            totalDeductions = totalDeductions.add(amount);
                        }
                        if (SalaryComponentCategory.BASIC.equals(comp.getCategory()) && isEarning) {
                            basicComponentPresent = true;
                        }
                        computedByCode.put(comp.getCode(), amount);
                        lines.add(new PayrollComponentLine(comp, amount));
                    } else {
                        lines.add(new PayrollComponentLine(comp, null));
                    }
                    continue;
                }
                // INT-09/INT-10: Statutory PF lines use PF wage engine unless employee has explicit AMOUNT override.
                if (EpfPayrollCalculationService.isStatutoryPfLine(comp)) {
                    EmployeeSalaryDetail pfDetail = detailByComponentId.get(comp.getComponentId());
                    if (pfDetail != null && ComponentValueType.AMOUNT.equals(pfDetail.getValueType()) && pfDetail.getAmount() != null) {
                        BigDecimal amount = pfDetail.getAmount().setScale(SCALE, ROUNDING);
                        amount = applyCeilingFloor(amount, comp);
                        amount = applyPayPeriodProration(emp, periodStartFinal, periodEnd, comp, amount);
                        boolean isEarning = "EARNING".equalsIgnoreCase(comp.getComponentType());
                        if (isEarning) {
                            totalEarnings = totalEarnings.add(amount);
                            grossSoFar = totalEarnings;
                        } else if (EpfPayrollCalculationService.isStatutoryPfEmployerLine(comp)) {
                            // Employer PF is informational on payslip; does not reduce employee net pay.
                        } else {
                            totalDeductions = totalDeductions.add(amount);
                        }
                        if (SalaryComponentCategory.BASIC.equals(comp.getCategory()) && isEarning) {
                            basicComponentPresent = true;
                        }
                        computedByCode.put(comp.getCode(), amount);
                        lines.add(new PayrollComponentLine(comp, amount));
                    } else {
                        lines.add(new PayrollComponentLine(comp, null));
                    }
                    continue;
                }
                EmployeeSalaryDetail detail = detailByComponentId.get(comp.getComponentId());
                BigDecimal amount = computeComponentAmount(comp, detail, computedByCode, grossSoFar);
                amount = applyCeilingFloor(amount, comp);
                boolean isEarning = "EARNING".equalsIgnoreCase(comp.getComponentType());
                if (isEarning) {
                    totalEarnings = totalEarnings.add(amount);
                    grossSoFar = totalEarnings;
                } else {
                    totalDeductions = totalDeductions.add(amount);
                }
                if (SalaryComponentCategory.BASIC.equals(comp.getCategory()) && isEarning) {
                    basicComponentPresent = true;
                }
                computedByCode.put(comp.getCode(), amount);
                lines.add(new PayrollComponentLine(comp, amount));
            }

            Optional<SalaryComponent> loanRepaymentMaster = componentsInOrder.stream()
                    .filter(c -> SalaryComponentCategory.LOAN_REPAYMENT.equals(c.getCategory()))
                    .filter(c -> "DEDUCTION".equalsIgnoreCase(c.getComponentType()))
                    .findFirst();
            if (loanRepaymentMaster.isPresent()) {
                BigDecimal loanRecovery = loanPayrollRecoveryService.getRecoveryAmountForEmployee(
                        orgId, emp.getEmployeeId(), periodEnd, payrollRunId);
                if (loanRecovery.compareTo(BigDecimal.ZERO) > 0) {
                    SalaryComponent lr = loanRepaymentMaster.get();
                    totalDeductions = totalDeductions.add(loanRecovery);
                    computedByCode.put(lr.getCode(), loanRecovery);
                    lines.add(new PayrollComponentLine(lr, loanRecovery));
                }
            }

            BigDecimal basicAmount = computedByCode.entrySet().stream()
                    .filter(e -> {
                        SalaryComponent c = componentsInOrder.stream().filter(x -> x.getCode().equals(e.getKey())).findFirst().orElse(null);
                        return c != null && SalaryComponentCategory.BASIC.equals(c.getCategory()) && "EARNING".equalsIgnoreCase(c.getComponentType());
                    })
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            if (!basicComponentPresent) {
                log.warn("INT-01–INT-08: Employee {} has no Basic earning component in salary for period ending {}. basicSalary will be 0 in payroll.", emp.getEmployeeId(), periodEnd);
                employeesMissingBasic.add(emp.getEmployeeId());
            }

            PayrollAttendanceRollup rollup = payrollAttendanceRollupService.rollupForPayPeriod(
                    orgId, emp, periodStartFinal, periodEnd, taPolicy);
            BigDecimal overtimeAmount = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
            BigDecimal lopAmountMoney = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
            if (rollup.hasAttendanceOrTimesheetData() && rollup.workingDays() > 0) {
                BigDecimal stdHours = overtimePayrollRuleResolver.resolveStandardHoursPerDay(
                        emp, periodStartFinal, periodEnd, taPolicy);
                BigDecimal otMult = overtimePayrollRuleResolver.resolveOvertimeRateMultiplier(
                        emp, periodStartFinal, periodEnd, taPolicy);
                overtimeAmount = computeOvertimePay(basicAmount, rollup.workingDays(), rollup.overtimeHours(), stdHours, otMult);
                lopAmountMoney = computeLopDeduction(basicAmount, rollup.workingDays(), rollup.lopDays());
            }
            totalEarnings = totalEarnings.add(overtimeAmount);
            totalDeductions = totalDeductions.add(lopAmountMoney);

            final BigDecimal overtimeAmountForLines = overtimeAmount;
            final BigDecimal lopAmountForLines = lopAmountMoney;
            if (overtimeAmountForLines.compareTo(BigDecimal.ZERO) > 0) {
                salaryComponentRepository.findByOrganizationIdAndCode(orgId, OPTIONAL_OT_COMPONENT_CODE).ifPresent(c -> {
                    if ("EARNING".equalsIgnoreCase(c.getComponentType())) {
                        lines.add(new PayrollComponentLine(c, overtimeAmountForLines));
                        computedByCode.put(c.getCode(), overtimeAmountForLines);
                    }
                });
            }
            if (lopAmountForLines.compareTo(BigDecimal.ZERO) > 0) {
                salaryComponentRepository.findByOrganizationIdAndCode(orgId, OPTIONAL_LOP_COMPONENT_CODE).ifPresent(c -> {
                    if ("DEDUCTION".equalsIgnoreCase(c.getComponentType())) {
                        lines.add(new PayrollComponentLine(c, lopAmountForLines));
                        computedByCode.put(c.getCode(), lopAmountForLines);
                    }
                });
            }

            boolean hasDeferredPf = lines.stream()
                    .anyMatch(l -> l.amount == null && EpfPayrollCalculationService.isStatutoryPfLine(l.component));
            if (hasDeferredPf) {
                EpfPayrollContributionResult pfResult = epfPayrollCalculationService.computeContributionsForPayroll(
                        emp, orgId, componentsInOrder, computedByCode);
                if (orgHasStatutoryPf && pfResult.getPfWageBeforeCeiling() != null
                        && pfResult.getPfWageBeforeCeiling().compareTo(BigDecimal.ZERO) == 0) {
                    warnings.add("Employee " + emp.getEmployeeId()
                            + ": PF wage base is 0 while PF statutory components exist. Tag earning lines with PF_WAGE or check amounts; statutory PF lines use 0 unless manual amounts override.");
                }
                for (int i = 0; i < lines.size(); i++) {
                    PayrollComponentLine line = lines.get(i);
                    if (line.amount != null || !EpfPayrollCalculationService.isStatutoryPfLine(line.component)) {
                        continue;
                    }
                    BigDecimal amt = epfPayrollCalculationService.amountForStatutoryPfLine(line.component, pfResult);
                    lines.set(i, new PayrollComponentLine(line.component, amt));
                    computedByCode.put(line.component.getCode(), amt);
                    if (EpfPayrollCalculationService.isStatutoryPfEmployeeLine(line.component)) {
                        totalDeductions = totalDeductions.add(amt);
                    }
                }
            }

            boolean hasDeferredOtherStatutory = lines.stream()
                    .anyMatch(l -> l.amount == null && StatutoryPayrollCalculationService.isDeferredStatutoryNonPfLine(l.component));
            if (hasDeferredOtherStatutory) {
                for (int i = 0; i < lines.size(); i++) {
                    PayrollComponentLine line = lines.get(i);
                    if (line.amount != null || !StatutoryPayrollCalculationService.isDeferredStatutoryNonPfLine(line.component)) {
                        continue;
                    }
                    BigDecimal amt = statutoryPayrollCalculationService.computeStatutoryLineAmount(
                            orgId, periodEnd, line.component, componentsInOrder, computedByCode);
                    amt = applyCeilingFloor(amt, line.component);
                    lines.set(i, new PayrollComponentLine(line.component, amt));
                    computedByCode.put(line.component.getCode(), amt);
                    if (StatutoryPayrollCalculationService.reducesEmployeeNet(line.component)) {
                        totalDeductions = totalDeductions.add(amt);
                    }
                }
            }

            // HR-PY-03: Include approved bonus lines for this employee in the period.
            List<Bonus> empBonuses = approvedBonusesByEmployee.getOrDefault(emp.getEmployeeId(), List.of());
            BigDecimal bonusTotalForEmployee = empBonuses.stream()
                    .map(Bonus::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (bonusTotalForEmployee.compareTo(BigDecimal.ZERO) > 0) {
                totalEarnings = totalEarnings.add(bonusTotalForEmployee);
                final BigDecimal bonusTotalFinal = bonusTotalForEmployee;
                salaryComponentRepository.findByOrganizationIdAndCode(orgId, OPTIONAL_BONUS_COMPONENT_CODE).ifPresent(c -> {
                    if ("EARNING".equalsIgnoreCase(c.getComponentType())) {
                        lines.add(new PayrollComponentLine(c, bonusTotalFinal));
                    }
                });
                empBonuses.forEach(b -> processedBonusIds.add(b.getBonusId()));
            }

            PayrollDetail pd = new PayrollDetail();
            pd.setPayrollRunId(payrollRunId);
            pd.setEmployeeId(emp.getEmployeeId());
            pd.setOrganizationId(orgId);
            pd.setBasicSalary(basicAmount);
            pd.setGrossSalary(totalEarnings);
            pd.setTotalDeductions(totalDeductions);
            pd.setTotalReimbursements(BigDecimal.ZERO);
            pd.setNetSalary(totalEarnings.subtract(totalDeductions).setScale(SCALE, ROUNDING));
            pd.setStatus("pending");
            if (rollup.hasAttendanceOrTimesheetData()) {
                pd.setWorkingDays(rollup.workingDays());
                pd.setPresentDays(rollup.presentDays());
                pd.setLeaveDays(rollup.leaveDays());
                pd.setOvertimeHours(rollup.overtimeHours());
                pd.setOvertimeAmount(overtimeAmount.compareTo(BigDecimal.ZERO) > 0 ? overtimeAmount : null);
                pd.setLopDays(rollup.lopDays());
                pd.setLopAmount(lopAmountMoney.compareTo(BigDecimal.ZERO) > 0 ? lopAmountMoney : null);
            }
            pd.setBonusAmount(bonusTotalForEmployee.compareTo(BigDecimal.ZERO) > 0 ? bonusTotalForEmployee : null);
            pd = payrollDetailRepository.save(pd);

            int order = 0;
            for (PayrollComponentLine line : lines) {
                if (line.amount == null) {
                    throw new IllegalStateException("Unresolved statutory line for component " + line.component.getCode());
                }
                PayrollComponent pc = new PayrollComponent();
                pc.setPayrollDetailId(pd.getPayrollDetailId());
                pc.setComponentId(line.component.getComponentId());
                pc.setComponentType(line.component.getComponentType());
                pc.setDisplayOrder(line.component.getDisplayOrder() != null ? line.component.getDisplayOrder() : order);
                pc.setAmount(line.amount);
                pc.setIsTaxable(line.component.getIsTaxable() != null ? line.component.getIsTaxable() : true);
                payrollComponentRepository.save(pc);
                order++;
            }
            count++;
        }

        run.setEmployeeCount(count);
        List<PayrollDetail> detailRows = payrollDetailRepository.findByPayrollRunId(payrollRunId);
        BigDecimal sumGross = detailRows.stream()
                .map(PayrollDetail::getGrossSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumDed = detailRows.stream()
                .map(PayrollDetail::getTotalDeductions)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumNet = detailRows.stream()
                .map(PayrollDetail::getNetSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        run.setTotalGrossPay(sumGross.setScale(SCALE, ROUNDING));
        run.setTotalDeductions(sumDed.setScale(SCALE, ROUNDING));
        run.setTotalNetPay(sumNet.setScale(SCALE, ROUNDING));
        payrollRunRepository.save(run);
        if (!processedBonusIds.isEmpty()) {
            compensationService.linkBonusesToPayrollRun(payrollRunId, processedBonusIds);
        }
        log.info("Populated payroll run {} with {} employees from salary. employeesWithoutAssignment={}, employeesMissingBasic={}",
                payrollRunId, count, employeesWithoutAssignment.size(), employeesMissingBasic.size());

        if (count > 0 && orgHasStatutoryPf) {
            warnings.add("ES-28: PF wage uses PF_WAGE-tagged earning lines after per-component pay-period proration. EPF policy ceiling still applies to PF wage.");
        }

        return PayrollPopulationSummaryDto.builder()
                .payrollRunId(payrollRunId)
                .employeesPopulated(count)
                .employeesWithoutAssignment(employeesWithoutAssignment)
                .employeesMissingBasic(employeesMissingBasic)
                .warnings(warnings)
                .build();
    }

    /**
     * INT-19–INT-23: Expose payroll results (by component and summary) for accounting integration.
     * Returns both detail lines (per employee/component) and aggregated totals per component.
     */
    @Transactional(readOnly = true)
    public PayrollAccountingExportDto getPayrollAccountingExport(UUID payrollRunId) {
        PayrollRun run = payrollRunRepository.findById(payrollRunId)
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found"));
        UUID orgId = run.getOrganizationId();

        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(payrollRunId);
        if (details.isEmpty()) {
            return PayrollAccountingExportDto.builder()
                    .organizationId(orgId)
                    .payrollRunId(payrollRunId)
                    .correlationId(IntegrationCorrelationIdHolder.get())
                    .idempotencyKey("PAYROLL-" + payrollRunId)
                    .payPeriodStart(run.getPayPeriodStart())
                    .payPeriodEnd(run.getPayPeriodEnd())
                    .totalGross(run.getTotalGrossPay())
                    .totalDeductions(run.getTotalDeductions())
                    .totalNet(run.getTotalNetPay())
                    .detailLines(List.of())
                    .summaryByComponent(List.of())
                    .build();
        }

        Map<UUID, PayrollDetail> detailById = details.stream()
                .collect(Collectors.toMap(PayrollDetail::getPayrollDetailId, d -> d));

        List<UUID> detailIds = details.stream().map(PayrollDetail::getPayrollDetailId).toList();
        List<PayrollComponent> components = new ArrayList<>();
        for (UUID detailId : detailIds) {
            components.addAll(payrollComponentRepository.findByPayrollDetailId(detailId));
        }

        Set<UUID> componentIds = components.stream()
                .map(PayrollComponent::getComponentId)
                .collect(Collectors.toSet());
        Map<UUID, SalaryComponent> salaryComponentById = salaryComponentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(SalaryComponent::getComponentId, c -> c, (a, b) -> a));

        List<PayrollAccountingLineDto> detailLines = new ArrayList<>();
        Map<String, PayrollAccountingLineDto> summaryByComponentMap = new LinkedHashMap<>();

        for (PayrollComponent pc : components) {
            PayrollDetail pd = detailById.get(pc.getPayrollDetailId());
            if (pd == null) continue;
            SalaryComponent sc = salaryComponentById.get(pc.getComponentId());

            String code = sc != null ? sc.getCode() : null;
            String type = sc != null ? sc.getComponentType() : pc.getComponentType();
            String category = sc != null && sc.getCategory() != null ? sc.getCategory().name() : null;
            String expenseGl = sc != null ? sc.getExpenseAccountCode() : null;
            String liabilityGl = sc != null ? sc.getLiabilityAccountCode() : null;

            PayrollAccountingLineDto detailLine = PayrollAccountingLineDto.builder()
                    .employeeId(pd.getEmployeeId())
                    .employeeNumber(null)
                    .componentId(pc.getComponentId())
                    .componentCode(code)
                    .componentType(type)
                    .category(category)
                    .amount(pc.getAmount())
                    .expenseAccountCode(expenseGl)
                    .liabilityAccountCode(liabilityGl)
                    .build();
            detailLines.add(detailLine);

            String summaryKey = (code != null ? code : pc.getComponentId().toString()) + "|" + type + "|" + category;
            PayrollAccountingLineDto summaryLine = summaryByComponentMap.get(summaryKey);
            if (summaryLine == null) {
                summaryLine = PayrollAccountingLineDto.builder()
                        .employeeId(null)
                        .employeeNumber(null)
                        .componentId(pc.getComponentId())
                        .componentCode(code)
                        .componentType(type)
                        .category(category)
                        .amount(pc.getAmount())
                        .expenseAccountCode(expenseGl)
                        .liabilityAccountCode(liabilityGl)
                        .build();
            } else {
                summaryLine.setAmount(summaryLine.getAmount().add(pc.getAmount()));
            }
            summaryByComponentMap.put(summaryKey, summaryLine);
        }

        return PayrollAccountingExportDto.builder()
                .organizationId(orgId)
                .payrollRunId(payrollRunId)
                .correlationId(IntegrationCorrelationIdHolder.get())
                .idempotencyKey("PAYROLL-" + payrollRunId)
                .payPeriodStart(run.getPayPeriodStart())
                .payPeriodEnd(run.getPayPeriodEnd())
                .totalGross(run.getTotalGrossPay())
                .totalDeductions(run.getTotalDeductions())
                .totalNet(run.getTotalNetPay())
                .detailLines(detailLines)
                .summaryByComponent(new ArrayList<>(summaryByComponentMap.values()))
                .build();
    }

    /** ES-26 / INT-01–INT-08: Payslip with component order for an employee in a payroll run. */
    public Optional<PayslipDto> getPayslip(UUID payrollRunId, UUID employeeId) {
        List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(payrollRunId).stream()
                .filter(pd -> pd.getEmployeeId().equals(employeeId))
                .toList();
        if (details.isEmpty()) return Optional.empty();
        PayrollDetail pd = details.get(0);
        List<PayrollComponent> pcs = payrollComponentRepository.findByPayrollDetailIdOrderByDisplayOrderAsc(pd.getPayrollDetailId());
        List<PayslipDto.PayslipLineDto> lines = new ArrayList<>();
        Map<String, BigDecimal> computedByCodeForTaxableGross = new HashMap<>();
        for (PayrollComponent pc : pcs) {
            SalaryComponent sc = salaryComponentRepository.findById(pc.getComponentId()).orElse(null);
            if (sc != null && sc.getCode() != null) {
                computedByCodeForTaxableGross.put(sc.getCode(),
                        pc.getAmount() != null ? pc.getAmount() : BigDecimal.ZERO);
            }
            lines.add(PayslipDto.PayslipLineDto.builder()
                    .componentId(pc.getComponentId())
                    .componentCode(sc != null ? sc.getCode() : null)
                    .componentName(sc != null ? (sc.getShortName() != null ? sc.getShortName() : sc.getComponentName()) : null)
                    .componentType(pc.getComponentType())
                    .displayOrder(pc.getDisplayOrder())
                    .amount(pc.getAmount())
                    .taxability(sc != null && sc.getTaxability() != null ? sc.getTaxability().name() : null)
                    .statutoryType(sc != null ? sc.getStatutoryType() : null)
                    .includedInPfWage(sc != null && hasStatutoryTag(sc, "PF_WAGE"))
                    .includedInEsiWage(sc != null && hasStatutoryTag(sc, "ESI_WAGE"))
                    .isTaxable(sc != null ? sc.getIsTaxable() : null)
                    .build());
        }
        Employee emp = employeeRepository.findById(employeeId).orElse(null);

        // Resolve currency and pay frequency from employee's salary structure for this payroll run.
        String currency = null;
        String payFrequency = null;
        PayrollRun run = payrollRunRepository.findById(payrollRunId).orElse(null);
        BigDecimal ytdGross = null;
        BigDecimal ytdDed = null;
        BigDecimal ytdNet = null;
        BigDecimal ytdTax = null;
        BigDecimal periodTaxableGross = null;
        if (run != null) {
            UUID orgId = run.getOrganizationId();
            LocalDate asOfDate = run.getPayPeriodEnd() != null ? run.getPayPeriodEnd() : run.getPayPeriodStart();
            Optional<EmployeeSalaryAssignment> assignmentOpt = salaryService.getAssignment(employeeId, orgId, asOfDate);
            if (assignmentOpt.isPresent()) {
                UUID structureId = assignmentOpt.get().getSalaryStructureId();
                SalaryStructure structure = salaryStructureRepository.findById(structureId).orElse(null);
                if (structure != null) {
                    currency = structure.getCurrency();
                    payFrequency = structure.getPayFrequency();
                }
            }
            LocalDate yearStart = LocalDate.of(asOfDate.getYear(), 1, 1);
            Object[] ytdRow = payrollDetailRepository.sumYearToDatePayrollTotals(
                    employeeId, orgId, yearStart, asOfDate, payrollRunId);
            if (ytdRow != null && ytdRow.length >= 3) {
                ytdGross = toBigDecimal(ytdRow[0]);
                ytdDed = toBigDecimal(ytdRow[1]);
                ytdNet = toBigDecimal(ytdRow[2]);
            }
            Object taxSum = payrollDetailRepository.sumYearToDateIncomeTaxWithheld(
                    employeeId, orgId, yearStart, asOfDate, payrollRunId);
            ytdTax = toBigDecimal(taxSum);

            List<SalaryComponent> compOrder = salaryService.getComponentsInDependencyOrder(orgId, asOfDate);
            periodTaxableGross = statutoryPayrollCalculationService.computeTaxableGross(compOrder, computedByCodeForTaxableGross);
        }

        return Optional.of(PayslipDto.builder()
                .payrollRunId(payrollRunId)
                .payrollDetailId(pd.getPayrollDetailId())
                .employeeId(pd.getEmployeeId())
                .employeeName(emp != null ? emp.getName() : null)
                .employeeNumber(emp != null ? emp.getEmployeeNumber() : null)
                .basicSalary(pd.getBasicSalary())
                .grossSalary(pd.getGrossSalary())
                .totalDeductions(pd.getTotalDeductions())
                .netSalary(pd.getNetSalary())
                .workingDays(pd.getWorkingDays())
                .presentDays(pd.getPresentDays())
                .leaveDays(pd.getLeaveDays())
                .overtimeHours(pd.getOvertimeHours())
                .overtimeAmount(pd.getOvertimeAmount())
                .lopDays(pd.getLopDays())
                .lopAmount(pd.getLopAmount())
                .currency(currency)
                .payFrequency(payFrequency)
                .yearToDateGross(ytdGross)
                .yearToDateDeductions(ytdDed)
                .yearToDateNet(ytdNet)
                .yearToDateIncomeTaxWithheld(ytdTax)
                .periodTaxableGross(periodTaxableGross)
                .lines(lines)
                .build());
    }

    private static boolean hasStatutoryTag(SalaryComponent sc, String tag) {
        if (sc == null || sc.getStatutoryTags() == null || tag == null) {
            return false;
        }
        String u = tag.toUpperCase(Locale.ROOT);
        return sc.getStatutoryTags().stream().anyMatch(t -> u.equalsIgnoreCase(t));
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BigDecimal bd) {
            return bd.setScale(SCALE, ROUNDING);
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(SCALE, ROUNDING);
        }
        return null;
    }

    private BigDecimal computeComponentAmount(SalaryComponent comp, EmployeeSalaryDetail detail,
                                               Map<String, BigDecimal> computedByCode, BigDecimal grossSoFar) {
        CalculationBasis basis = comp.getCalculationBasis() != null ? comp.getCalculationBasis() : CalculationBasis.FIXED;
        if (detail != null) {
            ComponentValueType vt = detail.getValueType() != null ? detail.getValueType() : ComponentValueType.USE_MASTER_DEFAULT;
            if (ComponentValueType.AMOUNT.equals(vt) && detail.getAmount() != null) {
                return detail.getAmount().setScale(SCALE, ROUNDING);
            }
            if (ComponentValueType.PERCENTAGE.equals(vt) && detail.getPercentage() != null && comp.getBaseComponentCode() != null) {
                BigDecimal base = computedByCode.getOrDefault(comp.getBaseComponentCode().trim(), BigDecimal.ZERO);
                return base.multiply(detail.getPercentage()).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);
            }
        }
        switch (basis) {
            case FIXED:
            case MANUAL:
                return (comp.getDefaultAmount() != null ? comp.getDefaultAmount() : BigDecimal.ZERO).setScale(SCALE, ROUNDING);
            case PERCENTAGE_OF_BASIC: {
                String baseCode = comp.getBaseComponentCode() != null ? comp.getBaseComponentCode().trim() : "BASIC";
                BigDecimal base = computedByCode.getOrDefault(baseCode, BigDecimal.ZERO);
                BigDecimal pct = comp.getPercentageValue() != null ? comp.getPercentageValue() : BigDecimal.ZERO;
                return base.multiply(pct).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);
            }
            case PERCENTAGE_OF_GROSS: {
                BigDecimal pct = comp.getPercentageValue() != null ? comp.getPercentageValue() : BigDecimal.ZERO;
                return grossSoFar.multiply(pct).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);
            }
            case FORMULA: {
                if (comp.getFormulaExpression() == null || comp.getFormulaExpression().isBlank()) return BigDecimal.ZERO;
                String expr = comp.getFormulaExpression();
                for (Map.Entry<String, BigDecimal> e : computedByCode.entrySet()) {
                    expr = expr.replaceAll("(?i)\\b" + e.getKey() + "\\b", "(" + e.getValue().toPlainString() + ")");
                }
                return evaluateFormula(expr);
            }
            case STATUTORY:
                // Deferred statutory (PF / tax / ESI) uses EpfPayrollCalculationService / StatutoryPayrollCalculationService
                // after regular lines and OT/LOP; those components are skipped earlier in the populate loop.
            default:
                return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
    }

    private BigDecimal evaluateFormula(String expr) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
            if (engine == null) return BigDecimal.ZERO;
            Object result = engine.eval(expr);
            if (result instanceof Number) {
                return BigDecimal.valueOf(((Number) result).doubleValue()).setScale(SCALE, ROUNDING);
            }
        } catch (ScriptException ignored) {
        }
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    private BigDecimal applyCeilingFloor(BigDecimal amount, SalaryComponent comp) {
        if (amount == null) return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        if (comp.getCeilingAmount() != null && amount.compareTo(comp.getCeilingAmount()) > 0) {
            amount = comp.getCeilingAmount();
        }
        if (comp.getFloorAmount() != null && amount.compareTo(comp.getFloorAmount()) < 0) {
            amount = comp.getFloorAmount();
        }
        return amount.setScale(SCALE, ROUNDING);
    }

    /**
     * ES-28–ES-30: Prorate a full-period component amount when the employee is not employed for the full pay period,
     * using the component's {@link com.easyops.hr.entity.ProrationRule} (default BY_DAYS).
     * Not applied to loan recovery or to OT/LOP lines (those are added separately).
     */
    private BigDecimal applyPayPeriodProration(
            Employee emp,
            LocalDate periodStart,
            LocalDate periodEnd,
            SalaryComponent comp,
            BigDecimal amountAfterCeilingFloor) {
        if (amountAfterCeilingFloor == null || amountAfterCeilingFloor.compareTo(BigDecimal.ZERO) == 0) {
            return amountAfterCeilingFloor != null ? amountAfterCeilingFloor : BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        ProrationRule rule = comp.getProrationRule() != null ? comp.getProrationRule() : ProrationRule.BY_DAYS;
        int daysWorked = ProrationService.daysWorkedInPeriod(
                periodStart,
                periodEnd,
                emp.getHireDate(),
                emp.getTerminationDate(),
                null,
                null);
        int totalDays = ProrationService.totalDaysInPeriod(periodStart, periodEnd);
        return ProrationService.prorate(amountAfterCeilingFloor, rule, daysWorked, totalDays, null, null);
    }

    /**
     * Hourly rate = basic ÷ (working days × standard hours per day); OT pay = hourly × multiplier × OT hours.
     */
    private BigDecimal computeOvertimePay(
            BigDecimal basicAmount,
            int workingDays,
            BigDecimal overtimeHours,
            BigDecimal standardHoursPerDay,
            BigDecimal overtimeRateMultiplier) {
        if (basicAmount == null || overtimeHours == null || overtimeHours.compareTo(BigDecimal.ZERO) <= 0 || workingDays <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        BigDecimal hoursPerDay = standardHoursPerDay != null && standardHoursPerDay.compareTo(BigDecimal.ZERO) > 0
                ? standardHoursPerDay
                : new BigDecimal("8");
        BigDecimal denom = BigDecimal.valueOf(workingDays).multiply(hoursPerDay);
        BigDecimal hourly = basicAmount.divide(denom, SCALE + 4, ROUNDING);
        BigDecimal mult = overtimeRateMultiplier != null && overtimeRateMultiplier.compareTo(BigDecimal.ZERO) > 0
                ? overtimeRateMultiplier
                : DEFAULT_OVERTIME_RATE_MULTIPLIER;
        return hourly.multiply(mult).multiply(overtimeHours).setScale(SCALE, ROUNDING);
    }

    /** LOP deduction = basic × (lop days ÷ working days in period). */
    private BigDecimal computeLopDeduction(BigDecimal basicAmount, int workingDays, BigDecimal lopDays) {
        if (basicAmount == null || lopDays == null || lopDays.compareTo(BigDecimal.ZERO) <= 0 || workingDays <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return basicAmount.multiply(lopDays)
                .divide(BigDecimal.valueOf(workingDays), SCALE + 4, ROUNDING)
                .setScale(SCALE, ROUNDING);
    }

    private static class PayrollComponentLine {
        final SalaryComponent component;
        /** Null = deferred statutory PF (INT-09/INT-10) filled after OT/LOP. */
        final BigDecimal amount;

        PayrollComponentLine(SalaryComponent component, BigDecimal amount) {
            this.component = component;
            this.amount = amount;
        }
    }
}
