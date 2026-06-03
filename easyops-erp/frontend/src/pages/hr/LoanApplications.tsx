import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { listLoanApplicationsApi } from '../../services/hrService';
import './Hr.css';

const LoanApplications: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId } = useAuth();
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryTypeFilter, setCategoryTypeFilter] = useState('');

  useEffect(() => {
    if (currentOrganizationId) load();
  }, [currentOrganizationId, statusFilter, categoryTypeFilter]);

  const load = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      const params =
        statusFilter || categoryTypeFilter
          ? {
              ...(statusFilter ? { status: statusFilter } : {}),
              ...(categoryTypeFilter ? { categoryType: categoryTypeFilter } : {}),
            }
          : undefined;
      const res = await listLoanApplicationsApi(currentOrganizationId, params);
      setRows(Array.isArray(res.data) ? res.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Failed to load applications');
    } finally {
      setLoading(false);
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading applications…</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header" style={{ flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1>Loan applications</h1>
          <p>HR → Finance workflow, clarification, and term vs salary-advance filters</p>
        </div>
        <div className="header-actions">
          <button type="button" className="btn-primary" onClick={() => navigate('/hr/loans/applications/new')}>
            New application
          </button>
        </div>
      </div>
      {error && <div className="error-message">{error}</div>}

      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
        <label>
          Status:{' '}
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            <option value="DRAFT">Draft</option>
            <option value="SUBMITTED">Submitted (awaiting HR)</option>
            <option value="PENDING_FINANCE_APPROVAL">Pending finance</option>
            <option value="AWAITING_CLARIFICATION">Awaiting clarification</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </label>
        <label>
          Loan type:{' '}
          <select value={categoryTypeFilter} onChange={(e) => setCategoryTypeFilter(e.target.value)}>
            <option value="">All</option>
            <option value="TERM_LOAN">Term loans</option>
            <option value="SALARY_ADVANCE">Salary advance</option>
          </select>
        </label>
        <button type="button" className="btn-secondary" onClick={() => load()}>
          Refresh
        </button>
      </div>

      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Application</th>
              <th>Employee</th>
              <th>Amount</th>
              <th>Tenure (mo)</th>
              <th>Est. / mo (AL-02)</th>
              <th>Total recovery</th>
              <th>Type</th>
              <th>Status</th>
              <th>Date</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.applicationId}>
                <td>{r.applicationId}</td>
                <td>{r.employeeId}</td>
                <td>{r.requestedAmount}</td>
                <td>{r.requestedTenureMonths}</td>
                <td>{r.recommendedInstallmentAmount ?? '—'}</td>
                <td>{r.totalScheduledRecovery ?? '—'}</td>
                <td>
                  {r.categoryType === 'SALARY_ADVANCE'
                    ? 'Salary advance'
                    : r.categoryType === 'TERM_LOAN'
                      ? 'Term loan'
                      : r.categoryType ?? '—'}
                </td>
                <td>{r.status}</td>
                <td>{r.applicationDate}</td>
                <td>
                  <Link to={`/hr/loans/applications/${r.applicationId}`}>Open</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LoanApplications;
