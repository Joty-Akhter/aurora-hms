package com.easyops.hr.service;

import com.easyops.hr.repository.EmployeeRepository;
import com.easyops.hr.repository.EmployeeSalaryAssignmentRepository;
import com.easyops.hr.repository.EmployeeSalaryDetailRepository;
import com.easyops.hr.repository.PayrollComponentRepository;
import com.easyops.hr.repository.PayrollDetailRepository;
import com.easyops.hr.repository.PayrollRunRepository;
import com.easyops.hr.repository.SalaryComponentRepository;
import com.easyops.hr.repository.SalaryStructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PayrollCalculationServiceTest {

    @Mock
    private PayrollRunRepository payrollRunRepository;
    @Mock
    private PayrollDetailRepository payrollDetailRepository;
    @Mock
    private PayrollComponentRepository payrollComponentRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeeSalaryDetailRepository employeeSalaryDetailRepository;
    @Mock
    private EmployeeSalaryAssignmentRepository employeeSalaryAssignmentRepository;
    @Mock
    private SalaryComponentRepository salaryComponentRepository;
    @Mock
    private SalaryStructureRepository salaryStructureRepository;

    private PayrollCalculationService payrollCalculationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        payrollCalculationService = new PayrollCalculationService(
                payrollRunRepository,
                payrollDetailRepository,
                payrollComponentRepository,
                employeeRepository,
                null,
                employeeSalaryDetailRepository,
                employeeSalaryAssignmentRepository,
                salaryComponentRepository,
                salaryStructureRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void evaluateFormula_appliesOperatorPrecedenceAndParentheses() {
        BigDecimal amount = ReflectionTestUtils.invokeMethod(
                payrollCalculationService,
                "evaluateFormula",
                "10 + 2 * (3 + 4) - 5 / 2"
        );

        assertThat(amount).isEqualByComparingTo(new BigDecimal("21.50"));
    }

    @Test
    void evaluateFormula_returnsZeroForInvalidExpression() {
        BigDecimal amount = ReflectionTestUtils.invokeMethod(
                payrollCalculationService,
                "evaluateFormula",
                "2 + (3 *"
        );

        assertThat(amount).isEqualByComparingTo(new BigDecimal("0.00"));
    }
}
