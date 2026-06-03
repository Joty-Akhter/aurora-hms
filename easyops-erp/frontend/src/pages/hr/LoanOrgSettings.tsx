import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import {
  LoanOrganizationSettings,
  LoanOrganizationSettingsPatch,
  getLoanOrganizationSettingsApi,
  patchLoanOrganizationSettingsApi,
  getLoanAccountingCoaMappingsApi,
  putLoanAccountingCoaMappingsApi,
  LoanHolidayShiftMode,
  LoanAccountingCoaMappingDto,
  LoanAccountingCoaMappingUpsertBody,
} from '../../services/hrService';
import './Hr.css';

const LoanOrgSettings: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [settings, setSettings] = useState<LoanOrganizationSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);

  const [minTenure, setMinTenure] = useState('');
  const [maxPrincipal, setMaxPrincipal] = useState('');
  const [currency, setCurrency] = useState('');
  const [enforceSingle, setEnforceSingle] = useState(false);
  const [allowAdvanceWithTerm, setAllowAdvanceWithTerm] = useState(false);
  const [enforceSettlementOrder, setEnforceSettlementOrder] = useState(false);
  const [skipFinance, setSkipFinance] = useState(false);
  const [salaryAdvanceSkipFinance, setSalaryAdvanceSkipFinance] = useState(false);
  const [disqualifyingStatuses, setDisqualifyingStatuses] = useState('');
  const [priorityText, setPriorityText] = useState('');
  const [shiftHoliday, setShiftHoliday] = useState(false);
  const [shiftMode, setShiftMode] = useState<LoanHolidayShiftMode>('NEXT_BUSINESS_DAY');
  const [coaByKey, setCoaByKey] = useState<{
    LOAN_DISBURSEMENT: { debit: string; credit: string; notes: string };
    LOAN_REPAYMENT: { debit: string; credit: string; notes: string };
  }>({
    LOAN_DISBURSEMENT: { debit: '', credit: '', notes: '' },
    LOAN_REPAYMENT: { debit: '', credit: '', notes: '' },
  });
  const [coaSaving, setCoaSaving] = useState(false);
  const [coaLoadError, setCoaLoadError] = useState<string | null>(null);

  useEffect(() => {
    if (!currentOrganizationId) return;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const settingsRes = await getLoanOrganizationSettingsApi(currentOrganizationId);
        const s = settingsRes.data;
        setSettings(s);
        setMinTenure(s.minTenureMonths != null ? String(s.minTenureMonths) : '');
        setMaxPrincipal(s.maxPrincipalAmount != null ? String(s.maxPrincipalAmount) : '');
        setCurrency(s.currency ?? '');
        setEnforceSingle(!!s.enforceSingleActiveLoan);
        setAllowAdvanceWithTerm(!!s.allowSalaryAdvanceWithActiveTermLoan);
        setEnforceSettlementOrder(!!s.enforceSettlementAllocationOrder);
        setSkipFinance(!!s.skipFinanceApproval);
        setSalaryAdvanceSkipFinance(!!s.salaryAdvanceSkipFinanceApproval);
        setDisqualifyingStatuses((s.disqualifyingEmploymentStatuses ?? []).join(', '));
        setPriorityText((s.settlementAllocationPriority ?? []).join('\n'));
        setShiftHoliday(!!s.shiftInstallmentDueDatesForHolidays);
        setShiftMode(s.loanHolidayShiftMode ?? 'NEXT_BUSINESS_DAY');
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
        setError(e.response?.data?.message || e.message || 'Failed to load settings');
      } finally {
        setLoading(false);
      }
    })();
  }, [currentOrganizationId]);

  const save = async () => {
    if (!currentOrganizationId) return;
    try {
      setSaving(true);
      setError(null);
      setOk(null);
      const patch: LoanOrganizationSettingsPatch = {
        minTenureMonths: minTenure ? Number(minTenure) : undefined,
        maxPrincipalAmount: maxPrincipal ? Number(maxPrincipal) : undefined,
        currency: currency.trim() || undefined,
        enforceSingleActiveLoan: enforceSingle,
        allowSalaryAdvanceWithActiveTermLoan: allowAdvanceWithTerm,
        enforceSettlementAllocationOrder: enforceSettlementOrder,
        skipFinanceApproval: skipFinance,
        salaryAdvanceSkipFinanceApproval: salaryAdvanceSkipFinance,
        disqualifyingEmploymentStatuses: disqualifyingStatuses
          .split(',')
          .map((x) => x.trim())
          .filter(Boolean),
        settlementAllocationPriority: priorityText
          .split(/[\n,]+/)
          .map((x) => x.trim())
          .filter(Boolean),
        shiftInstallmentDueDatesForHolidays: shiftHoliday,
        loanHolidayShiftMode: shiftMode,
      };
      const res = await patchLoanOrganizationSettingsApi(currentOrganizationId, patch);
      setSettings(res.data);
      setOk('Saved.');
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  if (!currentOrganizationId) {
    return <div className="error-message">Select an organization.</div>;
  }

  if (loading) {
    return <div className="loading">Loading loan settings…</div>;
  }

  return (
    <div className="hr-dashboard">
      <div className="page-header">
        <h1>Loan organization settings</h1>
        <p>Policy caps, eligibility flags, and BR-08 salary-advance finance shortcut.</p>
      </div>
      {error && <div className="error-message">{error}</div>}
      {ok && <div style={{ color: '#065f46', marginBottom: '0.75rem' }}>{ok}</div>}

      {settings && (
        <div style={{ background: '#fff', padding: '1.25rem', borderRadius: 12, maxWidth: 720 }}>
          <div className="field-group" style={{ marginBottom: '0.75rem' }}>
            <label>Min tenure (months)</label>
            <input type="number" min={0} value={minTenure} onChange={(e) => setMinTenure(e.target.value)} />
          </div>
          <div className="field-group" style={{ marginBottom: '0.75rem' }}>
            <label>Max principal (org cap)</label>
            <input type="number" min={0} step="0.01" value={maxPrincipal} onChange={(e) => setMaxPrincipal(e.target.value)} />
          </div>
          <div className="field-group" style={{ marginBottom: '0.75rem' }}>
            <label>Currency</label>
            <input value={currency} onChange={(e) => setCurrency(e.target.value)} placeholder="e.g. BDT" />
          </div>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: '0.75rem' }}>
            <input type="checkbox" checked={enforceSingle} onChange={(e) => setEnforceSingle(e.target.checked)} />
            Enforce single active loan (BR-02)
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: '0.75rem' }}>
            <input type="checkbox" checked={allowAdvanceWithTerm} onChange={(e) => setAllowAdvanceWithTerm(e.target.checked)} />
            Allow salary advance while term loan active (EL-06)
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: '0.75rem' }}>
            <input type="checkbox" checked={enforceSettlementOrder} onChange={(e) => setEnforceSettlementOrder(e.target.checked)} />
            Enforce settlement allocation order
          </label>
          <label
            style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: '0.75rem' }}
            title="Skip Finance step for all categories when enabled"
          >
            <input type="checkbox" checked={skipFinance} onChange={(e) => setSkipFinance(e.target.checked)} />
            Skip finance approval (all categories)
          </label>
          <label
            style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: '0.75rem' }}
            title="When set, salary-advance applications can finalize after HR approval without Finance"
          >
            <input type="checkbox" checked={salaryAdvanceSkipFinance} onChange={(e) => setSalaryAdvanceSkipFinance(e.target.checked)} />
            Salary advance: skip finance approval (BR-08)
          </label>
          <div className="field-group" style={{ marginBottom: '0.75rem' }}>
            <label>Disqualifying employment statuses (comma-separated)</label>
            <input value={disqualifyingStatuses} onChange={(e) => setDisqualifyingStatuses(e.target.value)} />
          </div>
          <div className="field-group" style={{ marginBottom: '0.75rem' }}>
            <label>Settlement allocation priority (one code per line)</label>
            <textarea rows={4} value={priorityText} onChange={(e) => setPriorityText(e.target.value)} />
          </div>

          <h2 style={{ marginTop: '1.5rem', fontSize: '1.1rem' }}>Holiday calendar — installment due dates (AD-03)</h2>
          <p style={{ color: '#555', fontSize: '0.95rem' }}>
            When enabled, new schedules and recalculations move due dates off weekends and HR holiday calendar entries.
          </p>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: '0.75rem' }}>
            <input type="checkbox" checked={shiftHoliday} onChange={(e) => setShiftHoliday(e.target.checked)} />
            Shift installment due dates for weekends and holidays
          </label>
          <div className="field-group" style={{ marginBottom: '0.75rem' }}>
            <label>
              Non-business-day rule:{' '}
              <select value={shiftMode} onChange={(e) => setShiftMode(e.target.value as LoanHolidayShiftMode)}>
                <option value="NEXT_BUSINESS_DAY">Next business day</option>
                <option value="PREVIOUS_BUSINESS_DAY">Previous business day</option>
              </select>
            </label>
          </div>

          <h2 style={{ marginTop: '1.5rem', fontSize: '1.1rem' }}>Accounting COA mapping (PI-05, optional)</h2>
          <p style={{ color: '#555', fontSize: '0.95rem', marginBottom: '0.75rem' }}>
            GL account codes appended to accounting CSV/JSON export lines. Save separately from policy above.
          </p>
          {coaLoadError && (
            <p style={{ color: '#b45309', fontSize: '0.95rem', marginBottom: '0.75rem' }}>{coaLoadError}</p>
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
            style={{ marginBottom: '1rem' }}
            onClick={async () => {
              if (!currentOrganizationId) return;
              setCoaSaving(true);
              setError(null);
              setOk(null);
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
                setOk('COA mappings saved.');
              } catch (e: any) {
                setError(e.response?.data?.message || e.message || 'Failed to save COA mappings');
              } finally {
                setCoaSaving(false);
              }
            }}
          >
            {coaSaving ? 'Saving…' : 'Save COA mappings'}
          </button>

          <button type="button" className="btn-primary" disabled={saving} onClick={save}>
            {saving ? 'Saving…' : 'Save policy settings'}
          </button>
        </div>
      )}
    </div>
  );
};

export default LoanOrgSettings;
