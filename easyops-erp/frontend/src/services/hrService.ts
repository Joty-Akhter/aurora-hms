import api from './api';
import { Department } from '../types/organization';

export interface EpfAccount {
  epfAccountId: string;
  employeeId: string;
  organizationId: string;
  epfAccountNumber: string;
  uanNumber?: string;
  accountStatus: string;
  openingDate: string;
  currentBalance: number;
  employeeContributionBalance: number;
  employerContributionBalance: number;
  interestBalance: number;
  isActive: boolean;
}

export interface EpfContribution {
  contributionId: string;
  epfAccountId: string;
  employeeId: string;
  contributionMonth: number;
  contributionYear: number;
  employeeBasicSalary: number;
  employeeContributionRate: number;
  employerContributionRate: number;
  employeeContributionAmount: number;
  employerContributionAmount: number;
  totalContribution: number;
}

const hrService = {
  // Provident Fund APIs
  getEpfAccounts: (organizationId: string) =>
    api.get<EpfAccount[]>(`/api/hr/provident-fund/accounts?organizationId=${organizationId}`),
  
  getEpfAccount: (accountId: string) =>
    api.get<EpfAccount>(`/api/hr/provident-fund/accounts/${accountId}`),
  
  createEpfAccount: (data: Partial<EpfAccount>) =>
    api.post<EpfAccount>('/api/hr/provident-fund/accounts', data),
  
  getContributions: (accountId: string) =>
    api.get<EpfContribution[]>(`/api/hr/provident-fund/contributions?epfAccountId=${accountId}`),
  
  createContribution: (data: Partial<EpfContribution>) =>
    api.post<EpfContribution>('/api/hr/provident-fund/contributions', data),
  
  calculateInterest: (accountId: string, financialYear: string, interestRate: number) =>
    api.post(`/api/hr/provident-fund/interest/calculate`, {
      epfAccountId: accountId,
      financialYear,
      interestRate,
    }),
  
  getWithdrawals: (accountId: string) =>
    api.get(`/api/hr/provident-fund/withdrawals?epfAccountId=${accountId}`),
  
  createWithdrawal: (data: any) =>
    api.post('/api/hr/provident-fund/withdrawals', data),
  
  getEmployeeStatement: (employeeId: string, accountId: string, startDate?: string, endDate?: string) => {
    let url = `/api/hr/provident-fund/employee/statements?employeeId=${employeeId}&epfAccountId=${accountId}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    return api.get(url);
  },
  
  // Phase 6 APIs - Advanced Provident Fund
  getProvidentFundRecommendations: (employeeId: string, organizationId: string) =>
    api.get(`/api/hr/provident-fund/advanced/recommendations?employeeId=${employeeId}&organizationId=${organizationId}`),
  
  optimizeProvidentFundContributions: (accountId: string) =>
    api.get(`/api/hr/provident-fund/advanced/optimize?epfAccountId=${accountId}`),
  
  forecastProvidentFund: (accountId: string, months: number, interestRate?: number) =>
    api.get(`/api/hr/provident-fund/advanced/forecast?epfAccountId=${accountId}&months=${months}${interestRate ? `&interestRate=${interestRate}` : ''}`),
  
  assessProvidentFundRisk: (accountId: string) =>
    api.get(`/api/hr/provident-fund/advanced/risk-assessment?epfAccountId=${accountId}`),
  
  checkProvidentFundCompliance: (organizationId: string, month: number, year: number) =>
    api.get(`/api/hr/provident-fund/advanced/compliance/check?organizationId=${organizationId}&month=${month}&year=${year}`),
  
  getProvidentFundParticipationMetrics: (organizationId: string) =>
    api.get(`/api/hr/provident-fund/advanced/analytics/participation?organizationId=${organizationId}`),
  
  getProvidentFundCostAnalysis: (organizationId: string, year: number) =>
    api.get(`/api/hr/provident-fund/advanced/analytics/costs?organizationId=${organizationId}&year=${year}`),
  
  getProvidentFundROI: (organizationId: string, year: string) =>
    api.get(`/api/hr/provident-fund/advanced/analytics/roi?organizationId=${organizationId}&year=${year}`),
  
  getProvidentFundImpactAnalysis: (organizationId: string) =>
    api.get(`/api/hr/provident-fund/advanced/analytics/impact?organizationId=${organizationId}`),
  
  // Employee Self-Service for PF
  getMyEpfAccount: (employeeId: string) =>
    api.get(`/api/hr/provident-fund/employee/account?employeeId=${employeeId}`),
  
  getMyContributions: (employeeId: string, epfAccountId?: string) =>
    api.get(`/api/hr/provident-fund/employee/contributions?employeeId=${employeeId}${epfAccountId ? `&epfAccountId=${epfAccountId}` : ''}`),
  
  getMyWithdrawals: (employeeId: string) =>
    api.get(`/api/hr/provident-fund/employee/withdrawals?employeeId=${employeeId}`),
  
  submitWithdrawalRequest: (data: any) =>
    api.post('/api/hr/provident-fund/employee/withdrawals', data),
  
  getMyNominations: (employeeId: string, epfAccountId: string) =>
    api.get(`/api/hr/provident-fund/employee/nominations?employeeId=${employeeId}&epfAccountId=${epfAccountId}`),
  
  createMyNomination: (data: any) =>
    api.post('/api/hr/provident-fund/employee/nominations', data),
  
  downloadMyStatement: (employeeId: string, epfAccountId: string, startDate?: string, endDate?: string) => {
    let url = `/api/hr/provident-fund/employee/statements?employeeId=${employeeId}&epfAccountId=${epfAccountId}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    return api.get(url);
  },
  
  // Phase 7 APIs - Reporting & Analytics
  // Provident Fund Reporting
  getExecutiveDashboard: (organizationId: string) =>
    api.get(`/api/hr/provident-fund/reports/executive-dashboard?organizationId=${organizationId}`),
  
  getManagerTeamReport: (managerId: string, departmentId: string, organizationId: string) =>
    api.get(`/api/hr/provident-fund/reports/manager-team?managerId=${managerId}&departmentId=${departmentId}&organizationId=${organizationId}`),
  
  getEmployeeStatementReport: (employeeId: string, epfAccountId: string, startDate?: string, endDate?: string) => {
    let url = `/api/hr/provident-fund/reports/employee-statement?employeeId=${employeeId}&epfAccountId=${epfAccountId}`;
    if (startDate) url += `&startDate=${startDate}`;
    if (endDate) url += `&endDate=${endDate}`;
    return api.get(url);
  },
  
  getComplianceReport: (organizationId: string, startDate: string, endDate: string) =>
    api.get(`/api/hr/provident-fund/reports/compliance?organizationId=${organizationId}&startDate=${startDate}&endDate=${endDate}`),
  
  getCostAnalysisReport: (organizationId: string, year: number) =>
    api.get(`/api/hr/provident-fund/reports/cost-analysis?organizationId=${organizationId}&year=${year}`),
  
  getTrendAnalysisReport: (organizationId: string, months?: number) =>
    api.get(`/api/hr/provident-fund/reports/trend-analysis?organizationId=${organizationId}${months ? `&months=${months}` : ''}`),
  
  // Advanced Analytics
  forecastProvidentFundParticipation: (organizationId: string, months?: number) =>
    api.get(`/api/hr/analytics/forecast/provident-fund-participation?organizationId=${organizationId}${months ? `&months=${months}` : ''}`),
  
  analyzeTrendsAndPatterns: (organizationId: string, entityType: string, months?: number) =>
    api.get(`/api/hr/analytics/trends?organizationId=${organizationId}&entityType=${entityType}${months ? `&months=${months}` : ''}`),
  
  buildCustomReport: (reportConfig: any) =>
    api.post('/api/hr/analytics/custom-report', reportConfig),
  
  getAvailableReportTypes: () =>
    api.get('/api/hr/analytics/custom-report/types'),
  
  // Scheduled Reporting
  createScheduledReport: (data: any) =>
    api.post('/api/hr/analytics/scheduled-reports', data),
  
  getScheduledReports: (organizationId: string) =>
    api.get(`/api/hr/analytics/scheduled-reports?organizationId=${organizationId}`),
  
  updateScheduledReport: (reportId: string, data: any) =>
    api.put(`/api/hr/analytics/scheduled-reports/${reportId}`, data),
  
  executeScheduledReport: (reportId: string) =>
    api.post(`/api/hr/analytics/scheduled-reports/${reportId}/execute`),
  
  // Phase 8 APIs - Integration & Testing (placeholder - would need actual endpoints)
  getIntegrationStatus: (organizationId: string) =>
    api.get(`/api/hr/integration/status?organizationId=${organizationId}`),
  
  getSystemHealth: () =>
    api.get('/api/hr/system/health'),
  
  getPerformanceMetrics: (organizationId: string) =>
    api.get(`/api/hr/system/performance?organizationId=${organizationId}`),

  // HR Dashboard APIs
  getDashboardStats: (organizationId: string) =>
    api.get(`/api/hr/dashboard/stats?organizationId=${organizationId}`),

  getRecentHires: (organizationId: string, limit: number = 5) =>
    api.get(`/api/hr/dashboard/recent-hires?organizationId=${organizationId}&limit=${limit}`),

  getHeadcountByDepartment: (organizationId: string) =>
    api.get(`/api/hr/dashboard/headcount-by-department?organizationId=${organizationId}`),
};

