import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getEmployeeLoanApi,
  getLoanCombinedAudit,
  listLoanRepaymentsApi,
  LoanRepaymentTransactionDto,
  reversePayrollLoanRepayment,
  patchLoanLegalWorkflow,
  recalculateLoanInstallmentHolidayDatesApi,
} from '../../services/hrService';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Hr.css';

function sourceLabel(source: string): string {
  switch (source) {
    case 'MANUAL':
      return 'Manual payment';
    case 'PAYROLL':
      return 'Payroll deduction';
    case 'PAYROLL_REVERSAL':
      return 'Payroll reversal (credit)';
    case 'PF_SETTLEMENT':
      return 'PF settlement';
    case 'FINAL_SALARY':
      return 'Final salary';
    case 'OTHER_DUES':
      return 'Other dues';
    default:
      return source;
  }
}

function sourceTooltip(r: LoanRepaymentTransactionDto): string {
  const parts: string[] = [sourceLabel(r.source)];
  if (r.source === 'PAYROLL_REVERSAL') {
    parts.push('Negative amount restores principal; this row offsets an earlier payroll deduction.');
    if (r.reversesTransactionId) {
      parts.push(`Reverses transaction ${r.reversesTransactionId}.`);
    }
  }
  if (r.source === 'PAYROLL' && r.payrollRunId) {
    parts.push(`Payroll run ${r.payrollRunId}.`);
  }
  if (r.notes) {
    parts.push(r.notes);
  }
  return parts.join(' ');
}

