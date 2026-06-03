import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  LoanApplicationCreateBody,
  LoanApplicationDecisionBody,
  LoanApplicationDto,
  LoanApplicationUpdateBody,
  LoanCategoryDto,
  approveLoanApplicationApi,
  cancelLoanApplicationApi,
  createLoanApplicationApi,
  delegateLoanApplicationApi,
  getLoanApplicationApi,
  listLoanApplicationActionsApi,
  listLoanCategoriesApi,
  rejectLoanApplicationApi,
  requestLoanApplicationClarificationApi,
  submitLoanApplicationApi,
  updateLoanApplicationApi,
  LoanApplicationActionDto,
  getEmployees,
} from '../../services/hrService';
import './Hr.css';

function parseAttachments(input: string): string[] | undefined {
  const raw = input
    .split(/[\n,]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  return raw.length ? raw : undefined;
}

function attachmentsToText(refs?: string[]): string {
  return (refs ?? []).join('\n');
}

const LoanApplicationDetail: React.FC = () => {
  const { applicationId } = useParams<{ applicationId: string }>();
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const isNew = applicationId === 'new';

  const [app, setApp] = useState<LoanApplicationDto | null>(null);
  const [categories, setCategories] = useState<LoanCategoryDto[]>([]);
  const [employees, setEmployees] = useState<{ id: string; label: string }[]>([]);
  const [actions, setActions] = useState<LoanApplicationActionDto[]>([]);
  const [loading, setLoading] = useState(!isNew);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formEmployeeId, setFormEmployeeId] = useState('');
  const [formCategoryId, setFormCategoryId] = useState('');
  const [formAmount, setFormAmount] = useState('');
  const [formTenure, setFormTenure] = useState('');
  const [formPurpose, setFormPurpose] = useState('');
  const [formAttachments, setFormAttachments] = useState('');
  const [formLimitOverride, setFormLimitOverride] = useState('');
  const [formFacilityOverride, setFormFacilityOverride] = useState('');

  const [approveComment, setApproveComment] = useState('');
  const [limitExpiry, setLimitExpiry] = useState('');
  const [facilityExpiry, setFacilityExpiry] = useState('');
  const [rejectReason, setRejectReason] = useState('');
  const [clarifyMsg, setClarifyMsg] = useState('');
  const [delegateUserId, setDelegateUserId] = useState('');

  const editable = useMemo(() => {
    if (!app) return false;
    return app.status === 'DRAFT' || app.status === 'AWAITING_CLARIFICATION';
  }, [app]);

  useEffect(() => {
    if (!currentOrganizationId) return;
    listLoanCategoriesApi(currentOrganizationId, false).then((res) => {
      setCategories(Array.isArray(res.data) ? res.data : []);
    });
    getEmployees(currentOrganizationId).then((res) => {
      const list = Array.isArray(res.data) ? res.data : [];
      setEmployees(
        list.map((e: any) => ({
          id: e.employeeId || e.id,
          label: `${e.name ?? ''} (${e.employeeNumber ?? e.employeeId ?? ''})`,
        }))
      );
    });
  }, [currentOrganizationId]);

  useEffect(() => {
    if (!currentOrganizationId || isNew || !applicationId) return;
    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const [appRes, actRes] = await Promise.all([
          getLoanApplicationApi(currentOrganizationId, applicationId),
          listLoanApplicationActionsApi(currentOrganizationId, applicationId),
        ]);
        const a = appRes.data;
        setApp(a);
        setActions(Array.isArray(actRes.data) ? actRes.data : []);
        setFormCategoryId(a.categoryId);
        setFormAmount(String(a.requestedAmount ?? ''));
        setFormTenure(String(a.requestedTenureMonths ?? ''));
        setFormPurpose(a.purposeNotes ?? '');
        setFormAttachments(attachmentsToText(a.attachmentReferences));
        setFormLimitOverride(a.limitOverrideReason ?? '');
        setFormFacilityOverride(a.facilityOverrideReason ?? '');
      } catch (e: any) {
        setError(e.response?.data?.message || e.message || 'Failed to load application');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [currentOrganizationId, applicationId, isNew]);

  const syncFromForm = (): LoanApplicationUpdateBody => ({
    categoryId: formCategoryId,
    requestedAmount: Number(formAmount),
    requestedTenureMonths: Number(formTenure),
    purposeNotes: formPurpose.trim() || undefined,
    attachmentReferences: parseAttachments(formAttachments),
    limitOverrideReason: formLimitOverride.trim() || undefined,
    facilityOverrideReason: formFacilityOverride.trim() || undefined,
  });

  const buildCreateBody = (): LoanApplicationCreateBody => ({
    employeeId: formEmployeeId,
    categoryId: formCategoryId,
    requestedAmount: Number(formAmount),
    requestedTenureMonths: Number(formTenure),
    purposeNotes: formPurpose.trim() || undefined,
    attachmentReferences: parseAttachments(formAttachments),
    limitOverrideReason: formLimitOverride.trim() || undefined,
    facilityOverrideReason: formFacilityOverride.trim() || undefined,
  });

  const handleCreate = async () => {
    if (!currentOrganizationId) return;
    try {
      setSaving(true);
      setError(null);
      const res = await createLoanApplicationApi(currentOrganizationId, buildCreateBody());
      const id = res.data.applicationId;
      navigate(`/hr/loans/applications/${id}`, { replace: true });
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Create failed');
    } finally {
      setSaving(false);
    }
  };

  const handleSave = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    try {
      setSaving(true);
      setError(null);
      const res = await updateLoanApplicationApi(currentOrganizationId, applicationId, syncFromForm());
      setApp(res.data);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  const handleSubmit = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    try {
      setSaving(true);
      setError(null);
      const res = await submitLoanApplicationApi(currentOrganizationId, applicationId);
      setApp(res.data);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Submit failed');
    } finally {
      setSaving(false);
    }
  };

  const handleCancelApp = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    if (!window.confirm('Cancel this application?')) return;
    try {
      setSaving(true);
      setError(null);
      const res = await cancelLoanApplicationApi(currentOrganizationId, applicationId);
      setApp(res.data);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Cancel failed');
    } finally {
      setSaving(false);
    }
  };

  const handleApprove = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    try {
      setSaving(true);
      setError(null);
      const body: LoanApplicationDecisionBody = {
        comment: approveComment.trim() || undefined,
        limitOverrideExpiresAt: limitExpiry || undefined,
        facilityOverrideExpiresAt: facilityExpiry || undefined,
      };
      const res = await approveLoanApplicationApi(currentOrganizationId, applicationId, body);
      setApp(res.data);
      setApproveComment('');
      setLimitExpiry('');
      setFacilityExpiry('');
      const actRes = await listLoanApplicationActionsApi(currentOrganizationId, applicationId);
      setActions(Array.isArray(actRes.data) ? actRes.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Approve failed');
    } finally {
      setSaving(false);
    }
  };

  const handleReject = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    if (!rejectReason.trim()) {
      setError('Rejection reason is required.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      const res = await rejectLoanApplicationApi(currentOrganizationId, applicationId, { reason: rejectReason.trim() });
      setApp(res.data);
      setRejectReason('');
      const actRes = await listLoanApplicationActionsApi(currentOrganizationId, applicationId);
      setActions(Array.isArray(actRes.data) ? actRes.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Reject failed');
    } finally {
      setSaving(false);
    }
  };

  const handleClarify = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    if (!clarifyMsg.trim()) {
      setError('Message is required.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      const res = await requestLoanApplicationClarificationApi(currentOrganizationId, applicationId, {
        message: clarifyMsg.trim(),
      });
      setApp(res.data);
      setClarifyMsg('');
      const actRes = await listLoanApplicationActionsApi(currentOrganizationId, applicationId);
      setActions(Array.isArray(actRes.data) ? actRes.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Request failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelegate = async () => {
    if (!currentOrganizationId || !applicationId || isNew) return;
    if (!delegateUserId.trim()) {
      setError('Delegate user id is required.');
      return;
    }
    try {
      setSaving(true);
      setError(null);
      const res = await delegateLoanApplicationApi(currentOrganizationId, applicationId, {
        delegateToUserId: delegateUserId.trim(),
      });
      setApp(res.data);
      setDelegateUserId('');
      const actRes = await listLoanApplicationActionsApi(currentOrganizationId, applicationId);
      setActions(Array.isArray(actRes.data) ? actRes.data : []);
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Delegate failed');
    } finally {
      setSaving(false);
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading application…</div>;
  }

  if (isNew) {
    return (
      <div className="hr-dashboard">
        <div className="page-header">
          <button type="button" className="btn-secondary" onClick={() => navigate('/hr/loans/applications')} style={{ marginBottom: '0.5rem' }}>
            ← Applications
          </button>
          <h1>New loan application</h1>
          <p>AL-01: employee, category, amount, tenure, notes, attachments; AD-02 / LC-05: override reasons when applicable.</p>
        </div>
        {error && <div className="error-message">{error}</div>}
        <div className="dashboard-cards" style={{ display: 'block', maxWidth: 720 }}>
          <div style={{ background: '#fff', padding: '1.25rem', borderRadius: 12, marginBottom: '1rem' }}>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Employee</label>
              <select value={formEmployeeId} onChange={(e) => setFormEmployeeId(e.target.value)}>
                <option value="">Select…</option>
                {employees.map((e) => (
                  <option key={e.id} value={e.id}>
                    {e.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Loan category</label>
              <select value={formCategoryId} onChange={(e) => setFormCategoryId(e.target.value)}>
                <option value="">Select…</option>
                {categories
                  .filter((c) => c.isActive !== false)
                  .map((c) => (
                    <option key={c.categoryId} value={c.categoryId}>
                      {c.name} ({c.categoryType ?? '—'})
                    </option>
                  ))}
              </select>
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Requested amount</label>
              <input type="number" min={0.01} step="0.01" value={formAmount} onChange={(e) => setFormAmount(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Tenure (months)</label>
              <input type="number" min={1} value={formTenure} onChange={(e) => setFormTenure(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Purpose / notes</label>
              <textarea rows={3} value={formPurpose} onChange={(e) => setFormPurpose(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Attachment references (URLs or document ids, one per line)</label>
              <textarea rows={3} value={formAttachments} onChange={(e) => setFormAttachments(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Limit override reason (AD-02, if above policy cap)</label>
              <textarea rows={2} value={formLimitOverride} onChange={(e) => setFormLimitOverride(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Facility override reason (LC-05, if second facility blocked)</label>
              <textarea rows={2} value={formFacilityOverride} onChange={(e) => setFormFacilityOverride(e.target.value)} />
            </div>
            <button type="button" className="btn-primary" disabled={saving} onClick={handleCreate}>
              {saving ? 'Saving…' : 'Create draft'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (error && !app) {
    return (
      <div>
        <div className="error-message">{error}</div>
        <button type="button" onClick={() => navigate('/hr/loans/applications')}>
          Back to list
        </button>
      </div>
    );
  }

  if (!app) return null;

  const showHrApprovalFields =
    app.status === 'SUBMITTED' && !!(app.limitOverrideReason?.trim() || app.facilityOverrideReason?.trim());

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <button type="button" className="btn-secondary" onClick={() => navigate('/hr/loans/applications')} style={{ marginBottom: '0.5rem' }}>
          ← Applications
        </button>
        <h1>Loan application</h1>
        <p>
          {app.applicationId} · {app.status}
          {app.categoryType === 'SALARY_ADVANCE' ? ' · Salary advance' : app.categoryType === 'TERM_LOAN' ? ' · Term loan' : ''}
        </p>
      </div>
      {error && <div className="error-message">{error}</div>}

      <div className="dashboard-cards" style={{ marginBottom: '1rem' }}>
        <div className="summary-card">
          <div className="card-content">
            <div className="card-value">{app.recommendedInstallmentAmount ?? '—'}</div>
            <div className="card-label">Recommended installment (AL-02)</div>
          </div>
        </div>
        <div className="summary-card">
          <div className="card-content">
            <div className="card-value">{app.totalScheduledRecovery ?? '—'}</div>
            <div className="card-label">Total scheduled recovery</div>
          </div>
        </div>
      </div>
      {app.installmentPreviewNote && <p className="muted-text">{app.installmentPreviewNote}</p>}

      <h2>Details</h2>
      <div style={{ background: '#fff', padding: '1rem', borderRadius: 12, marginBottom: '1rem' }}>
        <table className="data-table">
          <tbody>
            <tr>
              <th>Employee</th>
              <td>{app.employeeId}</td>
            </tr>
            <tr>
              <th>Category</th>
              <td>{app.categoryId}</td>
            </tr>
            <tr>
              <th>Amount / tenure</th>
              <td>
                {app.requestedAmount} / {app.requestedTenureMonths} mo
              </td>
            </tr>
            <tr>
              <th>Application date</th>
              <td>{app.applicationDate ?? '—'}</td>
            </tr>
            <tr>
              <th>Limit override</th>
              <td>
                {app.limitOverrideReason ?? '—'}
                {app.limitOverrideApprovedByUserId && (
                  <span className="muted-text">{` · Approved by ${app.limitOverrideApprovedByUserId}`}</span>
                )}
                {app.limitOverrideExpiresAt && <span>{` · Expires ${app.limitOverrideExpiresAt}`}</span>}
              </td>
            </tr>
            <tr>
              <th>Facility override</th>
              <td>
                {app.facilityOverrideReason ?? '—'}
                {app.facilityOverrideApprovedByUserId && (
                  <span className="muted-text">{` · Approved by ${app.facilityOverrideApprovedByUserId}`}</span>
                )}
                {app.facilityOverrideExpiresAt && <span>{` · Expires ${app.facilityOverrideExpiresAt}`}</span>}
              </td>
            </tr>
            <tr>
              <th>Clarification</th>
              <td>{app.clarificationMessage ?? '—'}</td>
            </tr>
            <tr>
              <th>Delegated to (user)</th>
              <td>{app.delegatedToUserId ?? '—'}</td>
            </tr>
            <tr>
              <th>Attachments</th>
              <td>
                {(app.attachmentReferences ?? []).length ? (
                  <ul style={{ margin: 0 }}>
                    {app.attachmentReferences!.map((r) => (
                      <li key={r}>{r}</li>
                    ))}
                  </ul>
                ) : (
                  '—'
                )}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {editable && (
        <>
          <h2>Edit (draft or clarification)</h2>
          <div style={{ background: '#fff', padding: '1.25rem', borderRadius: 12, marginBottom: '1rem' }}>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Loan category</label>
              <select value={formCategoryId} onChange={(e) => setFormCategoryId(e.target.value)}>
                {categories
                  .filter((c) => c.isActive !== false)
                  .map((c) => (
                    <option key={c.categoryId} value={c.categoryId}>
                      {c.name} ({c.categoryType ?? '—'})
                    </option>
                  ))}
              </select>
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Requested amount</label>
              <input type="number" min={0.01} step="0.01" value={formAmount} onChange={(e) => setFormAmount(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Tenure (months)</label>
              <input type="number" min={1} value={formTenure} onChange={(e) => setFormTenure(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Purpose / notes</label>
              <textarea rows={3} value={formPurpose} onChange={(e) => setFormPurpose(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Attachment references (one per line)</label>
              <textarea rows={3} value={formAttachments} onChange={(e) => setFormAttachments(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Limit override reason (AD-02)</label>
              <textarea rows={2} value={formLimitOverride} onChange={(e) => setFormLimitOverride(e.target.value)} />
            </div>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Facility override reason (LC-05)</label>
              <textarea rows={2} value={formFacilityOverride} onChange={(e) => setFormFacilityOverride(e.target.value)} />
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
              <button type="button" className="btn-secondary" disabled={saving} onClick={handleSave}>
                Save
              </button>
              <button type="button" className="btn-primary" disabled={saving} onClick={handleSubmit}>
                Submit
              </button>
              <button type="button" className="btn-secondary" disabled={saving} onClick={handleCancelApp}>
                Cancel application
              </button>
            </div>
          </div>
        </>
      )}

      {(app.status === 'SUBMITTED' || app.status === 'PENDING_FINANCE_APPROVAL') && (
        <>
          <h2>Approval & workflow</h2>
          <div style={{ background: '#fff', padding: '1.25rem', borderRadius: 12, marginBottom: '1rem' }}>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Comment (optional)</label>
              <textarea rows={2} value={approveComment} onChange={(e) => setApproveComment(e.target.value)} />
            </div>
            {app.status === 'SUBMITTED' && showHrApprovalFields && (
              <>
                <div className="field-group" style={{ marginBottom: '0.75rem' }}>
                  <label title="Optional expiry for limit exception (AD-02), when HR approves">Limit override expiry</label>
                  <input type="date" value={limitExpiry} onChange={(e) => setLimitExpiry(e.target.value)} />
                </div>
                <div className="field-group" style={{ marginBottom: '0.75rem' }}>
                  <label title="Optional expiry for facility exception (LC-05)">Facility override expiry</label>
                  <input type="date" value={facilityExpiry} onChange={(e) => setFacilityExpiry(e.target.value)} />
                </div>
              </>
            )}
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
              <button type="button" className="btn-primary" disabled={saving} onClick={handleApprove}>
                Approve
              </button>
            </div>
            <h3>Reject</h3>
            <div className="field-group" style={{ marginBottom: '0.75rem' }}>
              <label>Reason (required)</label>
              <textarea rows={2} value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
            </div>
            <button type="button" className="btn-secondary" disabled={saving} onClick={handleReject}>
              Reject
            </button>

            {app.status === 'SUBMITTED' && (
              <>
                <h3 style={{ marginTop: '1rem' }}>Request clarification</h3>
                <div className="field-group" style={{ marginBottom: '0.75rem' }}>
                  <label>Message</label>
                  <textarea rows={2} value={clarifyMsg} onChange={(e) => setClarifyMsg(e.target.value)} />
                </div>
                <button type="button" className="btn-secondary" disabled={saving} onClick={handleClarify}>
                  Send clarification request
                </button>

                <h3 style={{ marginTop: '1rem' }}>Delegate HR approval</h3>
                <div className="field-group" style={{ marginBottom: '0.75rem' }}>
                  <label>Delegate to user id (UUID)</label>
                  <input value={delegateUserId} onChange={(e) => setDelegateUserId(e.target.value)} placeholder="User UUID" />
                </div>
                <button type="button" className="btn-secondary" disabled={saving} onClick={handleDelegate}>
                  Delegate
                </button>
              </>
            )}
          </div>
        </>
      )}

      <h2>Workflow actions</h2>
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
            {actions.length === 0 ? (
              <tr>
                <td colSpan={4}>No actions yet.</td>
              </tr>
            ) : (
              actions.map((a) => (
                <tr key={a.actionId}>
                  <td>{a.createdAt}</td>
                  <td>{a.actionType}</td>
                  <td>{a.actorUserId ?? '—'}</td>
                  <td style={{ maxWidth: 360 }}>{a.commentText ?? '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LoanApplicationDetail;
