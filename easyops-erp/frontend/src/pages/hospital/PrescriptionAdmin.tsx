import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import hospitalService, { epConfigService, DoctorEPConfig, PharmacyDrugSuggestion } from '../../services/hospitalService';
import { pullEpWorkspaceFromServer, pushEpWorkspaceToServer } from '../../services/epWorkspaceSync';
import hospitalSchedulingService from '../../services/hospitalSchedulingService';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import './Hospital.css';

const DEFAULT_ADVICE = [
  'Take rest',
  'Drink plenty of water (2–3 L/day)',
  'Avoid oily / spicy food',
  'Complete antibiotic course',
  'Monitor BP at home daily',
  'Monitor blood glucose daily',
  'Regular exercise 30 min/day',
  'Avoid smoking and alcohol',
];

const DOSE_FORMAT_EXAMPLES = [
  { format: '1+0+1', description: 'Shorthand: morning + afternoon + evening doses. Example: 1+0+1 = twice daily (morning & evening).' },
  { format: '1+1+1', description: '3× daily — one tablet per dose time.' },
  { format: '½+0+½', description: 'Half-tablet twice daily.' },
  { format: '1+1+1+1', description: '4× daily — every 6 hours.' },
  { format: 'Once daily', description: 'Named frequency — rendered as text on the Rx.' },
  { format: 'As needed (PRN)', description: 'PRN — take when required.' },
];