const LoanDetail: React.FC = () => {
  const { loanId } = useParams<{ loanId: string }>();
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [loan, setLoan] = useState<any>(null);
  const [combinedAudit, setCombinedAudit] = useState<any>(null);
  const [repayments, setRepayments] = useState<LoanRepaymentTransactionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [reverseTx, setReverseTx] = useState<LoanRepaymentTransactionDto | null>(null);
  const [reverseReason, setReverseReason] = useState('');
  const [reverseBusy, setReverseBusy] = useState(false);
  const [legalWorkflowInput, setLegalWorkflowInput] = useState('');
  const [legalSaving, setLegalSaving] = useState(false);
  const [holidayRecalcBusy, setHolidayRecalcBusy] = useState(false);

  useEffect(() => {
    if (currentOrganizationId && loanId) load();
  }, [currentOrganizationId, loanId]);

  const load = async () => {
    if (!currentOrganizationId || !loanId) return;
    try {
      setLoading(true);
      setError(null);
      const [loanRes, auditRes, repRes] = await Promise.all([
        getEmployeeLoanApi(currentOrganizationId, loanId),
        getLoanCombinedAudit(currentOrganizationId, loanId),
        listLoanRepaymentsApi(currentOrganizationId, loanId),
      ]);
      setLoan(loanRes.data);
      setLegalWorkflowInput(loanRes.data?.legalWorkflowStatus ?? '');
      setCombinedAudit(auditRes.data ?? null);
      setRepayments(Array.isArray(repRes.data) ? repRes.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Failed to load loan');
    } finally {
      setLoading(false);
    }
  };

  const submitReverse = async () => {
    if (!currentOrganizationId || !loanId || !reverseTx || !reverseReason.trim()) return;
    try {
      setReverseBusy(true);
      setError(null);
      await reversePayrollLoanRepayment(currentOrganizationId, loanId, reverseTx.transactionId, {
        reason: reverseReason.trim(),
      });
      setReverseTx(null);
      setReverseReason('');
      await load();
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Reversal failed');
    } finally {
      setReverseBusy(false);
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading loan…</div>;
  }

  if (error || !loan) {
    return (
      <div>
        <div className="error-message">{error || 'Loan not found'}</div>
        <button type="button" onClick={() => navigate('/hr/loans')}>
          Back to register
        </button>
      </div>
    );
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <button type="button" className="btn-secondary" onClick={() => navigate('/hr/loans')} style={{ marginBottom: '0.5rem' }}>
          ← Loan register
        </button>
        <h1>Loan detail</h1>
        <p>
          {loan.employeeId} · {loan.status}
        </p>
      </div>

      {error && <div className="error-message">{error}</div>}

      <div className="dashboard-cards">
        <div className="summary-card">
          <div className="card-content">
            <div className="card-value">{loan.principalAmount}</div>
            <div className="card-label">Principal ({loan.currency})</div>
          </div>
        </div>
        <div className="summary-card">
          <div className="card-content">
            <div className="card-value">{loan.outstandingBalance}</div>
            <div className="card-label">Outstanding</div>
          </div>
        </div>
      </div>

      <div
        style={{
          marginBottom: '1.5rem',
          padding: '1rem',
          background: '#fafafa',
          border: '1px solid #e0e0e0',
          borderRadius: 8,
        }}
      >
        <h3 style={{ marginTop: 0 }}>Settlement &amp; PF (ST-02 / ST-03)</h3>
        <p style={{ color: '#444', marginBottom: '0.75rem' }}>
          Exit recovery uses the organization&apos;s allocation priority (PF settlement, final salary, other dues). PF
          amounts may be entered manually or via integration; treat this screen as the loan ledger of record.
        </p>
        <h3>Legal / write-off (ST-04)</h3>
        <p style={{ margin: '0.25rem 0' }}>
          <strong>Write-off path:</strong> {loan.settlementWriteOffPath ?? '—'}
        </p>
        <p style={{ margin: '0.25rem 0' }}>
          <strong>Legal case ref:</strong> {loan.legalCaseReference ?? '—'}
        </p>
        <p style={{ margin: '0.25rem 0' }}>
          <strong>Legal workflow status:</strong> {loan.legalWorkflowStatus ?? '—'}
          {loan.legalWorkflowUpdatedAt && (
            <span style={{ color: '#666' }}> (updated {loan.legalWorkflowUpdatedAt})</span>
          )}
        </p>
        <p style={{ fontSize: '0.9rem', color: '#555' }}>
          Full legal task routing is not automated — use labels for reporting and audit. HR/Finance with manage rights
          can update the workflow label.
        </p>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', alignItems: 'center', marginTop: '0.5rem' }}>
          <input
            type="text"
            value={legalWorkflowInput}
            onChange={(e) => setLegalWorkflowInput(e.target.value)}
            placeholder="e.g. PENDING_LEGAL, PENDING_BOARD"
            style={{ minWidth: 260 }}
          />
          <button
            type="button"
            className="btn-secondary"
            disabled={legalSaving || !legalWorkflowInput.trim()}
            onClick={async () => {
              if (!currentOrganizationId || !loanId || !legalWorkflowInput.trim()) return;
              setLegalSaving(true);
              try {
                const res = await patchLoanLegalWorkflow(currentOrganizationId, loanId, {
                  legalWorkflowStatus: legalWorkflowInput.trim(),
                });
                setLoan(res.data);
                setLegalWorkflowInput(res.data?.legalWorkflowStatus ?? '');
              } catch (e: any) {
                setError(e?.response?.data?.message || e?.message || 'Update failed');
              } finally {
                setLegalSaving(false);
              }
            }}
          >
            {legalSaving ? 'Saving…' : 'Save workflow label'}
          </button>
        </div>
      </div>

      <h2>Installments</h2>
      <p style={{ color: '#555', marginBottom: '0.75rem' }}>
        AD-03: when org settings shift due dates off weekends and the HR holiday calendar, use recalculate after
        calendar or policy changes. Fully paid installments (paid ≥ scheduled) are left unchanged; installments with a
        partial payment still have their due date recalculated; RP-01 skipped installments are not moved.
      </p>
      <div style={{ marginBottom: '0.75rem' }}>
        <button
          type="button"
          className="btn-secondary"
          disabled={holidayRecalcBusy}
          onClick={async () => {
            if (!currentOrganizationId || !loanId) return;
            setHolidayRecalcBusy(true);
            setError(null);
            try {
              const res = await recalculateLoanInstallmentHolidayDatesApi(currentOrganizationId, loanId);
              setLoan(res.data);
            } catch (e: any) {
              setError(e?.response?.data?.message || e?.message || 'Recalculate failed');
            } finally {
              setHolidayRecalcBusy(false);
            }
          }}
        >
          {holidayRecalcBusy ? 'Recalculating…' : 'Recalculate due dates (weekends / holidays)'}
        </button>
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
              <th>Skip reason</th>
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
                <td style={{ maxWidth: 280 }}>{i.skipReason ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2>Repayments</h2>
      <p className="muted-text" style={{ marginBottom: '0.75rem' }}>
        PAYROLL_REVERSAL rows show negative amounts and link to the original PAYROLL deduction they offset (RP-05).
      </p>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Amount</th>
              <th>Source</th>
              <th>Related</th>
              <th>Payroll run</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {repayments.map((r) => (
              <tr key={r.transactionId}>
                <td>{r.paymentDate}</td>
                <td
                  title={sourceTooltip(r)}
                  style={{
                    color: r.amount < 0 ? '#b45309' : undefined,
                    fontWeight: r.source === 'PAYROLL_REVERSAL' ? 600 : undefined,
                  }}
                >
                  {r.amount}
                  {r.source === 'PAYROLL_REVERSAL' && (
                    <span className="muted-text" style={{ marginLeft: 6 }}>
                      (reversal)
                    </span>
                  )}
                </td>
                <td title={sourceTooltip(r)}>
                  <span>{sourceLabel(r.source)}</span>
                  <span className="muted-text" style={{ marginLeft: 6 }}>
                    ({r.source})
                  </span>
                </td>
                <td>
                  {r.source === 'PAYROLL_REVERSAL' && r.reversesTransactionId ? (
                    <span title="Original payroll deduction transaction">
                      Reverses{' '}
                      <code style={{ fontSize: 12 }}>{r.reversesTransactionId}</code>
                    </span>
                  ) : r.source === 'PAYROLL' ? (
                    <span className="muted-text">—</span>
                  ) : (
                    <span className="muted-text">—</span>
                  )}
                </td>
                <td>{r.payrollRunId ?? '—'}</td>
                <td>
                  {r.source === 'PAYROLL' && r.amount > 0 && (
                    <button type="button" className="btn-secondary" onClick={() => setReverseTx(r)}>
                      Reverse…
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {reverseTx && portalLayoutOverlay(
        <div
          role="dialog"
          aria-modal
          className={LAYOUT_OVERLAY_DETECT_CLASS}
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.35)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: LAYOUT_OVERLAY_ROOT_Z,
          }}
        >
          <div style={{ background: '#fff', padding: '1.25rem', borderRadius: 12, maxWidth: 420, width: '100%' }}>
            <h3 style={{ marginTop: 0 }}>Reverse payroll posting</h3>
            <p className="muted-text">
              Transaction {reverseTx.transactionId} ({reverseTx.amount}). This creates a PAYROLL_REVERSAL row and restores
              installment allocation.
            </p>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Reason (required)</label>
              <textarea rows={3} value={reverseReason} onChange={(e) => setReverseReason(e.target.value)} />
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
              <button type="button" className="btn-secondary" onClick={() => setReverseTx(null)}>
                Cancel
              </button>
              <button type="button" className="btn-primary" disabled={reverseBusy || !reverseReason.trim()} onClick={submitReverse}>
                {reverseBusy ? 'Working…' : 'Confirm reversal'}
              </button>
            </div>
          </div>
        </div>
      )}

      <h2>Application workflow (RE-04)</h2>
      <p style={{ color: '#555', marginBottom: '0.75rem' }}>
        Originating application actions when this loan was created from an application.
      </p>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>When</th>
              <th>Action</th>
              <th>Actor</th>
              <th>Comment</th>
            </tr>
          </thead>
          <tbody>
            {(combinedAudit?.applicationWorkflowActions || []).length === 0 ? (
              <tr>
                <td colSpan={4}>No application workflow rows (loan may predate action log or have no application link).</td>
              </tr>
            ) : (
              (combinedAudit?.applicationWorkflowActions || []).map((a: any) => (
                <tr key={a.actionId}>
                  <td>{a.createdAt}</td>
                  <td>{a.actionType}</td>
                  <td>{a.actorUserId}</td>
                  <td style={{ maxWidth: 360 }}>{a.commentText ?? '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <h2>Loan account audit (RE-04)</h2>
      <p style={{ color: '#555', marginBottom: '0.75rem' }}>
        Principal, schedule, repayments, settlement, installment skip, payroll reversal, etc.
      </p>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>When</th>
              <th>Action</th>
              <th>Detail</th>
            </tr>
          </thead>
          <tbody>
            {(combinedAudit?.loanAuditLogs || []).length === 0 ? (
              <tr>
                <td colSpan={3}>No loan audit entries.</td>
              </tr>
            ) : (
              (combinedAudit?.loanAuditLogs || []).map((a: any) => (
                <tr key={a.auditId}>
                  <td>{a.performedAt}</td>
                  <td>{a.action}</td>
                  <td style={{ maxWidth: 480 }}>{a.newValues || a.oldValues || '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LoanDetail;