export interface HrDashboardStats {
  total_employees?: number;
  active_employees?: number;
  department_count?: number;
  position_count?: number;
  recent_hires_30d?: number;
  avg_tenure_years?: number;
  full_time_employees?: number;
  part_time_employees?: number;
  contract_employees?: number;
  intern_employees?: number;
}

// Export individual functions for convenience - defined directly to avoid module loading issues
export const getDashboardStats = (organizationId: string) =>
  api.get(`/api/hr/dashboard/stats?organizationId=${organizationId}`);

export const getRecentHires = (organizationId: string, limit: number = 5) =>
  api.get(`/api/hr/dashboard/recent-hires?organizationId=${organizationId}&limit=${limit}`);

export const getHeadcountByDepartment = (organizationId: string) =>
  api.get(`/api/hr/dashboard/headcount-by-department?organizationId=${organizationId}`);

// Employee Management APIs
export interface Employee {
  employeeId?: string;
  id?: string;
  organizationId: string;
  userId?: string;
  employeeNumber: string;
  name: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: string;
  hireDate: string;
  terminationDate?: string;
  departmentId?: string;
  positionId?: string;
  managerId?: string;
  employmentType?: string;
  employmentStatus?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  stateProvince?: string;
  postalCode?: string;
  country?: string;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  emergencyContactRelationship?: string;
  bankName?: string;
  bankBranch?: string;
  bankAccountNumber?: string;
  bankRoutingOrIban?: string;
  payrollOvertimeRateMultiplier?: number | null;
  payrollStandardHoursPerDay?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface Position {
  positionId?: string;
  id?: string;
  organizationId: string;
  title: string;
  description?: string;
  departmentId?: string;
  level?: string;
  /** HR-MD-03: lower rank number = junior; optional sort key */
  hierarchyRank?: number | null;
  salaryRangeMin?: number;
  salaryRangeMax?: number;
  currency?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Re-export Department type
export type { Department };

// Employee functions
/** Linked employee for current user — no HR_VIEW required (Phase A self-service). */
export const getMyEmployeeProfile = (organizationId: string) =>
  api.get<Employee>(`/api/hr/employees/me`, { params: { organizationId } });

export const getEmployees = (organizationId: string, params?: { status?: string; departmentId?: string; search?: string }) => {
  const queryParams: any = { organizationId };
  if (params?.status) queryParams.status = params.status;
  if (params?.departmentId) queryParams.departmentId = params.departmentId;
  if (params?.search) queryParams.search = params.search;
  return api.get<Employee[]>(`/api/hr/employees`, { params: queryParams });
};

export interface EmployeePagedResponse {
  content: Employee[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}

export const getEmployeesPaged = (
  organizationId: string,
  params: { status?: string; departmentId?: string; search?: string; page: number; size: number }
) => {
  const queryParams: Record<string, string | number> = {
    organizationId,
    page: params.page,
    size: params.size,
  };
  if (params.status) queryParams.status = params.status;
  if (params.departmentId) queryParams.departmentId = params.departmentId;
  if (params.search) queryParams.search = params.search;
  return api.get<EmployeePagedResponse>(`/api/hr/employees`, { params: queryParams });
};

export const getEmployeeById = (employeeId: string) =>
  api.get<Employee>(`/api/hr/employees/${employeeId}`);

export const createEmployee = (employee: Partial<Employee>) =>
  api.post<Employee>('/api/hr/employees', employee);

export const updateEmployee = (employeeId: string, employee: Partial<Employee>) =>
  api.put<Employee>(`/api/hr/employees/${employeeId}`, employee);

export const deleteEmployee = (employeeId: string) =>
  api.delete(`/api/hr/employees/${employeeId}`);

// Department functions (from HR service)
export const getDepartments = (organizationId: string, options?: { activeOnly?: boolean; parentDepartmentId?: string }) => {
  const params: any = { organizationId };
  if (options?.activeOnly !== undefined) params.activeOnly = options.activeOnly;
  if (options?.parentDepartmentId) params.parentDepartmentId = options.parentDepartmentId;
  return api.get<Department[]>(`/api/hr/departments`, { params });
};

// Position functions
export const getPositions = (organizationId: string, options?: { activeOnly?: boolean; departmentId?: string }) => {
  const params: any = { organizationId };
  if (options?.activeOnly !== undefined) params.activeOnly = options.activeOnly;
  if (options?.departmentId) params.departmentId = options.departmentId;
  return api.get<Position[]>(`/api/hr/positions`, { params });
};

export const getPositionById = (positionId: string) =>
  api.get<Position>(`/api/hr/positions/${positionId}`);

export const createPosition = (position: Partial<Position>) =>
  api.post<Position>(`/api/hr/positions`, position);

export const updatePosition = (positionId: string, position: Partial<Position>) =>
  api.put<Position>(`/api/hr/positions/${positionId}`, position);

export const deletePosition = (positionId: string) =>
  api.delete(`/api/hr/positions/${positionId}`);

// Attendance functions
export const getTodayAttendance = (organizationId: string, date?: string) => {
  const params: any = { organizationId };
  if (date) params.date = date;
  return api.get(`/api/hr/attendance/today`, { params });
};

export const clockIn = (data: { organizationId: string; employeeId?: string; userId?: string; workLocation?: string }) =>
  api.post(`/api/hr/attendance/clock-in`, data);

export const clockOut = (employeeId: string) =>
  api.post(`/api/hr/attendance/clock-out`, { employeeId });

// Timesheet functions
export const getTimesheets = (organizationId: string, options?: { employeeId?: string; status?: string }) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.status) params.status = options.status;
  return api.get(`/api/hr/timesheets`, { params });
};

export const createTimesheet = (timesheet: any) =>
  api.post(`/api/hr/timesheets`, timesheet);

export const submitTimesheet = (timesheetId: string) =>
  api.post(`/api/hr/timesheets/${timesheetId}/submit`);

// Leave functions
export const getLeaveTypes = (organizationId: string) =>
  api.get(`/api/hr/leave/types`, { params: { organizationId } });

export interface DepartmentLeaveApproverRow {
  stepOrder: number;
  approverEmployeeId: string;
}

export const getDepartmentLeaveApprovers = (organizationId: string, departmentId: string) =>
  api.get<DepartmentLeaveApproverRow[]>(`/api/hr/departments/${departmentId}/leave-approvers`, {
    params: { organizationId },
  });

export const replaceDepartmentLeaveApprovers = (
  organizationId: string,
  departmentId: string,
  rows: DepartmentLeaveApproverRow[]
) =>
  api.put<DepartmentLeaveApproverRow[]>(`/api/hr/departments/${departmentId}/leave-approvers`, rows, {
    params: { organizationId },
  });

export const getLeaveRequests = (
  organizationId: string,
  options?: { employeeId?: string; status?: string; pendingForApproverEmployeeId?: string }
) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.status) params.status = options.status;
  if (options?.pendingForApproverEmployeeId)
    params.pendingForApproverEmployeeId = options.pendingForApproverEmployeeId;
  return api.get(`/api/hr/leave/requests`, { params });
};

