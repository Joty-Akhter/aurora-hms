import React, { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  approveLeaveRequestApi,
  getEmployees,
  getLeaveRequests,
  getMyEmployeeProfile,
  rejectLeaveRequestApi,
  type Employee,
} from '../../services/hrService';
import './Hr.css';

type PendingLeaveRow = {
  leaveRequestId: string;
  employeeId: string;
  employeeName?: string;
  organizationId?: string;
  leaveTypeId?: string;
  startDate: string;
  endDate: string;
  totalDays?: number | string;
  status?: string;
  pendingStepIndex?: number;
  reason?: string;
};

/**
 * Matrix / manager queue: lists requests where the signed-in user is the next approver (Phase A — NF-03).
 */
const LeaveApprovals: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actorEmployee, setActorEmployee] = useState<Employee | null>(null);
  const [rows, setRows] = useState<PendingLeaveRow[]>([]);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!currentOrganizationId || !user?.id) {
      setActorEmployee(null);
      setRows([]);
      setLoading(false);
      setError(!currentOrganizationId ? 'No organization selected' : null);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      let match: Employee | null = null;
      try {
        const meRes = await getMyEmployeeProfile(currentOrganizationId);
        match = (meRes.data || null) as Employee | null;
      } catch {
        try {
          const employeesRes = await getEmployees(currentOrganizationId, { status: 'ACTIVE' });
          const employees = (employeesRes.data || []) as Employee[];
          match = employees.find((e) => e.userId === user.id) || null;
        } catch {
          match = null;
        }
      }

      if (!match?.employeeId) {
        setActorEmployee(null);
        setRows([]);
        setError('No employee profile is linked to this user. Approvals require a linked HR employee record.');
        return;
      }
      setActorEmployee(match);

      const pendingRes = await getLeaveRequests(currentOrganizationId, {
        pendingForApproverEmployeeId: match.employeeId,
      });
      setRows((pendingRes.data || []) as PendingLeaveRow[]);
    } catch (e: unknown) {
      console.error(e);
      setError('Failed to load pending leave approvals.');
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [currentOrganizationId, user?.id]);

  useEffect(() => {
    void load();
  }, [load]);

  const onApprove = async (row: PendingLeaveRow) => {
    if (!actorEmployee?.employeeId) return;
    setBusyId(row.leaveRequestId);
    try {
      await approveLeaveRequestApi(row.leaveRequestId, actorEmployee.employeeId);
      await load();
    } catch (e: unknown) {
      console.error(e);
      alert('Approve failed. Check that you are the current step approver.');
    } finally {
      setBusyId(null);
    }
  };

  const onReject = async (row: PendingLeaveRow) => {
    if (!actorEmployee?.employeeId) return;
    const rejectionReason = window.prompt('Rejection reason (optional):') ?? '';
    setBusyId(row.leaveRequestId);
    try {
      await rejectLeaveRequestApi(row.leaveRequestId, actorEmployee.employeeId, rejectionReason || undefined);
      await load();
    } catch (e: unknown) {
      console.error(e);
      alert('Reject failed.');
    } finally {
      setBusyId(null);
    }
  };

  if (loading) return <div className="loading">Loading approvals…</div>;

  return (
    <div className="hr-page">
      <div className="page-header">
        <div>
          <h1>Leave approvals</h1>
          <p>Requests waiting for your step (department matrix or manager fallback)</p>
        </div>
        <button type="button" className="btn-outline" onClick={() => void load()} disabled={!!busyId}>
          Refresh
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="hr-section">
        <table className="hr-table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Start</th>
              <th>End</th>
              <th>Days</th>
              <th>Step</th>
              <th>Reason</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center' }}>
                  No pending approvals for your role.
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr key={row.leaveRequestId}>
                  <td>{row.employeeName ?? row.employeeId}</td>
                  <td>{row.startDate ? new Date(row.startDate).toLocaleDateString() : '—'}</td>
                  <td>{row.endDate ? new Date(row.endDate).toLocaleDateString() : '—'}</td>
                  <td>{row.totalDays ?? '—'}</td>
                  <td>{row.pendingStepIndex ?? 1}</td>
                  <td style={{ maxWidth: 220, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {row.reason || '—'}
                  </td>
                  <td className="action-buttons">
                    <button
                      type="button"
                      className="btn-sm btn-primary"
                      disabled={busyId === row.leaveRequestId}
                      onClick={() => void onApprove(row)}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      className="btn-sm btn-delete"
                      disabled={busyId === row.leaveRequestId}
                      onClick={() => void onReject(row)}
                    >
                      Reject
                    </button>
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

export default LeaveApprovals;
