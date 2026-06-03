package com.easyops.hr.service;

import com.easyops.hr.entity.CalculationBasis;
import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.EmployeeSalaryAssignmentRepository;
import com.easyops.hr.repository.EmployeeSalaryDetailRepository;
import com.easyops.hr.repository.PayrollComponentRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.PositionRepository;
import com.easyops.hr.repository.SalaryAuditLogRepository;
import com.easyops.hr.repository.SalaryBandRepository;
import com.easyops.hr.repository.SalaryBulkRevisionRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import com.easyops.hr.repository.SalaryGradeRepository;
import com.easyops.hr.repository.SalaryStructureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryServiceTest {

    @Mock
    private SalaryStructureRepository salaryStructureRepository;
    @Mock
    private SalaryGradeRepository salaryGradeRepository;
    @Mock
    private SalaryBandRepository salaryBandRepository;
    @Mock
    private SalaryComponentRepository salaryComponentRepository;
    @Mock
    private EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    @Mock
    private PayrollComponentRepository payrollComponentRepository;
    @Mock
    private SalaryAuditLogRepository salaryAuditLogRepository;
    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeSalaryAssignmentRepository employeeSalaryAssignmentRepository;
    @Mock
    private SalaryBulkRevisionRepository salaryBulkRevisionRepository;

    @InjectMocks
    private SalaryService salaryService;

    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }

    @Test
    void createSalaryComponent_rejectsInvalidCodeFormat() {
        SalaryComponent component = baseComponent("BASIC-1");

        assertThatThrownBy(() -> salaryService.createSalaryComponent(component))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only letters, numbers, and underscore");
    }

    @Test
    void createSalaryComponent_trimsAndAcceptsValidCode() {
        SalaryComponent component = baseComponent(" BASIC_01 ");
        when(salaryComponentRepository.existsByOrganizationIdAndCode(organizationId, "BASIC_01"))
                .thenReturn(false);
        when(salaryComponentRepository.save(any(SalaryComponent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SalaryComponent saved = salaryService.createSalaryComponent(component);

        assertThat(saved.getCode()).isEqualTo("BASIC_01");
    }

    private SalaryComponent baseComponent(String code) {
        SalaryComponent component = new SalaryComponent();
        component.setOrganizationId(organizationId);
        component.setCode(code);
        component.setComponentName("Basic Salary");
        component.setComponentType("EARNING");
        component.setCalculationBasis(CalculationBasis.FIXED);
        component.setDefaultAmount(BigDecimal.valueOf(1000));
        component.setIsActive(true);
        return component;
    }
}