export const createLeaveRequest = (leaveRequest: any) =>
  api.post(`/api/hr/leave/requests`, leaveRequest);

export const approveLeaveRequestApi = (leaveRequestId: string, approvedBy: string) =>
  api.post(`/api/hr/leave/requests/${leaveRequestId}/approve`, { approvedBy });

export const rejectLeaveRequestApi = (
  leaveRequestId: string,
  rejectedBy: string,
  rejectionReason?: string
) => api.post(`/api/hr/leave/requests/${leaveRequestId}/reject`, { rejectedBy, rejectionReason });

export const getLeaveBalances = (employeeId: string, organizationId: string) =>
  api.get(`/api/hr/leave/balances`, { params: { employeeId, organizationId } });

// Payroll functions
export const getPayrollRuns = (organizationId: string, status?: string) => {
  const params: any = { organizationId };
  if (status) params.status = status;
  return api.get(`/api/hr/payroll/runs`, { params });
};

export const createPayrollRun = (payrollRun: any) =>
  api.post(`/api/hr/payroll/runs`, payrollRun);

/** ES-27: Process payroll run – processedBy optional (when user has linked employee). */
export const processPayrollRun = (runId: string, processedBy?: string) =>
  api.post(`/api/hr/payroll/runs/${runId}/process`, processedBy ? { processedBy } : {});

/** Approve payroll run – approvedBy optional. Only for PROCESSED runs. */
export const approvePayrollRun = (runId: string, approvedBy?: string) =>
  api.post(`/api/hr/payroll/runs/${runId}/approve`, approvedBy ? { approvedBy } : {});

/** Get payroll details (payslips) for a run. */
export const getPayrollDetails = (runId: string) =>
  api.get(`/api/hr/payroll/runs/${runId}/details`);

/** Mark a payroll detail (payslip) as paid. */
export const markPayrollDetailAsPaid = (detailId: string, paymentReference: string) =>
  api.post(`/api/hr/payroll/details/${detailId}/mark-paid`, { paymentReference });

/** Get payroll accounting export (for GL posting). */
export const getPayrollAccountingExport = (runId: string) =>
  api.get(`/api/hr/payroll/runs/${runId}/accounting-export`);

/** Post payroll run to accounting (create journal entry). */
export const postPayrollToAccounting = (runId: string) =>
  api.post(`/api/hr/payroll/runs/${runId}/post-to-accounting`);

/** Create EPF contributions from payroll run PF components. */
export const processEpfFromPayroll = (runId: string) =>
  api.post(`/api/hr/payroll/runs/${runId}/process-epf`);

/** Post EPF contributions to accounting for the payroll run period. */
export const postEpfToAccounting = (runId: string) =>
  api.post(`/api/hr/payroll/runs/${runId}/post-epf-to-accounting`);

