import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  getPayrollRuns,
  createPayrollRun,
  processPayrollRun,
  approvePayrollRun,
  populatePayrollFromSalary,
  getPayrollDetails,
  markPayrollDetailAsPaid,
  postPayrollToAccounting,
  processEpfFromPayroll,
  postEpfToAccounting,
  getPayrollTimeAttendancePolicy,
  putPayrollTimeAttendancePolicy,
} from '../../services/hrService';
import { portalLayoutOverlay, LAYOUT_OVERLAY_DETECT_CLASS } from '@/utils/layoutOverlayPortal';
import './Hr.css';

interface PayrollDetailItem {
  payrollDetailId: string;
  payrollRunId: string;
  employeeId: string;
  employeeName?: string;
  employeeNumber?: string;
  basicSalary?: number;
  grossSalary?: number;
  totalDeductions?: number;
  totalReimbursements?: number;
  netSalary?: number;
  workingDays?: number | null;
  presentDays?: number | null;
  leaveDays?: number | string | null;
  overtimeHours?: number | string | null;
  overtimeAmount?: number | string | null;
  lopDays?: number | string | null;
  lopAmount?: number | string | null;
  bonusAmount?: number | string | null;
  status?: string;
  paymentReference?: string;
  paidAt?: string;
}

const PayrollRunManager: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [payrollRuns, setPayrollRuns] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    runName: '',
    payPeriodStart: '',
    payPeriodEnd: '',
    paymentDate: '',
    notes: '',
  });
  const [formError, setFormError] = useState<string | null>(null);
  const [detailsRunId, setDetailsRunId] = useState<string | null>(null);
  const [details, setDetails] = useState<PayrollDetailItem[]>([]);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [markingPaidId, setMarkingPaidId] = useState<string | null>(null);
  const [paymentRef, setPaymentRef] = useState<Record<string, string>>({});
  const [taPolicyLoading, setTaPolicyLoading] = useState(false);
  const [taPolicySaving, setTaPolicySaving] = useState(false);
  const [taPolicyError, setTaPolicyError] = useState<string | null>(null);
  const [taPolicyForm, setTaPolicyForm] = useState({
    overtimeRateMultiplier: '1.5',
    standardHoursPerDay: '8',
    inferMissingWeekdayLop: false,
    leavePayrollBridgeEnabled: false,
    unpaidApprovedLeaveCountsAsLop: true,
    excludeActiveHolidaysFromWorkingDays: false,
    excludeActiveHolidaysFromLopInference: false,
  });

  useEffect(() => {
    if (currentOrganizationId) {
      loadPayrollRuns();
    } else {
      setLoading(false);
      setError('No organization selected');
    }
  }, [currentOrganizationId]);

  useEffect(() => {
    if (!currentOrganizationId) return;
    let cancelled = false;
    (async () => {
      setTaPolicyLoading(true);
      setTaPolicyError(null);
      try {
        const res = await getPayrollTimeAttendancePolicy(currentOrganizationId);
        const d = res.data;
        if (cancelled || !d) return;
        const m = d.overtimeRateMultiplier;
        const h = d.standardHoursPerDay;
        setTaPolicyForm({
          overtimeRateMultiplier:
            m !== undefined && m !== null && String(m) !== '' ? String(m) : '1.5',
          standardHoursPerDay:
            h !== undefined && h !== null && String(h) !== '' ? String(h) : '8',
          inferMissingWeekdayLop: Boolean(d.inferMissingWeekdayLop),
          leavePayrollBridgeEnabled: Boolean(d.leavePayrollBridgeEnabled),
          unpaidApprovedLeaveCountsAsLop: d.unpaidApprovedLeaveCountsAsLop !== false,
          excludeActiveHolidaysFromWorkingDays: Boolean(d.excludeActiveHolidaysFromWorkingDays),
          excludeActiveHolidaysFromLopInference: Boolean(d.excludeActiveHolidaysFromLopInference),
        });
      } catch (e) {
        console.error('Failed to load time & attendance policy', e);
        if (!cancelled) setTaPolicyError('Could not load time & attendance policy.');
      } finally {
        if (!cancelled) setTaPolicyLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId]);

  const handleSaveTaPolicy = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentOrganizationId) return;
    const mult = parseFloat(taPolicyForm.overtimeRateMultiplier);
    const hrs = parseFloat(taPolicyForm.standardHoursPerDay);
    if (Number.isNaN(mult) || mult <= 0) {
      setTaPolicyError('Overtime rate multiplier must be a positive number.');
      return;
    }
    if (Number.isNaN(hrs) || hrs <= 0) {
      setTaPolicyError('Standard hours per day must be a positive number.');
      return;
    }
    setTaPolicySaving(true);
    setTaPolicyError(null);
    try {
      await putPayrollTimeAttendancePolicy(currentOrganizationId, {
        overtimeRateMultiplier: mult,
        standardHoursPerDay: hrs,
        inferMissingWeekdayLop: taPolicyForm.inferMissingWeekdayLop,
        leavePayrollBridgeEnabled: taPolicyForm.leavePayrollBridgeEnabled,
        unpaidApprovedLeaveCountsAsLop: taPolicyForm.unpaidApprovedLeaveCountsAsLop,
        excludeActiveHolidaysFromWorkingDays: taPolicyForm.excludeActiveHolidaysFromWorkingDays,
        excludeActiveHolidaysFromLopInference: taPolicyForm.excludeActiveHolidaysFromLopInference,
      });
      alert('Time & attendance policy saved.');
    } catch (err: any) {
      console.error('Failed to save time & attendance policy', err);
      setTaPolicyError(
        err?.response?.data?.message || err?.response?.data?.error || 'Failed to save policy.'
      );
    } finally {
      setTaPolicySaving(false);
    }
  };

  const loadPayrollRuns = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      const response = await getPayrollRuns(currentOrganizationId);
      setPayrollRuns(response.data as any[]);
    } catch (err) {
      console.error('Failed to load payroll runs:', err);
      setError('Failed to load payroll runs');
    } finally {
      setLoading(false);
    }
  };

  const loadDetails = async (runId: string) => {
    setDetailsRunId(runId);
    setDetailsLoading(true);
    try {
      const res = await getPayrollDetails(runId);
      const rows = res.data;
      setDetails(Array.isArray(rows) ? (rows as PayrollDetailItem[]) : []);
    } catch (err) {
      console.error('Failed to load payroll details:', err);
      setDetails([]);
    } finally {
      setDetailsLoading(false);
    }
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentOrganizationId) {
      setFormError('No organization selected.');
      return;
    }

    if (!formData.payPeriodStart || !formData.payPeriodEnd || !formData.paymentDate) {
      setFormError('Please provide pay period start, end, and payment date.');
      return;
    }

    const start = new Date(formData.payPeriodStart);
    const end = new Date(formData.payPeriodEnd);
    if (end < start) {
      setFormError('Pay period end cannot be earlier than the start date.');
      return;
    }

    const runName =
      formData.runName.trim() ||
      `Payroll ${formData.payPeriodStart} - ${formData.payPeriodEnd}`;

    const payload = {
      organizationId: currentOrganizationId,
      runName,
      payPeriodStart: formData.payPeriodStart,
      payPeriodEnd: formData.payPeriodEnd,
      paymentDate: formData.paymentDate,
      notes: formData.notes.trim() || undefined,
    };

    try {
      setFormError(null);
      await createPayrollRun(payload as any);
      setShowForm(false);
      setFormData({
        runName: '',
        payPeriodStart: '',
        payPeriodEnd: '',
        paymentDate: '',
        notes: '',
      });
      loadPayrollRuns();
      alert('Payroll run created successfully!');
    } catch (err) {
      console.error('Failed to create payroll run:', err);
      setFormError('Failed to create payroll run. Please verify the details and try again.');
    }
  };

  const handlePopulate = async (runId: string) => {
    if (!confirm('Populate this payroll run from current salary assignments and components? This will overwrite any existing details for this run.')) {
      return;
    }
    try {
      const res = await populatePayrollFromSalary(runId);
      const data = res?.data;
      await loadPayrollRuns();
      const populated = data?.employeesPopulated ?? 0;
      const withoutAssignment = data?.employeesWithoutAssignment?.length ?? 0;
      const missingBasic = data?.employeesMissingBasic?.length ?? 0;
      const warnList: string[] = Array.isArray(data?.warnings) ? data.warnings : [];
      let msg = '';
      if (populated > 0) {
        msg = `${populated} employee(s) populated successfully.`;
        if (withoutAssignment > 0) msg += ` ${withoutAssignment} skipped (no salary assignment).`;
        if (missingBasic > 0) msg += ` ${missingBasic} added without Basic component.`;
      } else {
        msg = 'No employees were added. ';
        if (withoutAssignment > 0) {
          msg += `${withoutAssignment} employee(s) were skipped because they have no salary assignment (structure + grade). Go to Salary Structure Manager → Employee Salaries tab → assign salary structure and components first. `;
        }
        msg += 'Also ensure employees have employment status "ACTIVE" and hire date on or before the period end.';
      }
      if (warnList.length > 0) {
        msg += '\n\nNotices:\n' + warnList.map((w) => '• ' + w).join('\n');
      }
      alert(msg);
    } catch (err) {
      console.error('Failed to populate payroll run from salary:', err);
      alert('Failed to populate payroll run from salary. Please check assignments and try again.');
    }
  };

  const handleProcess = async (runId: string) => {
    if (!confirm('Are you sure you want to process this payroll run?')) return;

    try {
      await processPayrollRun(runId, user?.id);
      loadPayrollRuns();
      alert('Payroll run processed successfully!');
    } catch (err) {
      console.error('Failed to process payroll run:', err);
      alert('Failed to process payroll run');
    }
  };

  const handleApprove = async (runId: string) => {
    if (!confirm('Are you sure you want to approve this payroll run?')) return;

    try {
      await approvePayrollRun(runId, user?.id);
      loadPayrollRuns();
      if (detailsRunId === runId) loadDetails(runId);
      alert('Payroll run approved successfully!');
    } catch (err) {
      console.error('Failed to approve payroll run:', err);
      alert('Failed to approve payroll run');
    }
  };

  const handlePostToAccounting = async (runId: string) => {
    if (!confirm('Post this payroll run to accounting? This will create a journal entry (Salary expense, deductions, bank).')) return;

    try {
      await postPayrollToAccounting(runId);
      alert('Payroll posted to accounting successfully!');
    } catch (err: any) {
      console.error('Failed to post payroll to accounting:', err);
      alert(err?.response?.data?.message || err?.message || 'Failed to post payroll to accounting. Ensure Chart of Accounts has 6110, 2020, 1030.');
    }
  };

  const handleProcessEpf = async (runId: string) => {
    if (!confirm('Create EPF contributions from this payroll run? This extracts PF_EMPLOYEE and PF_EMPLOYER amounts from payroll and creates/updates EPF contribution records.')) return;

    try {
      const res = await processEpfFromPayroll(runId);
      const data = res?.data as { contributionsCreated?: number; contributionsUpdated?: number; employeesSkipped?: number; errors?: string[] };
      const created = data?.contributionsCreated ?? 0;
      const updated = data?.contributionsUpdated ?? 0;
      const skipped = data?.employeesSkipped ?? 0;
      const errs = data?.errors as string[] | undefined;
      let msg = `EPF processed: ${created} created, ${updated} updated.`;
      if (skipped > 0) msg += ` ${skipped} skipped (no EPF account).`;
      if (errs?.length) msg += ` Errors: ${errs.slice(0, 3).join('; ')}`;
      alert(msg);
    } catch (err: any) {
      console.error('Failed to process EPF from payroll:', err);
      alert(err?.response?.data?.message || err?.message || 'Failed to process EPF from payroll.');
    }
  };

  const handlePostEpfToAccounting = async (runId: string) => {
    if (!confirm('Post EPF contributions to accounting? Run Process EPF first if not done. Requires EPF_PAYABLE and CASH in Chart of Accounts.')) return;

    try {
      await postEpfToAccounting(runId);
      alert('EPF posted to accounting successfully!');
    } catch (err: any) {
      console.error('Failed to post EPF to accounting:', err);
      alert(err?.response?.data?.message || err?.message || 'Failed to post EPF to accounting. Run Process EPF first and ensure CoA has EPF_PAYABLE, CASH.');
    }
  };

  const handleMarkAsPaid = async (detailId: string) => {
    const ref = paymentRef[detailId]?.trim();
    if (!ref) {
      alert('Please enter a payment reference (e.g. bank ref, cheque number).');
      return;
    }
    setMarkingPaidId(detailId);
    try {
      await markPayrollDetailAsPaid(detailId, ref);
      if (detailsRunId) loadDetails(detailsRunId);
      setPaymentRef((prev) => ({ ...prev, [detailId]: '' }));
    } catch (err) {
      console.error('Failed to mark as paid:', err);
      alert('Failed to mark as paid');
    } finally {
      setMarkingPaidId(null);
    }
  };

  const formatDate = (value?: string) => {
    if (!value) return '-';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : date.toLocaleDateString();
  };

  const formatCurrency = (value?: number | string) => {
    const amount = Number(value ?? 0);
    if (Number.isNaN(amount)) return '$0';
    return amount.toLocaleString(undefined, { style: 'currency', currency: 'USD' });
  };

  const formatOptionalNumber = (value?: number | string | null) => {
    if (value === undefined || value === null || value === '') return '—';
    const n = typeof value === 'string' ? Number(value) : value;
    if (Number.isNaN(n)) return '—';
    return String(n);
  };

  if (loading) return <div className="loading">Loading payroll runs...</div>;
  if (error) return <div className="error-message">{error}</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <h1>Payroll Runs</h1>
        <p>Create and manage payroll processing runs</p>
        <button onClick={() => setShowForm(true)} className="btn-primary">
          + New Payroll Run
        </button>
      </div>

      <div className="hr-section" style={{ marginBottom: '1.5rem', padding: '1rem', border: '1px solid #ddd', borderRadius: 8 }}>
        <h2 style={{ marginTop: 0, fontSize: '1.1rem' }}>Time & attendance (payroll)</h2>
        <p style={{ color: '#666', marginTop: 0, fontSize: '0.9rem' }}>
          Overtime pay uses Basic ÷ (working days × standard hours per day) × overtime hours × multiplier below.
          LOP from attendance can include explicit statuses only, or also weekdays with no attendance row when enabled.
          Phase B: optional leave↔payroll bridge ties approved paid leave to inferred LOP; optional holiday exclusions use
          holidays from <strong>HR → Holidays</strong>. OT_PAY and LOP_DED components are created when amounts apply.
        </p>
        {taPolicyLoading ? (
          <p style={{ color: '#666' }}>Loading policy…</p>
        ) : (
          <form onSubmit={handleSaveTaPolicy} style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'flex-end' }}>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.875rem' }}>OT rate multiplier</label>
              <input
                type="number"
                step="0.01"
                min={0.01}
                value={taPolicyForm.overtimeRateMultiplier}
                onChange={(e) => setTaPolicyForm({ ...taPolicyForm, overtimeRateMultiplier: e.target.value })}
                style={{ width: 100, padding: '6px 8px' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.875rem' }}>Standard hours / day</label>
              <input
                type="number"
                step="0.25"
                min={0.25}
                value={taPolicyForm.standardHoursPerDay}
                onChange={(e) => setTaPolicyForm({ ...taPolicyForm, standardHoursPerDay: e.target.value })}
                style={{ width: 100, padding: '6px 8px' }}
              />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', userSelect: 'none' }}>
              <input
                type="checkbox"
                checked={taPolicyForm.inferMissingWeekdayLop}
                onChange={(e) =>
                  setTaPolicyForm({ ...taPolicyForm, inferMissingWeekdayLop: e.target.checked })
                }
              />
              Count missing weekdays (no row) as LOP
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', userSelect: 'none' }}>
              <input
                type="checkbox"
                checked={taPolicyForm.leavePayrollBridgeEnabled}
                onChange={(e) =>
                  setTaPolicyForm({ ...taPolicyForm, leavePayrollBridgeEnabled: e.target.checked })
                }
              />
              Leave ↔ payroll bridge (paid approved leave suppresses inferred LOP)
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', userSelect: 'none' }}>
              <input
                type="checkbox"
                checked={taPolicyForm.unpaidApprovedLeaveCountsAsLop}
                onChange={(e) =>
                  setTaPolicyForm({ ...taPolicyForm, unpaidApprovedLeaveCountsAsLop: e.target.checked })
                }
              />
              Unpaid approved leave adds inferred LOP (when missing-row inference is on)
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', userSelect: 'none' }}>
              <input
                type="checkbox"
                checked={taPolicyForm.excludeActiveHolidaysFromWorkingDays}
                onChange={(e) =>
                  setTaPolicyForm({ ...taPolicyForm, excludeActiveHolidaysFromWorkingDays: e.target.checked })
                }
              />
              Exclude holidays from working-day denominator
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', userSelect: 'none' }}>
              <input
                type="checkbox"
                checked={taPolicyForm.excludeActiveHolidaysFromLopInference}
                onChange={(e) =>
                  setTaPolicyForm({ ...taPolicyForm, excludeActiveHolidaysFromLopInference: e.target.checked })
                }
              />
              Do not infer LOP on holiday weekdays
            </label>
            <button type="submit" className="btn-primary" disabled={taPolicySaving}>
              {taPolicySaving ? 'Saving…' : 'Save policy'}
            </button>
          </form>
        )}
        {taPolicyError && (
          <div className="error-message" style={{ marginTop: '0.75rem' }}>
            {taPolicyError}
          </div>
        )}
      </div>

      {showForm && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={() => setShowForm(false)}>
          <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Create Payroll Run</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <label>Run Name *</label>
                <input
                  type="text"
                  value={formData.runName}
                  onChange={(e) => setFormData({ ...formData, runName: e.target.value })}
                  placeholder="e.g. November 2025 Payroll"
                />
              </div>

              <div className="form-row">
                <label>Period Start *</label>
                <input
                  type="date"
                  value={formData.payPeriodStart}
                  onChange={(e) => setFormData({ ...formData, payPeriodStart: e.target.value })}
                  required
                />
              </div>

              <div className="form-row">
                <label>Period End *</label>
                <input
                  type="date"
                  value={formData.payPeriodEnd}
                  onChange={(e) => setFormData({ ...formData, payPeriodEnd: e.target.value })}
                  required
                />
              </div>

              <div className="form-row">
                <label>Payment Date *</label>
                <input
                  type="date"
                  value={formData.paymentDate}
                  onChange={(e) => setFormData({ ...formData, paymentDate: e.target.value })}
                  required
                />
              </div>

              <div className="form-row">
                <label>Notes</label>
                <textarea
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  rows={3}
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
                <button type="submit" className="btn-primary">
                  Create Run
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {detailsRunId && portalLayoutOverlay(
        <div className={`hr-modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} onClick={() => setDetailsRunId(null)}>
          <div className="hr-modal" style={{ maxWidth: '1200px' }} onClick={(e) => e.stopPropagation()}>
            <h2>Payroll Details</h2>
            {detailsLoading ? (
              <div className="loading">Loading details...</div>
            ) : (
              <div className="hr-section">
                <table className="hr-table">
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th title="Working days in period (weekdays)">WD</th>
                      <th title="Present days (attendance)">Pres</th>
                      <th title="Paid leave days">Leave</th>
                      <th title="Loss of pay days">LOP d</th>
                      <th title="LOP amount">LOP $</th>
                      <th title="Overtime hours">OT h</th>
                      <th title="Overtime pay">OT $</th>
                      <th title="Bonus amount">Bonus $</th>
                      <th>Gross</th>
                      <th>Deductions</th>
                      <th>Net</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {details.length === 0 ? (
                      <tr>
                        <td colSpan={13} style={{ textAlign: 'center' }}>
                          No payroll details found.
                        </td>
                      </tr>
                    ) : (
                      details.map((d) => (
                        <tr key={d.payrollDetailId}>
                          <td>
                            {d.employeeName || d.employeeNumber || d.employeeId?.substring(0, 8) || '-'}
                          </td>
                          <td>{formatOptionalNumber(d.workingDays)}</td>
                          <td>{formatOptionalNumber(d.presentDays)}</td>
                          <td>{formatOptionalNumber(d.leaveDays)}</td>
                          <td>{formatOptionalNumber(d.lopDays)}</td>
                          <td>{d.lopAmount != null && d.lopAmount !== '' ? formatCurrency(d.lopAmount) : '—'}</td>
                          <td>{formatOptionalNumber(d.overtimeHours)}</td>
                          <td>{d.overtimeAmount != null && d.overtimeAmount !== '' ? formatCurrency(d.overtimeAmount) : '—'}</td>
                          <td>{d.bonusAmount != null && d.bonusAmount !== '' ? formatCurrency(d.bonusAmount) : '—'}</td>
                          <td>{formatCurrency(d.grossSalary)}</td>
                          <td>{formatCurrency(d.totalDeductions)}</td>
                          <td>{formatCurrency(d.netSalary)}</td>
                          <td>
                            <span className={`status-badge status-${(d.status || 'pending').toLowerCase()}`}>
                              {d.status || 'pending'}
                            </span>
                            {d.paidAt && (
                              <small style={{ display: 'block', marginTop: 4 }}>
                                Paid {formatDate(d.paidAt)}
                                {d.paymentReference && ` · ${d.paymentReference}`}
                              </small>
                            )}
                          </td>
                          <td>
                            {(d.status || '').toLowerCase() === 'pending' ? (
                              <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                                <input
                                  type="text"
                                  placeholder="Payment ref"
                                  value={paymentRef[d.payrollDetailId] || ''}
                                  onChange={(e) =>
                                    setPaymentRef((prev) => ({ ...prev, [d.payrollDetailId]: e.target.value }))
                                  }
                                  style={{ width: 120, padding: '4px 8px' }}
                                />
                                <button
                                  className="btn-sm btn-primary"
                                  onClick={() => handleMarkAsPaid(d.payrollDetailId)}
                                  disabled={markingPaidId === d.payrollDetailId}
                                >
                                  {markingPaidId === d.payrollDetailId ? '...' : 'Mark Paid'}
                                </button>
                              </div>
                            ) : (
                              '-'
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
            <div className="form-actions" style={{ marginTop: 16 }}>
              <button type="button" className="btn-secondary" onClick={() => setDetailsRunId(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="hr-section">
        <table className="hr-table">
          <thead>
            <tr>
              <th>Run ID</th>
              <th>Period</th>
              <th>Payment Date</th>
              <th>Employees</th>
              <th>Gross Amount</th>
              <th>Net Amount</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {payrollRuns.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center' }}>
                  No payroll runs found. Create your first payroll run to get started.
                </td>
              </tr>
            ) : (
              payrollRuns.map((run) => (
                <tr key={run.payrollRunId}>
                  <td title={run.runName || undefined}>
                    {run.runName || `#${run.payrollRunId?.substring(0, 8)}`}
                  </td>
                  <td>
                    {formatDate(run.payPeriodStart || run.periodStart)} - {formatDate(run.payPeriodEnd || run.periodEnd)}
                  </td>
                  <td>{formatDate(run.paymentDate)}</td>
                  <td>{run.employeeCount || 0}</td>
                  <td>{formatCurrency(run.totalGrossPay ?? run.totalGross)}</td>
                  <td>{formatCurrency(run.totalNetPay ?? run.totalNet)}</td>
                  <td>
                    <span className={`status-badge status-${run.status?.toLowerCase()}`}>
                      {run.status}
                    </span>
                  </td>
                  <td>
                    {run.status === 'DRAFT' && (
                      <>
                        <button
                          onClick={() => loadDetails(run.payrollRunId)}
                          className="btn-sm btn-secondary"
                          style={{ marginRight: '0.5rem' }}
                        >
                          View Details
                        </button>
                        <button
                          onClick={() => handlePopulate(run.payrollRunId)}
                          className="btn-sm btn-secondary"
                          style={{ marginRight: '0.5rem' }}
                        >
                          Populate from Salary
                        </button>
                        <button onClick={() => handleProcess(run.payrollRunId)} className="btn-sm btn-primary">
                          Process
                        </button>
                      </>
                    )}
                    {(run.status === 'PROCESSED' || run.status === 'APPROVED') && (
                      <>
                        <button
                          onClick={() => loadDetails(run.payrollRunId)}
                          className="btn-sm btn-secondary"
                          style={{ marginRight: '0.5rem' }}
                        >
                          View Details
                        </button>
                        {run.status === 'PROCESSED' && (
                          <button
                            onClick={() => handleApprove(run.payrollRunId)}
                            className="btn-sm btn-secondary"
                            style={{ marginRight: '0.5rem' }}
                          >
                            Approve
                          </button>
                        )}
                        <button
                          onClick={() => handleProcessEpf(run.payrollRunId)}
                          className="btn-sm btn-secondary"
                          style={{ marginRight: '0.5rem' }}
                          title="Create EPF contributions from PF components in payroll"
                        >
                          Process EPF
                        </button>
                        <button
                          onClick={() => handlePostEpfToAccounting(run.payrollRunId)}
                          className="btn-sm btn-secondary"
                          style={{ marginRight: '0.5rem' }}
                          title="Post EPF contributions to accounting"
                        >
                          Post EPF
                        </button>
                        <button
                          onClick={() => handlePostToAccounting(run.payrollRunId)}
                          className="btn-sm btn-primary"
                        >
                          Post to Accounting
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PayrollRunManager;
