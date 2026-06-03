import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getMyLoanSelf } from '../../services/hrService';
import './Hr.css';

/** RE-02: single-loan schedule view for the logged-in employee. */
const EmployeeMyLoanDetail: React.FC = () => {
  const { loanId } = useParams<{ loanId: string }>();
  const { currentOrganizationId, user } = useAuth();
  const navigate = useNavigate();
  const authReady = Boolean(currentOrganizationId && loanId && user?.id);
  const [loan, setLoan] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (currentOrganizationId && loanId && employeeId) load();
  }, [currentOrganizationId, loanId, employeeId]);

  const load = async () => {
    if (!currentOrganizationId || !loanId) return;
    try {
      setLoading(true);
      setError(null);
      const res = await getMyLoanSelf(currentOrganizationId, loanId);
      setLoan(res.data);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load loan');
      setLoan(null);
    } finally {
      setLoading(false);
    }
  };

  if (!user?.id) {
    return <div className="error-message">You must be signed in.</div>;
  }

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading…</div>;
  }

  if (error || !loan) {
    return (
      <div className="hr-dashboard">
        <div className="error-message">{error || 'Not found'}</div>
        <button type="button" className="btn-secondary" onClick={() => navigate('/hr/my-loans')}>
          Back
        </button>
      </div>
    );
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <button type="button" className="btn-secondary" onClick={() => navigate('/hr/my-loans')} style={{ marginBottom: 8 }}>
          ← My loans
        </button>
        <h1>Loan schedule</h1>
        <p>
          Outstanding: {loan.outstandingBalance} {loan.currency} · Status: {loan.status}
        </p>
      </div>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Due</th>
              <th>Scheduled</th>
              <th>Paid</th>
              <th>Remaining</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {(loan.installments || []).map((i: any) => (
              <tr key={i.installmentId}>
                <td>{i.sequenceNumber}</td>
                <td>{i.dueDate}</td>
                <td>{i.scheduledAmount}</td>
                <td>{i.paidAmount}</td>
                <td>{i.remainingAmount}</td>
                <td>{i.status}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default EmployeeMyLoanDetail;