/** ES-22–ES-27: Populate payroll from salary (assignment + components) for a DRAFT run. */
/** INT-09 / INT-12: Organization EPF rates, PF wage ceiling/floor, employment eligibility. */
export interface EpfOrganizationPolicy {
  organizationId: string;
  employeeContributionRate?: number | string;
  employerContributionRate?: number | string;
  pfWageCeiling?: number | string | null;
  pfWageFloor?: number | string | null;
  eligibleEmploymentTypes?: string | null;
  ineligibleEmploymentTypes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export const getEpfOrganizationPolicy = (organizationId: string) =>
  api.get<EpfOrganizationPolicy>(`/api/hr/epf/organization-policy`, { params: { organizationId } });

export const putEpfOrganizationPolicy = (body: EpfOrganizationPolicy) =>
  api.put<EpfOrganizationPolicy>(`/api/hr/epf/organization-policy`, body);

/** Response from populate-from-salary (DRAFT run). */
export interface PayrollPopulationSummaryDto {
  payrollRunId?: string;
  employeesPopulated: number;
  employeesWithoutAssignment?: string[];
  employeesMissingBasic?: string[];
  warnings?: string[];
}

export const populatePayrollFromSalary = (runId: string) =>
  api.post<PayrollPopulationSummaryDto>(`/api/hr/payroll/runs/${runId}/populate-from-salary`);

export const getPayrollStats = (organizationId: string) =>
  api.get(`/api/hr/payroll/stats`, { params: { organizationId } });

export interface PayrollTimeAttendancePolicyDto {
  organizationId?: string;
  overtimeRateMultiplier?: number | string;
  inferMissingWeekdayLop?: boolean;
  standardHoursPerDay?: number | string;
  /** HR-LV-03 — suppress inferred LOP when approved paid leave covers weekdays without attendance. */
  leavePayrollBridgeEnabled?: boolean;
  unpaidApprovedLeaveCountsAsLop?: boolean;
  /** HR-LV-02 — payroll rollup denominators / inferred LOP. */
  excludeActiveHolidaysFromWorkingDays?: boolean;
  excludeActiveHolidaysFromLopInference?: boolean;
}

export interface HolidayRecord {
  holidayId?: string;
  organizationId: string;
  holidayName: string;
  holidayDate: string;
  holidayType?: string;
  description?: string;
  isRecurring?: boolean;
  isActive?: boolean;
  departmentId?: string | null;
  employeeId?: string | null;
}

export const getHolidays = (
  organizationId: string,
  params?: { startDate?: string; endDate?: string; activeOnly?: boolean }
) => api.get<HolidayRecord[]>('/api/hr/holidays', { params: { organizationId, ...params } });

export const createHoliday = (body: HolidayRecord) => api.post<HolidayRecord>('/api/hr/holidays', body);

export const updateHoliday = (id: string, body: HolidayRecord) =>
  api.put<HolidayRecord>(`/api/hr/holidays/${id}`, body);

export const deleteHoliday = (id: string, organizationId: string) =>
  api.delete(`/api/hr/holidays/${id}`, { params: { organizationId } });

/** HMS Phase C — shift master (HR-AT-01) */
export interface ShiftDefinition {
  shiftDefinitionId?: string;
  organizationId: string;
  code: string;
  name: string;
  shiftType?: string;
  graceMinutes?: number;
  expectedHours?: number;
  overtimeRateMultiplier?: number | null;
  isActive?: boolean;
}

export const getShiftDefinitions = (organizationId: string, activeOnly?: boolean) => {
  const params: Record<string, unknown> = { organizationId };
  if (activeOnly !== undefined) params.activeOnly = activeOnly;
  return api.get<ShiftDefinition[]>(`/api/hr/shift-definitions`, { params });
};

export const createShiftDefinition = (body: Partial<ShiftDefinition>) =>
  api.post<ShiftDefinition>(`/api/hr/shift-definitions`, body);

export const updateShiftDefinition = (id: string, body: Partial<ShiftDefinition>) =>
  api.put<ShiftDefinition>(`/api/hr/shift-definitions/${id}`, body);

/** HMS Phase C — roster (HR-AT-02) */
export interface RosterScheduleRow {
  scheduleId?: string;
  employeeId: string;
  employeeName?: string | null;
  shiftDate: string;
  shiftDefinitionId?: string | null;
  shiftDefinitionName?: string | null;
  shiftName?: string | null;
  startTime: string;
  endTime: string;
  breakDuration?: number | null;
  isOvertime?: boolean | null;
  notes?: string | null;
}

export interface RosterConflictWarning {
  warningType: string;
  employeeId: string;
  employeeName?: string | null;
  date: string;
  message: string;
}

export interface RosterMonthView {
  schedules: RosterScheduleRow[];
  holidays: HolidayRecord[];
  approvedLeaves: Array<{
    leaveRequestId?: string;
    employeeId?: string;
    startDate?: string;
    endDate?: string;
    totalDays?: number;
    leaveTypeId?: string;
  }>;
  conflictWarnings: RosterConflictWarning[];
}

export const getRosterMonthView = (
  organizationId: string,
  year: number,
  month: number,
  departmentId?: string
) => {
  const params: Record<string, unknown> = { organizationId, year, month };
  if (departmentId) params.departmentId = departmentId;
  return api.get<RosterMonthView>(`/api/hr/roster/month-view`, { params });
};

export interface RosterScheduleWritePayload {
  organizationId: string;
  employeeId: string;
  shiftDefinitionId?: string | null;
  shiftDate: string;
  shiftName?: string | null;
  startTime: string;
  endTime: string;
  breakDuration?: number | null;
  isOvertime?: boolean | null;
  notes?: string | null;
}

export const createRosterSchedule = (body: RosterScheduleWritePayload) =>
  api.post(`/api/hr/roster/schedules`, body);

export const updateRosterSchedule = (scheduleId: string, body: Partial<RosterScheduleWritePayload>) =>
  api.put(`/api/hr/roster/schedules/${scheduleId}`, body);

export const deleteRosterSchedule = (scheduleId: string, organizationId: string) =>
  api.delete(`/api/hr/roster/schedules/${scheduleId}`, { params: { organizationId } });

export const getPayrollTimeAttendancePolicy = (organizationId: string) =>
  api.get<PayrollTimeAttendancePolicyDto>(`/api/hr/payroll/time-attendance-policy`, { params: { organizationId } });

export const putPayrollTimeAttendancePolicy = (
  organizationId: string,
  body: Partial<PayrollTimeAttendancePolicyDto>
) =>
  api.put<PayrollTimeAttendancePolicyDto>(`/api/hr/payroll/time-attendance-policy`, body, {
    params: { organizationId },
  });

/** ES-26 / INT-14–INT-18: Payslip line with salary-master semantics for reporting. */
export interface PayslipLineDto {
  componentId?: string;
  componentCode?: string | null;
  componentName?: string | null;
  componentType?: string | null;
  displayOrder?: number | null;
  amount?: number | string | null;
  taxability?: string | null;
  statutoryType?: string | null;
  includedInPfWage?: boolean | null;
  includedInEsiWage?: boolean | null;
  isTaxable?: boolean | null;
}

/** ES-26: Payslip payload; YTD and periodTaxableGross align with statutory reporting (INT-14). */
export interface PayslipDto {
  payrollRunId?: string;
  payrollDetailId?: string;
  employeeId?: string;
  employeeName?: string | null;
  employeeNumber?: string | null;
  basicSalary?: number | string | null;
  grossSalary?: number | string | null;
  totalDeductions?: number | string | null;
  netSalary?: number | string | null;
  workingDays?: number | null;
  presentDays?: number | null;
  leaveDays?: number | string | null;
  overtimeHours?: number | string | null;
  overtimeAmount?: number | string | null;
  lopDays?: number | string | null;
  lopAmount?: number | string | null;
  currency?: string | null;
  payFrequency?: string | null;
  lines?: PayslipLineDto[];
  yearToDateGross?: number | string | null;
  yearToDateDeductions?: number | string | null;
  yearToDateNet?: number | string | null;
  yearToDateIncomeTaxWithheld?: number | string | null;
  periodTaxableGross?: number | string | null;
}

/** ES-26: Payslip view for an employee in a payroll run. ES-54: Use for download (e.g. browser print/PDF). */
export const getPayslip = (runId: string, employeeId: string) =>
  api.get<PayslipDto>(`/api/hr/payroll/runs/${runId}/payslip`, { params: { employeeId } });

/** ES-53: Employee self-service – view own salary (structure, grade, components; amounts maskable). */
export const getSelfSalarySummary = (
  employeeId: string,
  organizationId: string,
  options?: { asOfDate?: string; maskAmounts?: boolean }
) => {
  const params: Record<string, string | boolean> = { employeeId, organizationId };
  if (options?.asOfDate) params.asOfDate = options.asOfDate;
  if (options?.maskAmounts != null) params.maskAmounts = options.maskAmounts;
  return api.get(`/api/hr/salary/self/summary`, { params });
};

/** ES-54: Employee self-service – list payroll runs (payslips) for this employee. */
export const getMyPayslips = (employeeId: string, organizationId: string) =>
  api.get(`/api/hr/payroll/self/payslips`, { params: { employeeId, organizationId } });

// Salary functions
export const getSalaryStructures = (
  organizationId: string,
  options?: { effectiveDate?: string; includeInactive?: boolean }
) => {
  const params: Record<string, string | boolean> = { organizationId };
  if (options?.effectiveDate) params.effectiveDate = options.effectiveDate;
  if (options?.includeInactive != null) params.includeInactive = options.includeInactive;
  return api.get(`/api/hr/salary/structures`, { params });
};

export const getSalaryStructure = (id: string) =>
  api.get(`/api/hr/salary/structures/${id}`);

/** SS-42: Structure with nested grades and bands (summary view). */
export const getStructureSummary = (id: string, effectiveDate?: string) =>
  api.get(`/api/hr/salary/structures/${id}/summary`, { params: effectiveDate ? { effectiveDate } : {} });

/** SS-29: Revision history (audit log) for a structure. */
export const getStructureRevisionHistory = (structureId: string, organizationId: string) =>
  api.get(`/api/hr/salary/structures/${structureId}/revision-history`, { params: { organizationId } });

export const getStructureSummaries = (
  organizationId: string,
  options?: { effectiveDate?: string; includeInactive?: boolean }
) => {
  const params: Record<string, string | boolean> = { organizationId };
  if (options?.effectiveDate) params.effectiveDate = options.effectiveDate;
  if (options?.includeInactive != null) params.includeInactive = options.includeInactive;
  return api.get(`/api/hr/salary/structures/summaries`, { params });
};

export type EmployeeSalaryDetailOptions = { employeeId?: string; asOfDate?: string };

/** ES-46: Get employee salary details; optional employeeId and asOfDate for one active per component per date. */
export const getEmployeeSalaryDetails = (
  organizationId: string,
  options?: EmployeeSalaryDetailOptions
) => {
  const params: Record<string, string> = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.asOfDate) params.asOfDate = options.asOfDate;
  return api.get(`/api/hr/salary/details`, { params });
};

