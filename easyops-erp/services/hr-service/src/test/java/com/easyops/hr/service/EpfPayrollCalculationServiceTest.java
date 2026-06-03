package com.easyops.hr.service;

import com.easyops.hr.dto.EpfPayrollContributionResult;
import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.EpfOrganizationPolicy;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.repository.EpfOrganizationPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpfPayrollCalculationServiceTest {

    @Mock
    private EpfOrganizationPolicyRepository policyRepository;

    private EpfPayrollCalculationService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EpfPayrollCalculationService(policyRepository);
        ReflectionTestUtils.setField(service, "defaultEmployeeRate", new BigDecimal("12.00"));
        ReflectionTestUtils.setField(service, "defaultEmployerRate", new BigDecimal("12.00"));
        ReflectionTestUtils.setField(service, "defaultEmployerPensionRate", new BigDecimal("8.33"));
        ReflectionTestUtils.setField(service, "defaultEdliRate", new BigDecimal("0.50"));
        ReflectionTestUtils.setField(service, "defaultAdminChargeRate", new BigDecimal("0.50"));
        ReflectionTestUtils.setField(service, "defaultPensionWageCap", new BigDecimal("15000.00"));
        ReflectionTestUtils.setField(service, "failOnMissingEpfPolicy", false);
    }

    @Test
    void computePfWageBase_sumsPfWageTaggedEarnings() {
        List<SalaryComponent> order = new ArrayList<>();
        SalaryComponent basic = earning("BASIC", List.of("PF_WAGE"));
        SalaryComponent hra = earning("HRA", List.of("PF_WAGE"));
        order.add(basic);
        order.add(hra);

        Map<String, BigDecimal> computed = new HashMap<>();
        computed.put("BASIC", new BigDecimal("10000.00"));
        computed.put("HRA", new BigDecimal("5000.00"));

        BigDecimal w = service.computePfWageBase(order, computed);
        assertThat(w).isEqualByComparingTo(new BigDecimal("15000.00"));
    }

    @Test
    void computeContributions_appliesCeilingAndRates() {
        EpfOrganizationPolicy policy = EpfOrganizationPolicy.builder()
                .organizationId(orgId)
                .employeeContributionRate(new BigDecimal("12.00"))
                .employerContributionRate(new BigDecimal("12.00"))
                .pfWageCeiling(new BigDecimal("15000.00"))
                .build();
        when(policyRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(policy));

        Employee emp = Employee.builder()
                .employeeId(UUID.randomUUID())
                .organizationId(orgId)
                .employmentType("FULL_TIME")
                .build();

        List<SalaryComponent> order = new ArrayList<>();
        order.add(earning("BASIC", List.of("PF_WAGE")));

        Map<String, BigDecimal> computed = new HashMap<>();
        computed.put("BASIC", new BigDecimal("20000.00"));

        EpfPayrollContributionResult r = service.computeContributionsForPayroll(emp, orgId, order, computed);

        assertThat(r.getPfWageAfterCeiling()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(r.getEmployeeContributionAmount()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(r.getEmployerContributionAmount()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(r.getEmployerPensionAmount()).isEqualByComparingTo(new BigDecimal("1249.50"));
        assertThat(r.getEmployerEpfAmount()).isEqualByComparingTo(new BigDecimal("550.50"));
        assertThat(r.getEmployerEdliAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(r.getEmployerAdminChargeAmount()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(r.isEligible()).isTrue();
    }

    @Test
    void computeContributions_ineligibleWhenEmploymentTypeExcluded() {
        EpfOrganizationPolicy policy = EpfOrganizationPolicy.builder()
                .organizationId(orgId)
                .employeeContributionRate(new BigDecimal("12.00"))
                .employerContributionRate(new BigDecimal("12.00"))
                .ineligibleEmploymentTypes("INTERN")
                .build();
        when(policyRepository.findByOrganizationId(orgId)).thenReturn(Optional.of(policy));

        Employee emp = Employee.builder()
                .employeeId(UUID.randomUUID())
                .organizationId(orgId)
                .employmentType("INTERN")
                .build();

        List<SalaryComponent> order = List.of(earning("BASIC", List.of("PF_WAGE")));
        Map<String, BigDecimal> computed = Map.of("BASIC", new BigDecimal("10000.00"));

        EpfPayrollContributionResult r = service.computeContributionsForPayroll(emp, orgId, order, computed);

        assertThat(r.isEligible()).isFalse();
        assertThat(r.getEmployeeContributionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getEmployerContributionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getEmployerPensionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getEmployerEpfAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getEmployerEdliAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getEmployerAdminChargeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static SalaryComponent earning(String code, List<String> tags) {
        SalaryComponent c = new SalaryComponent();
        c.setCode(code);
        c.setComponentType("EARNING");
        c.setCalculationBasis(CalculationBasis.FIXED);
        c.setStatutoryTags(new ArrayList<>(tags));
        return c;
    }
}
