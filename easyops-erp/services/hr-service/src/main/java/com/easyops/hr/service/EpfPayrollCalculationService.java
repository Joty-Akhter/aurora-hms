package com.easyops.hr.service;

import com.easyops.hr.dto.EpfPayrollContributionResult;
import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.EpfOrganizationPolicy;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.repository.EpfOrganizationPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * INT-09–INT-12 / INT-11 Option B: PF module — computes PF wage (sum of PF_WAGE earnings), applies ceiling,
 * eligibility, and employee/employer contributions from organization policy.
 * <p><b>Product depth (jurisdiction):</b> This engine is intentionally generic (percent of PF wage after ceiling,
 * eligibility flags). Country-specific schemes (e.g. India EPF admin/EDLI splits, Malaysia EPF age bands) are
 * not fully modeled here; extend policy data, posting, or integrate an external payroll engine if required.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EpfPayrollCalculationService {

    private static final String TAG_PF_WAGE = "PF_WAGE";
    private static final Set<String> PF_EMPLOYEE_KEYS = Set.of("PF_EMPLOYEE", "PF_EMP");
    private static final Set<String> PF_EMPLOYER_KEYS = Set.of("PF_EMPLOYER", "PF_EMPR");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final EpfOrganizationPolicyRepository policyRepository;

    @Value("${hr.payroll.default-epf-employee-rate:12.00}")
    private BigDecimal defaultEmployeeRate;

    @Value("${hr.payroll.default-epf-employer-rate:12.00}")
    private BigDecimal defaultEmployerRate;

    @Value("${hr.payroll.fail-on-missing-epf-policy:false}")
    private boolean failOnMissingEpfPolicy;

    /**
     * True if this component is STATUTORY PF employee line (deferred calculation in payroll).
     */
    public static boolean isStatutoryPfEmployeeLine(SalaryComponent comp) {
        if (comp == null || comp.getCalculationBasis() != CalculationBasis.STATUTORY) {
            return false;
        }
        String code = upper(comp.getCode());
        String st = upper(comp.getStatutoryType());
        return PF_EMPLOYEE_KEYS.contains(code) || "PF_EMPLOYEE".equals(st);
    }

    /**
     * True if this component is STATUTORY PF employer line.
     */
    public static boolean isStatutoryPfEmployerLine(SalaryComponent comp) {
        if (comp == null || comp.getCalculationBasis() != CalculationBasis.STATUTORY) {
            return false;
        }
        String code = upper(comp.getCode());
        String st = upper(comp.getStatutoryType());
        return PF_EMPLOYER_KEYS.contains(code) || "PF_EMPLOYER".equals(st);
    }

    public static boolean isStatutoryPfLine(SalaryComponent comp) {
        return isStatutoryPfEmployeeLine(comp) || isStatutoryPfEmployerLine(comp);
    }

    /**
     * Amount from INT-11 result for this payslip line.
     */
    public BigDecimal amountForStatutoryPfLine(SalaryComponent comp, EpfPayrollContributionResult result) {
        if (result == null) return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        if (isStatutoryPfEmployeeLine(comp)) {
            return result.getEmployeeContributionAmount().setScale(SCALE, ROUNDING);
        }
        if (isStatutoryPfEmployerLine(comp)) {
            return result.getEmployerContributionAmount().setScale(SCALE, ROUNDING);
        }
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    /**
     * SC-22 / INT-09: Sum of earning component amounts that are tagged PF_WAGE (using computedByCode).
     */
    public BigDecimal computePfWageBase(List<SalaryComponent> componentsInOrder, Map<String, BigDecimal> computedByCode) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SalaryComponent sc : componentsInOrder) {
            if (!"EARNING".equalsIgnoreCase(sc.getComponentType())) {
                continue;
            }
            if (sc.getStatutoryTags() == null || !sc.getStatutoryTags().stream().map(String::toUpperCase).collect(Collectors.toSet()).contains(TAG_PF_WAGE)) {
                continue;
            }
            String code = sc.getCode();
            if (code == null) continue;
            BigDecimal amt = computedByCode.getOrDefault(code, BigDecimal.ZERO);
            if (amt != null) {
                sum = sum.add(amt);
            }
        }
        return sum.setScale(SCALE + 2, ROUNDING);
    }

    /**
     * INT-11: PF module entry point — compute contributions for payroll (after all earning lines including OT are in computedByCode).
     */
    @Transactional(readOnly = true)
    public EpfPayrollContributionResult computeContributionsForPayroll(
            Employee employee,
            UUID organizationId,
            List<SalaryComponent> componentsInOrder,
            Map<String, BigDecimal> computedByCode) {

        Optional<EpfOrganizationPolicy> opt = policyRepository.findByOrganizationId(organizationId);
        if (opt.isEmpty()) {
            if (failOnMissingEpfPolicy) {
                throw new IllegalStateException("INT-43: EPF organization policy is required but not configured for organization " + organizationId);
            }
            EpfOrganizationPolicy def = defaultPolicy(organizationId);
            return computeWithPolicy(employee, def, componentsInOrder, computedByCode);
        }
        return computeWithPolicy(employee, opt.get(), componentsInOrder, computedByCode);
    }

    private EpfPayrollContributionResult computeWithPolicy(
            Employee employee,
            EpfOrganizationPolicy policy,
            List<SalaryComponent> componentsInOrder,
            Map<String, BigDecimal> computedByCode) {

        BigDecimal empRate = policy.getEmployeeContributionRate() != null ? policy.getEmployeeContributionRate() : defaultEmployeeRate;
        BigDecimal emprRate = policy.getEmployerContributionRate() != null ? policy.getEmployerContributionRate() : defaultEmployerRate;

        BigDecimal pfWageRaw = computePfWageBase(componentsInOrder, computedByCode);
        BigDecimal pfWageCapped = applyCeiling(pfWageRaw, policy.getPfWageCeiling());

        String eligibilityReason = resolveIneligibility(employee, policy, pfWageCapped);
        if (eligibilityReason != null) {
            log.debug("EPF ineligible for employee {}: {}", employee.getEmployeeId(), eligibilityReason);
            return EpfPayrollContributionResult.ineligible(pfWageRaw, pfWageCapped, empRate, emprRate, eligibilityReason);
        }

        BigDecimal empAmt = pfWageCapped.multiply(empRate).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);
        BigDecimal emprAmt = pfWageCapped.multiply(emprRate).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);

        return EpfPayrollContributionResult.builder()
                .pfWageBeforeCeiling(pfWageRaw.setScale(SCALE, ROUNDING))
                .pfWageAfterCeiling(pfWageCapped.setScale(SCALE, ROUNDING))
                .employeeContributionAmount(empAmt)
                .employerContributionAmount(emprAmt)
                .employeeRatePercent(empRate)
                .employerRatePercent(emprRate)
                .eligible(true)
                .ineligibilityReason(null)
                .build();
    }

    private EpfOrganizationPolicy defaultPolicy(UUID organizationId) {
        return EpfOrganizationPolicy.builder()
                .organizationId(organizationId)
                .employeeContributionRate(defaultEmployeeRate)
                .employerContributionRate(defaultEmployerRate)
                .build();
    }

    private static BigDecimal applyCeiling(BigDecimal pfWage, BigDecimal ceiling) {
        if (ceiling == null || ceiling.compareTo(BigDecimal.ZERO) <= 0) {
            return pfWage.max(BigDecimal.ZERO).setScale(SCALE + 2, ROUNDING);
        }
        return pfWage.min(ceiling).max(BigDecimal.ZERO).setScale(SCALE + 2, ROUNDING);
    }

    /**
     * @return null if eligible, else reason code/message
     */
    private String resolveIneligibility(Employee employee, EpfOrganizationPolicy policy, BigDecimal pfWageAfterCeiling) {
        String et = upper(employee.getEmploymentType());
        if (et == null) {
            et = "FULL_TIME";
        }
        if (policy.getIneligibleEmploymentTypes() != null && !policy.getIneligibleEmploymentTypes().isBlank()) {
            Set<String> bad = splitCsv(policy.getIneligibleEmploymentTypes());
            if (bad.contains(et)) {
                return "employment_type_ineligible:" + et;
            }
        }
        if (policy.getEligibleEmploymentTypes() != null && !policy.getEligibleEmploymentTypes().isBlank()) {
            Set<String> good = splitCsv(policy.getEligibleEmploymentTypes());
            if (!good.isEmpty() && !good.contains(et)) {
                return "employment_type_not_in_whitelist:" + et;
            }
        }
        if (policy.getPfWageFloor() != null && policy.getPfWageFloor().compareTo(BigDecimal.ZERO) > 0) {
            if (pfWageAfterCeiling.compareTo(policy.getPfWageFloor()) < 0) {
                return "pf_wage_below_floor";
            }
        }
        return null;
    }

    private static Set<String> splitCsv(String s) {
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .map(x -> x.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT).trim();
    }

    /**
     * INT-43: Call at start of payroll population when {@code hr.payroll.fail-on-missing-epf-policy=true}.
     */
    @Transactional(readOnly = true)
    public void ensureEpfPolicyRowExistsIfRequired(UUID organizationId) {
        if (!failOnMissingEpfPolicy) {
            return;
        }
        if (policyRepository.findByOrganizationId(organizationId).isEmpty()) {
            throw new IllegalStateException("INT-43: EPF organization policy missing for organization " + organizationId);
        }
    }
}
