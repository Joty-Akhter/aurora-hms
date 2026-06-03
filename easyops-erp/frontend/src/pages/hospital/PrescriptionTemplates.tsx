import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import { useAuth } from '@contexts/AuthContext';
import {
  default as hospitalService,
  epTemplateService,
  epConfigService,
  epLookupService,
  epAdviceService,
  EpLookupMap,
  PrescriptionTemplate,
  PrescriptionMedicationItem,
  COMMON_ICD10_CODES,
  Doctor,
  EpTemplateListScope,
  matchesEpTemplateScope,
} from '../../services/hospitalService';
import { pullEpWorkspaceFromServer } from '../../services/epWorkspaceSync';
import { blockNegativeNumberInput, parseOptionalNonNegativeInt } from '../../utils/formValidation';
import {
  dismissEpSuggestionLocally,
  filterEpAutocompleteOptions,
  loadEpDismissedSuggestions,
  renderEpDismissibleOption,
  type EpDismissedStore,
} from '../../utils/epDismissedSuggestions';
import {
  SUPPOSITORY_INSTRUCTION,
  ensureSuppositoryInstructionsOnMedicationLines,
  mergeMedicationLinePatchWithSuppositoryInstruction,
} from '../../utils/epSuppositoryInstruction';
import './Hospital.css';

const DEFAULT_FORM: Omit<PrescriptionTemplate, 'templateId' | 'templateType' | 'createdAt'> = {
  templateName: '',
  diseaseCategory: '',
  complaints: [],
  diagnoses: [],
  medications: [],
  advice: [],
  tests: [],
  followUpDays: undefined,
  referral: '',
  clinicalFindings: '',
};

function dedupeTemplateTests(
  tests: { testName: string; isPanel?: boolean; panelName?: string }[],
): { testName: string; isPanel: boolean; panelName?: string }[] {
  const seen = new Set<string>();
  const out: { testName: string; isPanel: boolean; panelName?: string }[] = [];
  for (const t of tests) {
    const key = t.testName.trim().toLowerCase();
    if (!key || seen.has(key)) continue;
    seen.add(key);
    out.push({ testName: t.testName.trim(), isPanel: t.isPanel ?? false, panelName: t.panelName });
  }
  return out;
}

function createBlankMed(): PrescriptionMedicationItem {
  return {
    medicationName: '',
    dosageStrength: undefined,
    dosageUnit: 'mg',
    dosageForm: 'TABLET',
    route: 'ORAL',
    frequency: '',
    instructions: '',
    startDate: new Date().toISOString().split('T')[0],
  };
}

const EMPTY_LOOKUPS: EpLookupMap = {
  DOSAGE_FORM: [], DISEASE_CATEGORY: [], FREQUENCY: [],
  INSTRUCTION: [], REFERRAL: [], COMPLAINT: [],
  MEDICATION: [], ADVICE: [], TEST: [],
};

/** Ensures SUPPOSITORY and other standard forms appear even before lookup migration runs. */
const DOSAGE_FORM_FALLBACK = [
  'TABLET', 'CAPSULE', 'SYRUP', 'LIQUID', 'SOLUTION', 'SUSPENSION',
  'INJECTION', 'INFUSION', 'CREAM', 'OINTMENT', 'LOTION', 'GEL',
  'POWDER', 'GRANULES', 'INHALER', 'DROPS', 'SUPPOSITORY', 'SPRAY',
  'PATCH', 'MOUTHWASH', 'OTHER',
] as const;

function formatDrugSuggestionLabel(drug: { brandName?: string; genericName?: string }): string {
  const brand = drug.brandName?.trim();
  const generic = drug.genericName?.trim();
  if (brand && generic) return `${brand} (${generic})`;
  return brand || generic || '';
}

const TEMPLATE_SCOPE_ORDER: EpTemplateListScope[] = ['mine', 'department', 'all', 'system', 'disease', 'doctor'];

