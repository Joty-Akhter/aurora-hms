import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  LoanRepaymentAnomalyDto,
  getPayrollRecoveryAnomalies,
  getPayrollRecoveryCrossCheck,
} from '../../services/hrService';
import './Hr.css';

const LoanPayrollRecoveries: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [since, setSince] = useState('');
  const [rows, setRows] = useState<LoanRepaymentAnomalyDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [runId, setRunId] = useState('');
  const [crossRows, setCrossRows] = useState<LoanRepaymentAnomalyDto[] | null>(null);
  const [crossLoading, setCrossLoading] = useState(false);

  const load = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      const params = since ? { since: `${since}T00:00:00` } : undefined;
      const res = await getPayrollRecoveryAnomalies(currentOrganizationId, params);
      setRows(Array.isArray(res.data) ? res.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Failed to load anomalies');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (currentOrganizationId) load();
  }, [currentOrganizationId]);

  const runCrossCheck = async () => {
    if (!currentOrganizationId || !runId.trim()) {
      setError('Enter a payroll run id (UUID).');
      return;
    }
    try {
      setCrossLoading(true);
      setError(null);
      const res = await getPayrollRecoveryCrossCheck(currentOrganizationId, runId.trim());
      setCrossRows(Array.isArray(res.data) ? res.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Cross-check failed');
      setCrossRows(null);
    } finally {
      setCrossLoading(false);
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading && rows.length === 0) {
    return <div className="loading">Loading payroll recovery anomalies…</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <h1>Loan payroll recoveries</h1>
        <p>RP-05: reversal alerts and payslip vs loan posting cross-check (read-only review; reverse from loan detail).</p>
      </div>
      {error && <div className="error-message">{error}</div>}

      <h2>Reversal events</h2>
      <p className="muted-text" style={{ marginBottom: '0.75rem' }}>
        Lists controlled PAYROLL_REVERSAL rows since the given date (default server window: last 90 days). Use loan detail →
        Repayments to reverse an original PAYROLL posting.
      </p>
      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
        <label>
          Since (optional, date){' '}
          <input type="date" value={since} onChange={(e) => setSince(e.target.value)} />
        </label>
        <button type="button" className="btn-secondary" onClick={() => load()}>
          Refresh
        </button>
      </div>

      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Detected</th>
              <th>Loan</th>
              <th>Employee</th>
              <th>Transaction</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={6}>No reversal anomalies in range.</td>
              </tr>
            ) : (
              rows.map((r, i) => (
                <tr key={`${r.transactionId}-${i}`}>
                  <td>{r.type}</td>
                  <td>{r.detectedAt ?? '—'}</td>
                  <td>
                    {r.loanId ? (
                      <Link to={`/hr/loans/${r.loanId}`}>{r.loanId}</Link>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{r.employeeId ?? '—'}</td>
                  <td>{r.transactionId ?? '—'}</td>
                  <td style={{ maxWidth: 420 }}>{r.message ?? '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <h2>Cross-check by payroll run</h2>
      <p className="muted-text" style={{ marginBottom: '0.75rem' }}>
        Compares payslip loan components to loan-module PAYROLL postings for a run (mismatches and missing postings).
      </p>
      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
        <input
          style={{ minWidth: 280 }}
          placeholder="Payroll run UUID"
          value={runId}
          onChange={(e) => setRunId(e.target.value)}
        />
        <button type="button" className="btn-secondary" disabled={crossLoading} onClick={runCrossCheck}>
          {crossLoading ? 'Running…' : 'Run cross-check'}
        </button>
      </div>

      {crossRows && (
        <div className="table-container" style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Employee</th>
                <th>Loan</th>
                <th>Payslip</th>
                <th>Posted</th>
                <th>Variance</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              {crossRows.length === 0 ? (
                <tr>
                  <td colSpan={7}>No issues for this run.</td>
                </tr>
              ) : (
                crossRows.map((r, i) => (
                  <tr key={`${r.type}-${r.employeeId}-${i}`}>
                    <td>{r.type}</td>
                    <td>{r.employeeId ?? '—'}</td>
                    <td>
                      {r.loanId ? <Link to={`/hr/loans/${r.loanId}`}>{r.loanId}</Link> : '—'}
                    </td>
                    <td>{r.payslipLoanAmount ?? '—'}</td>
                    <td>{r.postedLoanAmount ?? '—'}</td>
                    <td>{r.varianceAmount ?? '—'}</td>
                    <td style={{ maxWidth: 360 }}>{r.message ?? '—'}</td>
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

export default LoanPayrollRecoveries;
