package com.easyops.hr.service;

import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.TaxSlab;
import com.easyops.hr.entity.Taxability;
import com.easyops.hr.repository.TaxSlabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * INT-16 / INT-17: Non-PF statutory deductions — income tax (from slabs + taxable gross) and ESI (% of ESI wage base).
 * Deferred in {@link PayrollCalculationService} until OT/LOP are merged into {@code computedByCode}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatutoryPayrollCalculationService {

    private static final String TAG_TAXABLE = "TAXABLE";
    private static final String TAG_TAX_EXEMPT = "TAX_EXEMPT";
    private static final String TAG_ESI_WAGE = "ESI_WAGE";

    private static final Set<String> INCOME_TAX_KEYS = Set.of("INCOME_TAX", "TAX", "IT");
    private static final Set<String> ESI_EMPLOYEE_KEYS = Set.of("ESI_EMPLOYEE", "ESI_EMP");
    private static final Set<String> ESI_EMPLOYER_KEYS = Set.of("ESI_EMPLOYER", "ESI_EMPR");

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final TaxSlabRepository taxSlabRepository;

    @Value("${hr.payroll.esi-wage-ceiling:21000}")
    private BigDecimal esiWageCeiling;

    @Value("${hr.payroll.default-esi-employee-rate:0.75}")
    private BigDecimal defaultEsiEmployeeRate;

    @Value("${hr.payroll.default-esi-employer-rate:3.25}")
    private BigDecimal defaultEsiEmployerRate;

    /** INT-43: Fail populate-from-salary when INCOME_TAX statutory component exists but no active slabs for the year. */
    @Value("${hr.payroll.fail-on-missing-tax-slabs:false}")
    private boolean failOnMissingTaxSlabs;

    /**
     * Statutory lines that are not PF — computed after earnings + OT/LOP are known.
     */
    public static boolean isDeferredStatutoryNonPfLine(SalaryComponent comp) {
        if (comp == null || comp.getCalculationBasis() != CalculationBasis.STATUTORY) {
            return false;
        }
        return !EpfPayrollCalculationService.isStatutoryPfLine(comp);
    }

    /**
     * Employer ESI is informational on payslip; does not reduce employee net pay.
     */
    public static boolean isStatutoryEsiEmployerLine(SalaryComponent comp) {
        if (comp == null) {
            return false;
        }
        String code = upper(comp.getCode());
        String st = upper(comp.getStatutoryType());
        return ESI_EMPLOYER_KEYS.contains(code) || "ESI_EMPLOYER".equals(st);
    }

    /**
     * True if this statutory deduction reduces net pay (employee tax, employee ESI, etc.).
     */
    public static boolean reducesEmployeeNet(SalaryComponent comp) {
        if (comp == null || !"DEDUCTION".equalsIgnoreCase(comp.getComponentType())) {
            return false;
        }
        if (EpfPayrollCalculationService.isStatutoryPfEmployerLine(comp)) {
            return false;
        }
        if (isStatutoryEsiEmployerLine(comp)) {
            return false;
        }
        return true;
    }

    /**
     * INT-14/INT-15: Taxable gross for the period from computed earning lines (tags + taxability).
     */
    public BigDecimal computeTaxableGross(List<SalaryComponent> componentsInOrder, Map<String, BigDecimal> computedByCode) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SalaryComponent sc : componentsInOrder) {
            if (!"EARNING".equalsIgnoreCase(sc.getComponentType())) {
                continue;
            }
            String code = sc.getCode();
            if (code == null) {
                continue;
            }
            BigDecimal amt = computedByCode.getOrDefault(code, BigDecimal.ZERO);
            if (amt == null || amt.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            if (isExemptFromTax(sc)) {
                continue;
            }
            Taxability tb = sc.getTaxability();
            if (Taxability.EXEMPT.equals(tb)) {
                continue;
            }
            if (Taxability.PARTIALLY_TAXABLE.equals(tb)) {
                sum = sum.add(amt.multiply(new BigDecimal("0.50")).setScale(SCALE + 2, ROUNDING));
            } else {
                sum = sum.add(amt);
            }
        }
        return sum.setScale(SCALE + 2, ROUNDING);
    }

    private boolean isExemptFromTax(SalaryComponent sc) {
        if (sc.getStatutoryTags() != null) {
            Set<String> tags = sc.getStatutoryTags().stream().map(String::toUpperCase).collect(Collectors.toSet());
            if (tags.contains(TAG_TAX_EXEMPT)) {
                return true;
            }
        }
        if (Boolean.FALSE.equals(sc.getIsTaxable()) && sc.getTaxability() == null) {
            return true;
        }
        return false;
    }

    /**
     * Sum of earning amounts tagged {@code ESI_WAGE} (same pattern as PF wage).
     */
    public BigDecimal computeEsiWageBase(List<SalaryComponent> componentsInOrder, Map<String, BigDecimal> computedByCode) {
        BigDecimal sum = BigDecimal.ZERO;
        for (SalaryComponent sc : componentsInOrder) {
            if (!"EARNING".equalsIgnoreCase(sc.getComponentType())) {
                continue;
            }
            if (sc.getStatutoryTags() == null || !sc.getStatutoryTags().stream()
                    .map(String::toUpperCase).collect(Collectors.toSet()).contains(TAG_ESI_WAGE)) {
                continue;
            }
            if (sc.getCode() == null) {
                continue;
            }
            BigDecimal amt = computedByCode.getOrDefault(sc.getCode(), BigDecimal.ZERO);
            if (amt != null) {
                sum = sum.add(amt);
            }
        }
        return sum.setScale(SCALE + 2, ROUNDING);
    }

    /**
     * Computes amount for one deferred statutory line (INT-16/INT-17).
     */
    @Transactional(readOnly = true)
    public BigDecimal computeStatutoryLineAmount(
            UUID organizationId,
            LocalDate periodEnd,
            SalaryComponent comp,
            List<SalaryComponent> componentsInOrder,
            Map<String, BigDecimal> computedByCode) {

        int year = periodEnd.getYear();
        String code = upper(comp.getCode());
        String st = upper(comp.getStatutoryType());

        if (INCOME_TAX_KEYS.contains(code) || "INCOME_TAX".equals(st)) {
            BigDecimal taxableGross = computeTaxableGross(componentsInOrder, computedByCode);
            return computeIncomeTaxForSlab(organizationId, year, taxableGross);
        }

        if (ESI_EMPLOYEE_KEYS.contains(code) || "ESI_EMPLOYEE".equals(st)) {
            return computeEsiEmployeeAmount(comp, componentsInOrder, computedByCode);
        }

        if (ESI_EMPLOYER_KEYS.contains(code) || "ESI_EMPLOYER".equals(st)) {
            return computeEsiEmployerAmount(comp, componentsInOrder, computedByCode);
        }

        log.warn("Unknown statutory type for payroll: code={} statutoryType={} — using 0", comp.getCode(), comp.getStatutoryType());
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    /**
     * Containing slab: highest {@code minAmount} such that income falls in [min, max].
     * Tax = fixedAmount + (taxableGross - minAmount) × taxPercentage / 100 (simplified slab formula).
     */
    private BigDecimal computeIncomeTaxForSlab(UUID organizationId, int year, BigDecimal taxableGross) {
        List<TaxSlab> slabs = taxSlabRepository.findByOrganizationIdAndEffectiveYearAndIsActive(organizationId, year, true);
        if (slabs.isEmpty()) {
            log.debug("No active tax slabs for org {} year {} — income tax 0", organizationId, year);
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        BigDecimal income = taxableGross.max(BigDecimal.ZERO);
        List<TaxSlab> containing = slabs.stream()
                .filter(s -> income.compareTo(s.getMinAmount()) >= 0)
                .filter(s -> s.getMaxAmount() == null || income.compareTo(s.getMaxAmount()) <= 0)
                .sorted(Comparator.comparing(TaxSlab::getMinAmount).reversed())
                .toList();
        TaxSlab slab = containing.isEmpty() ? null : containing.get(0);
        if (slab == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        BigDecimal fixed = slab.getFixedAmount() != null ? slab.getFixedAmount() : BigDecimal.ZERO;
        BigDecimal pct = slab.getTaxPercentage() != null ? slab.getTaxPercentage() : BigDecimal.ZERO;
        BigDecimal excess = income.subtract(slab.getMinAmount()).max(BigDecimal.ZERO);
        BigDecimal pctPart = excess.multiply(pct).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING);
        return fixed.add(pctPart).setScale(SCALE, ROUNDING);
    }

    private BigDecimal computeEsiEmployeeAmount(SalaryComponent comp, List<SalaryComponent> componentsInOrder, Map<String, BigDecimal> computedByCode) {
        BigDecimal esiWage = computeEsiWageBase(componentsInOrder, computedByCode);
        BigDecimal capped = esiWage.min(esiWageCeiling != null ? esiWageCeiling : new BigDecimal("999999999"));
        BigDecimal rate = comp.getPercentageValue() != null ? comp.getPercentageValue() : defaultEsiEmployeeRate;
        return capped.multiply(rate).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);
    }

    private BigDecimal computeEsiEmployerAmount(SalaryComponent comp, List<SalaryComponent> componentsInOrder, Map<String, BigDecimal> computedByCode) {
        BigDecimal esiWage = computeEsiWageBase(componentsInOrder, computedByCode);
        BigDecimal capped = esiWage.min(esiWageCeiling != null ? esiWageCeiling : new BigDecimal("999999999"));
        BigDecimal rate = comp.getPercentageValue() != null ? comp.getPercentageValue() : defaultEsiEmployerRate;
        return capped.multiply(rate).divide(BigDecimal.valueOf(100), SCALE + 2, ROUNDING).setScale(SCALE, ROUNDING);
    }

    /**
     * INT-43: When {@code hr.payroll.fail-on-missing-tax-slabs} is true, require at least one active tax slab
     * for the calendar year of {@code periodEnd} if the salary component set includes a statutory income tax line.
     */
    @Transactional(readOnly = true)
    public void ensureTaxSlabsExistIfRequired(UUID organizationId, LocalDate periodEnd, List<SalaryComponent> componentsInOrder) {
        if (!failOnMissingTaxSlabs) {
            return;
        }
        boolean hasIncomeTax = componentsInOrder.stream().anyMatch(StatutoryPayrollCalculationService::isIncomeTaxStatutoryComponent);
        if (!hasIncomeTax) {
            return;
        }
        int year = periodEnd.getYear();
        if (taxSlabRepository.findByOrganizationIdAndEffectiveYearAndIsActive(organizationId, year, true).isEmpty()) {
            throw new IllegalStateException("INT-43: Active tax slabs are required for year " + year
                    + " but none are configured for organization " + organizationId
                    + ". Add slabs or set hr.payroll.fail-on-missing-tax-slabs=false.");
        }
    }

    /** True if master defines a statutory income tax deduction line for payroll. */
    public static boolean isIncomeTaxStatutoryComponent(SalaryComponent c) {
        if (c == null || c.getCalculationBasis() != CalculationBasis.STATUTORY) {
            return false;
        }
        String code = upper(c.getCode());
        String st = upper(c.getStatutoryType());
        return INCOME_TAX_KEYS.contains(code) || "INCOME_TAX".equals(st);
    }

    private static String upper(String s) {
        return s == null ? "" : s.toUpperCase(Locale.ROOT).trim();
    }
}
