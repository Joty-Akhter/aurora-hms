package com.easyops.hr.service;

import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.TaxSlab;
import com.easyops.hr.entity.Taxability;
import com.easyops.hr.repository.TaxSlabRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatutoryPayrollCalculationServiceTest {

    @Mock
    private TaxSlabRepository taxSlabRepository;

    private StatutoryPayrollCalculationService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StatutoryPayrollCalculationService(taxSlabRepository);
        ReflectionTestUtils.setField(service, "esiWageCeiling", new BigDecimal("21000"));
        ReflectionTestUtils.setField(service, "defaultEsiEmployeeRate", new BigDecimal("0.75"));
        ReflectionTestUtils.setField(service, "defaultEsiEmployerRate", new BigDecimal("3.25"));
    }

    @Test
    void computeTaxableGross_respectsExemptAndPartial() {
        List<SalaryComponent> order = new ArrayList<>();
        SalaryComponent taxable = earning("BASIC", Taxability.TAXABLE, false);
        SalaryComponent exempt = earning("LTA", Taxability.EXEMPT, false);
        SalaryComponent partial = earning("SPECIAL", Taxability.PARTIALLY_TAXABLE, false);
        order.add(taxable);
        order.add(exempt);
        order.add(partial);

        Map<String, BigDecimal> computed = new HashMap<>();
        computed.put("BASIC", new BigDecimal("10000.00"));
        computed.put("LTA", new BigDecimal("5000.00"));
        computed.put("SPECIAL", new BigDecimal("4000.00"));

        BigDecimal tg = service.computeTaxableGross(order, computed);
        assertThat(tg).isEqualByComparingTo(new BigDecimal("12000.00"));
    }

    @Test
    void computeIncomeTax_usesContainingSlab() {
        TaxSlab slab = new TaxSlab();
        slab.setMinAmount(new BigDecimal("0.00"));
        slab.setMaxAmount(new BigDecimal("500000.00"));
        slab.setTaxPercentage(new BigDecimal("5.00"));
        slab.setFixedAmount(BigDecimal.ZERO);
        slab.setEffectiveYear(2026);
        slab.setIsActive(true);
        when(taxSlabRepository.findByOrganizationIdAndEffectiveYearAndIsActive(eq(orgId), eq(2026), eq(true)))
                .thenReturn(List.of(slab));

        SalaryComponent taxComp = new SalaryComponent();
        taxComp.setCode("INCOME_TAX");
        taxComp.setComponentType("DEDUCTION");
        taxComp.setCalculationBasis(CalculationBasis.STATUTORY);
        taxComp.setStatutoryType("INCOME_TAX");

        List<SalaryComponent> order = List.of(earning("BASIC", Taxability.TAXABLE, false));
        Map<String, BigDecimal> computed = Map.of("BASIC", new BigDecimal("400000.00"));

        BigDecimal amt = service.computeStatutoryLineAmount(orgId,
                java.time.LocalDate.of(2026, 3, 31), taxComp, order, computed);
        assertThat(amt).isEqualByComparingTo(new BigDecimal("20000.00"));
    }

    @Test
    void computeEsi_usesEsiWageTagAndCeiling() {
        List<SalaryComponent> order = new ArrayList<>();
        SalaryComponent basic = earningWithTag("BASIC", "ESI_WAGE");
        order.add(basic);

        Map<String, BigDecimal> computed = Map.of("BASIC", new BigDecimal("25000.00"));

        SalaryComponent esiEmp = new SalaryComponent();
        esiEmp.setCode("ESI_EMP");
        esiEmp.setComponentType("DEDUCTION");
        esiEmp.setCalculationBasis(CalculationBasis.STATUTORY);
        esiEmp.setStatutoryType("ESI_EMPLOYEE");

        BigDecimal emp = service.computeStatutoryLineAmount(orgId,
                java.time.LocalDate.of(2026, 1, 31), esiEmp, order, computed);
        assertThat(emp).isEqualByComparingTo(new BigDecimal("157.50"));
    }

    private static SalaryComponent earning(String code, Taxability tb, boolean taxExemptTag) {
        SalaryComponent c = new SalaryComponent();
        c.setCode(code);
        c.setComponentType("EARNING");
        c.setTaxability(tb);
        c.setCalculationBasis(CalculationBasis.FIXED);
        if (taxExemptTag) {
            c.setStatutoryTags(List.of("TAX_EXEMPT"));
        }
        return c;
    }

    private static SalaryComponent earningWithTag(String code, String tag) {
        SalaryComponent c = new SalaryComponent();
        c.setCode(code);
        c.setComponentType("EARNING");
        c.setTaxability(Taxability.TAXABLE);
        c.setStatutoryTags(new ArrayList<>(List.of(tag)));
        c.setCalculationBasis(CalculationBasis.FIXED);
        return c;
    }
}