const PrescriptionTemplates: React.FC = () => {
  const navigate = useNavigate();
  const { currentOrganizationId, user } = useAuth();
  const orgId = currentOrganizationId ?? undefined;

  const [templates, setTemplates] = useState<PrescriptionTemplate[]>([]);
  const [favIds, setFavIds] = useState<string[]>([]);
  const [linkedDoctor, setLinkedDoctor] = useState<Doctor | null>(null);
  const [filter, setFilter] = useState<EpTemplateListScope>('mine');
  const [formTemplateKind, setFormTemplateKind] = useState<'DOCTOR' | 'DISEASE'>('DOCTOR');
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<PrescriptionTemplate | null>(null);
  const [form, setForm] = useState<typeof DEFAULT_FORM>({ ...DEFAULT_FORM });
  const [formMeds, setFormMeds] = useState<PrescriptionMedicationItem[]>([createBlankMed()]);
  const [icd10Query, setIcd10Query] = useState('');
  const [icd10Results, setIcd10Results] = useState<typeof COMMON_ICD10_CODES>([]);
  const [complaintInput, setComplaintInput] = useState('');
  const [adviceInput, setAdviceInput] = useState('');
  const [adviceRankedOptions, setAdviceRankedOptions] = useState<string[]>([]);
  const [testInput, setTestInput] = useState('');
  const [lookups, setLookups] = useState<EpLookupMap>(EMPTY_LOOKUPS);
  const [drugNameSuggestions, setDrugNameSuggestions] = useState<string[]>([]);
  const drugSuggestTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [clinicalChartTestOptions, setClinicalChartTestOptions] = useState<string[]>([]);
  const clinicalChartTestSuggestTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const clinicalChartInvSuggestSeq = useRef(0);
  const [dismissedSuggestions, setDismissedSuggestions] = useState<EpDismissedStore>(() =>
    loadEpDismissedSuggestions(undefined),
  );

  useEffect(() => {
    setDismissedSuggestions(loadEpDismissedSuggestions(user?.id, orgId));
  }, [user?.id, orgId]);

  const dismissAdviceSuggestion = useCallback(
    (line: string) => {
      if (!user?.id) return;
      const store = dismissEpSuggestionLocally(user.id, orgId, 'advice', line);
      setDismissedSuggestions(store);
      setAdviceRankedOptions(prev => prev.filter(o => o.trim().toLowerCase() !== line.trim().toLowerCase()));
      void epAdviceService
        .dismiss([line])
        .then(() => {
          void epAdviceService.getSuggestions({ query: adviceInput.trim(), limit: 120 }).then(setAdviceRankedOptions);
        })
        .catch(() => {});
    },
    [user?.id, orgId, adviceInput],
  );

  const dismissInvestigationSuggestion = useCallback(
    (line: string) => {
      if (!user?.id) return;
      setDismissedSuggestions(dismissEpSuggestionLocally(user.id, orgId, 'tests', line));
      const key = line.trim().toLowerCase();
      setClinicalChartTestOptions(prev => prev.filter(o => o.trim().toLowerCase() !== key));
    },
    [user?.id, orgId],
  );

  useEffect(() => {
    epLookupService.fetchAll().then(setLookups);
  }, []);

  const commitTemplateAdvice = useCallback((raw: string) => {
    const canon = raw.trim().replace(/\s+/g, ' ');
    if (!canon) return;
    setForm(p => {
      const cur = p.advice ?? [];
      if (cur.some(x => x.toLowerCase() === canon.toLowerCase())) return p;
      return { ...p, advice: [...cur, canon] };
    });
    void epAdviceService
      .ensure([canon])
      .then(() => {
        epLookupService.clearCache();
        return Promise.all([
          epAdviceService.getSuggestions({ query: '', limit: 120 }),
          epLookupService.fetchAll(),
        ]);
      })
      .then(([ranked, ep]) => {
        setAdviceRankedOptions(ranked);
        setLookups(ep);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!showForm || !user?.id) return;
    const timer = window.setTimeout(() => {
      void epAdviceService.getSuggestions({ query: adviceInput.trim(), limit: 120 }).then(setAdviceRankedOptions);
    }, adviceInput.trim() ? 260 : 0);
    return () => clearTimeout(timer);
  }, [showForm, user?.id, adviceInput]);

  useEffect(() => {
    if (!showForm) {
      setAdviceRankedOptions([]);
      setAdviceInput('');
      setTestInput('');
      setComplaintInput('');
      setIcd10Query('');
      setIcd10Results([]);
      setClinicalChartTestOptions([]);
    }
  }, [showForm]);

  useEffect(
    () => () => {
      if (drugSuggestTimer.current) clearTimeout(drugSuggestTimer.current);
      if (clinicalChartTestSuggestTimer.current) clearTimeout(clinicalChartTestSuggestTimer.current);
    },
    [],
  );

  const reload = () => {
    const all = epTemplateService.getAll();
    setTemplates(all);
    const cfg = epConfigService.get();
    setFavIds(cfg.favoriteTemplateIds ?? []);
  };

  useEffect(() => {
    if (!currentOrganizationId) {
      reload();
      return;
    }
    let cancelled = false;
    (async () => {
      await pullEpWorkspaceFromServer();
      if (cancelled) return;
      reload();
    })();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId]);

  useEffect(() => {
    if (!user?.id) {
      setLinkedDoctor(null);
      return;
    }
    let cancelled = false;
    hospitalService
      .getActiveDoctorsForPrescription()
      .then(res => {
        if (cancelled) return;
        const list = res.data ?? [];
        setLinkedDoctor(list.find(d => d.linkedUserId === user.id) ?? null);
      })
      .catch(() => {
        if (!cancelled) setLinkedDoctor(null);
      });
    return () => {
      cancelled = true;
    };
  }, [user?.id]);

  useEffect(() => {
    if (!icd10Query.trim()) { setIcd10Results([]); return; }
    const q = icd10Query.toLowerCase();
    setIcd10Results(
      COMMON_ICD10_CODES.filter(c => c.code.toLowerCase().includes(q) || c.description.toLowerCase().includes(q)).slice(0, 8)
    );
  }, [icd10Query]);

  const addCustomDiagnosis = (diseaseName: string) => {
    const name = diseaseName.trim();
    if (!name) return;
    const current = form.diagnoses ?? [];
    if (current.some(d => d.description.trim().toLowerCase() === name.toLowerCase())) {
      setIcd10Query('');
      return;
    }
    const existingCustom = current
      .map(d => d.code)
      .filter(code => /^CUSTOM-\d{1,3}$/i.test(code))
      .map(code => parseInt(code.split('-')[1] || '0', 10))
      .filter(n => Number.isFinite(n));
    const nextCustomNumber = (existingCustom.length ? Math.max(...existingCustom) : 0) + 1;
    const customCode = `CUSTOM-${nextCustomNumber}`.slice(0, 20);
    const primary = current.length === 0;
    setForm(p => ({
      ...p,
      diagnoses: [
        ...current.map(d => primary ? { ...d, isPrimary: false } : d),
        { code: customCode, description: name, isPrimary: primary },
      ],
    }));
    setIcd10Query('');
    setIcd10Results([]);
  };

  const toggleFav = (templateId: string) => {
    const cfg = epConfigService.get();
    const favs = cfg.favoriteTemplateIds ?? [];
    const next = favs.includes(templateId) ? favs.filter(id => id !== templateId) : [...favs, templateId];
    epConfigService.save({ ...cfg, favoriteTemplateIds: next });
    setFavIds(next);
  };

  const openCreate = () => {
    setEditing(null);
    setFormTemplateKind('DOCTOR');
    setForm({ ...DEFAULT_FORM });
    setFormMeds([createBlankMed()]);
    setAdviceInput('');
    setTestInput('');
    setComplaintInput('');
    setIcd10Query('');
    setIcd10Results([]);
    setClinicalChartTestOptions([]);
    setShowForm(true);
  };

  const openEdit = (t: PrescriptionTemplate) => {
    if (!epTemplateService.isDoctorOwned(t.templateId)) {
      alert('Built-in templates cannot be edited here. Use Apply Template on the prescribe screen, or create a new template.');
      return;
    }
    setFormTemplateKind(t.templateType === 'DISEASE' ? 'DISEASE' : 'DOCTOR');
    setEditing(t);
    setForm({
      templateName: t.templateName,
      diseaseCategory: t.diseaseCategory || '',
      complaints: [...(t.complaints ?? [])],
      diagnoses: [...(t.diagnoses ?? [])],
      medications: [...t.medications],
      advice: [...(t.advice ?? [])],
      tests: dedupeTemplateTests([...(t.tests ?? [])]),
      followUpDays: t.followUpDays,
      referral: t.referral || '',
      clinicalFindings: t.clinicalFindings || '',
    });
    setFormMeds(t.medications.length > 0 ? t.medications.map(m => ({ ...m })) : [createBlankMed()]);
    setAdviceInput('');
    setTestInput('');
    setComplaintInput('');
    setIcd10Query('');
    setIcd10Results([]);
    setClinicalChartTestOptions([]);
    setShowForm(true);
  };

  const handleDelete = (templateId: string) => {
    if (!epTemplateService.isDoctorOwned(templateId)) {
      alert('Built-in templates cannot be deleted.');
      return;
    }
    if (!window.confirm('Delete this template?')) return;
    epTemplateService.delete(templateId);
    reload();
  };

  const handleSave = () => {
    if (!form.templateName.trim()) { alert('Template name is required.'); return; }
    const validMeds = formMeds.filter(m => m.medicationName.trim());
    if (validMeds.length === 0) { alert('Add at least one medication.'); return; }

    const template: PrescriptionTemplate = {
      templateId: editing?.templateId ?? `doc-${Date.now()}`,
      templateType: formTemplateKind,
      createdAt: editing?.createdAt ?? new Date().toISOString(),
      createdBy: editing?.createdBy ?? user?.id,
      departmentId: editing?.departmentId ?? linkedDoctor?.departmentId,
      departmentName: editing?.departmentName ?? linkedDoctor?.departmentName,
      ...form,
      tests: dedupeTemplateTests(form.tests ?? []),
      medications: ensureSuppositoryInstructionsOnMedicationLines(validMeds),
    };
    epTemplateService.save(template);
    if ((template.advice ?? []).length > 0 && user?.id) {
      void epAdviceService.recordUsage(template.advice!).catch(() => {});
    }
    setShowForm(false);
    setFormTemplateKind('DOCTOR');
    reload();
  };

  const updateMed = (idx: number, patch: Partial<PrescriptionMedicationItem>) => {
    setFormMeds(prev =>
      prev.map((m, i) =>
        i !== idx ? m : { ...m, ...mergeMedicationLinePatchWithSuppositoryInstruction(m, patch) },
      ),
    );
  };

  const fetchDrugSuggestions = async (query: string) => {
    const q = query.trim();
    if (q.length < 2) return;
    try {
      const { data } = await hospitalService.searchDrugsForPrescription(q, 0, 25);
      const next = new Set<string>();
      (data.content ?? []).forEach((d) => {
        const label = formatDrugSuggestionLabel(d);
        if (label) next.add(label);
      });
      setDrugNameSuggestions((prev) => Array.from(new Set([...prev, ...Array.from(next)])).sort((a, b) => a.localeCompare(b)));
    } catch {
      // Best-effort suggestions only.
    }
  };

  const investigationAutocompleteOptions = useMemo(() => {
    const merged = Array.from(new Set([...clinicalChartTestOptions, ...lookups.TEST])).sort((a, b) =>
      a.localeCompare(b),
    );
    return filterEpAutocompleteOptions(merged, dismissedSuggestions, 'tests');
  }, [clinicalChartTestOptions, lookups.TEST, dismissedSuggestions]);

  const lastCommittedTestRef = useRef('');
  const commitTemplateTest = useCallback((raw: string) => {
    const name = raw.trim();
    if (!name) return;
    if (lastCommittedTestRef.current === name) return;
    lastCommittedTestRef.current = name;
    window.setTimeout(() => {
      lastCommittedTestRef.current = '';
    }, 0);
    setForm((p) => {
      const cur = p.tests ?? [];
      if (cur.some((t) => t.testName.trim().toLowerCase() === name.toLowerCase())) return p;
      return { ...p, tests: [...cur, { testName: name, isPanel: false }] };
    });
    setTestInput('');
    setClinicalChartTestOptions([]);
  }, []);

  const adviceAutocompleteMerged = useMemo(() => {
    const seen = new Set<string>();
    const out: string[] = [];
    const push = (x: string) => {
      const t = x?.trim();
      if (!t || seen.has(t)) return;
      seen.add(t);
      out.push(t);
    };
    adviceRankedOptions.forEach(push);
    lookups.ADVICE.forEach(push);
    return filterEpAutocompleteOptions(out, dismissedSuggestions, 'advice');
  }, [adviceRankedOptions, lookups.ADVICE, dismissedSuggestions]);

  const filtered = templates.filter(t => {
    const matchFilter = matchesEpTemplateScope(t, filter, {
      userId: user?.id,
      myDepartmentId: linkedDoctor?.departmentId,
      isDoctorOwned: id => epTemplateService.isDoctorOwned(id),
    });
    const matchSearch =
      !search ||
      t.templateName.toLowerCase().includes(search.toLowerCase()) ||
      (t.diseaseCategory || '').toLowerCase().includes(search.toLowerCase());
    return matchFilter && matchSearch;
  });

  const dosageFormOptions = useMemo(() => {
    const merged = new Set<string>([...DOSAGE_FORM_FALLBACK, ...(lookups.DOSAGE_FORM ?? [])]);
    return Array.from(merged).sort((a, b) => a.localeCompare(b));
  }, [lookups.DOSAGE_FORM]);

  const instructionOptions = useMemo(() => {
    const merged = new Set<string>([...(lookups.INSTRUCTION ?? []), SUPPOSITORY_INSTRUCTION]);
    return Array.from(merged).sort((a, b) => a.localeCompare(b));
  }, [lookups.INSTRUCTION]);

  const medicationSuggestions = useMemo(
    () =>
      Array.from(
        new Set([
          ...drugNameSuggestions,
          ...lookups.MEDICATION,
          ...templates
            .flatMap(t => t.medications ?? [])
            .map(m => (m.medicationName || '').trim())
            .filter(Boolean),
        ])
      ).sort((a, b) => a.localeCompare(b)),
    [lookups.MEDICATION, drugNameSuggestions, templates]
  );

  const medNameMatchesSuggestions = (name: string) => {
    const t = name.trim();
    if (!t) return true;
    return medicationSuggestions.some((s) => s.toLowerCase() === t.toLowerCase());
  };

  const renderForm = () => (
    <div className="form-container">
      <h3 style={{ marginTop: 0 }}>{editing ? 'Edit Template' : 'Create Template'}</h3>

      <div className="form-grid">
        <div className="form-group" style={{ gridColumn: '1 / -1' }}>
          <label>Template Name *</label>
          <input type="text" value={form.templateName} onChange={e => setForm(p => ({ ...p, templateName: e.target.value }))} placeholder="e.g. My URTI Protocol" autoFocus />
        </div>
        <div className="form-group">
          <label>Kind</label>
          <select value={formTemplateKind} onChange={e => setFormTemplateKind(e.target.value as 'DOCTOR' | 'DISEASE')} style={{ width: '100%', padding: '8px' }}>
            <option value="DOCTOR">Doctor protocol (personal)</option>
            <option value="DISEASE">Disease / condition protocol</option>
          </select>
        </div>
        <div className="form-group">
          <label>Disease Category</label>
          <select value={form.diseaseCategory} onChange={e => setForm(p => ({ ...p, diseaseCategory: e.target.value }))}>
            <option value="">— Select —</option>
            {lookups.DISEASE_CATEGORY.map(c => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div className="form-group">
          <label>Follow-up (days)</label>
          <input
            type="number"
            min="0"
            value={form.followUpDays ?? ''}
            onKeyDown={blockNegativeNumberInput}
            onChange={e => setForm(p => ({
              ...p,
              followUpDays: e.target.value ? parseOptionalNonNegativeInt(e.target.value) : undefined,
            }))}
          />
        </div>
        <div className="form-group" style={{ gridColumn: '1 / -1' }}>
          <label>Referral</label>
          <select value={form.referral} onChange={e => setForm(p => ({ ...p, referral: e.target.value }))}>
            <option value="">— None —</option>
            {lookups.REFERRAL.map(r => <option key={r} value={r}>{r}</option>)}
          </select>
        </div>
      </div>

      {/* Complaints */}
      <div className="form-section">
        <h4 className="form-section-title">Chief Complaints</h4>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px', marginBottom: '8px' }}>
          {(form.complaints ?? []).map((c, i) => (
            <span key={i} style={{ background: 'var(--color-primary, #1d4ed8)', color: '#fff', borderRadius: '10px', fontSize: '12px', padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              {c}
              <button type="button" onClick={() => setForm(p => ({ ...p, complaints: p.complaints?.filter((_, j) => j !== i) }))}
                style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '12px', padding: 0, lineHeight: 1 }}>×</button>
            </span>
          ))}
        </div>
        <div style={{ display: 'flex', gap: '6px' }}>
          <input
            type="text"
            list="template-complaint-suggestions"
            value={complaintInput}
            onChange={e => setComplaintInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); if (complaintInput.trim()) { setForm(p => ({ ...p, complaints: [...(p.complaints ?? []), complaintInput.trim()] })); setComplaintInput(''); } } }}
            placeholder="Select or type complaint…"
            style={{ flex: 1, padding: '5px 8px', fontSize: '13px' }}
          />
          <datalist id="template-complaint-suggestions">
            {lookups.COMPLAINT.map(c => <option key={c} value={c} />)}
          </datalist>
          <button type="button" className="btn-secondary"
            onClick={() => { if (complaintInput.trim()) { setForm(p => ({ ...p, complaints: [...(p.complaints ?? []), complaintInput.trim()] })); setComplaintInput(''); } }}>Add</button>
        </div>
      </div>

      {/* Diagnoses */}
      <div className="form-section">
        <h4 className="form-section-title">Diagnoses (ICD-10 / ICD-11 / Custom)</h4>
        {(form.diagnoses ?? []).map((dx, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', fontSize: '13px' }}>
            <span style={{ fontSize: '11px', fontWeight: 700, padding: '2px 6px', borderRadius: '8px', background: dx.isPrimary ? '#d1fae5' : '#f3f4f6', color: dx.isPrimary ? '#065f46' : '#374151' }}>
              {dx.isPrimary ? '1°' : '2°'}
            </span>
            <span style={{ flex: 1 }}><strong>{dx.code}</strong> — {dx.description}</span>
            <button type="button" onClick={() => setForm(p => ({ ...p, diagnoses: (p.diagnoses ?? []).filter((_, j) => j !== i) }))}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: '14px' }}>×</button>
          </div>
        ))}
        <div style={{ position: 'relative' }}>
          <input
            type="text"
            value={icd10Query}
            onChange={e => setIcd10Query(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') {
                e.preventDefault();
                addCustomDiagnosis(icd10Query);
              }
            }}
            placeholder="Search ICD-10 code/name or type custom disease and press Enter…"
            style={{ width: '100%', padding: '5px 8px', fontSize: '13px', boxSizing: 'border-box' }}
          />
          {icd10Results.length > 0 && (
            <div className="autocomplete-dropdown">
              {icd10Results.map(c => (
                <div key={c.code} className="autocomplete-item"
                  onMouseDown={() => {
                    const primary = (form.diagnoses ?? []).length === 0;
                    setForm(p => ({ ...p, diagnoses: [...(p.diagnoses ?? []).map(d => primary ? { ...d, isPrimary: false } : d), { code: c.code, description: c.description, isPrimary: primary }] }));
                    setIcd10Query('');
                    setIcd10Results([]);
                  }}>
                  <strong>{c.code}</strong> — {c.description}
                </div>
              ))}
            </div>
          )}
          {icd10Query.trim().length > 0 && (
            <div style={{ marginTop: '6px' }}>
              <button
                type="button"
                className="btn-secondary"
                style={{ fontSize: '11px', padding: '4px 10px' }}
                onClick={() => addCustomDiagnosis(icd10Query)}
              >
                Add as custom disease
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Medications */}
      <div className="form-section">
        <h4 className="form-section-title">Medications</h4>
        {formMeds.map((med, idx) => (
          <div key={idx} style={{ marginBottom: '10px', padding: '12px', border: '1px solid var(--border-color, #e5e7eb)', borderRadius: '8px', background: 'var(--bg-elevated, #fafafa)' }}>
            <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: '8px' }}>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Medicine Name *</label>
                <input
                  type="text"
                  list={`template-medication-${idx}`}
                  value={med.medicationName}
                  onChange={e => {
                    const v = e.target.value;
                    updateMed(idx, { medicationName: v });
                    if (drugSuggestTimer.current) clearTimeout(drugSuggestTimer.current);
                    drugSuggestTimer.current = setTimeout(() => void fetchDrugSuggestions(v), 280);
                  }}
                  placeholder="e.g. Paracetamol"
                />
                <small style={{ color: 'var(--text-secondary)', fontSize: '11px', display: 'block', marginTop: '4px' }}>
                  Pick from suggestions or type a name, then confirm below if it does not appear in the list.
                </small>
                {!medNameMatchesSuggestions(med.medicationName) && med.medicationName.trim().length >= 1 && (
                  <div style={{ marginTop: '8px' }}>
                    <button
                      type="button"
                      className="btn-secondary"
                      style={{ fontSize: '11px', padding: '4px 10px' }}
                      onClick={() => updateMed(idx, { medicationName: med.medicationName.trim() })}
                    >
                      Use “{med.medicationName.trim()}” as medicine (manual entry)
                    </button>
                  </div>
                )}
                <datalist id={`template-medication-${idx}`}>
                  {medicationSuggestions.map(name => <option key={name} value={name} />)}
                </datalist>
              </div>
              <div className="form-group">
                <label>Form</label>
                <select value={med.dosageForm} onChange={e => updateMed(idx, { dosageForm: e.target.value as any })}>
                  {dosageFormOptions.map(f => <option key={f} value={f}>{f}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label>Strength</label>
                <input type="number" value={med.dosageStrength ?? ''} onChange={e => updateMed(idx, { dosageStrength: e.target.value ? parseFloat(e.target.value) : undefined })} placeholder="500" />
              </div>
              <div className="form-group">
                <label>Unit</label>
                <input type="text" value={med.dosageUnit || ''} onChange={e => updateMed(idx, { dosageUnit: e.target.value })} placeholder="mg" />
              </div>
              <div className="form-group">
                <label>Frequency</label>
                <input
                  type="text"
                  list={`template-frequency-${idx}`}
                  value={med.frequency || ''}
                  onChange={e => updateMed(idx, { frequency: e.target.value })}
                  placeholder="1+0+1"
                />
                <datalist id={`template-frequency-${idx}`}>
                  {lookups.FREQUENCY.map(freq => <option key={freq} value={freq} />)}
                </datalist>
              </div>
              <div className="form-group">
                <label>Duration (days)</label>
                <input type="number" value={med.durationDays ?? ''} onChange={e => updateMed(idx, { durationDays: e.target.value ? parseInt(e.target.value) : undefined })} />
              </div>
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label>Instructions</label>
                <select
                  value={med.instructions}
                  onChange={e => updateMed(idx, { instructions: e.target.value })}
                >
                  <option value="">— Select —</option>
                  {instructionOptions.map(option => <option key={option} value={option}>{option}</option>)}
                </select>
              </div>
            </div>
            {formMeds.length > 1 && (
              <button type="button" onClick={() => setFormMeds(p => p.filter((_, i) => i !== idx))}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#ef4444', fontSize: '12px', marginTop: '6px' }}>
                Remove medication
              </button>
            )}
          </div>
        ))}
        <button type="button" className="btn-secondary" style={{ fontSize: '13px' }} onClick={() => setFormMeds(p => [...p, createBlankMed()])}>
          + Add Medication
        </button>
      </div>

      {/* Advice */}
      <div className="form-section">
        <h4 className="form-section-title">Advice</h4>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px', marginBottom: '8px' }}>
          {(form.advice ?? []).map((a, chipIdx) => (
            <span key={`${chipIdx}-${a}`} style={{ background: 'var(--color-primary, #1d4ed8)', color: '#fff', borderRadius: '10px', fontSize: '12px', padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              {a}
              <button type="button" onClick={() => setForm(p => ({ ...p, advice: (p.advice ?? []).filter((_, j) => j !== chipIdx) }))}
                style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '12px', padding: 0, lineHeight: 1 }}>×</button>
            </span>
          ))}
        </div>
        <div style={{ display: 'flex', gap: '6px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <Autocomplete
            freeSolo
            options={adviceAutocompleteMerged}
            value={null}
            inputValue={adviceInput}
            onOpen={() => {
              void epAdviceService.getSuggestions({ query: adviceInput.trim(), limit: 120 }).then(setAdviceRankedOptions);
            }}
            onInputChange={(_, v, reason) => {
              if (reason !== 'reset') setAdviceInput(v);
            }}
            onChange={(_, v) => {
              if (typeof v === 'string' && v.trim()) {
                commitTemplateAdvice(v);
                setAdviceInput('');
              }
            }}
            renderOption={(props, option) =>
              renderEpDismissibleOption(props, option, dismissAdviceSuggestion, Boolean(user?.id))
            }
            sx={{ flex: 1, minWidth: '200px' }}
            size="small"
            renderInput={params => (
              <TextField
                {...params}
                placeholder="Your frequent advice first — search or add new"
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    if (adviceInput.trim()) {
                      commitTemplateAdvice(adviceInput);
                      setAdviceInput('');
                    }
                  }
                }}
              />
            )}
          />
          <button type="button" className="btn-secondary" style={{ marginTop: 4 }}
            onClick={() => {
              if (adviceInput.trim()) {
                commitTemplateAdvice(adviceInput);
                setAdviceInput('');
              }
            }}>Add</button>
        </div>
        <p style={{ fontSize: '10px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
          Use × on a dropdown suggestion to remove it from future autocomplete lists.
        </p>
      </div>

      <div className="form-section">
        <h4 className="form-section-title">Clinical Findings</h4>
        <textarea
          value={form.clinicalFindings || ''}
          onChange={(e) => setForm((p) => ({ ...p, clinicalFindings: e.target.value }))}
          placeholder="Examination findings, observations…"
          rows={3}
          style={{ width: '100%', padding: '8px', fontSize: '13px', boxSizing: 'border-box', resize: 'vertical' }}
        />
      </div>

      {/* Tests */}
      <div className="form-section">
        <h4 className="form-section-title">Tests / Investigations</h4>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px', marginBottom: '8px' }}>
          {(form.tests ?? []).map((t, i) => (
            <span key={i} style={{ background: '#0d9488', color: '#fff', borderRadius: '10px', fontSize: '12px', padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}>
              {t.testName}
              <button type="button" onClick={() => setForm(p => ({ ...p, tests: p.tests?.filter((_, j) => j !== i) }))}
                style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '12px', padding: 0, lineHeight: 1 }}>×</button>
            </span>
          ))}
        </div>
        <div style={{ display: 'flex', gap: '6px', alignItems: 'flex-start' }}>
          <Autocomplete
            freeSolo
            blurOnSelect
            options={investigationAutocompleteOptions}
            value={null}
            inputValue={testInput}
            onOpen={() => {
              const seq = ++clinicalChartInvSuggestSeq.current;
              void (async () => {
                try {
                  const { data } = await hospitalService.autocompleteClinicalChartInvestigations('', 40);
                  if (seq !== clinicalChartInvSuggestSeq.current) return;
                  setClinicalChartTestOptions(Array.isArray(data) ? data : []);
                } catch {
                  if (seq !== clinicalChartInvSuggestSeq.current) return;
                  setClinicalChartTestOptions([]);
                }
              })();
            }}
            onInputChange={(_, value, reason) => {
              if (reason !== 'reset') setTestInput(value);
              if (clinicalChartTestSuggestTimer.current) clearTimeout(clinicalChartTestSuggestTimer.current);
              if (reason !== 'input') return;
              clinicalChartTestSuggestTimer.current = setTimeout(() => {
                const seq = ++clinicalChartInvSuggestSeq.current;
                void (async () => {
                  try {
                    const { data } = await hospitalService.autocompleteClinicalChartInvestigations(value.trim(), 40);
                    if (seq !== clinicalChartInvSuggestSeq.current) return;
                    setClinicalChartTestOptions(Array.isArray(data) ? data : []);
                  } catch {
                    if (seq !== clinicalChartInvSuggestSeq.current) return;
                    setClinicalChartTestOptions([]);
                  }
                })();
              }, 220);
            }}
            onChange={(_, val) => {
              if (val != null) commitTemplateTest(String(val));
            }}
            renderOption={(props, option) =>
              renderEpDismissibleOption(props, option, dismissInvestigationSuggestion, Boolean(user?.id))
            }
            sx={{ flex: 1 }}
            size="small"
            renderInput={(params) => (
              <TextField
                {...params}
                placeholder="Clinical chart investigations/tests or EP lookup tests…"
              />
            )}
          />
          <button
            type="button"
            className="btn-secondary"
            style={{ marginTop: 4 }}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => commitTemplateTest(testInput)}
          >
            Add
          </button>
        </div>
        <p style={{ fontSize: '10px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
          Use × on a dropdown suggestion to remove it from future autocomplete lists.
        </p>
      </div>

      <div className="form-actions">
        <button
          className="btn-secondary"
          onClick={() => {
            setShowForm(false);
            setFormTemplateKind('DOCTOR');
          }}
        >
          Cancel
        </button>
        <button className="btn-primary" onClick={handleSave}>Save Template</button>
      </div>
    </div>
  );

  return (
    <div className="hospital-page">
      <div className="page-header" style={{ marginBottom: '20px' }}>
        <div>
          <h1>Prescription Templates</h1>
          <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
            Default view is <strong>your</strong> saved templates; use <strong>My department</strong> to see custom templates from your department. Starred templates appear on the Doctor Dashboard.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button className="btn-secondary" onClick={() => navigate('/hospital/doctor-dashboard')}>Dashboard</button>
          <button className="btn-primary" onClick={openCreate}>+ New Template</button>
        </div>
      </div>

      {showForm && renderForm()}

      {/* Filter bar */}
      <div className="filters-section" style={{ marginBottom: '16px', display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
        <input type="text" value={search} onChange={e => setSearch(e.target.value)} placeholder="Search templates…" style={{ padding: '6px 10px', fontSize: '13px', flex: '1 1 200px' }} />
        <div style={{ display: 'flex', gap: '0', flexWrap: 'wrap' }}>
          {TEMPLATE_SCOPE_ORDER.map((f, i, arr) => (
            <button
              key={f}
              type="button"
              onClick={() => setFilter(f)}
              style={{
                padding: '5px 10px',
                fontSize: '12px',
                border: '1px solid var(--border-color, #d1d5db)',
                background: filter === f ? 'var(--color-primary, #1d4ed8)' : 'transparent',
                color: filter === f ? '#fff' : 'var(--text-secondary)',
                cursor: 'pointer',
                borderRadius: i === 0 ? '6px 0 0 6px' : i === arr.length - 1 ? '0 6px 6px 0' : '0',
                borderRight: i < arr.length - 1 ? 'none' : undefined,
              }}
            >
              {f === 'mine'
                ? 'My templates'
                : f === 'department'
                  ? 'My department'
                  : f === 'all'
                    ? 'All'
                    : f === 'system'
                      ? 'System'
                      : f === 'disease'
                        ? 'Disease'
                        : 'All custom'}
            </button>
          ))}
        </div>
      </div>
      {filter === 'department' && !linkedDoctor?.departmentId && (
        <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: '0 0 12px' }}>
          Link your login to a doctor record (with a department) to use this filter. New templates you save are tagged with your current department.
        </p>
      )}

      {filtered.length === 0 ? (
        <div className="empty-state">
          <p>No templates found</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '14px' }}>
          {filtered.map(t => (
            <div key={t.templateId} style={{ border: '1px solid var(--border-color, #e5e7eb)', borderRadius: '10px', padding: '16px', background: 'var(--bg-elevated, #fff)', display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 600, fontSize: '14px' }}>{t.templateName}</span>
                    {t.templateType === 'SYSTEM' && (
                      <span style={{ fontSize: '10px', fontWeight: 700, padding: '1px 6px', borderRadius: '8px', background: '#d1fae5', color: '#065f46' }}>System</span>
                    )}
                    {t.templateType === 'DISEASE' && (
                      <span style={{ fontSize: '10px', fontWeight: 700, padding: '1px 6px', borderRadius: '8px', background: '#ede9fe', color: '#5b21b6' }}>Disease</span>
                    )}
                    {t.templateType === 'DOCTOR' && epTemplateService.isDoctorOwned(t.templateId) && (
                      <span style={{ fontSize: '10px', fontWeight: 700, padding: '1px 6px', borderRadius: '8px', background: '#dbeafe', color: '#1d4ed8' }}>Mine</span>
                    )}
                  </div>
                  {t.diseaseCategory && <div style={{ fontSize: '12px', color: '#0d9488' }}>{t.diseaseCategory}</div>}
                  {t.departmentName && epTemplateService.isDoctorOwned(t.templateId) && (
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>{t.departmentName}</div>
                  )}
                </div>
                <button type="button" onClick={() => toggleFav(t.templateId)}
                  title={favIds.includes(t.templateId) ? 'Remove from favourites' : 'Star as favourite'}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px', color: favIds.includes(t.templateId) ? '#f59e0b' : '#d1d5db', flexShrink: 0 }}>★</button>
              </div>

              {/* Summary chips */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px' }}>
                {t.medications.slice(0, 3).map((m, i) => (
                  <span key={i} style={{ fontSize: '11px', padding: '2px 7px', background: 'var(--color-primary-light, #dbeafe)', color: 'var(--color-primary, #1d4ed8)', borderRadius: '10px' }}>
                    {m.medicationName} {m.dosageStrength ? `${m.dosageStrength}${m.dosageUnit}` : ''}
                  </span>
                ))}
                {t.medications.length > 3 && (
                  <span style={{ fontSize: '11px', color: 'var(--text-secondary)', padding: '2px 5px' }}>+{t.medications.length - 3} more</span>
                )}
              </div>

              {/* Meta */}
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)', display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                {t.diagnoses && t.diagnoses.length > 0 && (
                  <span>🩺 {t.diagnoses.map(d => d.code).join(', ')}</span>
                )}
                {t.tests && t.tests.length > 0 && <span>🧪 {t.tests.length} test(s)</span>}
                {t.followUpDays && <span>📅 Follow-up: {t.followUpDays}d</span>}
                {t.advice && t.advice.length > 0 && <span>💬 {t.advice.length} advice</span>}
              </div>

              {/* Actions */}
              <div style={{ display: 'flex', gap: '8px', marginTop: 'auto', flexWrap: 'wrap' }}>
                {epTemplateService.isDoctorOwned(t.templateId) ? (
                  <>
                    <button className="btn-secondary" style={{ fontSize: '12px', padding: '5px 10px' }} onClick={() => openEdit(t)}>Edit</button>
                    <button className="btn-secondary" style={{ fontSize: '12px', padding: '5px 10px', color: '#ef4444' }} onClick={() => handleDelete(t.templateId)}>Delete</button>
                  </>
                ) : (
                  <span style={{ fontSize: '11px', color: 'var(--text-secondary)', fontStyle: 'italic' }}>Built-in — use from prescribe screen (Apply Template)</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default PrescriptionTemplates;
