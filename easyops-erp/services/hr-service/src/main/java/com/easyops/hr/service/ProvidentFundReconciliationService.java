package com.easyops.hr.service;

import com.easyops.hr.entity.EpfContribution;
import com.easyops.hr.entity.EpfRemittance;
import com.easyops.hr.entity.PayrollComponent;
import com.easyops.hr.entity.PayrollDetail;
import com.easyops.hr.entity.PayrollRun;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.repository.EpfContributionRepository;
import com.easyops.hr.repository.EpfRemittanceRepository;
import com.easyops.hr.repository.PayrollComponentRepository;
import com.easyops.hr.repository.PayrollDetailRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProvidentFundReconciliationService {

    private static final Set<String> CLOSED_RUN_STATUSES = Set.of("PROCESSED", "APPROVED", "FINALIZED");
    private static final Set<String> PF_EMPLOYEE_KEYS = Set.of("PF_EMPLOYEE", "PF_EMP");
    private static final Set<String> PF_EMPLOYER_KEYS = Set.of("PF_EMPLOYER", "PF_EMPR");
    private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("1.00");

    private final PayrollRunRepository payrollRunRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollComponentRepository payrollComponentRepository;
    private final SalaryComponentRepository salaryComponentRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfRemittanceRepository epfRemittanceRepository;

    public Map<String, Object> reconcilePeriod(UUID organizationId, Integer month, Integer year) {
        return reconcilePeriod(organizationId, month, year, DEFAULT_TOLERANCE);
    }

    public Map<String, Object> reconcilePeriod(UUID organizationId, Integer month, Integer year, BigDecimal tolerance) {
        BigDecimal reconcTolerance = tolerance != null ? tolerance : DEFAULT_TOLERANCE;

        BigDecimal payrollPfTotal = computePayrollPfTotal(organizationId, month, year);
        List<EpfContribution> contributions = epfContributionRepository.findByOrganizationAndPeriod(organizationId, month, year);
        BigDecimal contributionTotal = contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        EpfRemittance remittance = epfRemittanceRepository
                .findFirstByOrganizationIdAndRemittanceMonthAndRemittanceYear(organizationId, month, year)
                .orElse(null);
        BigDecimal postedLiability = remittance != null ? nvl(remittance.getLiabilityAmount()) : BigDecimal.ZERO;
        BigDecimal paidAmount = remittance != null ? nvl(remittance.getAmountPaid()) : BigDecimal.ZERO;

        BigDecimal payrollVsContributionDelta = payrollPfTotal.subtract(contributionTotal);
        BigDecimal contributionVsPostedDelta = contributionTotal.subtract(postedLiability);
        BigDecimal postedVsPaidDelta = postedLiability.subtract(paidAmount);

        boolean payrollVsContributionOk = isWithinTolerance(payrollVsContributionDelta, reconcTolerance);
        boolean contributionVsPostedOk = remittance == null || isWithinTolerance(contributionVsPostedDelta, reconcTolerance);
        boolean postedVsPaidOk = remittance == null || "paid".equalsIgnoreCase(remittance.getStatus()) &&
                isWithinTolerance(postedVsPaidDelta, reconcTolerance);

        String riskStatus = payrollVsContributionOk && contributionVsPostedOk && postedVsPaidOk ? "RECONCILED" : "MISMATCH";

        Map<String, Object> response = new HashMap<>();
        response.put("organizationId", organizationId);
        response.put("month", month);
        response.put("year", year);
        response.put("tolerance", reconcTolerance);
        response.put("payrollPfTotal", payrollPfTotal);
        response.put("contributionTotal", contributionTotal);
        response.put("remittancePostedLiability", postedLiability);
        response.put("remittancePaidAmount", paidAmount);
        response.put("payrollVsContributionDelta", payrollVsContributionDelta);
        response.put("contributionVsPostedDelta", contributionVsPostedDelta);
        response.put("postedVsPaidDelta", postedVsPaidDelta);
        response.put("payrollVsContributionOk", payrollVsContributionOk);
        response.put("contributionVsPostedOk", contributionVsPostedOk);
        response.put("postedVsPaidOk", postedVsPaidOk);
        response.put("riskStatus", riskStatus);
        response.put("remittanceStatus", remittance != null ? remittance.getStatus() : "not_started");
        response.put("remittanceId", remittance != null ? remittance.getRemittanceId() : null);
        response.put("checkedAt", LocalDate.now());
        return response;
    }

    private BigDecimal computePayrollPfTotal(UUID organizationId, Integer month, Integer year) {
        Map<UUID, SalaryComponent> salaryComponentMap = salaryComponentRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(SalaryComponent::getComponentId, c -> c));

        List<PayrollRun> runs = payrollRunRepository.findByOrganizationId(organizationId).stream()
                .filter(run -> run.getPayPeriodEnd() != null)
                .filter(run -> run.getPayPeriodEnd().getMonthValue() == month && run.getPayPeriodEnd().getYear() == year)
                .filter(run -> run.getStatus() != null && CLOSED_RUN_STATUSES.contains(run.getStatus().toUpperCase(Locale.ROOT)))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (PayrollRun run : runs) {
            List<PayrollDetail> details = payrollDetailRepository.findByPayrollRunId(run.getPayrollRunId());
            for (PayrollDetail detail : details) {
                List<PayrollComponent> components = payrollComponentRepository.findByPayrollDetailId(detail.getPayrollDetailId());
                for (PayrollComponent component : components) {
                    SalaryComponent sc = salaryComponentMap.get(component.getComponentId());
                    if (sc == null) {
                        continue;
                    }
                    String code = sc.getCode() != null ? sc.getCode().toUpperCase(Locale.ROOT) : "";
                    String statutoryType = sc.getStatutoryType() != null ? sc.getStatutoryType().toUpperCase(Locale.ROOT) : "";
                    if (PF_EMPLOYEE_KEYS.contains(code) || PF_EMPLOYER_KEYS.contains(code)
                            || "PF_EMPLOYEE".equals(statutoryType) || "PF_EMPLOYER".equals(statutoryType)) {
                        total = total.add(nvl(component.getAmount()));
                    }
                }
            }
        }
        return total;
    }

    private boolean isWithinTolerance(BigDecimal delta, BigDecimal tolerance) {
        return delta.abs().compareTo(tolerance) <= 0;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