/** ES-01: Get active salary assignment for an employee as of a date (default today). */
export const getEmployeeAssignment = (
  employeeId: string,
  organizationId: string,
  asOfDate?: string
) => {
  const params: Record<string, string> = { organizationId };
  if (asOfDate) params.asOfDate = asOfDate;
  return api.get(`/api/hr/salary/assignments/employee/${employeeId}`, { params });
};

/** ES-18: Unified revision history (assignments + component details) for an employee. */
export const getEmployeeSalaryRevisionHistory = (
  employeeId: string,
  organizationId: string
) => {
  return api.get(`/api/hr/salary/employee/${employeeId}/revision-history`, {
    params: { organizationId },
  });
};

export const createSalaryStructure = (structure: any) =>
  api.post(`/api/hr/salary/structures`, structure);

export const updateSalaryStructure = (id: string, structure: any) =>
  api.put(`/api/hr/salary/structures/${id}`, structure);

// Salary grades (SS-33)
export const getGradesByStructure = (structureId: string) =>
  api.get(`/api/hr/salary/structures/${structureId}/grades`);
export const createGrade = (structureId: string, grade: any) =>
  api.post(`/api/hr/salary/structures/${structureId}/grades`, grade);
export const updateGrade = (gradeId: string, grade: any) =>
  api.put(`/api/hr/salary/grades/${gradeId}`, grade);

// Salary bands (SS-34)
export const getBandsByGrade = (gradeId: string) =>
  api.get(`/api/hr/salary/grades/${gradeId}/bands`);
export const createBand = (gradeId: string, band: any) =>
  api.post(`/api/hr/salary/grades/${gradeId}/bands`, band);
export const updateBand = (bandId: string, band: any) =>
  api.put(`/api/hr/salary/bands/${bandId}`, band);

export type SalaryComponentFilters = {
  effectiveDate?: string;
  type?: string;
  category?: string;
  includeInactive?: boolean;
};

export const getSalaryComponents = (organizationId: string, filters?: SalaryComponentFilters) => {
  const params: Record<string, string | boolean | undefined> = { organizationId };
  if (filters?.effectiveDate) params.effectiveDate = filters.effectiveDate;
  if (filters?.type) params.type = filters.type;
  if (filters?.category) params.category = filters.category;
  if (filters?.includeInactive !== undefined) params.includeInactive = filters.includeInactive;
  return api.get(`/api/hr/salary/components`, { params });
};

export const getSalaryComponentById = (id: string) =>
  api.get(`/api/hr/salary/components/${id}`);

/** Get component by organization and code. */
export const getSalaryComponentByCode = (organizationId: string, code: string) =>
  api.get(`/api/hr/salary/components/by-code`, { params: { organizationId, code } });

export const getComponentUsage = (id: string) =>
  api.get(`/api/hr/salary/components/${id}/usage`);

/** SC-48: Component master report export – returns blob URL for download. */
export const exportComponentMaster = (
  organizationId: string,
  format: 'excel' | 'pdf',
  options?: { effectiveDate?: string; includeInactive?: boolean; type?: string; category?: string }
) => {
  const params: Record<string, string | boolean> = { organizationId, format };
  if (options?.effectiveDate) params.effectiveDate = options.effectiveDate;
  if (options?.includeInactive != null) params.includeInactive = options.includeInactive;
  if (options?.type) params.type = options.type;
  if (options?.category) params.category = options.category;
  return api.get(`/api/hr/salary/components/export`, { params, responseType: 'blob' });
};

/** SC-49: Component dependency report (components that reference other components). */
export const getComponentDependencies = (organizationId: string) =>
  api.get(`/api/hr/salary/reports/component-dependencies`, { params: { organizationId } });

/** ES-58: Component-wise cost report – total amount per component across employees. */
export const getComponentWiseCost = (organizationId: string, asOfDate?: string) =>
  api.get(`/api/hr/salary/reports/component-cost`, { params: { organizationId, ...(asOfDate && { asOfDate }) } });

/** ES-58: Export component-wise cost to Excel or PDF. */
export const exportComponentWiseCost = (
  organizationId: string,
  format: 'excel' | 'pdf',
  options?: { asOfDate?: string }
) =>
  api.get(`/api/hr/salary/reports/component-cost/export`, {
    params: { organizationId, format, ...(options?.asOfDate && { asOfDate: options.asOfDate }) },
    responseType: 'blob',
  });

/** ES-59: Bulk export employee salary (assignment + components) for selected employees or by grade. */
export const bulkExportEmployeeSalary = (
  organizationId: string,
  format: 'excel' | 'pdf',
  options?: { asOfDate?: string; structureId?: string; gradeId?: string; bandId?: string; employeeIds?: string[] }
) => {
  const params: Record<string, string | string[] | undefined> = { organizationId, format };
  if (options?.asOfDate) params.asOfDate = options.asOfDate;
  if (options?.structureId) params.structureId = options.structureId;
  if (options?.gradeId) params.gradeId = options.gradeId;
  if (options?.bandId) params.bandId = options.bandId;
  if (options?.employeeIds?.length) params.employeeIds = options.employeeIds;
  return api.get(`/api/hr/salary/reports/employee-salary/bulk-export`, { params, responseType: 'blob' });
};

