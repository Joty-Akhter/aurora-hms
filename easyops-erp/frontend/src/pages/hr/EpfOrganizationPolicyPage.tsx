import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getEpfOrganizationPolicy, putEpfOrganizationPolicy, EpfOrganizationPolicy } from '../../services/hrService';
import './Hr.css';

const num = (v: string): number | undefined => {
  const t = v.trim();
  if (!t) return undefined;
  const n = Number(t);
  return Number.isFinite(n) ? n : undefined;
};

const EpfOrganizationPolicyPage: React.FC = () => {
  const { currentOrganizationId } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);
  const [hasSavedRow, setHasSavedRow] = useState(false);

  const [empRate, setEmpRate] = useState('12');
  const [emprRate, setEmprRate] = useState('12');
  const [ceiling, setCeiling] = useState('');
  const [floor, setFloor] = useState('');
  const [eligibleTypes, setEligibleTypes] = useState('');
  const [ineligibleTypes, setIneligibleTypes] = useState('');

  useEffect(() => {
    if (!currentOrganizationId) return;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await getEpfOrganizationPolicy(currentOrganizationId);
        const p: EpfOrganizationPolicy = res.data;
        setHasSavedRow(true);
        setEmpRate(p.employeeContributionRate != null ? String(p.employeeContributionRate) : '12');
        setEmprRate(p.employerContributionRate != null ? String(p.employerContributionRate) : '12');
        setCeiling(p.pfWageCeiling != null && p.pfWageCeiling !== '' ? String(p.pfWageCeiling) : '');
        setFloor(p.pfWageFloor != null && p.pfWageFloor !== '' ? String(p.pfWageFloor) : '');
        setEligibleTypes(p.eligibleEmploymentTypes?.trim() ?? '');
        setIneligibleTypes(p.ineligibleEmploymentTypes?.trim() ?? '');
      } catch (e: any) {
        if (e.response?.status === 404) {
          setHasSavedRow(false);
          setEmpRate('12');
          setEmprRate('12');
          setCeiling('');
          setFloor('');
          setEligibleTypes('');
          setIneligibleTypes('');
        } else {
          setError(e.response?.data?.message || e.message || 'Failed to load EPF policy');
        }
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
      const body: EpfOrganizationPolicy = {
        organizationId: currentOrganizationId,
        employeeContributionRate: num(empRate) ?? 12,
        employerContributionRate: num(emprRate) ?? 12,
        pfWageCeiling: num(ceiling) ?? null,
        pfWageFloor: num(floor) ?? null,
        eligibleEmploymentTypes: eligibleTypes.trim() || undefined,
        ineligibleEmploymentTypes: ineligibleTypes.trim() || undefined,
      };
      await putEpfOrganizationPolicy(body);
      setHasSavedRow(true);
      setOk('EPF organization policy saved.');
    } catch (e: any) {
      setError(e.response?.data?.message || e.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  };

  if (!currentOrganizationId) {
    return (
      <div className="hr-page">
        <p className="error-message">Select an organization to configure EPF policy.</p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="hr-page">
        <p>Loading…</p>
      </div>
    );
  }

  return (
    <div className="hr-page">
      <h2>EPF organization policy</h2>
      <p style={{ color: '#666', maxWidth: '640px' }}>
        INT-09 / INT-12: Employee and employer contribution rates, PF wage ceiling and floor, and optional employment-type
        eligibility. Used by payroll when statutory PF components are deferred (computed from PF wage). If no row exists
        yet, saving creates one.
      </p>
      {!hasSavedRow && (
        <p style={{ color: '#856404', background: '#fff3cd', padding: '0.5rem 0.75rem', borderRadius: 4 }}>
          No policy row found for this organization. Defaults below are shown; save to persist.
        </p>
      )}
      {error && <div className="error-message" style={{ marginBottom: '0.75rem' }}>{error}</div>}
      {ok && (
        <div style={{ marginBottom: '0.75rem', color: '#2e7d32' }} role="status">
          {ok}
        </div>
      )}

      <div className="form-row">
        <label htmlFor="epf-emp-rate">Employee contribution rate (%)</label>
        <input
          id="epf-emp-rate"
          type="text"
          inputMode="decimal"
          value={empRate}
          onChange={(e) => setEmpRate(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label htmlFor="epf-empr-rate">Employer contribution rate (%)</label>
        <input
          id="epf-empr-rate"
          type="text"
          inputMode="decimal"
          value={emprRate}
          onChange={(e) => setEmprRate(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label htmlFor="epf-ceiling">PF wage ceiling (optional)</label>
        <input
          id="epf-ceiling"
          type="text"
          inputMode="decimal"
          placeholder="No cap if empty"
          value={ceiling}
          onChange={(e) => setCeiling(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label htmlFor="epf-floor">PF wage floor (optional)</label>
        <input
          id="epf-floor"
          type="text"
          inputMode="decimal"
          placeholder="No floor if empty"
          value={floor}
          onChange={(e) => setFloor(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label htmlFor="epf-eligible">Eligible employment types (comma-separated)</label>
        <input
          id="epf-eligible"
          type="text"
          placeholder="e.g. FULL_TIME,PART_TIME — empty means not restricted by this list"
          value={eligibleTypes}
          onChange={(e) => setEligibleTypes(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label htmlFor="epf-ineligible">Ineligible employment types (comma-separated)</label>
        <input
          id="epf-ineligible"
          type="text"
          placeholder="e.g. INTERN,CONTRACT"
          value={ineligibleTypes}
          onChange={(e) => setIneligibleTypes(e.target.value)}
        />
      </div>

      <button type="button" className="btn-primary" onClick={save} disabled={saving}>
        {saving ? 'Saving…' : 'Save policy'}
      </button>
    </div>
  );
};

export default EpfOrganizationPolicyPage;
