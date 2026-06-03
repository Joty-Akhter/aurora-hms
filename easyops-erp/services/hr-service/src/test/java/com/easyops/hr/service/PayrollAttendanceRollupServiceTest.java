package com.easyops.hr.service;

import com.easyops.hr.dto.PayrollAttendanceRollup;
import com.easyops.hr.entity.AttendanceRecord;
import com.easyops.hr.entity.Employee;
import com.easyops.hr.entity.Holiday;
import com.easyops.hr.entity.LeaveRequest;
import com.easyops.hr.entity.LeaveType;
import com.easyops.hr.entity.PayrollTimeAttendancePolicy;
import com.easyops.hr.entity.Timesheet;
import com.easyops.hr.repository.AttendanceRecordRepository;
import com.easyops.hr.repository.HolidayRepository;
import com.easyops.hr.repository.LeaveRequestRepository;
import com.easyops.hr.repository.LeaveTypeRepository;
import com.easyops.hr.repository.TimesheetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollAttendanceRollupServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private TimesheetRepository timesheetRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private PayrollAttendanceRollupService service;

    @org.junit.jupiter.api.BeforeEach
    void stubNoScopedData() {
        lenient().when(holidayRepository.findHolidaysInRange(any(), any(), any())).thenReturn(List.of());
        lenient().when(leaveRequestRepository.findApprovedOverlappingPeriod(any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void countWeekdays_excludesWeekends() {
        assertEquals(5, PayrollAttendanceRollupService.countWeekdays(
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10)));
    }

    @Test
    void rollup_prefersAttendanceOvertimeOverTimesheet() {
        Employee emp = sampleEmployee();
        PayrollTimeAttendancePolicy pol = policy(false, false);
        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceDate(LocalDate.of(2026, 4, 7));
        ar.setStatus("present");
        ar.setOvertimeHours(new BigDecimal("2"));
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ar));
        Timesheet ts = new Timesheet();
        ts.setStatus("APPROVED");
        ts.setOvertimeHours(new BigDecimal("10"));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of(ts));

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertTrue(r.hasAttendanceOrTimesheetData());
        assertEquals(new BigDecimal("2"), r.overtimeHours());
    }

    @Test
    void rollup_usesTimesheetOvertimeWhenNoAttendanceOt() {
        Employee emp = sampleEmployee();
        PayrollTimeAttendancePolicy pol = policy(false, false);
        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceDate(LocalDate.of(2026, 4, 7));
        ar.setStatus("present");
        ar.setOvertimeHours(BigDecimal.ZERO);
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ar));
        Timesheet ts = new Timesheet();
        ts.setStatus("APPROVED");
        ts.setOvertimeHours(new BigDecimal("4"));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of(ts));

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(new BigDecimal("4"), r.overtimeHours());
    }

    @Test
    void rollup_classifiesLopStatuses() {
        Employee emp = sampleEmployee();
        PayrollTimeAttendancePolicy pol = policy(false, false);
        AttendanceRecord a1 = new AttendanceRecord();
        a1.setAttendanceDate(LocalDate.of(2026, 4, 7));
        a1.setStatus("absent");
        AttendanceRecord a2 = new AttendanceRecord();
        a2.setAttendanceDate(LocalDate.of(2026, 4, 8));
        a2.setStatus("leave_without_pay");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(a1, a2));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(new BigDecimal("2"), r.lopDays());
    }

    @Test
    void rollup_halfDayCountsAsHalfPresent() {
        Employee emp = sampleEmployee();
        PayrollTimeAttendancePolicy pol = policy(false, false);
        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceDate(LocalDate.of(2026, 4, 7));
        ar.setStatus("half_day");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ar));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(new BigDecimal("0.5"), r.presentDays());
    }

    /**
     * TS-05 regression (§8): with bridge disabled, infer_missing_weekday_lop unchanged vs pre–Phase B for mixed attendance —
     * no leave-repository involvement.
     */
    @Test
    void rollup_inferMissingWeekdaysAsLopWhenEnabled_bridgeDisabled() {
        Employee emp = sampleEmployee();
        PayrollTimeAttendancePolicy pol = policy(true, false);
        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceDate(LocalDate.of(2026, 4, 6));
        ar.setStatus("present");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ar));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(new BigDecimal("4"), r.lopDays());
        verifyNoInteractions(leaveRequestRepository);
    }

    /** AC-11 (missing attendance rows): paid approved leave suppresses inferred LOP when bridge on. */
    @Test
    void rollup_leaveBridge_paidApprovedSuppressesInferredLop() {
        Employee emp = sampleEmployee();
        UUID leaveTypeId = UUID.randomUUID();
        PayrollTimeAttendancePolicy pol = policy(true, true);
        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceDate(LocalDate.of(2026, 4, 6));
        ar.setStatus("present");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ar));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        LeaveRequest lr = new LeaveRequest();
        lr.setLeaveTypeId(leaveTypeId);
        lr.setStartDate(LocalDate.of(2026, 4, 7));
        lr.setEndDate(LocalDate.of(2026, 4, 10));
        lr.setStatus("approved");
        when(leaveRequestRepository.findApprovedOverlappingPeriod(any(), any(), any(), any())).thenReturn(List.of(lr));

        LeaveType lt = new LeaveType();
        lt.setLeaveTypeId(leaveTypeId);
        lt.setIsPaid(true);
        when(leaveTypeRepository.findAllById(any())).thenReturn(List.of(lt));

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(BigDecimal.ZERO, r.lopDays());
        assertEquals(new BigDecimal("4"), r.leaveDays());
    }

    /** AC-11 (dual entry): explicit attendance LOP/absent is treated as paid leave when bridge on and paid leave approved. */
    @Test
    void rollup_leaveBridge_reclassifiesExplicitLopRowWhenPaidApprovedCoversDate() {
        Employee emp = sampleEmployee();
        UUID leaveTypeId = UUID.randomUUID();
        PayrollTimeAttendancePolicy pol = policy(false, true);

        AttendanceRecord bad = new AttendanceRecord();
        bad.setAttendanceDate(LocalDate.of(2026, 4, 8));
        bad.setStatus("lop");
        AttendanceRecord ok = new AttendanceRecord();
        ok.setAttendanceDate(LocalDate.of(2026, 4, 7));
        ok.setStatus("present");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ok, bad));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        LeaveRequest lr = new LeaveRequest();
        lr.setLeaveTypeId(leaveTypeId);
        lr.setStartDate(LocalDate.of(2026, 4, 8));
        lr.setEndDate(LocalDate.of(2026, 4, 8));
        lr.setStatus("approved");
        when(leaveRequestRepository.findApprovedOverlappingPeriod(any(), any(), any(), any())).thenReturn(List.of(lr));

        LeaveType lt = new LeaveType();
        lt.setLeaveTypeId(leaveTypeId);
        lt.setIsPaid(true);
        when(leaveTypeRepository.findAllById(any())).thenReturn(List.of(lt));

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(BigDecimal.ZERO, r.lopDays());
        assertEquals(BigDecimal.ONE, r.leaveDays());
    }

    /** HR-LV-03: attendance tagged paid-leave becomes LOP when only unpaid approved leave covers that date (bridge on). */
    @Test
    void rollup_leaveBridge_reclassifiesPaidLeaveAttendanceToLopWhenOnlyUnpaidApprovedCoversDate() {
        Employee emp = sampleEmployee();
        UUID leaveTypeId = UUID.randomUUID();
        PayrollTimeAttendancePolicy pol = policy(false, true);

        AttendanceRecord row = new AttendanceRecord();
        row.setAttendanceDate(LocalDate.of(2026, 4, 8));
        row.setStatus("annual_leave");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(row));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        LeaveRequest lr = new LeaveRequest();
        lr.setLeaveTypeId(leaveTypeId);
        lr.setStartDate(LocalDate.of(2026, 4, 8));
        lr.setEndDate(LocalDate.of(2026, 4, 8));
        lr.setStatus("approved");
        when(leaveRequestRepository.findApprovedOverlappingPeriod(any(), any(), any(), any())).thenReturn(List.of(lr));

        LeaveType lt = new LeaveType();
        lt.setLeaveTypeId(leaveTypeId);
        lt.setIsPaid(false);
        when(leaveTypeRepository.findAllById(any())).thenReturn(List.of(lt));

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        assertEquals(BigDecimal.ZERO, r.leaveDays());
        assertEquals(BigDecimal.ONE, r.lopDays());
    }

    @Test
    void rollup_excludeHolidayFromLopInference_skipsInfer() {
        Employee emp = sampleEmployee();
        emp.setDepartmentId(UUID.randomUUID());
        PayrollTimeAttendancePolicy pol = PayrollTimeAttendancePolicy.builder()
                .organizationId(UUID.randomUUID())
                .inferMissingWeekdayLop(true)
                .leavePayrollBridgeEnabled(false)
                .excludeActiveHolidaysFromLopInference(true)
                .build();

        Holiday h = new Holiday();
        h.setHolidayDate(LocalDate.of(2026, 4, 8)); // Wednesday in range Apr 6–10 2026
        h.setIsActive(true);
        when(holidayRepository.findHolidaysInRange(any(), any(), any())).thenReturn(List.of(h));

        AttendanceRecord ar = new AttendanceRecord();
        ar.setAttendanceDate(LocalDate.of(2026, 4, 6));
        ar.setStatus("present");
        when(attendanceRecordRepository.findEmployeeAttendanceInRange(any(), any(), any())).thenReturn(List.of(ar));
        when(timesheetRepository.findByEmployeeIdAndWeekOverlappingPeriod(any(), any(), any())).thenReturn(List.of());

        PayrollAttendanceRollup r = service.rollupForPayPeriod(UUID.randomUUID(), emp,
                LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 10), pol);

        // Without holiday exclusion: 4 inferred LOP for Tue–Fri; Wed is holiday → 3 inferred LOP
        assertEquals(new BigDecimal("3"), r.lopDays());
    }

    private static PayrollTimeAttendancePolicy policy(boolean inferMissing, boolean bridge) {
        return PayrollTimeAttendancePolicy.builder()
                .organizationId(UUID.randomUUID())
                .inferMissingWeekdayLop(inferMissing)
                .leavePayrollBridgeEnabled(bridge)
                .unpaidApprovedLeaveCountsAsLop(true)
                .excludeActiveHolidaysFromWorkingDays(false)
                .excludeActiveHolidaysFromLopInference(false)
                .build();
    }

    private static Employee sampleEmployee() {
        Employee emp = new Employee();
        emp.setEmployeeId(UUID.randomUUID());
        emp.setOrganizationId(UUID.randomUUID());
        emp.setHireDate(LocalDate.of(2020, 1, 1));
        return emp;
    }
}