/** SC-50: Bulk import salary components from XLSX (sheet "Components"). */
export const bulkImportComponents = (organizationId: string, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post(`/api/hr/salary/components/bulk-import?organizationId=${organizationId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const createSalaryComponent = (component: any) =>
  api.post(`/api/hr/salary/components`, component);

export const updateSalaryComponent = (id: string, component: any) =>
  api.put(`/api/hr/salary/components/${id}`, component);

// Employee salary details (components)
export const createEmployeeSalaryDetail = (detail: any) =>
  api.post(`/api/hr/salary/employee/details`, detail);

/** ES-48: Update employee salary component (valueType, amount, percentage, effectiveTo). */
export const updateEmployeeSalaryDetail = (detailId: string, updates: any) =>
  api.put(`/api/hr/salary/employee/details/${detailId}`, updates);

/** ES-12: End component assignment with effective dating. */
export const endEmployeeSalaryDetail = (detailId: string, effectiveTo: string) =>
  api.delete(`/api/hr/salary/employee/details/${detailId}`, { params: { effectiveTo } });

// Employee salary assignments (structure/grade/band)
/** ES-01–ES-06: Create employee salary assignment. */
export const createEmployeeSalaryAssignment = (assignment: any) =>
  api.post(`/api/hr/salary/assignments`, assignment);

/** ES-01–ES-06: Update employee salary assignment (e.g. change grade/band or effectiveTo). */
export const updateEmployeeSalaryAssignment = (assignmentId: string, updates: any) =>
  api.put(`/api/hr/salary/assignments/${assignmentId}`, updates);

/** INT-28–INT-31: Default structure/grade/band from position (suggest assignment). */
export interface PositionSalaryDefaults {
  positionId?: string;
  organizationId?: string;
  defaultSalaryStructureId?: string | null;
  defaultSalaryGradeId?: string | null;
  defaultSalaryBandId?: string | null;
}

export const getPositionSalaryDefaults = (positionId: string) =>
  api.get<PositionSalaryDefaults>(`/api/hr/salary/positions/${positionId}/salary-defaults`);

/** ES-21: Bulk revision (e.g. % increase to a component by grade or structure). */
export interface SalaryBulkRevision {
  bulkRevisionId?: string;
  organizationId: string;
  revisionType?: string;
  targetType: 'BY_GRADE' | 'BY_STRUCTURE';
  targetGradeId?: string | null;
  targetStructureId?: string | null;
  componentCode: string;
  percentageValue: number;
  effectiveFrom: string;
  comment?: string | null;
  requestedBy?: string | null;
  status?: string;
  rowsApplied?: number;
  requestedAt?: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
  rejectionReason?: string | null;
}

export const createSalaryBulkRevision = (body: Partial<SalaryBulkRevision>) =>
  api.post<SalaryBulkRevision>(`/api/hr/salary/bulk-revisions`, body);

export const listSalaryBulkRevisions = (organizationId: string, status?: string) =>
  api.get<SalaryBulkRevision[]>(`/api/hr/salary/bulk-revisions`, {
    params: { organizationId, ...(status ? { status } : {}) },
  });

export const approveSalaryBulkRevision = (bulkRevisionId: string, approvedBy?: string) =>
  api.post<SalaryBulkRevision>(
    `/api/hr/salary/bulk-revisions/${bulkRevisionId}/approve`,
    null,
    { params: approvedBy ? { approvedBy } : {} }
  );

export const rejectSalaryBulkRevision = (bulkRevisionId: string, reason?: string) =>
  api.post<SalaryBulkRevision>(
    `/api/hr/salary/bulk-revisions/${bulkRevisionId}/reject`,
    null,
    { params: reason ? { reason } : {} }
  );

/** SS-48 / RPT-05: Grade & band headcount (for SS-22 warnings in Manage grades UI). */
export const getGradeHeadcountReport = (organizationId: string, asOfDate?: string) =>
  api.get<
    {
      structureId: string;
      gradeId?: string | null;
      bandId?: string | null;
      headcount: number;
      gradeName?: string;
      bandName?: string;
    }[]
  >(`/api/hr/salary/reports/grade-headcount`, {
    params: { organizationId, ...(asOfDate ? { asOfDate } : {}) },
  });

// Performance functions
export const getPerformanceReviews = (organizationId: string, options?: { employeeId?: string; cycleId?: string }) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.cycleId) params.cycleId = options.cycleId;
  return api.get(`/api/hr/performance/reviews`, { params });
};

export const getPerformanceCycles = (organizationId: string) =>
  api.get(`/api/hr/performance/cycles`, { params: { organizationId } });

// Goal functions
export const getGoals = (organizationId: string, options?: { employeeId?: string; status?: string }) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.status) params.status = options.status;
  return api.get(`/api/hr/goals`, { params });
};

export const createGoal = (goal: any) =>
  api.post(`/api/hr/goals`, goal);

// Development Plan functions
export const getDevelopmentPlans = (organizationId: string, options?: { employeeId?: string }) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  return api.get(`/api/hr/development-plans`, { params });
};

// Bonus functions
export interface BonusDto {
  bonusId?: string;
  employeeId: string;
  organizationId: string;
  bonusType: string;
  amount: number | string;
  currency?: string;
  bonusPeriod?: string;
  description?: string;
  status?: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
  paymentDate?: string | null;
  isTaxable?: boolean | null;
  payrollRunId?: string | null;
  createdAt?: string;
}

export const getBonuses = (
  organizationId: string,
  options?: { employeeId?: string; status?: string; payrollRunId?: string }
) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.status) params.status = options.status;
  if (options?.payrollRunId) params.payrollRunId = options.payrollRunId;
  return api.get<BonusDto[]>(`/api/hr/compensation/bonuses`, { params });
};

export const createBonus = (body: Partial<BonusDto>) =>
  api.post<BonusDto>(`/api/hr/compensation/bonuses`, body);

export const approveBonus = (bonusId: string, approvedBy: string) =>
  api.post<BonusDto>(`/api/hr/compensation/bonuses/${bonusId}/approve`, { approvedBy });

export const rejectBonus = (bonusId: string, rejectedBy: string, rejectionReason?: string) =>
  api.post<BonusDto>(`/api/hr/compensation/bonuses/${bonusId}/reject`, { rejectedBy, rejectionReason });

// HR-AT-03: Device attendance sync
export interface DevicePunchDto {
  employeeId?: string;
  deviceId?: string;
  punchTime: string; // ISO datetime
  punchType?: 'IN' | 'OUT' | 'UNKNOWN';
  rawEmployeeCode?: string;
  notes?: string;
}

export interface DeviceSyncResultDto {
  ingested: number;
  attendanceRecordsAffected: number;
  skipped: number;
  warnings?: string[];
}

export interface AttendanceRawLog {
  rawLogId?: string;
  organizationId?: string;
  employeeId?: string | null;
  deviceId?: string | null;
  punchTime: string;
  punchType?: string;
  source?: string;
  rawEmployeeCode?: string | null;
  processed?: boolean;
  processedAt?: string | null;
  attendanceRecordId?: string | null;
  notes?: string | null;
  createdAt?: string;
}

export const deviceSync = (organizationId: string, punches: DevicePunchDto[]) =>
  api.post<DeviceSyncResultDto>(`/api/hr/attendance/device-sync`, punches, {
    params: { organizationId },
  });

export const processDeviceLogs = (
  organizationId: string,
  from: string,
  to: string,
  standardHoursPerDay?: number
) =>
  api.post<DeviceSyncResultDto>(`/api/hr/attendance/device-sync/process`, null, {
    params: { organizationId, from, to, ...(standardHoursPerDay ? { standardHoursPerDay } : {}) },
  });

export const getDeviceLogs = (
  organizationId: string,
  params?: { from?: string; to?: string }
) =>
  api.get<AttendanceRawLog[]>(`/api/hr/attendance/device-logs`, {
    params: { organizationId, ...params },
  });

// Benefits functions
export const getBenefits = (organizationId: string) =>
  api.get(`/api/hr/benefits`, { params: { organizationId } });

export const getEmployeeBenefits = (employeeId: string, organizationId: string) =>
  api.get(`/api/hr/benefits/enrollments`, { params: { employeeId, organizationId } });

export const enrollEmployeeBenefit = (data: { employeeId: string; benefitId: string; organizationId: string }) =>
  api.post(`/api/hr/benefits/enrollments`, data);

// Reimbursement functions
export const getReimbursements = (organizationId: string, options?: { employeeId?: string; status?: string }) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.status) params.status = options.status;
  return api.get(`/api/hr/reimbursements`, { params });
};

export const createReimbursement = (reimbursement: any) =>
  api.post(`/api/hr/reimbursements`, reimbursement);

// Training functions
export const getTrainingCertifications = (organizationId: string, options?: { employeeId?: string; type?: string }) => {
  const params: any = { organizationId };
  if (options?.employeeId) params.employeeId = options.employeeId;
  if (options?.type) params.type = options.type;
  return api.get(`/api/hr/training`, { params });
};

// Department functions (additional)
export const getDepartmentById = (organizationId: string, departmentId: string) =>
  api.get<Department>(`/api/hr/departments/${departmentId}`, { params: { organizationId } });

export const createDepartment = (department: Partial<Department>, organizationId?: string) => {
  const params: any = {};
  if (organizationId) params.organizationId = organizationId;
  return api.post<Department>(`/api/hr/departments`, department, { params });
};

export const updateDepartment = (departmentId: string, department: Partial<Department>) =>
  api.put<Department>(`/api/hr/departments/${departmentId}`, department);

export const deleteDepartment = (departmentId: string, organizationId: string) =>
  api.delete(`/api/hr/departments/${departmentId}`, { params: { organizationId } });

// Employee loans — reporting & registers (Phase 6)
export const listLoanCategoriesApi = (organizationId: string, includeInactive = false) =>
  api.get(`/api/hr/loans/categories`, { params: { organizationId, includeInactive } });

/** RE-02: self-service — own loans (requires employee linked to user). */
export const getMyLoansSelf = (organizationId: string) =>
  api.get(`/api/hr/loans/self/my-loans`, { params: { organizationId } });

export const getMyLoanSelf = (organizationId: string, loanId: string) =>
  api.get(`/api/hr/loans/self/my-loans/${loanId}`, { params: { organizationId } });

/** RE-03 */
export const getLoanSelfNotifications = (organizationId: string, unreadOnly?: boolean) =>
  api.get(`/api/hr/loans/self/notifications`, {
    params: { organizationId, ...(unreadOnly != null ? { unreadOnly } : {}) },
  });

export const markLoanNotificationRead = (organizationId: string, eventId: string) =>
  api.patch(`/api/hr/loans/self/notifications/${eventId}/read`, null, { params: { organizationId } });

export const getLoanReportSummary = (organizationId: string) =>
  api.get(`/api/hr/loans/reports/summary`, { params: { organizationId } });

export const getLoanRegister = (
  organizationId: string,
  params?: { categoryId?: string; employeeId?: string; status?: string; categoryType?: string }
) => api.get(`/api/hr/loans/reports/register`, { params: { organizationId, ...params } });

export const getLoanSettlementAllocationPriority = (organizationId: string) =>
  api.get<string[]>(`/api/hr/loans/settlements/allocation-priority`, { params: { organizationId } });

export const getLoanArrearsReport = (organizationId: string, asOf?: string) =>
  api.get(`/api/hr/loans/reports/arrears`, { params: { organizationId, ...(asOf ? { asOf } : {}) } });

export const getLoanSettlementExitReport = (organizationId: string) =>
  api.get(`/api/hr/loans/reports/settlement-exit`, { params: { organizationId } });

/** Phase 7 (PI-05): loan disbursements + repayments for a period (JSON for integrations). */
export const getLoanAccountingExport = (organizationId: string, periodFrom: string, periodTo: string) =>
  api.get(`/api/hr/loans/reports/accounting-export`, { params: { organizationId, periodFrom, periodTo } });

export const exportLoanAccountingDisbursementsCsv = (
  organizationId: string,
  periodFrom: string,
  periodTo: string
) =>
  api.get(`/api/hr/loans/reports/accounting/disbursements/export`, {
    params: { organizationId, periodFrom, periodTo },
    responseType: 'blob',
  });

export const exportLoanAccountingRepaymentsCsv = (
  organizationId: string,
  periodFrom: string,
  periodTo: string
) =>
  api.get(`/api/hr/loans/reports/accounting/repayments/export`, {
    params: { organizationId, periodFrom, periodTo },
    responseType: 'blob',
  });

export const getLoanAuditLog = (organizationId: string, loanId: string) =>
  api.get(`/api/hr/loans/audit`, { params: { organizationId, loanId } });

/** RE-04: loan account audit + originating application workflow actions. */
export const getLoanCombinedAudit = (organizationId: string, loanId: string) =>
  api.get(`/api/hr/loans/audit/combined`, { params: { organizationId, loanId } });

/** RE-04: org-level loan audit (COA mapping changes, AD-03 bulk recalc); entity id = organization id. */
export interface LoanAuditLogRowDto {
  auditId: string;
  organizationId: string;
  entityType: string;
  entityId: string;
  action: string;
  performedBy?: string;
  performedAt: string;
  oldValues?: string;
  newValues?: string;
}

export const getLoanOrgAuditLogApi = (organizationId: string) =>
  api.get<LoanAuditLogRowDto[]>(`/api/hr/loans/audit/org`, { params: { organizationId } });

export const skipLoanInstallment = (
  organizationId: string,
  loanId: string,
  installmentId: string,
  body: { reason: string }
) =>
  api.post(`/api/hr/loans/accounts/${loanId}/installments/${installmentId}/skip`, body, {
    params: { organizationId },
  });

export const listEmployeeLoansApi = (organizationId: string, params?: { employeeId?: string; status?: string }) =>
  api.get(`/api/hr/loans/accounts`, { params: { organizationId, ...params } });

export const getEmployeeLoanApi = (organizationId: string, loanId: string) =>
  api.get(`/api/hr/loans/accounts/${loanId}`, { params: { organizationId } });

/** AD-03: re-apply weekend/holiday rules to unpaid installment due dates. */
export const recalculateLoanInstallmentHolidayDatesApi = (organizationId: string, loanId: string) =>
  api.post(`/api/hr/loans/accounts/${loanId}/installments/recalculate-holiday-dates`, null, {
    params: { organizationId },
  });

export interface LoanBulkHolidayRecalcFailureDto {
  loanId: string;
  message?: string;
}

export interface LoanBulkHolidayRecalcResultDto {
  organizationId: string;
  loansRecalculated: number;
  loansSkipped: number;
  failures?: LoanBulkHolidayRecalcFailureDto[];
}

/** AD-03: bulk re-apply for all active / settlement-pending loans with a disbursement date. */
export const recalculateLoanHolidayInstallmentDatesAllApi = (organizationId: string) =>
  api.post<LoanBulkHolidayRecalcResultDto>(
    `/api/hr/loans/accounts/recalculate-holiday-dates/all`,
    null,
    { params: { organizationId } }
  );

export const listLoanRepaymentsApi = (organizationId: string, loanId: string) =>
  api.get<LoanRepaymentTransactionDto[]>(`/api/hr/loans/accounts/${loanId}/repayments`, {
    params: { organizationId },
  });

/** Mirrors `LoanRepaymentTransaction` in hr-service (repayment ledger row). */
export interface LoanRepaymentTransactionDto {
  transactionId: string;
  loanId: string;
  amount: number;
  paymentDate: string;
  source: string;
  notes?: string;
  createdAt?: string;
  createdBy?: string;
  payrollRunId?: string;
  /** Original PAYROLL txn id when source is PAYROLL_REVERSAL (negative amount). */
  reversesTransactionId?: string;
}

/** RP-05: payroll reversal events for monitoring. */
export const getPayrollRecoveryAnomalies = (organizationId: string, params?: { since?: string }) =>
  api.get<LoanRepaymentAnomalyDto[]>(`/api/hr/loans/payroll-recoveries/anomalies`, {
    params: { organizationId, ...params },
  });

/** RP-05: payslip vs loan posting + missing postings for a payroll run. */
export const getPayrollRecoveryCrossCheck = (organizationId: string, payrollRunId: string) =>
  api.get<LoanRepaymentAnomalyDto[]>(`/api/hr/loans/payroll-recoveries/cross-check`, {
    params: { organizationId, payrollRunId },
  });

/** ST-04: legal workflow label on loan account. */
export const patchLoanLegalWorkflow = (
  organizationId: string,
  loanId: string,
  body: { legalWorkflowStatus: string }
) => api.patch(`/api/hr/loans/accounts/${loanId}/legal-workflow`, body, { params: { organizationId } });

export const reversePayrollLoanRepayment = (
  organizationId: string,
  loanId: string,
  transactionId: string,
  body: { reason: string }
) =>
  api.post(
    `/api/hr/loans/accounts/${loanId}/repayments/${transactionId}/reverse-payroll`,
    body,
    { params: { organizationId } }
  );

export const listLoanApplicationsApi = (
  organizationId: string,
  params?: { employeeId?: string; status?: string; categoryType?: string }
) => api.get(`/api/hr/loans/applications`, { params: { organizationId, ...params } });

export interface LoanApplicationDto {
  applicationId: string;
  organizationId: string;
  employeeId: string;
  categoryId: string;
  categoryType?: string;
  requestedAmount: number;
  requestedTenureMonths: number;
  purposeNotes?: string;
  attachmentReferences?: string[];
  delegatedToUserId?: string;
  clarificationMessage?: string;
  clarificationRequestedByUserId?: string;
  limitOverrideReason?: string;
  limitOverrideApprovedByUserId?: string;
  limitOverrideExpiresAt?: string;
  facilityOverrideReason?: string;
  facilityOverrideApprovedByUserId?: string;
  facilityOverrideExpiresAt?: string;
  status: string;
  applicationDate?: string;
  submittedAt?: string;
  decidedAt?: string;
  decidedByUserId?: string;
  rejectionReason?: string;
  createdAt?: string;
  updatedAt?: string;
  recommendedInstallmentAmount?: number;
  totalScheduledRecovery?: number;
  installmentPreviewNote?: string;
}

export interface LoanApplicationCreateBody {
  employeeId: string;
  categoryId: string;
  requestedAmount: number;
  requestedTenureMonths: number;
  purposeNotes?: string;
  attachmentReferences?: string[];
  limitOverrideReason?: string;
  facilityOverrideReason?: string;
}

export interface LoanApplicationUpdateBody {
  categoryId: string;
  requestedAmount: number;
  requestedTenureMonths: number;
  purposeNotes?: string;
  attachmentReferences?: string[];
  limitOverrideReason?: string;
  facilityOverrideReason?: string;
}

export interface LoanApplicationDecisionBody {
  comment?: string;
  limitOverrideExpiresAt?: string;
  facilityOverrideExpiresAt?: string;
}

export const getLoanApplicationApi = (organizationId: string, applicationId: string) =>
  api.get<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}`, {
    params: { organizationId },
  });

export const createLoanApplicationApi = (organizationId: string, body: LoanApplicationCreateBody) =>
  api.post<LoanApplicationDto>(`/api/hr/loans/applications`, body, { params: { organizationId } });

export const updateLoanApplicationApi = (
  organizationId: string,
  applicationId: string,
  body: LoanApplicationUpdateBody
) =>
  api.put<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}`, body, {
    params: { organizationId },
  });

export const submitLoanApplicationApi = (organizationId: string, applicationId: string) =>
  api.post<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}/submit`, {}, {
    params: { organizationId },
  });

