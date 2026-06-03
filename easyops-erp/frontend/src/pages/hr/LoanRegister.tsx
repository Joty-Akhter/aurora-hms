import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import {
  getLoanReportSummary,
  getLoanRegister,
  getLoanArrearsReport,
  getLoanSettlementExitReport,
  getLoanAccountingExport,
  exportLoanAccountingDisbursementsCsv,
  exportLoanAccountingRepaymentsCsv,
  getLoanOrganizationSettings,
  patchLoanOrganizationSettingsApi,
  getLoanAccountingCoaMappingsApi,
  putLoanAccountingCoaMappingsApi,
  LoanHolidayShiftMode,
  LoanAccountingCoaMappingDto,
  LoanAccountingCoaMappingUpsertBody,
  listLoanCategoriesApi,
  getPayrollRecoveryCrossCheck,
  recalculateLoanHolidayInstallmentDatesAllApi,
  LoanBulkHolidayRecalcResultDto,
} from '../../services/hrService';
import './Hr.css';

function calendarMonthBoundsIso(): { from: string; to: string } {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth(), 1);
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  return { from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) };
}

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

const LoanRegister: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<any>(null);
  const [rows, setRows] = useState<any[]>([]);
  const [arrears, setArrears] = useState<any[]>([]);
  const [settlements, setSettlements] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryTypeFilter, setCategoryTypeFilter] = useState('');
  const bounds = calendarMonthBoundsIso();
  const [acctFrom, setAcctFrom] = useState(bounds.from);
  const [acctTo, setAcctTo] = useState(bounds.to);
  const [acctPreview, setAcctPreview] = useState<{
    totalDisbursements: number;
    totalRepayments: number;
  } | null>(null);
  const [acctLoading, setAcctLoading] = useState(false);
  const [loanSettings, setLoanSettings] = useState<any>(null);
  const [loanCategories, setLoanCategories] = useState<any[]>([]);
  const [payrollRunIdCheck, setPayrollRunIdCheck] = useState('');
  const [crossCheckRows, setCrossCheckRows] = useState<any[]>([]);
  const [crossLoading, setCrossLoading] = useState(false);
  const [shiftHoliday, setShiftHoliday] = useState(false);
  const [shiftMode, setShiftMode] = useState<LoanHolidayShiftMode>('NEXT_BUSINESS_DAY');
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [coaByKey, setCoaByKey] = useState<{
    LOAN_DISBURSEMENT: { debit: string; credit: string; notes: string };
    LOAN_REPAYMENT: { debit: string; credit: string; notes: string };
  }>({
    LOAN_DISBURSEMENT: { debit: '', credit: '', notes: '' },
    LOAN_REPAYMENT: { debit: '', credit: '', notes: '' },
  });
  const [coaSaving, setCoaSaving] = useState(false);
  const [coaLoadError, setCoaLoadError] = useState<string | null>(null);
  const [bulkRecalcBusy, setBulkRecalcBusy] = useState(false);
  const [bulkRecalcResult, setBulkRecalcResult] = useState<LoanBulkHolidayRecalcResultDto | null>(null);

  useEffect(() => {
    if (!loanSettings) return;
    setShiftHoliday(!!loanSettings.shiftInstallmentDueDatesForHolidays);
    setShiftMode(loanSettings.loanHolidayShiftMode ?? 'NEXT_BUSINESS_DAY');
  }, [loanSettings]);

  useEffect(() => {
    if (currentOrganizationId) loadAll();
  }, [currentOrganizationId, statusFilter, categoryTypeFilter]);

  const loadAll = async () => {
    if (!currentOrganizationId) return;
    try {
      setLoading(true);
      setError(null);
      const registerParams =
        statusFilter || categoryTypeFilter
          ? {
              ...(statusFilter ? { status: statusFilter } : {}),
              ...(categoryTypeFilter ? { categoryType: categoryTypeFilter } : {}),
            }
          : undefined;
      const [sumRes, regRes, arrRes, setRes, settingsRes, catRes] = await Promise.all([
        getLoanReportSummary(currentOrganizationId),
        getLoanRegister(currentOrganizationId, registerParams),
        getLoanArrearsReport(currentOrganizationId),
        getLoanSettlementExitReport(currentOrganizationId),
        getLoanOrganizationSettings(currentOrganizationId),
        listLoanCategoriesApi(currentOrganizationId),
      ]);
      setSummary(sumRes.data);
      setRows(Array.isArray(regRes.data) ? regRes.data : []);
      setArrears(Array.isArray(arrRes.data) ? arrRes.data : []);
      setSettlements(Array.isArray(setRes.data) ? setRes.data : []);
      setLoanSettings(settingsRes.data ?? null);
      setLoanCategories(Array.isArray(catRes.data) ? catRes.data : []);
      setCoaLoadError(null);
      try {
        const coaRes = await getLoanAccountingCoaMappingsApi(currentOrganizationId);
        const coaList: LoanAccountingCoaMappingDto[] = Array.isArray(coaRes.data) ? coaRes.data : [];
        const byKey = Object.fromEntries(coaList.map((m) => [m.mappingKey, m])) as Record<
          string,
          LoanAccountingCoaMappingDto
        >;
        setCoaByKey({
          LOAN_DISBURSEMENT: {
            debit: byKey['LOAN_DISBURSEMENT']?.debitAccountCode ?? '',
            credit: byKey['LOAN_DISBURSEMENT']?.creditAccountCode ?? '',
            notes: byKey['LOAN_DISBURSEMENT']?.notes ?? '',
          },
          LOAN_REPAYMENT: {
            debit: byKey['LOAN_REPAYMENT']?.debitAccountCode ?? '',
            credit: byKey['LOAN_REPAYMENT']?.creditAccountCode ?? '',
            notes: byKey['LOAN_REPAYMENT']?.notes ?? '',
          },
        });
      } catch (coaErr: any) {
        setCoaLoadError(coaErr.response?.data?.message || coaErr.message || 'COA mappings could not be loaded');
      }
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Failed to load loan reports');
    } finally {
      setLoading(false);
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization to view loans.</div>;
  }

  if (loading && !summary) {
    return <div className="loading">Loading loan register…</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <h1>Employee loans</h1>
        <p>Active register, arrears, and settlement-at-exit (RE-01)</p>
      </div>
      {error && <div className="error-message">{error}</div>}

      {loanSettings && (
        <div
          style={{
            marginBottom: '1.5rem',
            padding: '1rem',
            background: '#f5f7fb',
            borderRadius: 8,
            border: '1px solid #dfe3e8',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Policy — salary advance vs term loan (EL-06)</h3>
          <p style={{ margin: '0.35rem 0' }}>
            <strong>Enforce single active loan (BR-02):</strong>{' '}
            {loanSettings.enforceSingleActiveLoan ? 'Yes' : 'No'}
          </p>
          <p style={{ margin: '0.35rem 0' }}>
            <strong>Allow salary advance while a term loan is active:</strong>{' '}
            {loanSettings.allowSalaryAdvanceWithActiveTermLoan ? 'Yes' : 'No'}
          </p>
          <p style={{ color: '#444', fontSize: '0.95rem', marginBottom: 0 }}>
            When enabled, employees may still open a <em>salary advance</em> (SALARY_ADVANCE category) while a term loan is
            active; otherwise that combination is blocked. Term-loan-on-term-loan remains governed by BR-02 and overrides
            (LC-05). These toggles are API-driven; assign <code>hr_loans</code> roles for approvers.
          </p>
        </div>
      )}

      {loanSettings && (
        <div
          style={{
            marginBottom: '1.5rem',
            padding: '1rem',
            background: '#f5f7fb',
            borderRadius: 8,
            border: '1px solid #dfe3e8',
          }}
        >
          <h3 style={{ marginTop: 0 }}>Holiday calendar — installment due dates (AD-03, optional)</h3>
          <p style={{ color: '#444', fontSize: '0.95rem', marginTop: 0 }}>
            When enabled, new schedules and recalculations move due dates off weekends and dates in the HR holiday
            calendar. Existing loans can be updated from loan detail (recalculate unpaid installments).
          </p>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <input
              type="checkbox"
              checked={shiftHoliday}
              onChange={(e) => setShiftHoliday(e.target.checked)}
            />
            Shift installment due dates for weekends and holidays
          </label>
          <div style={{ marginBottom: '0.75rem' }}>
            <label>
              When a due date falls on a non-business day:{' '}
              <select value={shiftMode} onChange={(e) => setShiftMode(e.target.value as LoanHolidayShiftMode)}>
                <option value="NEXT_BUSINESS_DAY">Next business day</option>
                <option value="PREVIOUS_BUSINESS_DAY">Previous business day</option>
              </select>
            </label>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
            <button
              type="button"
              className="btn-secondary"
              disabled={settingsSaving}
              onClick={async () => {
                if (!currentOrganizationId) return;
                setSettingsSaving(true);
                setError(null);
                try {
                  const res = await patchLoanOrganizationSettingsApi(currentOrganizationId, {
                    shiftInstallmentDueDatesForHolidays: shiftHoliday,
                    loanHolidayShiftMode: shiftMode,
                  });
                  setLoanSettings(res.data);
                } catch (e: any) {
                  setError(e.response?.data?.message || e.message || 'Failed to save loan settings');
                } finally {
                  setSettingsSaving(false);
                }
              }}
            >
              {settingsSaving ? 'Saving…' : 'Save holiday settings'}
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={bulkRecalcBusy}
              title="Re-run AD-03 rules for every active and settlement-pending loan that has been disbursed"
              onClick={async () => {
                if (!currentOrganizationId) return;
                if (
                  !window.confirm(
                    'Recalculate installment due dates for all active and settlement-pending disbursed loans? This may take a moment.'
                  )
                ) {
                  return;
                }
                setBulkRecalcBusy(true);
                setError(null);
                setBulkRecalcResult(null);
                try {
                  const res = await recalculateLoanHolidayInstallmentDatesAllApi(currentOrganizationId);
                  setBulkRecalcResult(res.data);
                } catch (e: any) {
                  setError(e.response?.data?.message || e.message || 'Bulk recalculation failed');
                } finally {
                  setBulkRecalcBusy(false);
                }
              }}
            >
              {bulkRecalcBusy ? 'Recalculating all…' : 'Recalculate all loans (AD-03)'}
            </button>
          </div>
          {bulkRecalcResult && (
            <div
              role="status"
              style={{
                marginTop: '1rem',
                padding: '0.75rem 1rem',
                borderRadius: 6,
                border: '1px solid var(--border-color, #ccc)',
                borderLeftWidth: 4,
                borderLeftColor:
                  (bulkRecalcResult.failures?.length ?? 0) > 0 ? 'var(--warning-color, #b45309)' : 'var(--success-color, #15803d)',
                background: 'var(--panel-bg, #fafafa)',
              }}
            >
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <p style={{ margin: 0 }}>
                  <strong>Bulk recalculation finished.</strong> Recalculated: {bulkRecalcResult.loansRecalculated}, skipped
                  (no disbursement): {bulkRecalcResult.loansSkipped}, failed: {bulkRecalcResult.failures?.length ?? 0}.
                </p>
                <button type="button" className="btn-secondary" onClick={() => setBulkRecalcResult(null)}>
                  Dismiss
                </button>
              </div>
              {(bulkRecalcResult.failures?.length ?? 0) > 0 && (
                <div className="table-container" style={{ marginTop: '0.75rem', overflowX: 'auto' }}>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Loan</th>
                        <th>Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bulkRecalcResult.failures!.map((f) => (
                        <tr key={f.loanId}>
                          <td>
                            <Link to={`/hr/loans/${f.loanId}`}>{f.loanId}</Link>
                          </td>
                          <td style={{ wordBreak: 'break-word' }}>{f.message?.trim() || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {summary && (
        <div className="dashboard-cards" style={{ marginBottom: '1.5rem' }}>
          <div className="summary-card">
            <div className="card-content">
              <div className="card-value">{summary.activeLoanCount ?? 0}</div>
              <div className="card-label">Active loans</div>
            </div>
          </div>
          <div className="summary-card">
            <div className="card-content">
              <div className="card-value">{summary.pendingDisbursementCount ?? 0}</div>
              <div className="card-label">Pending disbursement</div>
            </div>
          </div>
          <div className="summary-card">
            <div className="card-content">
              <div className="card-value">{summary.settlementPendingCount ?? 0}</div>
              <div className="card-label">Settlement pending</div>
            </div>
          </div>
          <div className="summary-card">
            <div className="card-content">
              <div className="card-value">{Number(summary.totalOutstanding ?? 0).toFixed(2)}</div>
              <div className="card-label">Total outstanding</div>
            </div>
          </div>
          <div className="summary-card">
            <div className="card-content">
              <div className="card-value">{Number(summary.totalArrearsRemaining ?? 0).toFixed(2)}</div>
              <div className="card-label">Arrears (remaining)</div>
            </div>
          </div>
        </div>
      )}

      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', alignItems: 'center' }}>
        <label>
          Status filter:{' '}
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            <option value="PENDING_DISBURSEMENT">Pending disbursement</option>
            <option value="ACTIVE">Active</option>
            <option value="SETTLEMENT_PENDING">Settlement pending</option>
            <option value="CLOSED">Closed</option>
          </select>
        </label>
        <label>
          Loan type:{' '}
          <select value={categoryTypeFilter} onChange={(e) => setCategoryTypeFilter(e.target.value)}>
            <option value="">All (term + salary advance)</option>
            <option value="TERM_LOAN">Term loans</option>
            <option value="SALARY_ADVANCE">Salary advance</option>
          </select>
        </label>
        <button type="button" className="btn-secondary" onClick={() => loadAll()}>
          Refresh
        </button>
      </div>

      {loanCategories.length > 0 && (
        <>
          <h2 style={{ marginTop: '1.5rem' }}>Loan categories (LC-04)</h2>
          <p style={{ color: '#555', marginBottom: '0.75rem' }}>
            LC-04: disbursement builds schedules from the category interest method — NONE (equal principal), FLAT (total
            interest on principal for the tenure, equal combined payments), or REDUCING_BALANCE (EMI-style amortization).
          </p>
          <div className="table-container" style={{ overflowX: 'auto', marginBottom: '1.5rem' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Interest</th>
                  <th>Rate % (flat)</th>
                  <th>Schedule note</th>
                </tr>
              </thead>
              <tbody>
                {loanCategories.map((c) => (
                  <tr key={c.categoryId}>
                    <td>{c.code}</td>
                    <td>{c.name}</td>
                    <td>{c.categoryType === 'SALARY_ADVANCE' ? 'Salary advance' : 'Term loan'}</td>
                    <td>{c.interestMethod ?? 'NONE'}</td>
                    <td>{c.flatAnnualRatePercent ?? '—'}</td>
                    <td style={{ maxWidth: 320 }}>{c.scheduleInterestNote ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <h2 style={{ marginTop: '1.5rem' }}>Payroll cross-check (RP-05)</h2>
      <p style={{ color: '#555', marginBottom: '0.75rem' }}>
        Compare payslip loan-repayment lines to loan-module postings for a payroll run, and flag expected recoveries not
        posted after a run is finalized.
      </p>
      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
        <label>
          Payroll run ID{' '}
          <input
            type="text"
            value={payrollRunIdCheck}
            onChange={(e) => setPayrollRunIdCheck(e.target.value)}
            placeholder="UUID"
            style={{ minWidth: 280 }}
          />
        </label>
        <button
          type="button"
          className="btn-secondary"
          disabled={crossLoading || !payrollRunIdCheck.trim() || !currentOrganizationId}
          onClick={async () => {
            if (!currentOrganizationId || !payrollRunIdCheck.trim()) return;
            setCrossLoading(true);
            setCrossCheckRows([]);
            try {
              const res = await getPayrollRecoveryCrossCheck(currentOrganizationId, payrollRunIdCheck.trim());
              setCrossCheckRows(Array.isArray(res.data) ? res.data : []);
            } catch (e: any) {
              setError(e.response?.data?.message || e.message || 'Cross-check failed');
            } finally {
              setCrossLoading(false);
            }
          }}
        >
          {crossLoading ? 'Checking…' : 'Run cross-check'}
        </button>
      </div>
      {crossCheckRows.length > 0 && (
        <div className="table-container" style={{ overflowX: 'auto', marginBottom: '1.5rem' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Employee</th>
                <th>Message</th>
                <th>Payslip</th>
                <th>Posted</th>
                <th>Variance</th>
              </tr>
            </thead>
            <tbody>
              {crossCheckRows.map((r, i) => (
                <tr key={i}>
                  <td>{r.type}</td>
                  <td>{r.employeeId ?? '—'}</td>
                  <td>{r.message}</td>
                  <td>{r.payslipLoanAmount ?? '—'}</td>
                  <td>{r.postedLoanAmount ?? '—'}</td>
                  <td>{r.varianceAmount ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h2 style={{ marginTop: '1.5rem' }}>Accounting export (PI-05)</h2>
      <p style={{ marginBottom: '0.75rem', color: '#555' }}>
        Disbursements and repayments in a date range for finance reconciliation. CSV includes suggested labels and, when
        configured below, optional GL account codes for import — full automated GL posting remains out of scope.
      </p>

      <div
        style={{
          marginBottom: '1.25rem',
          padding: '1rem',
          background: '#fafafa',
          border: '1px solid #e0e0e0',
          borderRadius: 8,
        }}
      >
        <h3 style={{ marginTop: 0 }}>Chart of accounts mapping (optional)</h3>
        <p style={{ color: '#555', fontSize: '0.95rem', marginTop: 0 }}>
          Map disbursement and repayment export lines to your accounting codes. Leave blank to omit COA columns from
          exports.
        </p>
        {coaLoadError && (
          <p style={{ color: '#b45309', fontSize: '0.95rem', marginBottom: '0.75rem' }}>
            {coaLoadError} (register and exports still work; retry refresh.)
          </p>
        )}
        <div className="table-container" style={{ overflowX: 'auto', marginBottom: '0.75rem' }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Event</th>
                <th>Debit account</th>
                <th>Credit account</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {(['LOAN_DISBURSEMENT', 'LOAN_REPAYMENT'] as const).map((key) => (
                <tr key={key}>
                  <td>{key === 'LOAN_DISBURSEMENT' ? 'Loan disbursement' : 'Loan repayment'}</td>
                  <td>
                    <input
                      type="text"
                      value={coaByKey[key].debit}
                      onChange={(e) =>
                        setCoaByKey((prev) => ({
                          ...prev,
                          [key]: { ...prev[key], debit: e.target.value },
                        }))
                      }
                      placeholder="e.g. 1200"
                      style={{ minWidth: 100 }}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      value={coaByKey[key].credit}
                      onChange={(e) =>
                        setCoaByKey((prev) => ({
                          ...prev,
                          [key]: { ...prev[key], credit: e.target.value },
                        }))
                      }
                      placeholder="e.g. 2100"
                      style={{ minWidth: 100 }}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      value={coaByKey[key].notes}
                      onChange={(e) =>
                        setCoaByKey((prev) => ({
                          ...prev,
                          [key]: { ...prev[key], notes: e.target.value },
                        }))
                      }
                      placeholder="Optional"
                      style={{ minWidth: 160 }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <button
          type="button"
          className="btn-secondary"
          disabled={coaSaving}
          onClick={async () => {
            if (!currentOrganizationId) return;
            setCoaSaving(true);
            setError(null);
            try {
              const body: LoanAccountingCoaMappingUpsertBody[] = [];
              (['LOAN_DISBURSEMENT', 'LOAN_REPAYMENT'] as const).forEach((key) => {
                const d = coaByKey[key].debit.trim();
                const c = coaByKey[key].credit.trim();
                if (d && c) {
                  body.push({
                    mappingKey: key,
                    debitAccountCode: d,
                    creditAccountCode: c,
                    notes: coaByKey[key].notes.trim() || undefined,
                  });
                }
              });
              await putLoanAccountingCoaMappingsApi(currentOrganizationId, body);
              const refreshed = await getLoanAccountingCoaMappingsApi(currentOrganizationId);
              const coaList: LoanAccountingCoaMappingDto[] = Array.isArray(refreshed.data) ? refreshed.data : [];
              const byKey = Object.fromEntries(coaList.map((m) => [m.mappingKey, m])) as Record<
                string,
                LoanAccountingCoaMappingDto
              >;
              setCoaByKey({
                LOAN_DISBURSEMENT: {
                  debit: byKey['LOAN_DISBURSEMENT']?.debitAccountCode ?? '',
                  credit: byKey['LOAN_DISBURSEMENT']?.creditAccountCode ?? '',
                  notes: byKey['LOAN_DISBURSEMENT']?.notes ?? '',
                },
                LOAN_REPAYMENT: {
                  debit: byKey['LOAN_REPAYMENT']?.debitAccountCode ?? '',
                  credit: byKey['LOAN_REPAYMENT']?.creditAccountCode ?? '',
                  notes: byKey['LOAN_REPAYMENT']?.notes ?? '',
                },
              });
            } catch (e: any) {
              setError(e.response?.data?.message || e.message || 'Failed to save COA mappings');
            } finally {
              setCoaSaving(false);
            }
          }}
        >
          {coaSaving ? 'Saving…' : 'Save COA mappings'}
        </button>
      </div>
      <div style={{ marginBottom: '1rem', display: 'flex', flexWrap: 'wrap', gap: '0.75rem', alignItems: 'center' }}>
        <label>
          From{' '}
          <input type="date" value={acctFrom} onChange={(e) => setAcctFrom(e.target.value)} />
        </label>
        <label>
          To{' '}
          <input type="date" value={acctTo} onChange={(e) => setAcctTo(e.target.value)} />
        </label>
        <button
          type="button"
          className="btn-secondary"
          disabled={acctLoading}
          onClick={async () => {
            if (!currentOrganizationId) return;
            setAcctLoading(true);
            setAcctPreview(null);
            try {
              const res = await getLoanAccountingExport(currentOrganizationId, acctFrom, acctTo);
              const d = res.data as {
                totalDisbursements?: number | string;
                totalRepayments?: number | string;
              };
              setAcctPreview({
                totalDisbursements: Number(d.totalDisbursements ?? 0),
                totalRepayments: Number(d.totalRepayments ?? 0),
              });
            } catch (e: any) {
              setError(e.response?.data?.message || e.message || 'Failed to load accounting export');
            } finally {
              setAcctLoading(false);
            }
          }}
        >
          Preview totals
        </button>
        <button
          type="button"
          className="btn-secondary"
          onClick={async () => {
            if (!currentOrganizationId) return;
            try {
              const res = await exportLoanAccountingDisbursementsCsv(currentOrganizationId, acctFrom, acctTo);
              downloadBlob(res.data as Blob, `loan-accounting-disbursements-${acctFrom}-${acctTo}.csv`);
            } catch (e: any) {
              setError(e.response?.data?.message || e.message || 'Export failed');
            }
          }}
        >
          Download disbursements CSV
        </button>
        <button
          type="button"
          className="btn-secondary"
          onClick={async () => {
            if (!currentOrganizationId) return;
            try {
              const res = await exportLoanAccountingRepaymentsCsv(currentOrganizationId, acctFrom, acctTo);
              downloadBlob(res.data as Blob, `loan-accounting-repayments-${acctFrom}-${acctTo}.csv`);
            } catch (e: any) {
              setError(e.response?.data?.message || e.message || 'Export failed');
            }
          }}
        >
          Download repayments CSV
        </button>
      </div>
      {acctPreview && (
        <p style={{ marginBottom: '1rem' }}>
          Period totals — disbursements: {acctPreview.totalDisbursements.toFixed(2)}, repayments:{' '}
          {acctPreview.totalRepayments.toFixed(2)}
        </p>
      )}

      <h2>Loan register</h2>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Category</th>
              <th>Type</th>
              <th>Principal</th>
              <th>Outstanding</th>
              <th>Status</th>
              <th>Disbursed</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.loanId}>
                <td>
                  {r.employeeName || '—'} ({r.employeeNumber || '—'})
                </td>
                <td>{r.categoryName || r.categoryId}</td>
                <td>
                  {r.categoryType === 'SALARY_ADVANCE'
                    ? 'Salary advance'
                    : r.categoryType === 'TERM_LOAN'
                      ? 'Term loan'
                      : r.categoryType ?? '—'}
                </td>
                <td>
                  {r.principalAmount} {r.currency}
                </td>
                <td>
                  {r.outstandingBalance} {r.currency}
                </td>
                <td>{r.status}</td>
                <td>{r.disbursementDate || '—'}</td>
                <td>
                  <button type="button" className="btn-secondary" onClick={() => navigate(`/hr/loans/${r.loanId}`)}>
                    Detail
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h2 style={{ marginTop: '2rem' }}>Arrears (installments past due)</h2>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Seq</th>
              <th>Due</th>
              <th>Remaining</th>
              <th>Days past due</th>
            </tr>
          </thead>
          <tbody>
            {arrears.slice(0, 50).map((a) => (
              <tr key={`${a.loanId}-${a.installmentId}`}>
                <td>{a.employeeName}</td>
                <td>{a.sequenceNumber}</td>
                <td>{a.dueDate}</td>
                <td>{a.remainingAmount}</td>
                <td>{a.daysPastDue}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {arrears.length > 50 && <p>Showing first 50 of {arrears.length} rows. Export CSV from API if needed.</p>}
      </div>

      <h2 style={{ marginTop: '2rem' }}>Settlement at exit</h2>
      <div className="table-container" style={{ overflowX: 'auto' }}>
        <table className="data-table">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Outstanding</th>
              <th>Shortfall (audit)</th>
              <th>Separation date</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {settlements.map((s) => (
              <tr key={s.loanId}>
                <td>{s.employeeName}</td>
                <td>
                  {s.outstandingBalance} {s.currency}
                </td>
                <td>{s.settlementShortfallAmount ?? '—'}</td>
                <td>{s.separationEffectiveDate || '—'}</td>
                <td>
                  <button type="button" className="btn-secondary" onClick={() => navigate(`/hr/loans/${s.loanId}`)}>
                    Detail
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default LoanRegister;
