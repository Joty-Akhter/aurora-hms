import React, { useState, useEffect, FormEvent } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getLeaveRequests,
  createLeaveRequest,
  getLeaveTypes,
  getEmployees,
  getLeaveBalances,
  getMyEmployeeProfile,
  type Employee,
} from '../../services/hrService';
import { portalLayoutOverlay, LAYOUT_OVERLAY_DETECT_CLASS } from '@/utils/layoutOverlayPortal';
import './Hr.css';

type LeaveTypeApi = {
  leaveTypeId: string;
  typeName?: string;
  name?: string;
  description?: string;
  maxDaysPerYear?: number;
};

type LeaveBalanceApi = {
  leaveTypeId: string;
  
  allocatedDays?: number | string;
  carriedForwardDays?: number | string;
  usedDays?: number | string;
};

type LeaveTypeOption = {
  leaveTypeId: string;
  typeName: string;
  description?: string;
  availableDays?: number;
  maxDaysPerYear?: number;
};

type LeaveRequestFormData = {
  leaveTypeId: string;
  startDate: string;
  endDate: string;
  reason: string;
  isHalfDay: boolean;
};

const LeaveRequestForm: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [requests, setRequests] = useState<any[]>([]);
  const [baseLeaveTypes, setBaseLeaveTypes] = useState<LeaveTypeApi[]>([]);
  const [leaveTypes, setLeaveTypes] = useState<LeaveTypeOption[]>([]);
  const [currentEmployee, setCurrentEmployee] = useState<Employee | null>(null);
  const [employeeLoadError, setEmployeeLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState<LeaveRequestFormData>({
    leaveTypeId: '',
    startDate: '',
    endDate: '',
    reason: '',
    isHalfDay: false,
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!currentOrganizationId || !user?.id) {
      setCurrentEmployee(null);
      setRequests([]);
      setLeaveTypes([]);
      setLoading(false);
      if (!currentOrganizationId) {
        setError('No organization selected');
      }
      return;
    }

    const initialize = async () => {
      setLoading(true);
      setError(null);

      try {
        const typesRes = await getLeaveTypes(currentOrganizationId);
        const rawTypes: LeaveTypeApi[] = (typesRes.data || []).map((type: any) => ({
          ...type,
          typeName: type.typeName || type.name || 'Leave Type',
        }));
        setBaseLeaveTypes(rawTypes);

        let employeeMatch: Employee | null = null;
        try {
          const meRes = await getMyEmployeeProfile(currentOrganizationId);
          employeeMatch = (meRes.data || null) as Employee | null;
        } catch {
          try {
            const employeesRes = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
            const employees = (employeesRes.data || []) as Employee[];
            employeeMatch = employees.find((emp) => emp.userId === user.id) || null;
          } catch {
            employeeMatch = null;
          }
        }

        if (!employeeMatch?.employeeId) {
          setCurrentEmployee(null);
          setEmployeeLoadError(
            'No employee profile is linked to this user. Please contact your administrator.'
          );
          setRequests([]);
          setLeaveTypes(rawTypes);
          return;
        }

        setCurrentEmployee(employeeMatch);
        setEmployeeLoadError(null);

        await refreshLeaveData(currentOrganizationId, employeeMatch.employeeId, rawTypes);
      } catch (err) {
        console.error('Failed to load leave data:', err);
        setError('Failed to load leave data');
      } finally {
        setLoading(false);
      }
    };

    void initialize();
  }, [currentOrganizationId, user?.id]);

  const numberFrom = (value: number | string | undefined | null) => {
    if (value === null || value === undefined) return 0;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? 0 : parsed;
  };

  const enrichLeaveTypes = (types: LeaveTypeApi[], balances: LeaveBalanceApi[]): LeaveTypeOption[] => {
    const balanceMap = new Map<string, LeaveBalanceApi>(
      balances.map((balance) => [balance.leaveTypeId, balance])
    );

    return types.map((type) => {
      const balance = balanceMap.get(type.leaveTypeId);
      const allocated = numberFrom(balance?.allocatedDays);
      const carried = numberFrom(balance?.carriedForwardDays);
      const used = numberFrom(balance?.usedDays);
      const total = allocated + carried;
      const remaining = total ? Math.max(total - used, 0) : undefined;

      return {
        leaveTypeId: type.leaveTypeId,
        typeName: type.typeName || 'Leave Type',
        description: type.description,
        availableDays: remaining ?? numberFrom(type.maxDaysPerYear),
        maxDaysPerYear: numberFrom(type.maxDaysPerYear),
      };
    });
  };

  const refreshLeaveData = async (
    organizationId: string,
    employeeId: string,
    typesSnapshot: LeaveTypeApi[]
  ) => {
    try {
      const [requestsRes, balancesRes] = await Promise.all([
        getLeaveRequests(organizationId, { employeeId }),
        getLeaveBalances(organizationId, employeeId),
      ]);

      setRequests(requestsRes.data || []);
      const balances: LeaveBalanceApi[] = balancesRes.data || [];
      const enrichedTypes = enrichLeaveTypes(typesSnapshot, balances);
      setLeaveTypes(enrichedTypes);
    } catch (err) {
      console.error('Failed to refresh leave data:', err);
      setError('Failed to refresh leave data');
      setRequests([]);
      setLeaveTypes(typesSnapshot.map((type) => ({
        leaveTypeId: type.leaveTypeId,
        typeName: type.typeName || 'Leave Type',
        description: type.description,
        availableDays: type.maxDaysPerYear,
        maxDaysPerYear: type.maxDaysPerYear,
      })));
    }
  };

  const calculateTotalDays = (start: string, end: string, isHalfDay: boolean) => {
    if (!start || !end) return 0;
    const startDate = new Date(start);
    const endDate = new Date(end);
    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) return 0;

    if (endDate < startDate) return -1;

    const millisecondsPerDay = 1000 * 60 * 60 * 24;
    const diffDays = Math.floor((endDate.getTime() - startDate.getTime()) / millisecondsPerDay) + 1;
    if (isHalfDay) {
      return diffDays - 0.5;
    }
    return diffDays;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentOrganizationId || !currentEmployee?.employeeId) {
      setFormError('Employee context is missing. Please contact your administrator.');
      return;
    }

    const totalDays = calculateTotalDays(
      formData.startDate,
      formData.endDate,
      formData.isHalfDay
    );

    if (totalDays === -1) {
      setFormError('End date cannot be before start date.');
      return;
    }

    if (totalDays <= 0) {
      setFormError('Please choose a valid date range.');
      return;
    }

    const selectedType = leaveTypes.find(
      (type: LeaveTypeOption) => type.leaveTypeId === formData.leaveTypeId
    );
    if (selectedType?.availableDays !== undefined && totalDays > selectedType.availableDays) {
      setFormError(
        `Requested ${totalDays} day(s) exceeds the available balance of ${selectedType.availableDays} day(s).`
      );
      return;
    }

    setIsSubmitting(true);
    setFormError(null);

    try {
      await createLeaveRequest({
        leaveTypeId: formData.leaveTypeId,
        startDate: formData.startDate,
        endDate: formData.endDate,
        reason: formData.reason,
        isHalfDay: formData.isHalfDay,
        totalDays,
        employeeId: currentEmployee.employeeId,
        organizationId: currentOrganizationId,
      } as any);

      alert('Leave request submitted successfully!');
      setShowForm(false);
      setFormData({ leaveTypeId: '', startDate: '', endDate: '', reason: '', isHalfDay: false });
      if (currentEmployee) {
        await refreshLeaveData(currentOrganizationId, currentEmployee.employeeId, baseLeaveTypes);
      }
    } catch (err) {
      console.error('Failed to submit leave request:', err);
      setFormError('Failed to submit leave request. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) return <div className="loading">Loading leave requests...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Leave Requests</h1>
        <p>Submit and track your leave requests</p>
        <button
          onClick={() => {
            setFormError(null);
            setShowForm(true);
          }}
          className="btn-primary"
          disabled={!currentEmployee?.employeeId}
          title={!currentEmployee?.employeeId ? 'No employee profile linked to your account.' : undefined}
        >
          + Request Leave
        </button>
      </div>

      {employeeLoadError && (
        <div className="alert alert-warning" role="alert">
          {employeeLoadError}
        </div>
      )}

      {showForm && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={() => setShowForm(false)}>
          <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
            <h2>New Leave Request</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <label>Leave Type *</label>
                <select
                  value={formData.leaveTypeId}
                  onChange={(e) => setFormData({ ...formData, leaveTypeId: e.target.value })}
                  required
                >
                  <option value="">Select leave type</option>
                  {leaveTypes.map((type: LeaveTypeOption) => (
                    <option key={type.leaveTypeId} value={type.leaveTypeId}>
                      {type.typeName}{' '}
                      {type.availableDays !== undefined
                        ? `(${type.availableDays} days available)`
                        : type.maxDaysPerYear
                        ? `(${type.maxDaysPerYear} days per year)`
                        : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <label>Start Date *</label>
                <input
                  type="date"
                  value={formData.startDate}
                  onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                    setFormData({ ...formData, startDate: event.target.value })
                  }
                  required
                />
              </div>

              <div className="form-row">
                <label>End Date *</label>
                <input
                  type="date"
                  value={formData.endDate}
                  onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                    setFormData({ ...formData, endDate: event.target.value })
                  }
                  required
                />
              </div>

              <div className="form-row">
                <label>
                  <input
                    type="checkbox"
                    checked={formData.isHalfDay}
                    onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                      setFormData({ ...formData, isHalfDay: event.target.checked })
                    }
                  />
                  Half Day
                </label>
              </div>

              <div className="form-row">
                <label>Reason *</label>
                <textarea
                  value={formData.reason}
                  onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) =>
                    setFormData({ ...formData, reason: event.target.value })
                  }
                  rows={4}
                  required
                />
              </div>

              {formError && (
                <div className="error-message" style={{ marginBottom: '1rem' }}>
                  {formError}
                </div>
              )}

              <div className="form-actions">
                <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={isSubmitting}>
                  {isSubmitting ? 'Submitting...' : 'Submit Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="hr-section">
        <h2>My Leave Requests</h2>
        <table className="hr-table">
          <thead>
            <tr>
              <th>Leave Type</th>
              <th>Start Date</th>
              <th>End Date</th>
              <th>Days</th>
              <th>Status</th>
              <th>Submitted</th>
            </tr>
          </thead>
          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center' }}>
                  No leave requests found
                </td>
              </tr>
            ) : (
              requests.map((req: any) => (
                <tr key={req.leaveRequestId}>
                  <td>{req.leaveTypeName || 'N/A'}</td>
                  <td>{new Date(req.startDate).toLocaleDateString()}</td>
                  <td>{new Date(req.endDate).toLocaleDateString()}</td>
                  <td>{req.totalDays || 0}</td>
                  <td>
                    <span className={`status-badge status-${req.status?.toLowerCase()?.replace(/\s+/g, '-')}`}>
                      {req.status === 'pending' &&
                      req.pendingStepIndex != null &&
                      Number(req.pendingStepIndex) > 1
                        ? `pending (awaiting approver step ${req.pendingStepIndex})`
                        : req.status}
                    </span>
                  </td>
                  <td>{req.requestedAt ? new Date(req.requestedAt).toLocaleDateString() : '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LeaveRequestForm;