export const approveLoanApplicationApi = (
  organizationId: string,
  applicationId: string,
  body?: LoanApplicationDecisionBody
) =>
  api.post<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}/approve`, body ?? {}, {
    params: { organizationId },
  });

export const rejectLoanApplicationApi = (
  organizationId: string,
  applicationId: string,
  body: { reason: string }
) =>
  api.post<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}/reject`, body, {
    params: { organizationId },
  });

export const cancelLoanApplicationApi = (organizationId: string, applicationId: string) =>
  api.post<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}/cancel`, {}, {
    params: { organizationId },
  });

export const requestLoanApplicationClarificationApi = (
  organizationId: string,
  applicationId: string,
  body: { message: string }
) =>
  api.post<LoanApplicationDto>(
    `/api/hr/loans/applications/${applicationId}/request-clarification`,
    body,
    { params: { organizationId } }
  );

export const delegateLoanApplicationApi = (
  organizationId: string,
  applicationId: string,
  body: { delegateToUserId: string }
) =>
  api.post<LoanApplicationDto>(`/api/hr/loans/applications/${applicationId}/delegate`, body, {
    params: { organizationId },
  });

export interface LoanApplicationActionDto {
  actionId: string;
  applicationId: string;
  actionType: string;
  actorUserId?: string;
  commentText?: string;
  createdAt?: string;
}

export const listLoanApplicationActionsApi = (organizationId: string, applicationId: string) =>
  api.get<LoanApplicationActionDto[]>(`/api/hr/loans/applications/${applicationId}/actions`, {
    params: { organizationId },
  });

export type LoanHolidayShiftMode = 'NEXT_BUSINESS_DAY' | 'PREVIOUS_BUSINESS_DAY';

export interface LoanOrganizationSettings {
  organizationId: string;
  minTenureMonths?: number;
  maxPrincipalAmount?: number;
  currency?: string;
  enforceSingleActiveLoan?: boolean;
  allowSalaryAdvanceWithActiveTermLoan?: boolean;
  disqualifyingEmploymentStatuses?: string[];
  settlementAllocationPriority?: string[];
  enforceSettlementAllocationOrder?: boolean;
  skipFinanceApproval?: boolean;
  salaryAdvanceSkipFinanceApproval?: boolean;
  /** AD-03: shift installment due dates off weekends and org holiday calendar. */
  shiftInstallmentDueDatesForHolidays?: boolean;
  loanHolidayShiftMode?: LoanHolidayShiftMode;
  createdAt?: string;
  updatedAt?: string;
}

export type LoanOrganizationSettingsPatch = Partial<
  Pick<
    LoanOrganizationSettings,
    | 'minTenureMonths'
    | 'maxPrincipalAmount'
    | 'currency'
    | 'enforceSingleActiveLoan'
    | 'allowSalaryAdvanceWithActiveTermLoan'
    | 'disqualifyingEmploymentStatuses'
    | 'settlementAllocationPriority'
    | 'enforceSettlementAllocationOrder'
    | 'skipFinanceApproval'
    | 'salaryAdvanceSkipFinanceApproval'
    | 'shiftInstallmentDueDatesForHolidays'
    | 'loanHolidayShiftMode'
  >
>;

export const getLoanOrganizationSettings = (organizationId: string) =>
  api.get<LoanOrganizationSettings>(`/api/hr/loans/settings`, { params: { organizationId } });

/** @deprecated Use {@link getLoanOrganizationSettings} — alias for older imports. */
export const getLoanOrganizationSettingsApi = getLoanOrganizationSettings;

export const patchLoanOrganizationSettingsApi = (
  organizationId: string,
  body: LoanOrganizationSettingsPatch
) =>
  api.patch<LoanOrganizationSettings>(`/api/hr/loans/settings`, body, { params: { organizationId } });

/** PI-05 optional: GL account codes on accounting export lines. */
export interface LoanAccountingCoaMappingDto {
  mappingId?: string;
  organizationId?: string;
  mappingKey: string;
  debitAccountCode: string;
  creditAccountCode: string;
  notes?: string;
}

export type LoanAccountingCoaMappingUpsertBody = {
  mappingKey: string;
  debitAccountCode: string;
  creditAccountCode: string;
  notes?: string;
};

export const getLoanAccountingCoaMappingsApi = (organizationId: string) =>
  api.get<LoanAccountingCoaMappingDto[]>(`/api/hr/loans/accounting-coa-mappings`, {
    params: { organizationId },
  });

export const putLoanAccountingCoaMappingsApi = (
  organizationId: string,
  body: LoanAccountingCoaMappingUpsertBody[]
) =>
  api.put<LoanAccountingCoaMappingDto[]>(`/api/hr/loans/accounting-coa-mappings`, body, {
    params: { organizationId },
  });

export interface LoanCategoryDto {
  categoryId: string;
  organizationId: string;
  code: string;
  name: string;
  description?: string;
  categoryType?: string;
  isActive?: boolean;
  sortOrder?: number;
  maxPrincipalAmount?: number;
  maxTenureMonths?: number;
}

export interface LoanRepaymentAnomalyDto {
  type: string;
  payrollRunId?: string;
  loanId?: string;
  employeeId?: string;
  transactionId?: string;
  message?: string;
  detectedAt?: string;
  payslipLoanAmount?: number;
  postedLoanAmount?: number;
  varianceAmount?: number;
}

export default hrService;
