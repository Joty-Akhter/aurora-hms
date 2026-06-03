import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getMyLoansSelf,
  getLoanSelfNotifications,
  markLoanNotificationRead,
} from '../../services/hrService';
import './Hr.css';

/**
 * RE-02 / RE-03: Employee self-service — own loan balances/schedules and in-app loan notifications.
 */
const EmployeeMyLoans: React.FC = () => {
  const { currentOrganizationId, user } = useAuth();
  const navigate = useNavigate();
  const authReady = Boolean(currentOrganizationId && user?.id);
  const [tab, setTab] = useState(0);
  const [loans, setLoans] = useState<any[]>([]);
  const [notifications, setNotifications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (authReady) {
      load();
    } else {
      setLoading(false);
      if (!user?.id && currentOrganizationId) setError('You must be signed in to view loans.');
      else setError(null);
    }
  }, [authReady, currentOrganizationId, user?.id, tab]);

  const load = async () => {
    if (!employeeId || !currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      if (tab === 0) {
        const res = await getMyLoansSelf(currentOrganizationId);
        setLoans(Array.isArray(res.data) ? res.data : []);
      } else {
        const res = await getLoanSelfNotifications(currentOrganizationId);
        setNotifications(Array.isArray(res.data) ? res.data : []);
      }
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load');
      if (tab === 0) setLoans([]);
      else setNotifications([]);
    } finally {
      setLoading(false);
    }
  };

  const markRead = async (eventId: string) => {
    if (!currentOrganizationId) return;
    try {
      await markLoanNotificationRead(currentOrganizationId, eventId);
      setNotifications((prev) => prev.map((n) => (n.eventId === eventId ? { ...n, readAt: new Date().toISOString() } : n)));
    } catch {
      /* ignore */
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <h1>My loans</h1>
        <p>View your active loan balances and installment schedule (RE-02).</p>
      </div>
      {error && <div className="error-message">{error}</div>}

      <div style={{ marginBottom: '1rem', display: 'flex', gap: '0.5rem' }}>
        <button type="button" className={tab === 0 ? 'btn-primary' : 'btn-secondary'} onClick={() => setTab(0)}>
          Loans
        </button>
        <button type="button" className={tab === 1 ? 'btn-primary' : 'btn-secondary'} onClick={() => setTab(1)}>
          Notifications
        </button>
      </div>

      {loading ? (
        <div className="loading">Loading…</div>
      ) : tab === 0 ? (
        <div className="table-container" style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Principal</th>
                <th>Outstanding</th>
                <th>Tenure (mo)</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {loans.length === 0 ? (
                <tr>
                  <td colSpan={5}>No loan accounts found for your profile.</td>
                </tr>
              ) : (
                loans.map((l) => (
                  <tr key={l.loanId}>
                    <td>{l.status}</td>
                    <td>
                      {l.principalAmount} {l.currency}
                    </td>
                    <td>
                      {l.outstandingBalance} {l.currency}
                    </td>
                    <td>{l.tenureMonths}</td>
                    <td>
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => navigate(`/hr/my-loans/${l.loanId}`)}
                      >
                        Schedule
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="table-container" style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>When</th>
                <th>Title</th>
                <th>Detail</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {notifications.length === 0 ? (
                <tr>
                  <td colSpan={4}>No loan notifications.</td>
                </tr>
              ) : (
                notifications.map((n) => (
                  <tr key={n.eventId} style={{ opacity: n.readAt ? 0.65 : 1 }}>
                    <td>{n.createdAt}</td>
                    <td>{n.title}</td>
                    <td style={{ maxWidth: 400 }}>{n.body ?? '—'}</td>
                    <td>
                      {!n.readAt && (
                        <button type="button" className="btn-secondary" onClick={() => markRead(n.eventId)}>
                          Mark read
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default EmployeeMyLoans;
