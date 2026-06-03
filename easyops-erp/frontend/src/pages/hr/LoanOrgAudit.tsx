import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { getLoanOrgAuditLogApi, LoanAuditLogRowDto } from '../../services/hrService';
import './Hr.css';

function entityLabel(entityType: string): string {
  switch (entityType) {
    case 'LOAN_ACCOUNTING_COA':
      return 'COA mapping (PI-05)';
    case 'LOAN_ORG':
      return 'AD-03 bulk';
    default:
      return entityType;
  }
}

function actionLabel(action: string): string {
  switch (action) {
    case 'COA_MAPPINGS_REPLACED':
      return 'COA mappings replaced';
    case 'BULK_HOLIDAY_RECALC_COMPLETED':
      return 'Bulk holiday recalc completed';
    default:
      return action;
  }
}

const LoanOrgAudit: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [rows, setRows] = useState<LoanAuditLogRowDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) return;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await getLoanOrgAuditLogApi(currentOrganizationId);
        setRows(Array.isArray(res.data) ? res.data : []);
      } catch (e: any) {
        setError(e.response?.data?.message || e.message || 'Failed to load org audit');
      } finally {
        setLoading(false);
      }
    })();
  }, [currentOrganizationId]);

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading organization loan audit…</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <Link to="/hr/loans" className="btn-secondary" style={{ display: 'inline-block', marginBottom: '0.5rem' }}>
          ← Loan register
        </Link>
        <h1>Organization loan audit</h1>
        <p>
          RE-04: PI-05 chart-of-account mapping changes and AD-03 bulk holiday recalculation summaries. Per-loan events stay
          on each loan&apos;s detail page.
        </p>
      </div>
      {error && <div className="error-message">{error}</div>}

      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>When</th>
              <th>Area</th>
              <th>Action</th>
              <th>Performed by</th>
              <th>Previous</th>
              <th>New / summary</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ color: '#666' }}>
                  No org-level loan audit entries yet (COA saves and bulk AD-03 recalculations appear here).
                </td>
              </tr>
            ) : (
              rows.map((r) => (
                <tr key={r.auditId}>
                  <td>{r.performedAt ? String(r.performedAt).replace('T', ' ').slice(0, 19) : '—'}</td>
                  <td>{entityLabel(r.entityType)}</td>
                  <td>{actionLabel(r.action)}</td>
                  <td>{r.performedBy ?? '—'}</td>
                  <td style={{ maxWidth: 280, wordBreak: 'break-word' }}>{r.oldValues ?? '—'}</td>
                  <td style={{ maxWidth: 360, wordBreak: 'break-word' }}>{r.newValues ?? '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LoanOrgAudit;
