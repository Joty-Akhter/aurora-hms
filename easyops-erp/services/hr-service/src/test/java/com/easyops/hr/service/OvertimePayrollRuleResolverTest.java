package com.easyops.hr.service;

import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.entity.ShiftDefinition;
import com.easyops.hr.entity.ShiftSchedule;
import com.easyops.hr.repository.ShiftDefinitionRepository;
import com.easyops.hr.repository.ShiftScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OvertimePayrollRuleResolverTest {

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private ShiftDefinitionRepository shiftDefinitionRepository;

    @InjectMocks
    private OvertimePayrollRuleResolver resolver;

    private Employee emp;
    private PayrollTimeAttendancePolicy policy;
    private final UUID nightDefId = UUID.randomUUID();
    private final UUID dayDefId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        emp = Employee.builder()
                .employeeId(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .employeeNumber("E1")
                .name("Test")
                .hireDate(LocalDate.of(2020, 1, 1))
                .build();
        policy = PayrollTimeAttendancePolicy.builder()
                .organizationId(emp.getOrganizationId())
                .overtimeRateMultiplier(new BigDecimal("1.50"))
                .standardHoursPerDay(new BigDecimal("8"))
                .build();
    }

    @Test
    void prefersEmployeeMultiplierAndHours() {
        emp.setPayrollOvertimeRateMultiplier(new BigDecimal("2.0"));
        emp.setPayrollStandardHoursPerDay(new BigDecimal("7.5"));

        assertThat(resolver.resolveOvertimeRateMultiplier(emp, LocalDate.now(), LocalDate.now(), policy))
                .isEqualByComparingTo("2.0");
        assertThat(resolver.resolveStandardHoursPerDay(emp, LocalDate.now(), LocalDate.now(), policy))
                .isEqualByComparingTo("7.5");
    }

    @Test
    void usesDominantShiftBandBeforeOrgPolicy() {
        LocalDate d1 = LocalDate.of(2026, 3, 2);
        LocalDate d2 = LocalDate.of(2026, 3, 3);
        ShiftSchedule s1 = new ShiftSchedule();
        s1.setShiftDefinitionId(dayDefId);
        ShiftSchedule s2 = new ShiftSchedule();
        s2.setShiftDefinitionId(nightDefId);
        ShiftSchedule s3 = new ShiftSchedule();
        s3.setShiftDefinitionId(nightDefId);
        when(shiftScheduleRepository.findEmployeeShiftSchedulesInRange(emp.getEmployeeId(), d1, d2))
                .thenReturn(List.of(s1, s2, s3));

        ShiftDefinition night = ShiftDefinition.builder()
                .shiftDefinitionId(nightDefId)
                .organizationId(emp.getOrganizationId())
                .expectedHours(new BigDecimal("10"))
                .overtimeRateMultiplier(new BigDecimal("2.25"))
                .build();
        when(shiftDefinitionRepository.findById(nightDefId)).thenReturn(Optional.of(night));

        assertThat(resolver.resolveOvertimeRateMultiplier(emp, d1, d2, policy))
                .isEqualByComparingTo("2.25");
        assertThat(resolver.resolveStandardHoursPerDay(emp, d1, d2, policy))
                .isEqualByComparingTo("10");
    }

    @Test
    void fallsBackToPolicyWhenNoRosterOrShiftMultiplierUnset() {
        LocalDate d1 = LocalDate.of(2026, 4, 1);
        when(shiftScheduleRepository.findEmployeeShiftSchedulesInRange(any(), any(), any())).thenReturn(List.of());

        assertThat(resolver.resolveOvertimeRateMultiplier(emp, d1, d1, policy))
                .isEqualByComparingTo("1.50");
        assertThat(resolver.resolveStandardHoursPerDay(emp, d1, d1, policy))
                .isEqualByComparingTo("8");
    }

    @Test
    void skipsShiftMultiplierWhenZeroUsesOrgInstead() {
        LocalDate d1 = LocalDate.of(2026, 5, 1);
        ShiftSchedule s1 = new ShiftSchedule();
        s1.setShiftDefinitionId(dayDefId);
        when(shiftScheduleRepository.findEmployeeShiftSchedulesInRange(emp.getEmployeeId(), d1, d1))
                .thenReturn(List.of(s1));

        ShiftDefinition day = ShiftDefinition.builder()
                .shiftDefinitionId(dayDefId)
                .organizationId(emp.getOrganizationId())
                .expectedHours(new BigDecimal("8"))
                .overtimeRateMultiplier(null)
                .build();
        when(shiftDefinitionRepository.findById(dayDefId)).thenReturn(Optional.of(day));

        assertThat(resolver.resolveOvertimeRateMultiplier(emp, d1, d1, policy))
                .isEqualByComparingTo("1.50");
    }
}