const PrescriptionAdmin: React.FC = () => {
  const navigate = useNavigate();
  const sigInputRef = useRef<HTMLInputElement>(null);
  const stampInputRef = useRef<HTMLInputElement>(null);

  const [config, setConfig] = useState<DoctorEPConfig>({});
  const [saved, setSaved] = useState(false);
  const [defaultAdviceList, setDefaultAdviceList] = useState<string[]>(DEFAULT_ADVICE);
  const [newAdvice, setNewAdvice] = useState('');
  const [preferredMedicines, setPreferredMedicines] = useState<string[]>([]);
  const [newPreferredMed, setNewPreferredMed] = useState('');
  const [schedulingResources, setSchedulingResources] = useState<{ id: string; label: string }[]>([]);
  const [schedulingResourcesError, setSchedulingResourcesError] = useState<string | null>(null);

  const [catalogQuery, setCatalogQuery] = useState('');
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [catalogRows, setCatalogRows] = useState<PharmacyDrugSuggestion[]>([]);
  const [catalogError, setCatalogError] = useState<string | null>(null);
  const [workspaceSyncing, setWorkspaceSyncing] = useState(false);
  const [workspaceSyncMessage, setWorkspaceSyncMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      await pullEpWorkspaceFromServer();
      if (cancelled) return;
      const cfg = epConfigService.get();
      setConfig(cfg);
      if (cfg.defaultAdvice && cfg.defaultAdvice.length > 0) {
        setDefaultAdviceList(cfg.defaultAdvice);
      }
      setPreferredMedicines(cfg.preferredMedicineNames ?? []);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await hospitalSchedulingService.getResources({ size: 200 });
        const rows = (data.content ?? []).map(r => ({
          id: r.id,
          label: `${r.name || r.externalReferenceId || r.id}${r.resourceType ? ` (${r.resourceType})` : ''}`,
        }));
        if (!cancelled) {
          setSchedulingResources(rows);
          setSchedulingResourcesError(null);
        }
      } catch (e: unknown) {
        if (!cancelled) {
          setSchedulingResourcesError(
            ehrApiErrorMessage(e, 'Scheduling service unavailable — enter resource UUID manually below.'),
          );
        }
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const q = catalogQuery.trim();
    if (q.length < 2) {
      setCatalogRows([]);
      setCatalogError(null);
      return;
    }
    let cancelled = false;
    const t = setTimeout(async () => {
      setCatalogLoading(true);
      setCatalogError(null);
      try {
        const r = await hospitalService.searchDrugsForPrescription(q, 0, 15);
        if (!cancelled) setCatalogRows(r.data.content ?? []);
      } catch (e: unknown) {
        if (!cancelled) {
          setCatalogRows([]);
          setCatalogError(ehrApiErrorMessage(e, 'Could not search catalog (check pharmacy service).'));
        }
      } finally {
        if (!cancelled) setCatalogLoading(false);
      }
    }, 350);
    return () => {
      cancelled = true;
      clearTimeout(t);
    };
  }, [catalogQuery]);

  const save = () => {
    epConfigService.save({
      ...config,
      defaultAdvice: defaultAdviceList,
      preferredMedicineNames: preferredMedicines,
    });
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const syncWorkspaceWithServer = async () => {
    setWorkspaceSyncing(true);
    setWorkspaceSyncMessage(null);
    try {
      const pulled = await pullEpWorkspaceFromServer();
      const cfgAfterPull = epConfigService.get();
      setConfig(cfgAfterPull);
      if (cfgAfterPull.defaultAdvice && cfgAfterPull.defaultAdvice.length > 0) {
        setDefaultAdviceList(cfgAfterPull.defaultAdvice);
      }
      setPreferredMedicines(cfgAfterPull.preferredMedicineNames ?? []);
      const pushed = await pushEpWorkspaceToServer();
      if (pulled || pushed) {
        setWorkspaceSyncMessage('Workspace synced with server (pull + push).');
      } else {
        setWorkspaceSyncMessage('No organization selected or server had no workspace data; local settings were pushed if possible.');
      }
    } catch {
      setWorkspaceSyncMessage('Sync failed — check network and hospital-service workspace API.');
    } finally {
      setWorkspaceSyncing(false);
    }
  };

  const handleImageUpload = (field: 'signatureDataUrl' | 'stampDataUrl', file: File) => {
    const reader = new FileReader();
    reader.onload = e => {
      const dataUrl = e.target?.result as string;
      setConfig(prev => ({ ...prev, [field]: dataUrl }));
    };
    reader.readAsDataURL(file);
  };

  const clearImage = (field: 'signatureDataUrl' | 'stampDataUrl') => {
    setConfig(prev => ({ ...prev, [field]: undefined }));
  };

  return (
    <div className="hospital-page">
      <div className="page-header" style={{ marginBottom: '24px' }}>
        <div>
          <h1>Easy Prescription — Admin Settings</h1>
          <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
            Configure doctor profile, print header/footer, signature, stamp, and default advice
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn-secondary" onClick={() => navigate('/hospital/doctor-dashboard')}>Dashboard</button>
          <button className="btn-secondary" onClick={() => navigate('/hospital/prescriptions/templates')}>Templates</button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', alignItems: 'start' }}>

        {/* Left column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

          {/* Print Header / Footer */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Print Header & Footer (EP-8)</h3>
            <div className="form-group">
              <label>Header Line 1 (Hospital / Clinic Name)</label>
              <input
                type="text"
                value={config.headerLine1 || ''}
                onChange={e => setConfig(p => ({ ...p, headerLine1: e.target.value }))}
                placeholder="e.g. City General Hospital"
              />
            </div>
            <div className="form-group">
              <label>Header Line 2 (Dept / Address / Phone)</label>
              <input
                type="text"
                value={config.headerLine2 || ''}
                onChange={e => setConfig(p => ({ ...p, headerLine2: e.target.value }))}
                placeholder="e.g. Dept. of Internal Medicine | Tel: 01700-000000"
              />
            </div>
            <div className="form-group">
              <label>Footer Text (Qualifications / Reg. No.)</label>
              <input
                type="text"
                value={config.footerText || ''}
                onChange={e => setConfig(p => ({ ...p, footerText: e.target.value }))}
                placeholder="e.g. MBBS, MD (Internal Medicine) | BMDC Reg: 12345"
              />
            </div>
            <div className="form-group">
              <label>Print Format</label>
              <select value={config.printFormat || 'A4'} onChange={e => setConfig(p => ({ ...p, printFormat: e.target.value as 'A4' | 'COMPACT' }))}>
                <option value="A4">A4 (standard)</option>
                <option value="COMPACT">Compact (half-page)</option>
              </select>
            </div>
            <div className="form-group">
              <label>Default clinical mode (EP-1 / EP-11)</label>
              <select
                value={config.epEncounterModeDefault || 'OPD'}
                onChange={e => setConfig(p => ({ ...p, epEncounterModeDefault: e.target.value as 'OPD' | 'IPD' }))}
              >
                <option value="OPD">OPD (outpatient)</option>
                <option value="IPD">IPD (inpatient)</option>
              </select>
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
                Used for Doctor Dashboard default and prescribe deep links when <code>epMode</code> is omitted.
              </p>
            </div>
            <div className="form-group">
              <label>Prescribe SLA target (minutes)</label>
              <input
                type="number"
                min={1}
                max={60}
                value={config.rxSlaTargetMinutes ?? 2}
                onChange={e => {
                  const v = parseInt(e.target.value, 10);
                  setConfig(p => ({ ...p, rxSlaTargetMinutes: Number.isFinite(v) ? v : 2 }));
                }}
              />
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
                Session timer compares elapsed time to this target (default 2 minutes).
              </p>
            </div>
          </div>

          {/* Doctor Signature */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Signature & Stamp (EP-9)</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: 0 }}>
              Upload your signature and/or stamp image. These appear at the bottom of every printed prescription.
              Supported formats: PNG, JPG (transparent background PNG recommended for best results).
            </p>

            {/* Signature */}
            <div className="form-group">
              <label>Doctor Signature</label>
              {config.signatureDataUrl ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '8px' }}>
                  <img src={config.signatureDataUrl} alt="Signature" style={{ maxHeight: '60px', maxWidth: '200px', border: '1px solid #e5e7eb', borderRadius: '4px', padding: '4px', background: '#fff' }} />
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <button className="btn-secondary" style={{ fontSize: '12px', padding: '4px 10px' }} onClick={() => sigInputRef.current?.click()}>Replace</button>
                    <button style={{ fontSize: '12px', padding: '4px 10px', background: 'none', border: '1px solid #ef4444', color: '#ef4444', borderRadius: '6px', cursor: 'pointer' }} onClick={() => clearImage('signatureDataUrl')}>Remove</button>
                  </div>
                </div>
              ) : (
                <div style={{ marginTop: '8px' }}>
                  <button className="btn-secondary" onClick={() => sigInputRef.current?.click()}>Upload Signature Image</button>
                  <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '6px' }}>PNG with transparent background recommended</p>
                </div>
              )}
              <input ref={sigInputRef} type="file" accept="image/png,image/jpeg" style={{ display: 'none' }}
                onChange={e => { if (e.target.files?.[0]) handleImageUpload('signatureDataUrl', e.target.files[0]); }} />
            </div>

            {/* Stamp */}
            <div className="form-group">
              <label>Doctor / Clinic Stamp</label>
              {config.stampDataUrl ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '8px' }}>
                  <img src={config.stampDataUrl} alt="Stamp" style={{ maxHeight: '80px', maxWidth: '200px', border: '1px solid #e5e7eb', borderRadius: '4px', padding: '4px', background: '#fff' }} />
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <button className="btn-secondary" style={{ fontSize: '12px', padding: '4px 10px' }} onClick={() => stampInputRef.current?.click()}>Replace</button>
                    <button style={{ fontSize: '12px', padding: '4px 10px', background: 'none', border: '1px solid #ef4444', color: '#ef4444', borderRadius: '6px', cursor: 'pointer' }} onClick={() => clearImage('stampDataUrl')}>Remove</button>
                  </div>
                </div>
              ) : (
                <div style={{ marginTop: '8px' }}>
                  <button className="btn-secondary" onClick={() => stampInputRef.current?.click()}>Upload Stamp Image</button>
                </div>
              )}
              <input ref={stampInputRef} type="file" accept="image/png,image/jpeg" style={{ display: 'none' }}
                onChange={e => { if (e.target.files?.[0]) handleImageUpload('stampDataUrl', e.target.files[0]); }} />
            </div>
          </div>

          {/* Default Pharmacy */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Default Pharmacy</h3>
            <div className="form-group">
              <label>Default Pharmacy Name</label>
              <input
                type="text"
                value={config.defaultPharmacy || ''}
                onChange={e => setConfig(p => ({ ...p, defaultPharmacy: e.target.value }))}
                placeholder="e.g. City Pharmacy"
              />
            </div>
          </div>

          {/* My OPD queue (doctor filter) EP-2 */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>My OPD queue (scheduling resource)</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '0 0 12px' }}>
              When set, <strong>Doctor Dashboard → Today&apos;s OPD Queue</strong> only loads appointments for this resource (e.g. your physician column).
              Leave empty to see all appointments for the day (organization-wide).
            </p>
            <div className="form-group">
              <label>Scheduling resource</label>
              <select
                value={config.mySchedulingResourceId && schedulingResources.some(r => r.id === config.mySchedulingResourceId) ? config.mySchedulingResourceId : ''}
                onChange={e => setConfig(p => ({ ...p, mySchedulingResourceId: e.target.value || undefined }))}
              >
                <option value="">— All resources (no filter) —</option>
                {schedulingResources.map(r => (
                  <option key={r.id} value={r.id}>{r.label}</option>
                ))}
              </select>
              {schedulingResourcesError && (
                <p style={{ fontSize: '12px', color: '#b45309', marginTop: '8px' }}>{schedulingResourcesError}</p>
              )}
            </div>
            <div className="form-group">
              <label>Or paste resource UUID</label>
              <input
                type="text"
                value={config.mySchedulingResourceId || ''}
                onChange={e => setConfig(p => ({ ...p, mySchedulingResourceId: e.target.value.trim() || undefined }))}
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
              />
            </div>
          </div>
        </div>

        {/* Right column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

          {/* Dose Format Reference (EP-9) */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Dose Shorthand Reference (EP-5)</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '0 0 12px' }}>
              The system automatically calculates tablet quantities using the shorthand frequency and duration.
              Format: <code style={{ background: '#f3f4f6', padding: '1px 5px', borderRadius: '4px' }}>morning+afternoon+evening</code>
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {DOSE_FORMAT_EXAMPLES.map(ex => (
                <div key={ex.format} style={{ display: 'flex', gap: '12px', padding: '8px 12px', background: 'var(--bg-elevated, #f9fafb)', borderRadius: '6px', border: '1px solid var(--border-color, #e5e7eb)' }}>
                  <code style={{ flexShrink: 0, fontWeight: 700, fontSize: '13px', color: 'var(--color-primary, #1d4ed8)', minWidth: '80px' }}>{ex.format}</code>
                  <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{ex.description}</span>
                </div>
              ))}
            </div>
            <div style={{ marginTop: '12px', padding: '12px', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '6px', fontSize: '13px' }}>
              <strong>Course size (not stored):</strong> Frequency <code>1+0+1</code> for <strong>7 days</strong> →
              2 doses/day × 7 days ≈ <strong>14 units</strong> (shown as an estimate from frequency and duration)
            </div>
          </div>

          {/* Prescribe screen hints + patient portal (EP-8 / EP-9) */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Prescribe screen &amp; sharing</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '0 0 12px' }}>
              Optional text shown on the prescription form for your team&apos;s dose notation, and a patient-portal base URL for a shareable link (your portal must implement the path below).
            </p>
            <div className="form-group">
              <label>Frequency field placeholder</label>
              <input
                type="text"
                value={config.doseFrequencyPlaceholder || ''}
                onChange={e => setConfig(p => ({ ...p, doseFrequencyPlaceholder: e.target.value || undefined }))}
                placeholder="e.g. 1-0-1 or BID (leave empty for default)"
              />
            </div>
            <div className="form-group">
              <label>Hint under frequency (one line, optional)</label>
              <textarea
                rows={2}
                value={config.doseFormatNote || ''}
                onChange={e => setConfig(p => ({ ...p, doseFormatNote: e.target.value || undefined }))}
                placeholder="e.g. Use 1-0-1 = morning–noon–evening tablet counts."
                style={{ width: '100%', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>
            <div className="form-group">
              <label>Patient portal base URL (optional)</label>
              <input
                type="url"
                value={config.patientPortalBaseUrl || ''}
                onChange={e => setConfig(p => ({ ...p, patientPortalBaseUrl: e.target.value.trim() || undefined }))}
                placeholder="https://portal.example.com/app"
              />
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
                Link copied for patients: <code style={{ fontSize: '11px' }}>{'{base}'}/patient/{'{patientId}'}/prescriptions</code>
              </p>
            </div>
          </div>

          {/* Default Advice (EP-9) */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Default Advice Items</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: 0 }}>
              These advice items appear as quick-select chips on the prescription form.
              Reorder by dragging (not supported here — edit the list directly).
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '12px' }}>
              {defaultAdviceList.map((a, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 10px', background: 'var(--bg-elevated, #f9fafb)', borderRadius: '6px', border: '1px solid var(--border-color, #e5e7eb)' }}>
                  <span style={{ flex: 1, fontSize: '13px' }}>{a}</span>
                  <button type="button" onClick={() => setDefaultAdviceList(p => p.filter((_, j) => j !== i))}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: '14px' }}>×</button>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                type="text"
                value={newAdvice}
                onChange={e => setNewAdvice(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    if (newAdvice.trim()) { setDefaultAdviceList(p => [...p, newAdvice.trim()]); setNewAdvice(''); }
                  }
                }}
                placeholder="Add advice item…"
                style={{ flex: 1, padding: '6px 10px', fontSize: '13px' }}
              />
              <button className="btn-secondary" onClick={() => { if (newAdvice.trim()) { setDefaultAdviceList(p => [...p, newAdvice.trim()]); setNewAdvice(''); } }}>Add</button>
            </div>
          </div>

          {/* Preferred medicine shortcuts */}
          <div className="form-container" style={{ padding: '20px' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Preferred medicine shortcuts (EP-9)</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '0 0 12px' }}>
              One-tap add on the prescribing screen (e.g. your common go-to drugs). Names should match catalog entries when possible.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginBottom: '12px' }}>
              {preferredMedicines.map((m, i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 10px', background: 'var(--bg-elevated, #f9fafb)', borderRadius: '6px', border: '1px solid var(--border-color, #e5e7eb)' }}>
                  <span style={{ flex: 1, fontSize: '13px' }}>{m}</span>
                  <button type="button" onClick={() => setPreferredMedicines(p => p.filter((_, j) => j !== i))}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: '14px' }}>×</button>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <input
                type="text"
                value={newPreferredMed}
                onChange={e => setNewPreferredMed(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    if (newPreferredMed.trim()) {
                      setPreferredMedicines(p => [...p, newPreferredMed.trim()]);
                      setNewPreferredMed('');
                    }
                  }
                }}
                placeholder="e.g. Paracetamol 500 mg"
                style={{ flex: 1, padding: '6px 10px', fontSize: '13px' }}
              />
              <button className="btn-secondary" onClick={() => {
                if (newPreferredMed.trim()) {
                  setPreferredMedicines(p => [...p, newPreferredMed.trim()]);
                  setNewPreferredMed('');
                }
              }}>Add</button>
            </div>
          </div>

          {/* Medicine master — search + deep links (EP-9) */}
          <div className="form-container" style={{ padding: '20px', background: 'var(--bg-elevated, #f9fafb)' }}>
            <h3 style={{ marginTop: 0, fontSize: '15px' }}>Medicine Master (EP-9)</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: '0 0 12px' }}>
              Search the live pharmacy drug catalog (read-only). To add or edit products, use Pharmacy Catalog.
            </p>
            <input
              type="search"
              value={catalogQuery}
              onChange={e => setCatalogQuery(e.target.value)}
              placeholder="Type at least 2 characters to search…"
              style={{ width: '100%', padding: '8px 10px', fontSize: '13px', marginBottom: '10px', boxSizing: 'border-box' }}
            />
            {catalogLoading && <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Searching…</p>}
            {catalogError && <p style={{ fontSize: '12px', color: '#b45309' }}>{catalogError}</p>}
            {!catalogLoading && catalogQuery.trim().length >= 2 && catalogRows.length === 0 && !catalogError && (
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>No matches.</p>
            )}
            {catalogRows.length > 0 && (
              <div style={{ maxHeight: '220px', overflow: 'auto', border: '1px solid var(--border-color, #e5e7eb)', borderRadius: '6px', background: '#fff' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px' }}>
                  <thead>
                    <tr style={{ background: '#f3f4f6', textAlign: 'left' }}>
                      <th style={{ padding: '6px 8px' }}>Name</th>
                      <th style={{ padding: '6px 8px' }}>Strength / form</th>
                    </tr>
                  </thead>
                  <tbody>
                    {catalogRows.map(drug => (
                      <tr key={drug.id} style={{ borderTop: '1px solid #eee' }}>
                        <td style={{ padding: '6px 8px', verticalAlign: 'top' }}>
                          <strong>{drug.brandName || drug.genericName}</strong>
                          {drug.brandName && <div style={{ color: '#6b7280' }}>{drug.genericName}</div>}
                        </td>
                        <td style={{ padding: '6px 8px', color: '#374151' }}>
                          {[drug.strength, drug.form].filter(Boolean).join(' · ') || '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginTop: '12px' }}>
              <button className="btn-secondary" onClick={() => navigate('/hospital/pharmacy/catalog')}>
                Open Pharmacy Catalog →
              </button>
              <button className="btn-secondary" onClick={() => navigate('/hospital/pharmacy/stock')}>
                Pharmacy Stock →
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Save bar */}
      <div style={{ position: 'sticky', bottom: 0, background: 'var(--bg-surface, #fff)', borderTop: '1px solid var(--border-color, #e5e7eb)', padding: '14px 0', marginTop: '24px', display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
        <button className="btn-primary" onClick={save}>Save Settings</button>
        <button type="button" className="btn-secondary" disabled={workspaceSyncing} onClick={() => void syncWorkspaceWithServer()}>
          {workspaceSyncing ? 'Syncing…' : 'Sync workspace (server)'}
        </button>
        {saved && (
          <span style={{ fontSize: '13px', color: '#059669', fontWeight: 500 }}>✓ Settings saved</span>
        )}
        {workspaceSyncMessage && (
          <span style={{ fontSize: '13px', color: 'var(--text-secondary, #6b7280)', maxWidth: '420px' }}>{workspaceSyncMessage}</span>
        )}
      </div>
    </div>
  );
};

export default PrescriptionAdmin;
