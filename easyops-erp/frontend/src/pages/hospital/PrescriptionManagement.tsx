import React, { useEffect, useState, useCallback, useRef, useMemo } from 'react';
import html2pdf from 'html2pdf.js';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import { useAuth } from '@contexts/AuthContext';
import hospitalService, {
  Prescription,
  PrescriptionRequest,
  PrescriptionMedicationItem,
  PrescriptionTransmitRequest,
  PrescriptionInteraction,
  PrescriptionAllergyCheck,
  DrugInteractionCheckResponse,
  AllergyCheckResponse,
  Patient,
  PharmacyDrugSuggestion,
  PharmacyDirectoryEntry,
  epTemplateService,
  epConfigService,
  epAdviceService,
  epLookupService,
  PrescriptionTemplate,
  Doctor,
  EpTemplateListScope,
  matchesEpTemplateScope,
  COMMON_ICD10_CODES,
  ICD10Code,
  DoctorEPConfig,
  epRecentRxService,
  EP_DOCTOR_CONFIG_STORAGE_KEY,
  EP_DOCTOR_CONFIG_UPDATED_EVENT,
  PDMPQueryRequest,
  PDMPQueryResponse,
  FormularyCheckRequest,
  FormularyCheckResponse,
  PriorAuthorizationRequest,
  PriorAuthorizationResponse,
  PrescriptionTransmissionRecord,
  DrugInteractionCheckRequest,
  PregnancyStatus,
  MedicalHistory,
  VitalSigns,
} from '../../services/hospitalService';
import { pullEpWorkspaceFromServer, scheduleEpWorkspacePush } from '../../services/epWorkspaceSync';
import {
  dismissEpSuggestionLocally,
  filterEpAutocompleteOptions,
  loadEpDismissedSuggestions,
  renderEpDismissibleOption,
  type EpDismissedStore,
} from '../../utils/epDismissedSuggestions';
import hospitalSchedulingService from '../../services/hospitalSchedulingService';
import hospitalPharmacyService, {
  type Drug,
  type DrugRequest,
  type Manufacturer,
} from '../../services/hospitalPharmacyService';
import { formatAge } from '../../utils/ageUtils';
import { formatGenderLabel, formatClinicalEnumLabel } from '../../utils/patientDisplay';
import { ehrApiErrorMessage } from '../../utils/ehrApiError';
import { setEpPrescriptionReturnPath } from '../../utils/epPrescriptionReturn';
import { blockNegativeNumberInput } from '../../utils/formValidation';
import {
  SUPPOSITORY_INSTRUCTION,
  ensureSuppositoryInstructionsOnMedicationLines,
  mergeMedicationLinePatchWithSuppositoryInstruction,
} from '../../utils/epSuppositoryInstruction';
import {
  portalLayoutOverlay,
  LAYOUT_OVERLAY_ROOT_Z,
  LAYOUT_OVERLAY_DETECT_CLASS,
} from '@/utils/layoutOverlayPortal';
import './Hospital.css';

// ─── Constants ───────────────────────────────────────────────────────────────

const DURATION_PRESETS = [
  { label: '3 days', days: 3 },
  { label: '5 days', days: 5 },
  { label: '7 days', days: 7 },
  { label: '10 days', days: 10 },
  { label: '14 days', days: 14 },
  { label: '1 month', days: 30 },
  { label: '2 months', days: 60 },
  { label: '3 months', days: 90 },
  { label: '6 months', days: 180 },
];

const FREQUENCY_SUGGESTIONS = [
  '1+0+0', '0+1+0', '0+0+1',
  '1+0+1', '1+1+0', '0+1+1', '1+1+1',
  '1-0-1', '0-1-0', '0-0-1', '1-1-0', '0-1-1', '1-1-1', '1-0-0',
  '½+0+0', '0+0+½', '½+0+½', '½-0-½',
  '1+1+1+1',
  'Once daily', 'Twice daily', 'Three times daily', 'Four times daily',
  'Every 8 hours', 'Every 12 hours',
  'Once a week', 'Twice a week', 'Once a month',
  'As needed (PRN)', 'Stat (once only)',
];

const INSTRUCTION_SUGGESTIONS = [
  'Before meal',
  'After meal',
  'With food',
  'Empty stomach',
  'At bedtime',
  'In the morning',
  'In the evening',
  'As needed (PRN)',
  'Do not crush or chew',
  'Swallow whole with water',
  'Shake well before use',
  'Apply thin layer to affected area',
  'Complete full course',
  'Avoid alcohol while taking this medicine',
  SUPPOSITORY_INSTRUCTION,
];

/** Form presets aligned with Pharmacy Catalog (`PharmacyCatalog.tsx`) for new master drugs. */
const PHARMACY_DRUG_FORM_PRESET_OPTIONS = [
  'Tablet',
  'Chewable Tablet',
  'Dispersible Tablet',
  'Effervescent Tablet',
  'Capsule',
  'Syrup',
  'Oral Suspension',
  'Powder for Suspension',
  'Oral Solution',
  'Solution',
  'Suspension',
  'Injection',
  'IV Injection',
  'IM Injection',
  'SC Injection',
  'IV Infusion',
  'Infusion',
  'Cream',
  'Ointment',
  'Lotion',
  'Gel',
  'Powder',
  'Granules',
  'Inhaler',
  'Drops',
  'Pediatric Drops',
  'Nasal Drop',
  'Ear Drop',
  'Ophthalmic Solution',
  'Nasal Spray',
  'Spray',
  'Suppository',
  'Patch',
  'Mouthwash',
] as const;

const INITIAL_ADD_CATALOG_DRUG_FORM: DrugRequest = {
  genericName: '',
  brandName: '',
  strength: '',
  form: '',
  route: '',
  packSize: '',
  unitOfMeasure: '',
  active: true,
  controlledDrugFlag: false,
  batchRequired: true,
  expiryRequired: true,
  manufacturerId: '',
};

function drugEntityToPharmacySuggestion(d: Drug): PharmacyDrugSuggestion {
  return {
    id: d.id,
    genericName: d.genericName,
    brandName: d.brandName,
    strength: d.strength,
    form: d.form,
    route: d.route,
  };
}

const COMMON_COMPLAINTS = [
  'Fever', 'Cough', 'Running nose', 'Sore throat', 'Headache',
  'Body ache', 'Fatigue', 'Shortness of breath', 'Chest pain',
  'Abdominal pain', 'Nausea', 'Vomiting', 'Loose stool / Diarrhoea',
  'Constipation', 'Burning urination', 'Frequent urination',
  'Back pain', 'Joint pain', 'Dizziness', 'Swelling (oedema)',
  'Skin rash', 'Itching', 'Loss of appetite', 'Weight loss',
  'Increased thirst', 'Blurred vision', 'Ear pain', 'Nasal block',
];

const DRUG_FORM_MAP: Record<string, PrescriptionMedicationItem['dosageForm']> = {
  tablet: 'TABLET', chewable_tablet: 'TABLET', dispersible_tablet: 'TABLET',
  capsule: 'CAPSULE', sprinkle_capsule: 'CAPSULE',
  syrup: 'SYRUP', liquid: 'LIQUID',
  solution: 'SOLUTION', oral_solution: 'SOLUTION', topical_solution: 'SOLUTION',
  nebuliser_solution: 'SOLUTION',
  suspension: 'SUSPENSION', oral_suspension: 'SUSPENSION',
  injection: 'INJECTION', iv_injection: 'INJECTION', im_injection: 'INJECTION',
  infusion: 'INFUSION', iv_infusion: 'INFUSION',
  cream: 'CREAM', ointment: 'OINTMENT', gel: 'GEL', lotion: 'LOTION',
  powder: 'POWDER', granules: 'GRANULES',
  inhaler: 'INHALER', drops: 'DROPS', spray: 'SPRAY',
  patch: 'PATCH', suppository: 'SUPPOSITORY', mouthwash: 'MOUTHWASH',
};

const mapDrugForm = (form: string): PrescriptionMedicationItem['dosageForm'] =>
  DRUG_FORM_MAP[form.toLowerCase().replace(/\s+/g, '_')] ?? 'OTHER';

const DOSAGE_FORM_OPTIONS = [
  ['TABLET', 'Tablet'], ['CAPSULE', 'Capsule'], ['SYRUP', 'Syrup'],
  ['LIQUID', 'Liquid'], ['SOLUTION', 'Solution'], ['SUSPENSION', 'Suspension'],
  ['INJECTION', 'Injection'], ['INFUSION', 'Infusion'],
  ['CREAM', 'Cream'], ['OINTMENT', 'Ointment'], ['LOTION', 'Lotion'], ['GEL', 'Gel'],
  ['POWDER', 'Powder'], ['GRANULES', 'Granules'], ['INHALER', 'Inhaler'],
  ['DROPS', 'Drops'], ['SUPPOSITORY', 'Suppository'], ['SPRAY', 'Spray'],
  ['PATCH', 'Patch'], ['MOUTHWASH', 'Mouthwash'], ['OTHER', 'Other'],
] as const;

/** Max medicine lines on the first print/PDF page (product rule). */
const RX_MEDS_PER_PAGE = 8;

type RxPrintSlice =
  | { kind: 'meds'; meds: PrescriptionMedicationItem[]; medPageIndex: number; medPageCount: number }
  | { kind: 'advice' };

function buildRxPrintSlices(
  lines: PrescriptionMedicationItem[],
  adviceItems: string[],
): RxPrintSlice[] {
  const slices: RxPrintSlice[] = [];
  const medPages: PrescriptionMedicationItem[][] = [];
  for (let i = 0; i < lines.length; i += RX_MEDS_PER_PAGE) {
    medPages.push(lines.slice(i, i + RX_MEDS_PER_PAGE));
  }
  if (medPages.length === 0) {
    if (adviceItems.length > 0) {
      slices.push({ kind: 'advice' });
    } else {
      slices.push({ kind: 'meds', meds: [], medPageIndex: 0, medPageCount: 1 });
    }
    return slices;
  }
  medPages.forEach((meds, medPageIndex) => {
    slices.push({ kind: 'meds', meds, medPageIndex, medPageCount: medPages.length });
  });
  if (adviceItems.length > 0) {
    const lastMeds = medPages[medPages.length - 1];
    if (lastMeds.length >= RX_MEDS_PER_PAGE) {
      slices.push({ kind: 'advice' });
    }
  }
  return slices;
}

/** A4 preview/print/PDF page insets requested by product: top ~2.65in, bottom 1.5in. */
const RX_A4_PAGE_TOP = '2.65in';
const RX_A4_PAGE_BOTTOM = '1.5in';
/** Left/right content inset inside the printable area. */
const RX_A4_PAGE_MARGIN_LR = '5mm';
/** Main body: clinical left column / Rx+advice right column. */
const RX_MAIN_COLS = 'minmax(0, 60%) minmax(0, 40%)';
/** Reserved space above page footer for signature block (fixed bottom-right). */
function rxSignatureZonePadding(isCompact: boolean): string {
  return isCompact ? '4.5rem' : '5.5rem';
}

/** Strip forced page breaks when the Rx fits on one printed page. */
function prepareRxPrintInnerHtml(html: string, singlePage: boolean): string {
  if (!singlePage) return html;
  return html.replace(/page-break-after:\s*always/gi, 'page-break-after: auto');
}

/** html2pdf uses screen CSS — align signature reserve and page breaks with print output. */
function applyRxPdfCaptureAdjustments(
  root: HTMLElement,
  opts: { singlePage: boolean; isCompact: boolean },
): () => void {
  const rxPages = Array.from(root.querySelectorAll<HTMLElement>('.rx-page'));
  const signatureMains = Array.from(root.querySelectorAll<HTMLElement>('.rx-page--signature .rx-main'));
  const savedPageBreaks = rxPages.map(p => p.style.pageBreakAfter);
  const savedMainPads = signatureMains.map(m => m.style.paddingBottom);
  const savedPagePadding = rxPages.map(p => p.style.padding);
  const savedPageMinHeight = rxPages.map(p => p.style.minHeight);
  if (opts.singlePage) {
    rxPages.forEach(p => {
      p.style.pageBreakAfter = 'auto';
      p.style.minHeight = 'auto';
      if (opts.isCompact) {
        p.style.padding = '24.2mm 12px 14mm';
      } else {
        p.style.padding = '18mm 5mm 12mm 5mm';
      }
    });
  }
  const pad = rxSignatureZonePadding(opts.isCompact);
  signatureMains.forEach(m => {
    m.style.paddingBottom = opts.singlePage ? '3.5rem' : pad;
  });
  return () => {
    rxPages.forEach((p, i) => {
      p.style.pageBreakAfter = savedPageBreaks[i];
      p.style.padding = savedPagePadding[i];
      p.style.minHeight = savedPageMinHeight[i];
    });
    signatureMains.forEach((m, i) => {
      m.style.paddingBottom = savedMainPads[i];
    });
  };
}

/** Safe segment for PDF/HTML filenames (MRN may contain slashes/spaces from imports). */
function sanitizePrescriptionFilenameMrn(mrn: string | undefined | null): string {
  const t = (mrn ?? '').trim();
  if (!t) return 'preview';
  const cleaned = t.replace(/[^\w.-]+/g, '_').replace(/^_+|_+$/g, '');
  return cleaned || 'preview';
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

function newMedicationClientRowId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `row-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

function ensureMedicationLineKeys(lines: PrescriptionMedicationItem[]): PrescriptionMedicationItem[] {
  return lines.map(m => (m.clientRowId ? m : { ...m, clientRowId: newMedicationClientRowId() }));
}

function getDurationSelectValue(days?: number): string {
  if (days === undefined || days === null) return '';
  const preset = DURATION_PRESETS.find(p => p.days === days);
  return preset ? String(days) : 'custom';
}

/** Normalise Unicode fractions and vulgar fractions to ASCII decimals */
function normaliseFraction(s: string): string {
  return s.trim()
    .replace(/½/g, '0.5')
    .replace(/⅓/g, '0.333')
    .replace(/¼/g, '0.25')
    .replace(/¾/g, '0.75')
    .replace(/⅔/g, '0.667');
}

/** Parse dose-shorthand like "1+0+1", "1-0-1", "½+0+½" → total doses/day */
function parseFrequencyDoses(freq: string): number {
  const normalised = normaliseFraction(freq).replace(/\s+/g, '');
  const parts = normalised.split(/[+\-×x]/);
  if (parts.length >= 2 && parts.every(p => /^\d*\.?\d+$/.test(p.trim()))) {
    return parts.slice(0, 4).reduce((s, p) => s + (parseFloat(p) || 0), 0);
  }
  const named: Record<string, number> = {
    'once daily': 1, 'twice daily': 2, 'three times daily': 3, 'four times daily': 4,
    'every 8 hours': 3, 'every 12 hours': 2, 'every 6 hours': 4,
    'once a week': 1 / 7, 'twice a week': 2 / 7, 'once a month': 1 / 30,
    'as needed (prn)': 1, 'stat (once only)': 1,
  };
  return named[freq.toLowerCase().trim()] || 0;
}

/** True when auto quantity from duration × frequency should be skipped (PRN / stat). */
function isPrnOrStatFrequency(freq: string | undefined): boolean {
  if (!freq?.trim()) return false;
  const f = freq.toLowerCase();
  return f.includes('prn') || f.includes('as needed') || f.includes('stat');
}

/** Human-readable mapping for dose shorthand (e.g. 1-0-1 → Morning + Evening). */
function describeFrequencyShorthand(freq: string): string | null {
  if (!freq?.trim()) return null;
  const raw = normaliseFraction(freq).replace(/\s+/g, '');
  const parts = raw.split(/[+\-×x]/).map(p => parseFloat(p.trim()));
  if (parts.length >= 2 && parts.every(p => !Number.isNaN(p))) {
    const slotLabels = ['Morning', 'Noon', 'Evening', 'Night'];
    const bits = parts.slice(0, 4).map((p, i) => (p > 0 ? `${slotLabels[i] || `Slot ${i + 1}`}: ${p % 1 === 0 ? String(p) : String(p)}` : null)).filter(Boolean);
    const dpm = parts.slice(0, 4).reduce((a, b) => a + b, 0);
    if (bits.length === 0) return null;
    return `${bits.join(' · ')} — ${dpm % 1 === 0 ? dpm : dpm.toFixed(2)} dose(s)/day`;
  }
  const dpm = parseFrequencyDoses(freq);
  if (dpm > 0 && !isPrnOrStatFrequency(freq)) {
    return `≈ ${dpm % 1 === 0 ? dpm : dpm.toFixed(2)} dose(s)/day`;
  }
  return null;
}

/** Estimated total units for the course (frequency × duration); not stored on the prescription. */
function autoCalcQuantity(line: PrescriptionMedicationItem): number {
  if (isPrnOrStatFrequency(line.frequency)) return 1;
  const dpm = parseFrequencyDoses(line.frequency || '');
  const days = line.durationDays;
  if (dpm > 0 && days && days > 0) return Math.ceil(dpm * days);
  return 1;
}

const FAV_ICD_STORAGE = 'ep_fav_icd10_entries';

function loadFavoriteIcd10Entries(): { code: string; description: string }[] {
  try {
    const raw = localStorage.getItem(FAV_ICD_STORAGE);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0 && parsed[0]?.code) return parsed;
    }
    const legacy = localStorage.getItem('ep_fav_icd10');
    if (legacy) {
      const codes: string[] = JSON.parse(legacy);
      const migrated = codes.map(code => ({
        code,
        description: COMMON_ICD10_CODES.find(c => c.code === code)?.description || code,
      }));
      localStorage.setItem(FAV_ICD_STORAGE, JSON.stringify(migrated));
      localStorage.removeItem('ep_fav_icd10');
      return migrated;
    }
  } catch {
    // ignore
  }
  return [];
}

function saveFavoriteIcd10Entries(entries: { code: string; description: string }[]) {
  try {
    localStorage.setItem(FAV_ICD_STORAGE, JSON.stringify(entries));
  } catch {
    // ignore
  }
}

function createDefaultMedicationLine(): PrescriptionMedicationItem {
  return {
    medicationName: '',
    medicationCode: '',
    medicationCodeType: 'RXNORM',
    dosageStrength: undefined,
    dosageUnit: '',
    dosageForm: 'TABLET',
    route: 'ORAL',
    frequency: '',
    instructions: '',
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    durationDays: undefined,
    refillsAuthorized: 0,
    refillsRemaining: 0,
    substitutionAllowed: true,
    isControlledSubstance: false,
    schedule: undefined,
    deaNumber: '',
    clientRowId: newMedicationClientRowId(),
  };
}

type DiagnosisEntry = { code: string; description: string; isPrimary: boolean };
type TestEntry = { testName: string; isPanel: boolean; panelName?: string };

const prescriptionTodayIso = (): string => new Date().toISOString().split('T')[0];

function normalizeDiagnosesPayload(diagnoses: DiagnosisEntry[]) {
  if (diagnoses.length === 0) return undefined;
  const primaryIdx = diagnoses.findIndex(d => d.isPrimary);
  const normalized = diagnoses.map((d, i) => ({
    ...d,
    isPrimary: primaryIdx >= 0 ? i === primaryIdx : i === 0,
  }));
  return normalized.map(d => ({
    diagnosisCode: d.code,
    diagnosisDescription: d.description,
    isPrimary: Boolean(d.isPrimary),
  }));
}

function prescriptionSaveErrorMessage(err: unknown, fallback: string): string {
  const data = (err as { response?: { data?: Record<string, unknown> } })?.response?.data;
  const code = data?.code as string | undefined;
  const apiMsg = (data?.error ?? data?.message) as string | undefined;
  if (code === 'DUPLICATE_DIAGNOSIS_CODE' && apiMsg) {
    return apiMsg;
  }
  if (code === 'MULTIPLE_PRIMARY_DIAGNOSES' && apiMsg) {
    return apiMsg;
  }
  return ehrApiErrorMessage(err, fallback);
}

function dedupeTestEntries(entries: TestEntry[]): TestEntry[] {
  const seen = new Set<string>();
  const out: TestEntry[] = [];
  for (const t of entries) {
    const key = t.testName.trim().toLowerCase();
    if (!key || seen.has(key)) continue;
    seen.add(key);
    out.push(t);
  }
  return out;
}

/** Parse C/C, Dx, advice, tests, follow-up, referral from structured prescription notes (EP-6). */
function formatDrugSuggestionLabel(drug: PharmacyDrugSuggestion): string {
  const brand = drug.brandName?.trim();
  const generic = drug.genericName?.trim();
  if (brand && generic) return `${brand} (${generic})`;
  return brand || generic || '';
}

/** Medical history rows with at least one meaningful clinical value (for print). */
function medicalHistoryWithValues(items: MedicalHistory[]): MedicalHistory[] {
  return items.filter((mh) => {
    const hasCondition = Boolean(mh.conditionName?.trim());
    const hasIcd = Boolean(mh.icd10Code?.trim() || mh.icd11Code?.trim());
    const hasSeverity = Boolean(mh.severity?.trim());
    const hasNotes = Boolean(mh.notes?.trim());
    return hasCondition || hasIcd || hasSeverity || hasNotes;
  });
}

function formatMedicalHistoryLine(mh: MedicalHistory): string {
  const parts: string[] = [];
  if (mh.conditionName?.trim()) parts.push(mh.conditionName.trim());
  if (mh.icd11Code?.trim()) parts.push(`ICD-11: ${mh.icd11Code.trim()}`);
  if (mh.icd10Code?.trim()) parts.push(`ICD-10: ${mh.icd10Code.trim()}`);
  if (mh.severity?.trim()) parts.push(`Severity: ${mh.severity.trim()}`);
  if (mh.notes?.trim()) parts.push(`Note: ${mh.notes.trim()}`);
  return parts.join(' | ');
}

function parseStructuredClinicalFromPrescription(rx: Prescription): {
  chiefComplaints: string[];
  clinicalFindings: string;
  diagnoses: DiagnosisEntry[];
  adviceItems: string[];
  orderedTests: TestEntry[];
  followUpDate: string;
  referral: string;
  freeNotes: string;
} {
  const parsedCC: string[] = [];
  const parsedDx: DiagnosisEntry[] = [];
  const parsedAdvice: string[] = [];
  const parsedTests: TestEntry[] = [];
  let parsedFollowUp = '';
  let parsedReferral = '';
  let parsedClinicalFindings = '';
  let cleanNotes = rx.notes || '';

  if (rx.notes) {
    const lines = rx.notes.split('\n');
    for (const line of lines) {
      if (line.startsWith('Clinical Findings: ')) {
        parsedClinicalFindings = line.replace('Clinical Findings: ', '').trim();
      } else if (line.startsWith('C/C: ')) {
        parsedCC.push(...line.replace('C/C: ', '').split(', ').filter(Boolean));
      } else if (line.startsWith('Dx: ')) {
        const dxParts = line.replace('Dx: ', '').split('; ');
        for (const part of dxParts) {
          const m = part.match(/^(\S+)\s+\((.+?)\)\s+\[(Primary|Secondary)\]$/);
          if (m) parsedDx.push({ code: m[1], description: m[2], isPrimary: m[3] === 'Primary' });
        }
      } else if (line.startsWith('Advice: ')) {
        parsedAdvice.push(...line.replace('Advice: ', '').split('; ').filter(Boolean));
      } else if (line.startsWith('Tests: ')) {
        parsedTests.push(...line.replace('Tests: ', '').split(', ').filter(Boolean).map(t => ({ testName: t, isPanel: false })));
      } else if (line.startsWith('Follow-up: ')) {
        const rest = line.replace('Follow-up: ', '').trim();
        const iso = rest.match(/\d{4}-\d{2}-\d{2}/);
        if (iso) parsedFollowUp = iso[0];
        else {
          const d = Date.parse(rest);
          if (!Number.isNaN(d)) parsedFollowUp = new Date(d).toISOString().split('T')[0];
        }
      } else if (line.startsWith('Referral: ')) {
        parsedReferral = line.replace('Referral: ', '');
      }
    }
    cleanNotes = lines
      .filter(
        l =>
          !l.startsWith('Clinical Findings: ') &&
          !l.startsWith('C/C: ') &&
          !l.startsWith('Dx: ') &&
          !l.startsWith('Advice: ') &&
          !l.startsWith('Tests: ') &&
          !l.startsWith('Follow-up: ') &&
          !l.startsWith('Referral: ')
      )
      .join('\n')
      .trim();
  }

  // FR-P1.4a: if structured notes didn't yield diagnoses, use the normalised diagnoses array,
  // then fall back to the legacy single diagnosisCode
  if (parsedDx.length === 0) {
    if (rx.diagnoses && rx.diagnoses.length > 0) {
      rx.diagnoses.forEach(d =>
        parsedDx.push({ code: d.diagnosisCode, description: d.diagnosisDescription || d.diagnosisCode, isPrimary: d.isPrimary })
      );
    } else if (rx.diagnosisCode) {
      const icd = COMMON_ICD10_CODES.find(c => c.code === rx.diagnosisCode);
      parsedDx.push({ code: rx.diagnosisCode, description: icd?.description || rx.diagnosisCode, isPrimary: true });
    }
  }

  return {
    chiefComplaints: parsedCC,
    clinicalFindings: parsedClinicalFindings,
    diagnoses: parsedDx,
    adviceItems: parsedAdvice,
    orderedTests: parsedTests,
    followUpDate: parsedFollowUp,
    referral: parsedReferral,
    freeNotes: cleanNotes,
  };
}

// ─── Print layout ─────────────────────────────────────────────────────────────

interface PrintData {
  sourcePrescription?: Prescription;
  patient: Patient | null;
  doctorConfig: DoctorEPConfig;
  prescribingProviderName: string;
  chiefComplaints: string[];
  clinicalFindings?: string;
  medicalHistory?: MedicalHistory[];
  diagnoses: DiagnosisEntry[];
  medicationLines: PrescriptionMedicationItem[];
  adviceItems: string[];
  orderedTests: TestEntry[];
  followUpDate: string;
  referral: string;
  notes: string;
  prescriptionNumber?: string;
  printFormat?: 'A4' | 'COMPACT';
  latestVitalSigns?: VitalSigns | null;
}

function buildRxPrintDocumentHtml(innerBodyHtml: string, title: string, isCompact: boolean): string {
  /**
   * A4 (210×297mm): @page uses a single, modest margin so browser print + PDF export do not
   * double-count horizontal insets (was @page 5mm + .rx-page 5mm padding + html2pdf 5mm).
   * Letterhead space is padding-top only on .rx-page — not duplicated as @page margin.
   */
  const pageSize = isCompact ? 'A5 portrait' : 'A4 portrait';
  const pageMargins = isCompact ? '11mm' : '0';
  const signaturePad = rxSignatureZonePadding(isCompact);
  const baseFs = isCompact ? '10px' : '11px';
  const maxWidth = isCompact ? '148mm' : '100%';
  const rxPagePadTop = isCompact ? '24.2mm' : RX_A4_PAGE_TOP;
  const rxPagePadLR = isCompact ? '12px' : RX_A4_PAGE_MARGIN_LR;
  const demoCols = 'repeat(4, minmax(0, 1fr))';
  const rxMainCols = RX_MAIN_COLS;
  const rxMainGap = isCompact ? '16px' : '4px';
  return `<!DOCTYPE html><html><head>
<meta charset="utf-8">
<title>${title}</title>
<style>
  * { box-sizing: border-box; }
  @page { size: ${pageSize}; margin: ${pageMargins}; }
  html, body { margin: 0; padding: 0; height: auto; min-height: 0; }
  body { font-family: "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: ${baseFs}; color: #14213d; background: #fff; display: ${isCompact ? 'block' : 'flex'}; flex-direction: ${isCompact ? 'initial' : 'column'}; }
  .rx-page { max-width: ${maxWidth}; margin: 0 auto; padding: ${isCompact ? '24.2mm 12px 14mm' : `${rxPagePadTop} ${rxPagePadLR} ${RX_A4_PAGE_BOTTOM} ${rxPagePadLR}`}; position: relative; display: flex; flex-direction: column; min-height: 0; flex: none; box-sizing: border-box; page-break-after: auto; page-break-inside: auto; }
  .rx-page-bottom { position: absolute; left: 0; right: 0; bottom: 0; z-index: 1; }
  .rx-header { display: flex; justify-content: space-between; gap: 14px; align-items: flex-end; border-bottom: 0; padding-bottom: 10px; margin-bottom: 10px; }
  .rx-header-left h2 { margin: 0 0 2px; font-size: ${isCompact ? '14px' : '20px'}; color: #005f73; }
  .rx-header-left p { margin: 0; color: #4b5563; line-height: 1.4; font-size: ${isCompact ? '9px' : '10px'}; }
  .rx-header-right { text-align: right; }
  .rx-header-right h3 { margin: 0 0 2px; font-size: ${isCompact ? '13px' : '17px'}; color: #1f2937; }
  .rx-header-right p { margin: 0; color: #374151; line-height: 1.35; font-size: ${isCompact ? '9px' : '10px'}; }
  .rx-demographics { display: grid; grid-template-columns: ${demoCols}; background: #e0fbfc; border-radius: 6px; padding: 8px 10px; gap: 6px 8px; margin-bottom: 12px; font-size: ${baseFs}; color: #1f2937; width: 100%; box-sizing: border-box; }
  .rx-demographics-cell { min-width: 0; white-space: normal; overflow-wrap: anywhere; word-break: break-word; }
  .rx-main { flex: 1; min-height: 0; display: grid; grid-template-columns: ${rxMainCols}; gap: ${rxMainGap}; align-items: stretch; page-break-inside: auto; }
  .rx-page.rx-page--signature .rx-main { padding-bottom: ${signaturePad}; }
  .rx-sidebar { align-self: start; min-width: 0; max-width: 100%; border-right: 2px solid #374151; padding-right: 4px; padding-left: 0; margin-left: 0; overflow-wrap: anywhere; word-break: break-word; }
  .rx-section-title { font-size: ${isCompact ? '11px' : '12px'}; font-weight: 700; color: #000000; text-transform: uppercase; border-bottom: 1px solid #000000; margin: 0 0 6px; padding-bottom: 3px; letter-spacing: 0.04em; }
  .rx-sidebar-item { font-size: ${isCompact ? '10px' : '11.5px'}; color: #334155; margin: 0 0 5px; line-height: 1.45; overflow-wrap: anywhere; word-break: break-word; }
  .rx-sidebar-item.rx-clinical-value { font-size: ${isCompact ? '11px' : '13px'}; color: #1e293b; }
  .rx-area { align-self: stretch; min-width: 0; max-width: 100%; min-height: 0; height: 100%; padding-left: 0; display: flex; flex-direction: column; }
  .rx-symbol { font-family: serif; font-size: ${isCompact ? '26px' : '34px'}; font-weight: 700; color: #005f73; margin: 0 0 10px; line-height: 1; }
  .med-item { margin: 0 0 14px; page-break-inside: avoid; }
  .med-item--compact { margin-bottom: 8px; }
  .rx-main--continuation { grid-template-columns: 1fr !important; }
  .rx-main--continuation .rx-sidebar { display: none; }
  .rx-advice-section { margin-top: 8px; page-break-before: auto; page-break-inside: avoid; }
  .med-name { font-size: ${isCompact ? '11px' : '13px'}; font-weight: 700; color: #1f2937; margin: 0; }
  .med-dose { font-size: ${isCompact ? '9px' : '10.5px'}; color: #475569; margin: 4px 0 0; line-height: 1.35; }
  .advice-list { margin: 6px 0 0; padding-left: 16px; font-size: ${isCompact ? '9px' : '10.5px'}; color: #334155; line-height: 1.5; }
  .rx-followup { margin-top: 10px; font-size: ${isCompact ? '10px' : '11.5px'}; color: #374151; display: flex; flex-wrap: wrap; gap: 12px; }
  .rx-area-section { margin-top: 10px; }
  .rx-area-item { font-size: ${isCompact ? '10px' : '11.5px'}; color: #1e293b; margin: 0 0 5px; line-height: 1.45; overflow-wrap: anywhere; word-break: break-word; }
  .rx-notes { margin-top: 8px; font-size: ${isCompact ? '9px' : '10px'}; color: #4b5563; overflow-wrap: anywhere; word-break: break-word; }
  .rx-signature-block { margin-top: 0; padding-top: 0; display: flex; justify-content: flex-end; }
  .rx-signature-box { text-align: center; min-width: 180px; }
  .rx-signature-line { border: none; border-top: none; padding-top: 4px; font-size: ${isCompact ? '9px' : '10px'}; color: #374151; }
  .rx-stamp { margin-top: 10px; max-height: 68px; max-width: 150px; }
  .rx-footer { margin-top: 8px; padding-top: 0; border: none; border-top: none; text-align: center; color: #6b7280; font-size: ${isCompact ? '8px' : '9px'}; line-height: 1.35; }
  @media screen and (max-width: 700px) {
    .rx-demographics { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .rx-main { grid-template-columns: 1fr; }
    .rx-sidebar { border-right: 0; border-bottom: 1px solid #e5e7eb; padding-right: 0; padding-bottom: 8px; margin-bottom: 8px; }
  }
  @media print {
    .rx-page { margin: 0 auto; max-width: 100%; min-height: 0; padding-left: ${rxPagePadLR}; padding-right: ${rxPagePadLR}; page-break-inside: auto; }
    .rx-main { grid-template-columns: ${rxMainCols}; page-break-inside: auto; }
    .rx-page.rx-page--signature .rx-main { padding-bottom: ${signaturePad}; }
    .rx-page-bottom { position: absolute; left: 0; right: 0; bottom: 0; }
    body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  }
</style></head><body>${innerBodyHtml}</body></html>`;
}

const PrintableRx: React.FC<{ data: PrintData; onClose: () => void; onEdit?: () => void; onSaveBeforePrint?: () => Promise<boolean>; onAfterPrint?: () => void }> = ({ data, onClose, onEdit, onSaveBeforePrint, onAfterPrint }) => {
  const printRef = useRef<HTMLDivElement>(null);
  const [pdfLoading, setPdfLoading] = useState(false);
  const isCompact = (data.printFormat || 'A4') === 'COMPACT';
  const previewFs = isCompact ? '10px' : '11px';
  const printSlices = useMemo(
    () => buildRxPrintSlices(data.medicationLines || [], data.adviceItems || []),
    [data.medicationLines, data.adviceItems],
  );
  const totalPrintPages = printSlices.length;

  const handlePrint = async () => {
    const win = window.open('', '_blank', 'width=800,height=900');
    if (!win) return;
    win.document.write('<!doctype html><html><head><title>Preparing preview</title></head><body style="font-family:Arial,sans-serif;padding:20px;">Preparing preview...</body></html>');
    win.document.close();
    if (onSaveBeforePrint) {
      const saved = await onSaveBeforePrint();
      if (!saved) {
        win.close();
        return;
      }
      // Wait one paint so preview DOM reflects saved prescription metadata (e.g. Rx #).
      await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
    }
    let content = printRef.current?.innerHTML;
    if (!content) {
      win.close();
      return;
    }
    content = prepareRxPrintInnerHtml(content, totalPrintPages === 1);
    win.document.write(buildRxPrintDocumentHtml(content, 'Prescription', isCompact));
    win.document.close();
    const hasImages = !!(data.doctorConfig.signatureDataUrl || data.doctorConfig.stampDataUrl);
    const delayMs = hasImages ? 450 : 120;
    const run = () => {
      win.focus();
      win.onafterprint = () => {
        win.close();
        onAfterPrint?.();
      };
      win.print();
    };
    if (win.document.readyState === 'complete') {
      window.setTimeout(run, delayMs);
    } else {
      win.onload = () => window.setTimeout(run, delayMs);
    }
  };

  const handleDownloadPdf = async () => {
    const el = printRef.current;
    if (!el) return;
    setPdfLoading(true);
    // Clear any inline min-heights before capture so html2pdf does not round up to a blank page.
    const savedMinHeight = el.style.minHeight;
    const savedAlign = el.style.alignItems;
    el.style.minHeight = 'auto';
    el.style.alignItems = 'flex-start';
    const rxPageEls = Array.from(el.querySelectorAll<HTMLElement>('.rx-page'));
    const savedRxPageMinHeights = rxPageEls.map((p) => p.style.minHeight);
    rxPageEls.forEach((p) => { p.style.minHeight = 'auto'; });
    const restorePdfCapture = applyRxPdfCaptureAdjustments(el, {
      singlePage: totalPrintPages === 1,
      isCompact,
    });
    try {
      const mrnSeg = sanitizePrescriptionFilenameMrn(data.patient?.mrn);
      /** jsPDF A4 margins 0 — printable inset comes only from @page / body in HTML (avoids double L/R with canvas). */
      await html2pdf()
        .set({
          margin: isCompact ? 8 : [0, 0, 0, 0],
          filename: `prescription-${mrnSeg}.pdf`,
          image: { type: 'jpeg', quality: 0.92 },
          html2canvas: {
            scale: 2,
            useCORS: true,
            logging: false,
            scrollY: 0,
            scrollX: 0,
            windowHeight: el.scrollHeight,
          },
          jsPDF: {
            unit: 'mm',
            format: isCompact ? 'a5' : 'a4',
            orientation: 'portrait',
          },
          pagebreak: totalPrintPages === 1 ? { mode: ['avoid-all'] } : { mode: ['css', 'legacy'] },
        })
        .from(el)
        .save();
    } catch {
      window.alert('Could not generate PDF. Use Print → Save as PDF, or Download HTML.');
    } finally {
      restorePdfCapture();
      el.style.minHeight = savedMinHeight;
      el.style.alignItems = savedAlign;
      rxPageEls.forEach((p, i) => { p.style.minHeight = savedRxPageMinHeights[i]; });
      setPdfLoading(false);
    }
  };

  const handleDownloadHtml = () => {
    let content = printRef.current?.innerHTML;
    if (!content) return;
    content = prepareRxPrintInnerHtml(content, totalPrintPages === 1);
    const html = buildRxPrintDocumentHtml(content, 'Prescription', isCompact);
    const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `prescription-${sanitizePrescriptionFilenameMrn(data.patient?.mrn)}.html`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const d = data;
  const today = new Date().toLocaleDateString('en-GB');

  return portalLayoutOverlay(
    <div
      className={`rx-print-preview-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`}
      style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}
    >
      <div className="rx-print-preview-panel">
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '16px 20px', borderBottom: '1px solid #e5e7eb', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
          <div>
            <h3 style={{ margin: 0 }}>Prescription Preview</h3>
            <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#6b7280' }}>
              Layout: <strong>{isCompact ? 'Compact (A5 PDF / tight print)' : 'A4'}</strong>. Download PDF for a file, or use Print → Save as PDF.
            </p>
          </div>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button type="button" className="btn-primary" onClick={handlePrint}>Save & Print</button>
            <button type="button" className="btn-secondary" onClick={handleDownloadPdf} disabled={pdfLoading}>
              {pdfLoading ? 'Building PDF…' : 'Download PDF'}
            </button>
            <button type="button" className="btn-secondary" onClick={handleDownloadHtml}>Download HTML</button>
            {onEdit && <button type="button" className="btn-secondary" onClick={onEdit}>Edit</button>}
            <button type="button" className="btn-secondary" onClick={onClose}>Close</button>
          </div>
        </div>

        <div
          ref={printRef}
          className={`rx-preview-scope ${isCompact ? 'rx-preview-scope--compact' : 'rx-preview-scope--a4'}`}
          style={{
            padding: 0,
            fontFamily: '"Helvetica Neue", Helvetica, Arial, sans-serif',
            fontSize: previewFs,
            color: '#14213d',
            margin: '0 auto',
            ...(!isCompact
              ? {
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                }
              : {}),
          }}
        >
          {printSlices.map((slice, pageIndex) => {
            const isFirstPage = pageIndex === 0;
            const isLastPage = pageIndex === totalPrintPages - 1;
            const isMeds = slice.kind === 'meds';
            const pageMeds = isMeds ? slice.meds : [];
            const globalMedOffset = isMeds ? slice.medPageIndex * RX_MEDS_PER_PAGE : 0;
            const showAdviceInline =
              isMeds &&
              slice.medPageIndex === slice.medPageCount - 1 &&
              d.adviceItems.length > 0 &&
              pageMeds.length < RX_MEDS_PER_PAGE;
            const showAdviceBlock = showAdviceInline || slice.kind === 'advice';
            return (
          <div
            className={`rx-page${isLastPage ? ' rx-page--signature' : ''}`}
            key={`rx-page-${pageIndex}-${slice.kind}`}
            style={{ pageBreakAfter: pageIndex < totalPrintPages - 1 ? 'always' : 'auto' }}
          >
            {isFirstPage && (
              <>
            <div className="rx-header">
              <div className="rx-header-left" />
              <div className="rx-header-right">
                <p>
                  Date: {today}
                  {` | Rx#: ${d.prescriptionNumber || '—'}`}
                  {totalPrintPages > 1 && ` | Page ${pageIndex + 1} of ${totalPrintPages}`}
                </p>
              </div>
            </div>

            {d.patient && (
              <div className="rx-demographics">
                <div className="rx-demographics-cell"><strong>Name:</strong> {d.patient.fullName}</div>
                <div className="rx-demographics-cell"><strong>MRN:</strong> {d.patient.mrn || '—'}</div>
                <div className="rx-demographics-cell"><strong>Age:</strong> {d.patient.dateOfBirth ? formatAge(d.patient.dateOfBirth) : '—'}</div>
                <div className="rx-demographics-cell"><strong>Sex:</strong> {formatGenderLabel(d.patient.gender) || '—'}</div>
                <div className="rx-demographics-cell"><strong>Weight:</strong> {d.latestVitalSigns?.weight ? `${d.latestVitalSigns.weight} ${d.latestVitalSigns.weightUnit || 'kg'}` : '—'}</div>
                <div className="rx-demographics-cell"><strong>Height:</strong> {d.latestVitalSigns?.height ? ((!d.latestVitalSigns.heightUnit || d.latestVitalSigns.heightUnit === 'in') ? `${Math.floor(d.latestVitalSigns.height / 12)} ft ${Math.round((d.latestVitalSigns.height % 12) * 10) / 10} in` : `${d.latestVitalSigns.height} ${d.latestVitalSigns.heightUnit}`) : '—'}</div>
                <div className="rx-demographics-cell"><strong>BP:</strong> {(d.latestVitalSigns?.systolicBp && d.latestVitalSigns?.diastolicBp) ? `${d.latestVitalSigns.systolicBp}/${d.latestVitalSigns.diastolicBp}` : '—'}</div>
                <div className="rx-demographics-cell"><strong>Pulse:</strong> {d.latestVitalSigns?.heartRate ? `${d.latestVitalSigns.heartRate} bpm` : '—'}</div>
              </div>
            )}
              </>
            )}

            {!isFirstPage && (
              <div className="rx-header" style={{ marginBottom: '8px' }}>
                <div className="rx-header-right" style={{ width: '100%', textAlign: 'right' }}>
                  <p style={{ margin: 0 }}>
                    {d.patient?.fullName ? `${d.patient.fullName} — ` : ''}
                    Rx# {d.prescriptionNumber || '—'}
                    {` · Page ${pageIndex + 1} of ${totalPrintPages}`}
                  </p>
                </div>
              </div>
            )}

            <div className={`rx-main${isFirstPage ? '' : ' rx-main--continuation'}`}>
              {isFirstPage && (
              <div className="rx-sidebar">
                {d.chiefComplaints.length > 0 && (
                  <>
                    <p className="rx-section-title">Chief Complaints</p>
                    {d.chiefComplaints.map((c, i) => <p key={`${c}-${i}`} className="rx-sidebar-item rx-clinical-value">- {c}</p>)}
                  </>
                )}

                {d.medicalHistory && medicalHistoryWithValues(d.medicalHistory).length > 0 && (
                  <>
                    <p className="rx-section-title" style={{ marginTop: '10px' }}>Medical History</p>
                    {medicalHistoryWithValues(d.medicalHistory).map((mh, i) => (
                      <p key={`${mh.historyId}-${i}`} className="rx-sidebar-item">
                        {formatMedicalHistoryLine(mh)}
                      </p>
                    ))}
                  </>
                )}

                {d.clinicalFindings?.trim() && (
                  <>
                    <p className="rx-section-title" style={{ marginTop: '10px' }}>Clinical Findings</p>
                    <p className="rx-sidebar-item rx-clinical-value">{d.clinicalFindings.trim()}</p>
                  </>
                )}

                {d.diagnoses.length > 0 && (
                  <>
                    <p className="rx-section-title" style={{ marginTop: '10px' }}>Diagnosis</p>
                    {d.diagnoses.map((dx, i) => (
                      <p key={`${dx.code}-${i}`} className="rx-sidebar-item rx-clinical-value">
                        {dx.isPrimary ? '▸' : '-'} <strong>{dx.code}</strong> - {dx.description}
                      </p>
                    ))}
                  </>
                )}

                {d.orderedTests.length > 0 && (
                  <>
                    <p className="rx-section-title" style={{ marginTop: '10px' }}>Investigation</p>
                    {d.orderedTests.map((t, i) => (
                      <p key={`${t.testName}-${i}`} className="rx-sidebar-item rx-clinical-value">{i + 1}. {t.testName}</p>
                    ))}
                  </>
                )}
                <p className="rx-section-title" style={{ marginTop: '10px' }}>Follow Up</p>
                <p className="rx-sidebar-item rx-clinical-value"><strong>Follow-up:</strong> {d.followUpDate ? new Date(d.followUpDate).toLocaleDateString('en-GB') : '—'}</p>
                <p className="rx-sidebar-item rx-clinical-value"><strong>Referral:</strong> {d.referral || '—'}</p>
              </div>
              )}

              <div className="rx-area">
                {isFirstPage && isMeds && <p className="rx-symbol">℞</p>}

                {isMeds && pageMeds.map((m, i) => (
                  <div
                    className={`med-item${pageMeds.length >= 7 ? ' med-item--compact' : ''}`}
                    key={m.prescriptionMedicationId || m.clientRowId || `${m.medicationName}-${pageIndex}-${i}`}
                  >
                    <p className="med-name">{globalMedOffset + i + 1}. {m.medicationName}</p>
                    <p className="med-dose">
                      <strong>{m.frequency || '—'}</strong>
                      {' | '}
                      {m.dosageStrength}{m.dosageUnit ? ` ${m.dosageUnit}` : ''} {m.dosageForm || ''}
                      {' | '}
                      {m.durationDays ? (m.durationDays === 0 ? 'Ongoing' : `${m.durationDays} days`) : '—'}
                      {m.instructions ? ` | ${m.instructions}` : ''}
                    </p>
                  </div>
                ))}

                {showAdviceBlock && d.adviceItems.length > 0 && (
                  <div className="rx-area-section rx-advice-section">
                    <p className="rx-section-title">Advice</p>
                    {d.adviceItems.map((a, i) => (
                      <p key={`${a}-${i}`} className="rx-area-item">- {a}</p>
                    ))}
                  </div>
                )}

                {isLastPage && d.notes && (
                  <div className="rx-area-section rx-notes">
                    <p className="rx-section-title">Notes</p>
                    <p className="rx-area-item" style={{ margin: 0 }}>{d.notes}</p>
                  </div>
                )}

              </div>
            </div>

            {isLastPage && (
              <div className="rx-page-bottom">
                <div className="rx-signature-block">
                  <div className="rx-signature-box">
                    {d.doctorConfig.signatureDataUrl && (
                      <img src={d.doctorConfig.signatureDataUrl} alt="Signature" style={{ maxHeight: '50px', marginBottom: '4px' }} />
                    )}
                    <div className="rx-signature-line"><strong>{d.prescribingProviderName}</strong><br />Signature &amp; Seal</div>
                    {d.doctorConfig.stampDataUrl && (
                      <img className="rx-stamp" src={d.doctorConfig.stampDataUrl} alt="Stamp" />
                    )}
                  </div>
                </div>
                <div className="rx-footer">{d.doctorConfig.footerText || ''}</div>
              </div>
            )}
          </div>
            );
          })}
        </div>
      </div>
    </div>,
  );
};

// ─── Main component ───────────────────────────────────────────────────────────

function readQuickModeDefault(): boolean {
  try {
    const v = localStorage.getItem('ep_quick_mode_default');
    if (v === 'false') return false;
    if (v === 'true') return true;
    return typeof window !== 'undefined' && window.matchMedia('(min-width: 768px)').matches;
  } catch {
    return true;
  }
}

/** EP-1: format seconds as m:ss for prescribe session timer */
function formatEpMmSs(totalSec: number): string {
  const m = Math.floor(totalSec / 60);
  const s = totalSec % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

function digitsOnly(value?: string | null): string {
  return (value || '').replace(/\D/g, '');
}

const PRESCRIBED_TODAY_PREFIX = 'ep_prescribed_today_';

function markPatientPrescribedToday(patientId?: string) {
  if (!patientId) return;
  const today = new Date().toISOString().split('T')[0];
  const key = `${PRESCRIBED_TODAY_PREFIX}${today}`;
  try {
    const raw = localStorage.getItem(key);
    const list: unknown = raw ? JSON.parse(raw) : [];
    const ids = Array.isArray(list) ? list.filter((id): id is string => typeof id === 'string') : [];
    if (!ids.includes(patientId)) {
      ids.push(patientId);
      localStorage.setItem(key, JSON.stringify(ids));
    }
  } catch {
    // ignore best-effort local UI sync key
  }
}

const PrescriptionManagementPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasAnyPermission, user, currentOrganizationId } = useAuth();

  const schedulingAppointmentId = (location.state as { schedulingAppointmentId?: string } | null)?.schedulingAppointmentId;

  // Local-market rollout: keep advanced eRx workflows hidden for now.
  const enableTransmitFeature = false;
  const enableFormularyAndPaFeatures = false;

  const canViewRx = hasAnyPermission(['HOSPITAL_PRESCRIPTION_VIEW', 'HOSPITAL_VIEW', 'HOSPITAL_MANAGE']);
  const canPrescribeRx = hasAnyPermission(['HOSPITAL_PRESCRIPTION_PRESCRIBE', 'HOSPITAL_MANAGE']);
  const canTransmitRx =
    enableTransmitFeature && hasAnyPermission(['HOSPITAL_PRESCRIPTION_TRANSMIT', 'HOSPITAL_MANAGE']);

  // ── List state ──
  const [patient, setPatient] = useState<Patient | null>(null);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [statusUpdatingId, setStatusUpdatingId] = useState<string | null>(null);
  const [cancelDialogId, setCancelDialogId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');

  // ── Form visibility ──
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Prescription | null>(null);
  const [quickMode, setQuickMode] = useState(readQuickModeDefault);
  /** When false in quick mode, Rx history table is hidden for a full-width prescribing layout (EP-3). */
  const [showRxListWhilePrescribing, setShowRxListWhilePrescribing] = useState(true);
  const [lastDraftSavedAt, setLastDraftSavedAt] = useState<number | null>(null);
  const [preferredMedicines, setPreferredMedicines] = useState<string[]>([]);
  const [medicalHistoryItems, setMedicalHistoryItems] = useState<MedicalHistory[]>([]);
  const [latestVitalSigns, setLatestVitalSigns] = useState<VitalSigns | null>(null);

  // ── Drug search ──
  const [drugSuggestions, setDrugSuggestions] = useState<PharmacyDrugSuggestion[]>([]);
  const [showDrugSuggestions, setShowDrugSuggestions] = useState(false);
  const [drugSearchLoading, setDrugSearchLoading] = useState(false);
  const [drugSearchFailed, setDrugSearchFailed] = useState(false);
  const [drugSearchQuery, setDrugSearchQuery] = useState('');
  const [expandedLines, setExpandedLines] = useState<Set<number>>(new Set());
  const [customDurationLines, setCustomDurationLines] = useState<Set<number>>(new Set());

  /** Add master drug from Rx flow (same API as Pharmacy Drug Catalog) and append to this prescription. */
  const [showAddCatalogDrugModal, setShowAddCatalogDrugModal] = useState(false);
  const [addCatalogDrugForm, setAddCatalogDrugForm] = useState<DrugRequest>(() => ({ ...INITIAL_ADD_CATALOG_DRUG_FORM }));
  const [addCatalogDrugFormSelect, setAddCatalogDrugFormSelect] = useState('');
  const [addCatalogManufacturers, setAddCatalogManufacturers] = useState<Manufacturer[]>([]);
  const [addCatalogManufacturersLoading, setAddCatalogManufacturersLoading] = useState(false);
  const [addCatalogDrugSaving, setAddCatalogDrugSaving] = useState(false);

  // ── FR-P3.5: Pharmacy directory search ──
  const [pharmacyQuery, setPharmacyQuery] = useState('');
  const [pharmacySuggestions, setPharmacySuggestions] = useState<PharmacyDirectoryEntry[]>([]);
  const [showPharmacySuggestions, setShowPharmacySuggestions] = useState(false);
  const [pharmacySearchLoading, setPharmacySearchLoading] = useState(false);
  const pharmacySearchRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ── Clinical info (EP-4) ──
  const [chiefComplaints, setChiefComplaints] = useState<string[]>([]);
  const [diagnoses, setDiagnoses] = useState<DiagnosisEntry[]>([]);
  const [diagnosisError, setDiagnosisError] = useState<string | null>(null);
  const [icd10Query, setIcd10Query] = useState('');
  const [icd10Results, setIcd10Results] = useState<ICD10Code[]>([]);
  const [icd10RemoteLoading, setIcd10RemoteLoading] = useState(false);
  const [showIcd10, setShowIcd10] = useState(false);
  const [favoriteIcd10Entries, setFavoriteIcd10Entries] = useState(loadFavoriteIcd10Entries);
  // ── Advice / Tests / Follow-up (EP-6) ──
  const [adviceItems, setAdviceItems] = useState<string[]>([]);
  const [customAdvice, setCustomAdvice] = useState('');
  const [customComplaint, setCustomComplaint] = useState('');
  const [adviceOptions, setAdviceOptions] = useState<string[]>([]);
  /** Fallback when ranked suggestions fail or omit rows — merged into the advice autocomplete. */
  const [adviceLookupFallback, setAdviceLookupFallback] = useState<string[]>([]);
  const [orderedTests, setOrderedTests] = useState<TestEntry[]>([]);
  const [customTest, setCustomTest] = useState('');
  const [clinicalChartTestOptions, setClinicalChartTestOptions] = useState<string[]>([]);
  const clinicalChartTestSuggestTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const clinicalChartInvSuggestSeq = useRef(0);
  const [epTestLookupOptions, setEpTestLookupOptions] = useState<string[]>([]);
  const [followUpDate, setFollowUpDate] = useState('');
  const [followUpDays, setFollowUpDays] = useState<number | ''>('');
  const [referral, setReferral] = useState('');
  const [clinicalFindings, setClinicalFindings] = useState('');
  const [dismissedSuggestions, setDismissedSuggestions] = useState<EpDismissedStore>(() =>
    loadEpDismissedSuggestions(undefined),
  );

  useEffect(() => {
    setDismissedSuggestions(loadEpDismissedSuggestions(user?.id, currentOrganizationId ?? undefined));
  }, [user?.id, currentOrganizationId]);

  const dismissAdviceSuggestion = useCallback(
    (line: string) => {
      if (!user?.id) return;
      const store = dismissEpSuggestionLocally(user.id, currentOrganizationId ?? undefined, 'advice', line);
      setDismissedSuggestions(store);
      setAdviceOptions(prev => prev.filter(o => o.trim().toLowerCase() !== line.trim().toLowerCase()));
      setAdviceLookupFallback(prev => prev.filter(o => o.trim().toLowerCase() !== line.trim().toLowerCase()));
      void epAdviceService
        .dismiss([line])
        .then(() => {
          void epAdviceService.getSuggestions({ query: customAdvice.trim(), limit: 120 }).then(setAdviceOptions);
        })
        .catch(() => {});
    },
    [user?.id, currentOrganizationId, customAdvice],
  );

  const dismissInvestigationSuggestion = useCallback(
    (line: string) => {
      if (!user?.id) return;
      setDismissedSuggestions(
        dismissEpSuggestionLocally(user.id, currentOrganizationId ?? undefined, 'tests', line),
      );
      const key = line.trim().toLowerCase();
      setClinicalChartTestOptions(prev => prev.filter(o => o.trim().toLowerCase() !== key));
      setEpTestLookupOptions(prev => prev.filter(o => o.trim().toLowerCase() !== key));
    },
    [user?.id, currentOrganizationId],
  );

  const investigationAutocompleteOptions = useMemo(() => {
    const merged = Array.from(new Set([...clinicalChartTestOptions, ...epTestLookupOptions])).sort((a, b) =>
      a.localeCompare(b),
    );
    return filterEpAutocompleteOptions(merged, dismissedSuggestions, 'tests');
  }, [clinicalChartTestOptions, epTestLookupOptions, dismissedSuggestions]);

  const lastCommittedInvestigationRef = useRef('');
  const appendInvestigationTest = useCallback((raw: string) => {
    const name = raw.trim();
    if (!name) return;
    if (lastCommittedInvestigationRef.current === name) return;
    lastCommittedInvestigationRef.current = name;
    window.setTimeout(() => {
      lastCommittedInvestigationRef.current = '';
    }, 0);
    setOrderedTests((p) => {
      if (p.some((t) => t.testName.trim().toLowerCase() === name.toLowerCase())) return p;
      return [...p, { testName: name, isPanel: false }];
    });
    setCustomTest('');
    setClinicalChartTestOptions([]);
  }, []);

  useEffect(() => {
    void epLookupService
      .fetchAll()
      .then((m) => setEpTestLookupOptions(m.TEST ?? []))
      .catch(() => setEpTestLookupOptions([]));
  }, []);

  useEffect(() => () => {
    if (clinicalChartTestSuggestTimer.current) clearTimeout(clinicalChartTestSuggestTimer.current);
  }, []);

  // ── Templates (EP-7) ──
  const [templates, setTemplates] = useState<PrescriptionTemplate[]>([]);
  const [showTemplateDialog, setShowTemplateDialog] = useState(false);
  const [showSaveTemplate, setShowSaveTemplate] = useState(false);
  const [newTemplateName, setNewTemplateName] = useState('');
  const [newTemplateKind, setNewTemplateKind] = useState<'DOCTOR' | 'DISEASE'>('DOCTOR');
  const [newTemplateDiseaseCategory, setNewTemplateDiseaseCategory] = useState('');
  const [templateFilter, setTemplateFilter] = useState<EpTemplateListScope>('mine');
  const [templateApplySearch, setTemplateApplySearch] = useState('');
  const [linkedDoctor, setLinkedDoctor] = useState<Doctor | null>(null);
  const [showCopyPrevious, setShowCopyPrevious] = useState(false);

  // ── Print (EP-8) ──
  const [showPrint, setShowPrint] = useState(false);
  const [printData, setPrintData] = useState<PrintData | null>(null);
  const [doctorConfig, setDoctorConfig] = useState<DoctorEPConfig>({});
  const [hasDraft, setHasDraft] = useState(false);

  /** EP-1 / EP-11: OPD vs IPD context + SLA session timer */
  const [epSessionMode, setEpSessionMode] = useState<'OPD' | 'IPD'>('OPD');
  const [rxElapsedSec, setRxElapsedSec] = useState(0);
  const rxSessionStartRef = useRef<number | null>(null);

  const setEpSessionModeAndUrl = useCallback(
    (mode: 'OPD' | 'IPD') => {
      setEpSessionMode(mode);
      const params = new URLSearchParams(location.search);
      params.set('epMode', mode);
      navigate({ pathname: location.pathname, search: `?${params.toString()}` }, { replace: true });
    },
    [location.pathname, location.search, navigate]
  );

  // ── Transmit / checks ──
  const [showTransmitDialog, setShowTransmitDialog] = useState(false);
  const [prescriptionToTransmit, setPrescriptionToTransmit] = useState<Prescription | null>(null);
  const [interactionCheck, setInteractionCheck] = useState<DrugInteractionCheckResponse | null>(null);
  const [allergyCheck, setAllergyCheck] = useState<AllergyCheckResponse | null>(null);
  const [showInteractionCheck, setShowInteractionCheck] = useState(false);
  /** Optional FR-P1.7 overrides for ad-hoc interaction screening (weight, labs, pregnancy). */
  const [interactionScreeningContext, setInteractionScreeningContext] = useState<{
    weightKg: string;
    egfrMlMin: string;
    serumCreatinineMgDl: string;
    pregnancyStatus: '' | PregnancyStatus;
    lactating: boolean;
  }>({ weightKg: '', egfrMlMin: '', serumCreatinineMgDl: '', pregnancyStatus: '', lactating: false });
  const [showAllergyCheck, setShowAllergyCheck] = useState(false);
  const [transmitData, setTransmitData] = useState<PrescriptionTransmitRequest>({
    overrideInteractions: false,
    overrideAllergies: false,
    overrideReason: '',
    overridePdmpCheck: false,
  });

  // ── PDMP ──
  const [showPdmpModal, setShowPdmpModal] = useState(false);
  const [pdmpPrescription, setPdmpPrescription] = useState<Prescription | null>(null);
  const [pdmpQueryResult, setPdmpQueryResult] = useState<PDMPQueryResponse | null>(null);
  const [pdmpHistory, setPdmpHistory] = useState<PDMPQueryResponse[]>([]);
  const [pdmpQueryState, setPdmpQueryState] = useState('');
  const [pdmpReason, setPdmpReason] = useState('');
  const [pdmpDeaNumber, setPdmpDeaNumber] = useState('');
  const [pdmpLoading, setPdmpLoading] = useState(false);
  /** Whether the inline PDMP query panel is expanded inside the transmit dialog. */
  const [showInlinePdmp, setShowInlinePdmp] = useState(false);
  /** Result from an inline PDMP query run without leaving the transmit dialog. */
  const [inlinePdmpResult, setInlinePdmpResult] = useState<PDMPQueryResponse | null>(null);

  // ── Formulary ──
  const [showFormularyModal, setShowFormularyModal] = useState(false);
  const [formularyPrescription, setFormularyPrescription] = useState<Prescription | null>(null);
  const [formularyResult, setFormularyResult] = useState<FormularyCheckResponse | null>(null);
  const [formularyLoading, setFormularyLoading] = useState(false);

  // ── Prior Authorization ──
  const [showPaModal, setShowPaModal] = useState(false);
  const [paPrescription, setPaPrescription] = useState<Prescription | null>(null);
  const [paList, setPaList] = useState<PriorAuthorizationResponse[]>([]);
  const [paJustification, setPaJustification] = useState('');
  const [paDocs, setPaDocs] = useState('');
  const [paLoading, setPaLoading] = useState(false);
  const [paManualUpdateId, setPaManualUpdateId] = useState<string | null>(null);
  const [paManualStatus, setPaManualStatus] = useState<string>('');
  const [paManualNumber, setPaManualNumber] = useState('');
  const [paManualExpiry, setPaManualExpiry] = useState('');
  const [paManualDenial, setPaManualDenial] = useState('');
  const [paManualSaving, setPaManualSaving] = useState(false);

  // ── Transmission Monitoring ──
  const [showTransmissionsModal, setShowTransmissionsModal] = useState(false);
  const [transmissionsPrescription, setTransmissionsPrescription] = useState<Prescription | null>(null);
  const [transmissionsList, setTransmissionsList] = useState<PrescriptionTransmissionRecord[]>([]);
  const [transmissionsLoading, setTransmissionsLoading] = useState(false);
  const [retryingTransmissionId, setRetryingTransmissionId] = useState<string | null>(null);

  // ── Interaction Acknowledgment ──
  const [showInteractionAckModal, setShowInteractionAckModal] = useState(false);
  const [ackPrescription, setAckPrescription] = useState<Prescription | null>(null);
  const [ackInteractions, setAckInteractions] = useState<PrescriptionInteraction[]>([]);
  const [ackReasonMap, setAckReasonMap] = useState<Record<string, string>>({});
  const [ackLoading, setAckLoading] = useState(false);
  const [ackSavingId, setAckSavingId] = useState<string | null>(null);

  // ── Allergy Acknowledgment ──
  const [showAllergyAckModal, setShowAllergyAckModal] = useState(false);
  const [allergyAckPrescription, setAllergyAckPrescription] = useState<Prescription | null>(null);
  const [allergyAckChecks, setAllergyAckChecks] = useState<PrescriptionAllergyCheck[]>([]);
  const [allergyAckReasonMap, setAllergyAckReasonMap] = useState<Record<string, string>>({});
  const [allergyAckLoading, setAllergyAckLoading] = useState(false);
  const [allergyAckSavingId, setAllergyAckSavingId] = useState<string | null>(null);

  // ── Shared form meta ──
  const [formData, setFormData] = useState({
    patientId: id || '',
    prescriptionType: 'ELECTRONIC' as const,
    pharmacyName: '',
    pharmacyNpi: '',
    pharmacyPhone: '',
    prescribingProviderId: user?.id || '',
    prescribingProviderNpi: '',
    prescribingProviderName: user
      ? [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || ''
      : '',
    notes: '',
    specialInstructions: '',
  });

  const [medicationLines, setMedicationLines] = useState<PrescriptionMedicationItem[]>([]);

  const buildPrescriptionPayload = (): PrescriptionRequest => {
    const clinicalParts: string[] = [];
    if (chiefComplaints.length > 0) clinicalParts.push(`C/C: ${chiefComplaints.join(', ')}`);
    if (clinicalFindings.trim()) clinicalParts.push(`Clinical Findings: ${clinicalFindings.trim()}`);
    if (diagnoses.length > 0) {
      const dxStr = diagnoses.map(d => `${d.code} (${d.description})${d.isPrimary ? ' [Primary]' : ' [Secondary]'}`).join('; ');
      clinicalParts.push(`Dx: ${dxStr}`);
    }
    if (adviceItems.length > 0) clinicalParts.push(`Advice: ${adviceItems.join('; ')}`);
    if (orderedTests.length > 0) clinicalParts.push(`Tests: ${orderedTests.map(t => t.testName).join(', ')}`);
    if (followUpDate) clinicalParts.push(`Follow-up: ${followUpDate}`);
    if (referral) clinicalParts.push(`Referral: ${referral}`);

    const clinicalSummary = clinicalParts.join('\n');
    const combinedNotes = [clinicalSummary, formData.notes].filter(Boolean).join('\n\n');

    const medsForSave = ensureSuppositoryInstructionsOnMedicationLines(
      medicationLines.map(({ clientRowId: _rowKey, ...line }) => ({
        ...line,
        dosageForm: line.dosageForm || 'TABLET',
        route: line.route || 'ORAL',
        startDate: line.startDate || new Date().toISOString().split('T')[0],
        endDate: line.endDate || undefined,
      })),
    );

    return {
      patientId: id!,
      prescriptionType: formData.prescriptionType,
      epEncounterMode: epSessionMode,
      medications: medsForSave,
      pharmacyName: formData.pharmacyName || undefined,
      pharmacyNpi: formData.pharmacyNpi || undefined,
      pharmacyPhone: formData.pharmacyPhone || undefined,
      prescribingProviderId: formData.prescribingProviderId,
      prescribingProviderNpi: formData.prescribingProviderNpi || undefined,
      prescribingProviderName: formData.prescribingProviderName || undefined,
      notes: combinedNotes || undefined,
      specialInstructions: formData.specialInstructions || undefined,
      diagnoses: normalizeDiagnosesPayload(diagnoses),
    };
  };

  const resetForm = useCallback(() => {
    setFormData({
      patientId: id || '',
      prescriptionType: 'ELECTRONIC',
      pharmacyName: '',
      pharmacyNpi: '',
      pharmacyPhone: '',
      prescribingProviderId: user?.id || '',
      prescribingProviderNpi: '',
      prescribingProviderName: user
        ? [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || ''
        : '',
      notes: '',
      specialInstructions: '',
    });
    setMedicationLines([]);
    setChiefComplaints([]);
    setDiagnoses([]);
    setAdviceItems([]);
    setOrderedTests([]);
    setFollowUpDate('');
    setFollowUpDays('');
    setReferral('');
    setDrugSearchQuery('');
    setExpandedLines(new Set());
    setCustomDurationLines(new Set());
    setPharmacyQuery('');
    setPharmacySuggestions([]);
    setShowPharmacySuggestions(false);
    setCustomAdvice('');
    setAdviceOptions([]);
    setAdviceLookupFallback([]);
    setCustomTest('');
    setClinicalChartTestOptions([]);
  }, [id, user]);

  const commitAdviceLine = useCallback((raw: string) => {
    const canon = raw.trim().replace(/\s+/g, ' ');
    if (!canon) return;
    setAdviceItems(prev => {
      if (prev.some(x => x.toLowerCase() === canon.toLowerCase())) return prev;
      return [...prev, canon];
    });
    void epAdviceService
      .ensure([canon])
      .then(() => {
        epLookupService.clearCache();
        return Promise.all([
          epAdviceService.getSuggestions({ query: '', limit: 120 }),
          epLookupService.fetchAll().then(m => m.ADVICE ?? []),
        ]);
      })
      .then(([ranked, adv]) => {
        setAdviceOptions(ranked);
        setAdviceLookupFallback(adv);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!showForm) return;
    let cancelled = false;
    void epLookupService.fetchAll().then(m => {
      if (!cancelled) setAdviceLookupFallback(m.ADVICE ?? []);
    }).catch(() => {
      if (!cancelled) setAdviceLookupFallback([]);
    });
    return () => {
      cancelled = true;
    };
  }, [showForm]);

  useEffect(() => {
    if (!showForm || !user?.id) return;
    const timer = window.setTimeout(() => {
      void epAdviceService.getSuggestions({ query: customAdvice.trim(), limit: 120 }).then(setAdviceOptions);
    }, customAdvice.trim() ? 260 : 0);
    return () => clearTimeout(timer);
  }, [showForm, user?.id, customAdvice]);

  const adviceAutocompleteOptions = useMemo(() => {
    const seen = new Set<string>();
    const out: string[] = [];
    const push = (x: string) => {
      const t = x?.trim();
      if (!t || seen.has(t)) return;
      seen.add(t);
      out.push(t);
    };
    adviceOptions.forEach(push);
    adviceLookupFallback.forEach(push);
    return filterEpAutocompleteOptions(out, dismissedSuggestions, 'advice');
  }, [adviceOptions, adviceLookupFallback, dismissedSuggestions]);

  const applyEpConfigFromStorage = useCallback(() => {
    const cfg = epConfigService.get();
    setDoctorConfig(cfg);
    setPreferredMedicines(cfg.preferredMedicineNames ?? []);
  }, []);

  /** Server-backed EP workspace (config, templates, drafts, recent Rx) — hydrate before relying on browser-only cache. */
  useEffect(() => {
    if (!currentOrganizationId || !id) return;
    let cancelled = false;
    (async () => {
      await pullEpWorkspaceFromServer();
      if (cancelled) return;
      applyEpConfigFromStorage();
      setTemplates(epTemplateService.getAll());
    })();
    return () => {
      cancelled = true;
    };
  }, [currentOrganizationId, id, applyEpConfigFromStorage]);

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

  // ── Auto-save (EP-8) ──
  const autoSaveRef = useRef<NodeJS.Timeout | null>(null);
  const DRAFT_KEY = `ep_draft_${id}`;

  useEffect(() => {
    if (!showForm || !id) return;
    if (autoSaveRef.current) clearTimeout(autoSaveRef.current);
    autoSaveRef.current = setTimeout(() => {
      localStorage.setItem(DRAFT_KEY, JSON.stringify({
        formData, medicationLines, chiefComplaints, clinicalFindings, diagnoses,
        adviceItems, orderedTests, followUpDate, followUpDays, referral,
      }));
      setHasDraft(true);
      setLastDraftSavedAt(Date.now());
      scheduleEpWorkspacePush();
    }, 1500);
    return () => { if (autoSaveRef.current) clearTimeout(autoSaveRef.current); };
  }, [formData, medicationLines, chiefComplaints, clinicalFindings, diagnoses, adviceItems, orderedTests, followUpDate, followUpDays, referral, showForm, id]);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const m = params.get('epMode');
    if (m === 'IPD' || m === 'OPD') {
      setEpSessionMode(m);
    } else {
      const d = epConfigService.get().epEncounterModeDefault;
      if (d === 'IPD' || d === 'OPD') setEpSessionMode(d);
    }
  }, [location.search, id]);

  useEffect(() => {
    if (!showForm) {
      rxSessionStartRef.current = null;
      setRxElapsedSec(0);
      return;
    }
    rxSessionStartRef.current = Date.now();
    const tick = () => {
      const start = rxSessionStartRef.current;
      if (start) setRxElapsedSec(Math.floor((Date.now() - start) / 1000));
    };
    tick();
    const iv = window.setInterval(tick, 1000);
    return () => window.clearInterval(iv);
  }, [showForm, id]);

  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (showForm && lastDraftSavedAt !== null) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [showForm, lastDraftSavedAt]);

  useEffect(() => {
    if (showForm && quickMode) {
      setShowRxListWhilePrescribing(false);
    } else {
      setShowRxListWhilePrescribing(true);
    }
  }, [showForm, quickMode]);

  // ── ICD-10 search: API (patient-scoped endpoint) + local fallback + merge (EP-4) ──
  useEffect(() => {
    const q = icd10Query.trim();
    if (!q) {
      setIcd10Results([]);
      setIcd10RemoteLoading(false);
      return;
    }
    const qLower = q.toLowerCase();
    const localHits = COMMON_ICD10_CODES.filter(
      c => c.code.toLowerCase().includes(qLower) || c.description.toLowerCase().includes(qLower)
    ).slice(0, 12);

    let cancelled = false;
    const run = async () => {
      if (id && q.length >= 2) {
        setIcd10RemoteLoading(true);
        try {
          const r = await hospitalService.searchIcd10Codes(id, q, 20);
          if (cancelled) return;
          const seen = new Set<string>();
          const merged: ICD10Code[] = [];
          for (const row of r.data) {
            const item: ICD10Code = { code: row.code, description: row.description, category: 'ICD-10' };
            if (!seen.has(item.code)) {
              seen.add(item.code);
              merged.push(item);
            }
          }
          for (const row of localHits) {
            if (!seen.has(row.code)) {
              seen.add(row.code);
              merged.push(row);
            }
          }
          setIcd10Results(merged.slice(0, 24));
        } catch {
          if (!cancelled) setIcd10Results(localHits);
        } finally {
          if (!cancelled) setIcd10RemoteLoading(false);
        }
      } else {
        setIcd10Results(localHits);
        setIcd10RemoteLoading(false);
      }
      if (!cancelled) setShowIcd10(true);
    };
    const t = setTimeout(run, 280);
    return () => {
      cancelled = true;
      clearTimeout(t);
    };
  }, [icd10Query, id]);

  useEffect(() => {
    if (showForm && id) {
      setEpPrescriptionReturnPath(`${location.pathname}${location.search}`);
    }
  }, [showForm, id, location.pathname, location.search]);

  // Load on mount
  useEffect(() => {
    if (id) {
      loadPatientData();
      loadPrescriptions();
      hospitalService
        .getPastMedicalHistory(id)
        .then((r) =>
          setMedicalHistoryItems(
            (r.data || []).filter((mh) => mh.status === 'ACTIVE' || mh.status === 'CHRONIC')
          )
        )
        .catch(() => setMedicalHistoryItems([]));
      const cfg = epConfigService.get();
      setDoctorConfig(cfg);
      setTemplates(epTemplateService.getAll());
      const draft = localStorage.getItem(`ep_draft_${id}`);
      if (draft) setHasDraft(true);
      // Apply admin config
      if (cfg.defaultPharmacy) {
        setFormData(prev => ({ ...prev, pharmacyName: prev.pharmacyName || cfg.defaultPharmacy || '' }));
      }
      setPreferredMedicines(cfg.preferredMedicineNames ?? []);
    }
  }, [id]);

  /** Reload EP settings when route changes (e.g. back from Rx Settings); does not touch form fields. */
  useEffect(() => {
    applyEpConfigFromStorage();
  }, [location.pathname, id, applyEpConfigFromStorage]);

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key === EP_DOCTOR_CONFIG_STORAGE_KEY && e.newValue) {
        try {
          const cfg = JSON.parse(e.newValue) as DoctorEPConfig;
          setDoctorConfig(cfg);
          setPreferredMedicines(cfg.preferredMedicineNames ?? []);
        } catch {
          applyEpConfigFromStorage();
        }
      }
    };
    const onSameTabSave = () => applyEpConfigFromStorage();
    window.addEventListener('storage', onStorage);
    window.addEventListener(EP_DOCTOR_CONFIG_UPDATED_EVENT, onSameTabSave);
    return () => {
      window.removeEventListener('storage', onStorage);
      window.removeEventListener(EP_DOCTOR_CONFIG_UPDATED_EVENT, onSameTabSave);
    };
  }, [applyEpConfigFromStorage]);

  useEffect(() => {
    const onVis = () => {
      if (document.visibilityState === 'visible') applyEpConfigFromStorage();
    };
    document.addEventListener('visibilitychange', onVis);
    return () => document.removeEventListener('visibilitychange', onVis);
  }, [applyEpConfigFromStorage]);

  /**
   * EP-2 / EP-3 deep links from Doctor Dashboard:
   * - ?epQuick=1 — one-screen Quick Mode
   * - ?epNew=1 — open a fresh prescription form (not modal; inline layout)
   * - ?applyTemplate=id — apply template (optional with epNew / epQuick)
   */
  useEffect(() => {
    if (!id) return;
    const params = new URLSearchParams(location.search);
    if (params.toString() === '') return;

    const tid = params.get('applyTemplate');
    const epQuick = params.get('epQuick') === '1';
    const epNew = params.get('epNew') === '1';

    if (epNew && canPrescribeRx) {
      resetForm();
      setEditing(null);
      setShowForm(true);
    }

    if (tid) {
      const t = epTemplateService.getAll().find(x => x.templateId === tid);
      if (t) {
        applyTemplate(t);
        setShowForm(true);
        setQuickMode(true);
        try {
          localStorage.setItem('ep_quick_mode_default', 'true');
        } catch {
          // ignore
        }
      } else {
        alert('Prescription template not found. It may have been removed from this device.');
      }
    }

    if (epQuick) {
      setQuickMode(true);
      try {
        localStorage.setItem('ep_quick_mode_default', 'true');
      } catch {
        // ignore
      }
    }

    navigate({ pathname: location.pathname, search: '' }, { replace: true });
  }, [id, location.search, location.pathname, navigate, canPrescribeRx, resetForm]);

  useEffect(() => { if (id) loadPrescriptions(); }, [filterStatus]);

  const loadPatientData = async () => {
    if (!id) return;
    try {
      const r = await hospitalService.getPatient(id);
      setPatient(r.data);
    } catch {}
  };

  const loadPrescriptions = async () => {
    if (!id) return;
    try {
      setLoading(true);
      let response;
      if (filterStatus === 'active') response = await hospitalService.getActivePrescriptions(id);
      else if (filterStatus === 'drafts') response = await hospitalService.getDraftPrescriptions(id);
      else response = await hospitalService.getPrescriptions(id);
      setPrescriptions(response.data);
      const latestVitalsRes = await hospitalService.getLatestVitalSigns(id).catch(() => ({ data: null }));
      setLatestVitalSigns(latestVitalsRes.data ?? null);
    } catch (err: any) {
      const status = err.response?.status;
      setError(
        status === 403
          ? 'Not authorized to view prescriptions. Ask admin to assign HOSPITAL_PRESCRIPTION_VIEW.'
          : ehrApiErrorMessage(err, 'Failed to load prescriptions')
      );
    } finally {
      setLoading(false);
    }
  };

  // ── Drug search ──
  const debouncedSearchDrugs = useCallback((() => {
    let t: NodeJS.Timeout | null = null;
    return (q: string) => {
      if (t) clearTimeout(t);
      if (!q || q.length < 2) {
        setDrugSuggestions([]);
        setShowDrugSuggestions(false);
        return;
      }
      t = setTimeout(async () => {
        try {
          setDrugSearchLoading(true);
          setDrugSearchFailed(false);
          setShowDrugSuggestions(true);
          const r = await hospitalService.searchDrugsForPrescription(q, 0, 10);
          setDrugSuggestions(r.data.content);
        } catch (err) {
          setDrugSuggestions([]);
          setDrugSearchFailed(true);
          console.warn('Drug catalog search failed', err);
        } finally {
          setDrugSearchLoading(false);
        }
      }, 300);
    };
  })(), []);

  // ── FR-P3.5: Pharmacy directory search handler ──
  const handlePharmacyQueryChange = useCallback((q: string) => {
    setPharmacyQuery(q);
    if (pharmacySearchRef.current) clearTimeout(pharmacySearchRef.current);
    if (!q || q.length < 2) { setPharmacySuggestions([]); setShowPharmacySuggestions(false); return; }
    pharmacySearchRef.current = setTimeout(async () => {
      try {
        setPharmacySearchLoading(true);
        const r = await hospitalService.searchPharmacyDirectory(q, undefined, false);
        setPharmacySuggestions(r.data.slice(0, 10));
        setShowPharmacySuggestions(true);
      } catch { setPharmacySuggestions([]); } finally { setPharmacySearchLoading(false); }
    }, 300);
  }, []);

  const handleSelectPharmacy = useCallback((entry: PharmacyDirectoryEntry) => {
    setFormData(prev => ({
      ...prev,
      pharmacyName: entry.name,
      pharmacyNpi: digitsOnly(entry.npi),
      pharmacyPhone: digitsOnly(entry.phone),
    }));
    setPharmacyQuery(entry.name);
    setPharmacySuggestions([]);
    setShowPharmacySuggestions(false);
  }, []);

  const handleAddDrugFromSearch = (drug: PharmacyDrugSuggestion) => {
    const strengthParts = drug.strength ? drug.strength.split(' ') : [];
    const line: PrescriptionMedicationItem = ensureSuppositoryInstructionsOnMedicationLines([
      {
        ...createDefaultMedicationLine(),
        medicationName: formatDrugSuggestionLabel(drug),
        medicationCode: undefined,
        dosageStrength: drug.strength ? parseFloat(strengthParts[0]) : undefined,
        dosageUnit: strengthParts.length > 1 ? strengthParts[1] || '' : '',
        dosageForm: drug.form ? mapDrugForm(drug.form) : 'TABLET',
        route: (drug.route as PrescriptionMedicationItem['route']) || 'ORAL',
      },
    ])[0];
    setMedicationLines(prev => [...prev, line]);
    setDrugSearchQuery('');
    setShowDrugSuggestions(false);
    setDrugSuggestions([]);
  };

  const openAddCatalogDrugModal = useCallback(async () => {
    setShowDrugSuggestions(false);
    setAddCatalogDrugForm({ ...INITIAL_ADD_CATALOG_DRUG_FORM, genericName: drugSearchQuery.trim() });
    setAddCatalogDrugFormSelect('');
    setShowAddCatalogDrugModal(true);
    setAddCatalogManufacturersLoading(true);
    try {
      const list = await hospitalPharmacyService.getManufacturers({ activeOnly: true });
      setAddCatalogManufacturers(list);
      if (list.length === 0) {
        alert('No manufacturers found. Add a manufacturer in Pharmacy Catalog before creating a new drug.');
      }
    } catch (err) {
      setAddCatalogManufacturers([]);
      setShowAddCatalogDrugModal(false);
      alert(
        ehrApiErrorMessage(
          err,
          'Could not load manufacturers. Ensure hospital-pharmacy-service is running and Pharmacy Catalog has at least one manufacturer.',
        ),
      );
    } finally {
      setAddCatalogManufacturersLoading(false);
    }
  }, [drugSearchQuery]);

  const closeAddCatalogDrugModal = useCallback(() => {
    setShowAddCatalogDrugModal(false);
    setAddCatalogDrugForm({ ...INITIAL_ADD_CATALOG_DRUG_FORM });
    setAddCatalogDrugFormSelect('');
    setAddCatalogDrugSaving(false);
  }, []);

  async function saveAddCatalogDrug() {
    if (!addCatalogDrugForm.genericName?.trim()) {
      alert('Generic name is required.');
      return;
    }
    if (!addCatalogDrugForm.manufacturerId?.trim()) {
      alert(
        addCatalogManufacturers.length === 0
          ? 'No manufacturers available. Add a manufacturer in Pharmacy Catalog first.'
          : 'Select a manufacturer from the list.',
      );
      return;
    }
    setAddCatalogDrugSaving(true);
    try {
      const created = await hospitalPharmacyService.createDrug(addCatalogDrugForm);
      handleAddDrugFromSearch(drugEntityToPharmacySuggestion(created));
      closeAddCatalogDrugModal();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to save drug to catalog'));
    } finally {
      setAddCatalogDrugSaving(false);
    }
  }

  const updateMedicationLine = (idx: number, patch: Partial<PrescriptionMedicationItem>) => {
    setMedicationLines(prev =>
      prev.map((line, i) =>
        i !== idx ? line : { ...line, ...mergeMedicationLinePatchWithSuppositoryInstruction(line, patch) },
      ),
    );
  };

  const removeMedicationLine = (idx: number) => {
    if (medicationLines.length <= 1) return;
    setMedicationLines(prev => prev.filter((_, i) => i !== idx));
    setExpandedLines(prev => {
      if (!prev.size) return prev;
      const next = new Set<number>();
      prev.forEach(i => {
        if (i === idx) return;
        next.add(i < idx ? i : i - 1);
      });
      return next;
    });
    setCustomDurationLines(prev => {
      if (!prev.size) return prev;
      const next = new Set<number>();
      prev.forEach(i => {
        if (i === idx) return;
        next.add(i < idx ? i : i - 1);
      });
      return next;
    });
  };

  // ── ICD-10 helpers ──
  const toggleFavIcd10 = (code: string, description: string) => {
    setFavoriteIcd10Entries(prev => {
      const exists = prev.some(e => e.code === code);
      const next = exists ? prev.filter(e => e.code !== code) : [...prev, { code, description }];
      saveFavoriteIcd10Entries(next);
      return next;
    });
  };

  const addDiagnosis = (code: ICD10Code, primary: boolean) => {
    const codeNorm = code.code.trim().toUpperCase();
    if (diagnoses.some(d => d.code.trim().toUpperCase() === codeNorm)) {
      setDiagnosisError(`ICD-10 code ${code.code} is already on this prescription.`);
      return;
    }
    setDiagnosisError(null);
    setDiagnoses(prev => [
      ...prev.map(d => primary ? { ...d, isPrimary: false } : d),
      { code: code.code, description: code.description, isPrimary: primary },
    ]);
    setIcd10Query('');
    setShowIcd10(false);
  };

  const addCustomDiagnosis = (diseaseName: string) => {
    const name = diseaseName.trim();
    if (!name) return;
    if (diagnoses.some(d => d.description.trim().toLowerCase() === name.toLowerCase())) {
      setIcd10Query('');
      setShowIcd10(false);
      return;
    }
    const existingCustom = diagnoses
      .map(d => d.code)
      .filter(code => /^CUSTOM-\d{1,3}$/i.test(code))
      .map(code => parseInt(code.split('-')[1] || '0', 10))
      .filter(n => Number.isFinite(n));
    const nextCustomNumber = (existingCustom.length ? Math.max(...existingCustom) : 0) + 1;
    const customCode = `CUSTOM-${nextCustomNumber}`.slice(0, 20);
    const primary = diagnoses.length === 0;
    setDiagnoses(prev => [
      ...prev.map(d => primary ? { ...d, isPrimary: false } : d),
      { code: customCode, description: name, isPrimary: primary },
    ]);
    setIcd10Query('');
    setShowIcd10(false);
  };

  // ── Template apply (EP-7) ──
  const applyTemplate = (t: PrescriptionTemplate) => {
    if (t.complaints) setChiefComplaints(t.complaints);
    setClinicalFindings(t.clinicalFindings?.trim() || '');
    if (t.diagnoses) setDiagnoses(t.diagnoses);
    if (t.medications && t.medications.length > 0) {
      const today = new Date().toISOString().split('T')[0];
      setMedicationLines(
        ensureMedicationLineKeys(
          ensureSuppositoryInstructionsOnMedicationLines(t.medications.map(m => ({ ...m, startDate: today }))),
        ),
      );
    }
    if (t.advice) setAdviceItems(t.advice);
    if (t.tests) {
      setOrderedTests(
        dedupeTestEntries(
          t.tests.map((x) => ({
            testName: x.testName,
            isPanel: x.isPanel ?? false,
            panelName: x.panelName,
          })),
        ),
      );
    }
    if (t.followUpDays) {
      setFollowUpDays(t.followUpDays);
      const d = new Date();
      d.setDate(d.getDate() + t.followUpDays);
      setFollowUpDate(d.toISOString().split('T')[0]);
    }
    if (t.referral) setReferral(t.referral);
    setShowTemplateDialog(false);
    setTemplateApplySearch('');
  };

  const copyPreviousRx = (rx: Prescription) => {
    const meds = rx.medications && rx.medications.length > 0
      ? rx.medications
      : [{ medicationName: rx.medicationName, medicationCode: rx.medicationCode, medicationCodeType: rx.medicationCodeType, dosageStrength: rx.dosageStrength, dosageUnit: rx.dosageUnit, dosageForm: rx.dosageForm, route: rx.route, frequency: rx.frequency, instructions: rx.instructions, startDate: new Date().toISOString().split('T')[0], durationDays: rx.durationDays, refillsAuthorized: 0, refillsRemaining: 0, substitutionAllowed: rx.substitutionAllowed !== false, isControlledSubstance: rx.isControlledSubstance || false }] as PrescriptionMedicationItem[];
    const today = new Date().toISOString().split('T')[0];
    setMedicationLines(
      ensureMedicationLineKeys(
        ensureSuppositoryInstructionsOnMedicationLines(meds.map(m => ({ ...m, startDate: today }))),
      ),
    );

    const parsed = parseStructuredClinicalFromPrescription(rx);
    if (parsed.chiefComplaints.length > 0) setChiefComplaints(parsed.chiefComplaints);
    setClinicalFindings(parsed.clinicalFindings || '');
    if (parsed.diagnoses.length > 0) setDiagnoses(parsed.diagnoses);
    if (parsed.adviceItems.length > 0) setAdviceItems(parsed.adviceItems);
    if (parsed.orderedTests.length > 0) setOrderedTests(dedupeTestEntries(parsed.orderedTests));
    if (parsed.followUpDate) {
      setFollowUpDate(parsed.followUpDate);
      setFollowUpDays('');
    }
    if (parsed.referral) setReferral(parsed.referral);
    setFormData(prev => ({ ...prev, notes: parsed.freeNotes }));

    setShowCopyPrevious(false);
  };

  const saveAsTemplate = () => {
    if (!newTemplateName.trim()) { alert('Enter a template name'); return; }
    const kind = newTemplateKind;
    const t: PrescriptionTemplate = {
      templateId: `doc-${Date.now()}`,
      templateName: newTemplateName.trim(),
      templateType: kind,
      diseaseCategory: kind === 'DISEASE' && newTemplateDiseaseCategory.trim() ? newTemplateDiseaseCategory.trim() : undefined,
      complaints: chiefComplaints,
      clinicalFindings: clinicalFindings.trim() || undefined,
      diagnoses,
      medications: medicationLines.map(({ clientRowId: _rk, ...m }) => ({ ...m })),
      advice: adviceItems,
      tests: dedupeTestEntries(orderedTests),
      followUpDays: typeof followUpDays === 'number' ? followUpDays : undefined,
      referral: referral || undefined,
      createdAt: new Date().toISOString(),
      createdBy: user?.id,
      departmentId: linkedDoctor?.departmentId,
      departmentName: linkedDoctor?.departmentName,
    };
    epTemplateService.save(t);
    setTemplates(epTemplateService.getAll());
    setNewTemplateName('');
    setNewTemplateDiseaseCategory('');
    setNewTemplateKind('DOCTOR');
    setShowSaveTemplate(false);
    if (adviceItems.length > 0 && user?.id) {
      void epAdviceService.recordUsage(adviceItems).catch(() => {});
    }
    alert('Template saved!');
  };

  /** Persist prescription; returns saved entity for print, or null on validation/API failure. */
  const performPrescriptionSave = async (options: { closeForm: boolean }): Promise<Prescription | null> => {
    if (!id) return null;
    if (medicationLines.length === 0) {
      alert('Add at least one medicine.');
      return null;
    }
    for (const line of medicationLines) {
      if (!line.medicationName?.trim()) {
        alert('Each medicine must have a name.');
        return null;
      }
      if (!line.frequency?.trim()) {
        alert('Each medicine must have a frequency (e.g. 1-0-1 or BID).');
        return null;
      }
      if (line.durationDays == null || line.durationDays <= 0) {
        alert('Each medicine must have a duration greater than zero days.');
        return null;
      }
      const unit = (line.dosageUnit ?? '').trim();
      if (!unit) {
        alert('Each medicine must have a unit (e.g. mg, ml, tablet).');
        return null;
      }
      if (line.dosageStrength != null && line.dosageStrength <= 0) {
        alert('Medicine strength must be greater than zero when specified.');
        return null;
      }
    }

    const todayIso = prescriptionTodayIso();
    if (followUpDate && followUpDate < todayIso) {
      alert('Follow-up date cannot be in the past.');
      return null;
    }

    const payload: PrescriptionRequest = buildPrescriptionPayload();
    if (payload.medications?.length) {
      setMedicationLines(prev =>
        ensureMedicationLineKeys(
          payload.medications!.map((m, i) => ({
            ...(m as PrescriptionMedicationItem),
            clientRowId: prev[i]?.clientRowId,
          })),
        ),
      );
    }

    try {
      let savedRx: Prescription;
      if (editing) {
        await hospitalService.updatePrescription(editing.prescriptionId, {
          patientId: id!,
          encounterId: payload.encounterId,
          prescriptionType: payload.prescriptionType,
          epEncounterMode: payload.epEncounterMode,
          medications: payload.medications,
          pharmacyId: payload.pharmacyId,
          pharmacyName: payload.pharmacyName,
          pharmacyNpi: payload.pharmacyNpi,
          pharmacyPhone: payload.pharmacyPhone,
          prescribingProviderId: payload.prescribingProviderId,
          prescribingProviderNpi: payload.prescribingProviderNpi,
          prescribingProviderName: payload.prescribingProviderName,
          notes: payload.notes,
          specialInstructions: payload.specialInstructions,
          diagnoses: payload.diagnoses,
        });
        const refreshed = await hospitalService.getPrescription(editing.prescriptionId);
        savedRx = refreshed.data;
        if (id) {
          epRecentRxService.push({
            prescriptionId: savedRx.prescriptionId,
            patientId: savedRx.patientId || patient?.patientId || id,
            patientName: patient?.fullName || '',
            prescriptionNumber: savedRx.prescriptionNumber,
            medicationSummary:
              medicationLines
                .map(m => m.medicationName)
                .filter(Boolean)
                .slice(0, 4)
                .join(', ') || '—',
            createdAt: new Date().toISOString(),
          });
        }
      } else {
        const res = await hospitalService.createPrescription(payload);
        savedRx = res.data;
        if (id) {
          epRecentRxService.push({
            prescriptionId: res.data.prescriptionId,
            patientId: res.data.patientId || patient?.patientId || id,
            patientName: patient?.fullName || '',
            prescriptionNumber: res.data.prescriptionNumber,
            medicationSummary:
              medicationLines
                .map(m => m.medicationName)
                .filter(Boolean)
                .slice(0, 4)
                .join(', ') || '—',
            createdAt: new Date().toISOString(),
          });
        }
      }
      localStorage.removeItem(DRAFT_KEY);
      markPatientPrescribedToday(id);
      setHasDraft(false);
      loadPrescriptions();

      if (options.closeForm && adviceItems.length > 0 && user?.id) {
        void epAdviceService.recordUsage(adviceItems).catch(() => {});
      }

      // OPD queue integration: after the Rx is saved, mark the linked scheduling appointment as COMPLETED.
      if (options.closeForm && schedulingAppointmentId && payload.epEncounterMode === 'OPD') {
        try {
          await hospitalSchedulingService.completeAppointment(schedulingAppointmentId);
          window.dispatchEvent(new Event('ep-queue-updated'));
        } catch (e) {
          console.error('Failed to mark scheduling appointment completed', e);
          // Don't block prescription flow on scheduling-service issues.
        }
      }
      if (options.closeForm) {
        setShowForm(false);
        setEditing(null);
        resetForm();
      }
      return savedRx;
    } catch (err: any) {
      if (err.response?.status === 403) {
        alert('Not authorized to create/edit prescriptions. Ask admin to assign HOSPITAL_PRESCRIPTION_PRESCRIBE.');
        return null;
      }
      alert(prescriptionSaveErrorMessage(err, 'Failed to save prescription'));
      return null;
    }
  };

  const handleFollowUpDateChange = (value: string) => {
    const todayIso = prescriptionTodayIso();
    if (value && value < todayIso) {
      alert('Follow-up date cannot be in the past.');
      return;
    }
    setFollowUpDate(value);
    setFollowUpDays('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await performPrescriptionSave({ closeForm: true });
  };

  /** Build PrintData from a saved Prescription entity (parses back the structured notes). */
  const buildPrintDataFromRx = (rx: Prescription): PrintData => {
    const rawMeds: PrescriptionMedicationItem[] = rx.medications && rx.medications.length > 0
      ? rx.medications
      : [{ medicationName: rx.medicationName, dosageStrength: rx.dosageStrength, dosageUnit: rx.dosageUnit, dosageForm: rx.dosageForm, route: rx.route, frequency: rx.frequency, instructions: rx.instructions, startDate: rx.startDate, durationDays: rx.durationDays, refillsAuthorized: rx.refillsAuthorized, refillsRemaining: rx.refillsRemaining }];
    const meds = ensureSuppositoryInstructionsOnMedicationLines(rawMeds);

    const parsed = parseStructuredClinicalFromPrescription(rx);

    return {
      sourcePrescription: rx,
      patient,
      doctorConfig,
      prescribingProviderName: rx.prescribingProviderName || formData.prescribingProviderName,
      chiefComplaints: parsed.chiefComplaints,
      clinicalFindings: parsed.clinicalFindings,
      medicalHistory: medicalHistoryWithValues(medicalHistoryItems),
      diagnoses: parsed.diagnoses,
      medicationLines: ensureMedicationLineKeys(meds),
      adviceItems: parsed.adviceItems,
      orderedTests: parsed.orderedTests,
      followUpDate: parsed.followUpDate,
      referral: parsed.referral,
      notes: parsed.freeNotes,
      prescriptionNumber: rx.prescriptionNumber,
      printFormat: doctorConfig.printFormat,
      latestVitalSigns,
    };
  };

  const handlePreview = async () => {
    const meds = medicationLines.filter(m => m.medicationName?.trim());
    if (meds.length === 0) {
      alert('Add at least one medicine before preview.');
      return;
    }
    const savedRx = await performPrescriptionSave({ closeForm: false });
    if (!savedRx) return;
    // Preview already persists once so Rx# is visible immediately.
    // Avoid a second duplicate save when "Save & Print" is clicked.
    setPrintData(buildPrintDataFromRx(savedRx));
    setShowPrint(true);
  };

  const restoreDraft = () => {
    try {
      const raw = localStorage.getItem(DRAFT_KEY);
      if (!raw) return;
      const d = JSON.parse(raw);
      if (d.formData) setFormData(d.formData);
      if (d.medicationLines) {
        setMedicationLines(ensureMedicationLineKeys(d.medicationLines));
      }
      if (d.chiefComplaints) setChiefComplaints(d.chiefComplaints);
      if (d.clinicalFindings) setClinicalFindings(d.clinicalFindings);
      if (d.diagnoses) setDiagnoses(d.diagnoses);
      if (d.adviceItems) setAdviceItems(d.adviceItems);
      if (d.orderedTests) setOrderedTests(dedupeTestEntries(d.orderedTests));
      if (d.followUpDate) setFollowUpDate(d.followUpDate);
      if (d.followUpDays !== undefined && d.followUpDays !== '') setFollowUpDays(d.followUpDays);
      if (d.referral) setReferral(d.referral);
      setShowForm(true);
    } catch {}
  };

  const handleEdit = (prescription: Prescription) => {
    if (prescription.prescriptionStatus !== 'DRAFT') {
      alert('Only DRAFT prescriptions can be edited.');
      return;
    }
    setEditing(prescription);
    if (prescription.epEncounterMode === 'OPD' || prescription.epEncounterMode === 'IPD') {
      setEpSessionModeAndUrl(prescription.epEncounterMode);
    }
    const parsed = parseStructuredClinicalFromPrescription(prescription);
    setFormData({
      patientId: prescription.patientId,
      prescriptionType: (prescription.prescriptionType || 'ELECTRONIC') as 'ELECTRONIC',
      pharmacyName: prescription.pharmacyName || '',
      pharmacyNpi: digitsOnly(prescription.pharmacyNpi),
      pharmacyPhone: digitsOnly(prescription.pharmacyPhone),
      prescribingProviderId: prescription.prescribingProviderId,
      prescribingProviderNpi: prescription.prescribingProviderNpi || '',
      prescribingProviderName: prescription.prescribingProviderName || '',
      notes: parsed.freeNotes,
      specialInstructions: prescription.specialInstructions || '',
    });
    const meds = prescription.medications && prescription.medications.length > 0
      ? prescription.medications
      : [{ medicationName: prescription.medicationName, medicationCode: prescription.medicationCode, medicationCodeType: prescription.medicationCodeType, dosageStrength: prescription.dosageStrength, dosageUnit: prescription.dosageUnit, dosageForm: prescription.dosageForm, route: prescription.route, frequency: prescription.frequency, instructions: prescription.instructions, startDate: prescription.startDate, endDate: prescription.endDate, durationDays: prescription.durationDays, refillsAuthorized: prescription.refillsAuthorized ?? 0, refillsRemaining: prescription.refillsRemaining ?? 0, substitutionAllowed: prescription.substitutionAllowed !== false, isControlledSubstance: prescription.isControlledSubstance || false, schedule: prescription.schedule, deaNumber: prescription.deaNumber }] as PrescriptionMedicationItem[];
    setMedicationLines(ensureMedicationLineKeys(meds.map(m => ({ ...m, endDate: m.endDate || '' }))));
    setPharmacyQuery(prescription.pharmacyName || '');
    setChiefComplaints(parsed.chiefComplaints);
    setClinicalFindings(parsed.clinicalFindings || '');
    // parseStructuredClinicalFromPrescription already handles the fallback to
    // normalized diagnoses array and legacy single diagnosisCode
    setDiagnoses(parsed.diagnoses);
    setAdviceItems(parsed.adviceItems);
    setOrderedTests(dedupeTestEntries(parsed.orderedTests));
    setFollowUpDate(parsed.followUpDate);
    setReferral(parsed.referral);
    setShowForm(true);
  };

  const handlePrintSavedRx = async (prescriptionId: string) => {
    try {
      const r = await hospitalService.getPrescription(prescriptionId);
      setPrintData(buildPrintDataFromRx(r.data));
      setShowPrint(true);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load prescription for printing'));
    }
  };

  useEffect(() => {
    const pendingPrintId = (location.state as { printPrescriptionId?: string } | null)?.printPrescriptionId;
    if (!pendingPrintId) return;
    handlePrintSavedRx(pendingPrintId);
    navigate(location.pathname, { replace: true, state: null });
  }, [location.state, location.pathname]);

  const handleDelete = async (prescriptionId: string) => {
    if (!window.confirm('Delete this prescription?')) return;
    try {
      await hospitalService.deletePrescription(prescriptionId);
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to delete'));
    }
  };

  const handleCheckInteractions = async () => {
    /** Index 0 = most recently added (list is newest-first). Sibling line codes catch duplicate RxNorm/NDC in the same draft. */
    const first = medicationLines[0];
    if (!id || !first?.medicationName?.trim()) { alert('Enter at least one medication first'); return; }
    const siblingCodes = Array.from(
      new Set(
        medicationLines
          .slice(1)
          .map(m => m.medicationCode?.trim())
          .filter((c): c is string => Boolean(c))
      )
    );
    try {
      const payload: DrugInteractionCheckRequest = {
        medicationName: first.medicationName,
        medicationCode: first.medicationCode,
        existingMedicationCodes: siblingCodes.length > 0 ? siblingCodes : undefined,
        doseStrengthMg: first.dosageStrength,
        doseUnit: first.dosageUnit,
      };
      const w = interactionScreeningContext.weightKg.trim();
      if (w !== '' && !Number.isNaN(Number(w))) payload.weightKg = Number(w);
      const eg = interactionScreeningContext.egfrMlMin.trim();
      if (eg !== '' && !Number.isNaN(Number(eg))) payload.egfrMlMin = Number(eg);
      const cr = interactionScreeningContext.serumCreatinineMgDl.trim();
      if (cr !== '' && !Number.isNaN(Number(cr))) payload.serumCreatinineMgDl = Number(cr);
      if (interactionScreeningContext.pregnancyStatus) {
        payload.pregnancyStatus = interactionScreeningContext.pregnancyStatus;
      }
      if (interactionScreeningContext.lactating) {
        payload.lactating = true;
      }
      const r = await hospitalService.checkDrugInteractions(id, payload);
      setInteractionCheck(r.data);
      setShowInteractionCheck(true);
    } catch (err: any) { alert(ehrApiErrorMessage(err, 'Failed to check interactions')); }
  };

  const handleCheckAllergies = async () => {
    const first = medicationLines[0];
    if (!id || !first?.medicationName?.trim()) { alert('Enter at least one medication first'); return; }
    try {
      const r = await hospitalService.checkAllergies({ patientId: id, medicationName: first.medicationName, medicationCode: first.medicationCode });
      setAllergyCheck(r.data);
      setShowAllergyCheck(true);
    } catch (err: any) { alert(ehrApiErrorMessage(err, 'Failed to check allergies')); }
  };

  const handleValidate = async (prescriptionId: string) => {
    try {
      await hospitalService.validatePrescription(prescriptionId);
      loadPrescriptions();
    } catch (err: any) { alert(ehrApiErrorMessage(err, 'Failed to validate')); }
  };

  const handleTransmit = async () => {
    if (!prescriptionToTransmit) return;
    // Pharmacy (pharmacyId or pharmacyNpi) is required by the backend
    const hasPharmacy =
      prescriptionToTransmit.pharmacyId ||
      prescriptionToTransmit.pharmacyNpi ||
      transmitData.pharmacyId ||
      transmitData.pharmacyNpi;
    if (!hasPharmacy) {
      alert('Please enter a Pharmacy NPI before transmitting.');
      return;
    }
    try {
      // Never send a partial PUT update right before transmit; it can overwrite
      // existing medication lines and trigger "At least one medication is required".
      // Instead, send transmit request with pharmacy fields merged from Rx + dialog.
      const mergedTransmitData: PrescriptionTransmitRequest = {
        ...transmitData,
        pharmacyId: transmitData.pharmacyId || prescriptionToTransmit.pharmacyId || undefined,
        pharmacyNpi:
          digitsOnly(transmitData.pharmacyNpi || '') ||
          digitsOnly(prescriptionToTransmit.pharmacyNpi || '') ||
          undefined,
        pharmacyName: transmitData.pharmacyName || prescriptionToTransmit.pharmacyName || undefined,
      };
      await hospitalService.transmitPrescription(
        prescriptionToTransmit.prescriptionId,
        mergedTransmitData
      );
      setShowTransmitDialog(false);
      setPrescriptionToTransmit(null);
      setTransmitData({ overrideInteractions: false, overrideAllergies: false, overrideReason: '', overridePdmpCheck: false });
      setShowInlinePdmp(false);
      setInlinePdmpResult(null);
      loadPrescriptions();
    } catch (err: any) {
      if (err.response?.status === 403) { alert('Not authorized to transmit. Ask admin to assign HOSPITAL_PRESCRIPTION_TRANSMIT.'); return; }
      alert(ehrApiErrorMessage(err, 'Failed to transmit'));
    }
  };

  const handleCancelConfirm = async () => {
    if (!cancelDialogId) return;
    try {
      setStatusUpdatingId(cancelDialogId);
      await hospitalService.cancelPrescription(cancelDialogId, cancelReason || 'Cancelled by provider');
      setCancelDialogId(null);
      setCancelReason('');
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to cancel'));
    } finally {
      setStatusUpdatingId(null);
    }
  };

  const handleCancel = async (prescriptionId: string) => {
    setCancelDialogId(prescriptionId);
    setCancelReason('');
  };

  const getQuickStatusOptions = (status: Prescription['prescriptionStatus']) => {
    if (status === 'DRAFT') {
      return [{ value: 'PENDING', label: 'Submit (→ Pending)' }, { value: 'CANCELLED', label: 'Cancel Rx' }];
    }
    if (status === 'PENDING') {
      return [{ value: 'CANCELLED', label: 'Cancel Rx' }];
    }
    return [];
  };

  const handleQuickStatusChange = async (
    prescriptionId: string,
    currentStatus: Prescription['prescriptionStatus'],
    nextStatus: string
  ) => {
    if (!nextStatus || nextStatus === currentStatus) return;
    // Handle non-async transitions before setting updating state
    if (nextStatus === 'CANCELLED') {
      setCancelDialogId(prescriptionId);
      setCancelReason('');
      return;
    }
    try {
      setStatusUpdatingId(prescriptionId);
      if (nextStatus === 'PENDING' && currentStatus === 'DRAFT') {
        await hospitalService.validatePrescription(prescriptionId);
      } else {
        alert(`Unsupported status transition: ${currentStatus} → ${nextStatus}`);
        return;
      }
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to update status'));
    } finally {
      setStatusUpdatingId(null);
    }
  };

  // ── PDMP handlers ──
  const handleOpenPdmp = async (rx: Prescription) => {
    setPdmpPrescription(rx);
    setPdmpQueryResult(null);
    setPdmpHistory([]);
    setPdmpQueryState('');
    setPdmpReason('');
    setPdmpDeaNumber(rx.deaNumber || '');
    setShowPdmpModal(true);
    try {
      const r = await hospitalService.getPDMPResults(rx.prescriptionId);
      setPdmpHistory(r.data || []);
      if (r.data && r.data.length > 0) setPdmpQueryResult(r.data[0]);
    } catch { /* no history yet */ }
  };

  const handleQueryPdmp = async () => {
    if (!pdmpPrescription || !id) return;
    if (!pdmpQueryState.trim()) { alert('Query state is required'); return; }
    setPdmpLoading(true);
    try {
      const req: PDMPQueryRequest = {
        patientId: id,
        queryState: pdmpQueryState,
        queryReason: pdmpReason || 'Pre-prescribing controlled substance review',
        deaNumber: pdmpDeaNumber || pdmpPrescription.deaNumber,
      };
      const r = await hospitalService.queryPDMP(
        pdmpPrescription.prescriptionId,
        req,
        pdmpPrescription.prescribingProviderNpi,
        pdmpPrescription.prescribingProviderName,
      );
      setPdmpQueryResult(r.data);
      setPdmpHistory(prev => [r.data, ...prev]);
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'PDMP query failed'));
    } finally {
      setPdmpLoading(false);
    }
  };

  /**
   * Runs a PDMP query from inside the transmit dialog without closing it.
   * On success, marks prescriptionToTransmit.pdmpQueried = true in local state
   * so the Transmit button unlocks immediately without re-opening the dialog.
   */
  const handleInlinePdmpQuery = async () => {
    if (!prescriptionToTransmit || !id) return;
    if (!pdmpQueryState.trim()) { alert('Query state is required'); return; }
    setPdmpLoading(true);
    try {
      const req: PDMPQueryRequest = {
        patientId: id,
        queryState: pdmpQueryState,
        queryReason: pdmpReason || 'Pre-prescribing controlled substance review',
        deaNumber: pdmpDeaNumber || prescriptionToTransmit.deaNumber,
      };
      const r = await hospitalService.queryPDMP(
        prescriptionToTransmit.prescriptionId,
        req,
        prescriptionToTransmit.prescribingProviderNpi,
        prescriptionToTransmit.prescribingProviderName,
      );
      setInlinePdmpResult(r.data);
      // Unlock the Transmit button without closing the dialog.
      setPrescriptionToTransmit(prev => prev ? { ...prev, pdmpQueried: true } : prev);
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'PDMP query failed'));
    } finally {
      setPdmpLoading(false);
    }
  };

  // ── Formulary handlers ──
  const handleOpenFormulary = async (rx: Prescription) => {
    setFormularyPrescription(rx);
    setFormularyResult(null);
    setShowFormularyModal(true);
    setFormularyLoading(true);
    try {
      const r = await hospitalService.getLatestFormularyCheck(rx.prescriptionId);
      setFormularyResult(r.data);
    } catch { /* no check yet */ } finally {
      setFormularyLoading(false);
    }
  };

  const handleCheckFormulary = async () => {
    if (!formularyPrescription || !id) return;
    setFormularyLoading(true);
    try {
      const req: FormularyCheckRequest = {
        patientId: id,
        prescriptionId: formularyPrescription.prescriptionId,
        medicationName: formularyPrescription.medicationName,
        medicationCode: formularyPrescription.medicationCode,
        includeAlternatives: true,
        estimateCosts: true,
      };
      const r = await hospitalService.checkFormulary(formularyPrescription.prescriptionId, {
        ...req,
        prescriptionId: formularyPrescription.prescriptionId,
      });
      setFormularyResult(r.data);
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Formulary check failed'));
    } finally {
      setFormularyLoading(false);
    }
  };

  // ── Prior Auth handlers ──
  const handleOpenPa = async (rx: Prescription) => {
    setPaPrescription(rx);
    setPaList([]);
    setPaJustification('');
    setPaDocs('');
    setShowPaModal(true);
    setPaLoading(true);
    try {
      const r = await hospitalService.getPriorAuthorizations(rx.prescriptionId);
      setPaList(r.data || []);
    } catch { /* no PAs yet */ } finally {
      setPaLoading(false);
    }
  };

  const handleSubmitPa = async () => {
    if (!paPrescription) return;
    if (!paJustification.trim()) { alert('Clinical justification is required'); return; }
    setPaLoading(true);
    try {
      const req: PriorAuthorizationRequest = {
        prescriptionId: paPrescription.prescriptionId,
        clinicalJustification: paJustification,
        supportingDocumentation: paDocs || undefined,
      };
      const r = await hospitalService.submitPriorAuthorization(paPrescription.prescriptionId, req);
      setPaList(prev => [r.data, ...prev]);
      setPaJustification('');
      setPaDocs('');
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to submit prior authorization'));
    } finally {
      setPaLoading(false);
    }
  };

  const handleCheckPaStatus = async (priorAuthId: string) => {
    try {
      const r = await hospitalService.checkPriorAuthorizationStatus(priorAuthId);
      setPaList(prev => prev.map(pa => pa.priorAuthId === priorAuthId ? r.data : pa));
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to check status'));
    }
  };

  const handleManualPaUpdate = async () => {
    if (!paManualUpdateId || !paManualStatus) return;
    setPaManualSaving(true);
    try {
      const r = await hospitalService.updatePriorAuthorizationStatus(
        paManualUpdateId,
        paManualStatus as any,
        {
          priorAuthNumber: paManualNumber || undefined,
          expirationDate: paManualExpiry || undefined,
          denialReason: paManualDenial || undefined,
        },
      );
      setPaList(prev => prev.map(pa => pa.priorAuthId === paManualUpdateId ? r.data : pa));
      setPaManualUpdateId(null);
      setPaManualStatus('');
      setPaManualNumber('');
      setPaManualExpiry('');
      setPaManualDenial('');
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to update prior authorization status'));
    } finally {
      setPaManualSaving(false);
    }
  };

  // ── Transmission monitoring handlers ──
  const handleOpenTransmissions = async (rx: Prescription) => {
    setTransmissionsPrescription(rx);
    setTransmissionsList([]);
    setShowTransmissionsModal(true);
    setTransmissionsLoading(true);
    try {
      const r = await hospitalService.getPrescriptionTransmissions(rx.prescriptionId);
      setTransmissionsList(r.data || []);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load transmissions'));
    } finally {
      setTransmissionsLoading(false);
    }
  };

  const handleRetryTransmission = async (transmissionId: string) => {
    setRetryingTransmissionId(transmissionId);
    try {
      const r = await hospitalService.retryTransmission(transmissionId);
      setTransmissionsList(prev => prev.map(t => t.transmissionId === transmissionId ? r.data : t));
      loadPrescriptions();
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Retry failed'));
    } finally {
      setRetryingTransmissionId(null);
    }
  };

  // ── Interaction acknowledgment handlers ──
  const handleOpenInteractionAck = async (rx: Prescription) => {
    setAckPrescription(rx);
    setAckInteractions([]);
    setAckReasonMap({});
    setShowInteractionAckModal(true);
    setAckLoading(true);
    try {
      // Use dedicated interactions endpoint; fall back to check-interactions
      let interactions: PrescriptionInteraction[] = [];
      try {
        const r = await hospitalService.getPrescriptionInteractions(rx.prescriptionId);
        interactions = r.data || [];
      } catch {
        const r = await hospitalService.checkPrescriptionInteractions(rx.prescriptionId);
        interactions = r.data.interactions || [];
      }
      setAckInteractions(interactions);
      const initialReasons: Record<string, string> = {};
      interactions.forEach(ix => {
        initialReasons[ix.interactionId] = ix.overrideReason || '';
      });
      setAckReasonMap(initialReasons);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load interactions'));
    } finally {
      setAckLoading(false);
    }
  };

  const handleAcknowledgeSingleInteraction = async (ix: PrescriptionInteraction) => {
    if (!ackPrescription) return;
    const reason = ackReasonMap[ix.interactionId]?.trim();
    if (!reason) {
      alert('Please enter an override reason before acknowledging this interaction.');
      return;
    }
    setAckSavingId(ix.interactionId);
    try {
      await hospitalService.acknowledgeInteraction(ackPrescription.prescriptionId, ix.interactionId, reason);
      setAckInteractions(prev =>
        prev.map(i => i.interactionId === ix.interactionId ? { ...i, isAcknowledged: true, overrideReason: reason } : i)
      );
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to acknowledge interaction'));
    } finally {
      setAckSavingId(null);
    }
  };

  const handleConfirmInteractionAck = () => {
    const unacknowledgedInDb = ackInteractions.filter(ix => !ix.isAcknowledged);
    if (unacknowledgedInDb.length > 0) {
      alert(`${unacknowledgedInDb.length} interaction(s) are not yet acknowledged. Use the "Acknowledge" button on each one first.`);
      return;
    }
    // All acknowledged in backend — proceed to transmit without override flag needed
    setShowInteractionAckModal(false);
    if (ackPrescription) {
      setPrescriptionToTransmit(ackPrescription);
      setShowTransmitDialog(true);
    }
  };

  // ── Allergy acknowledgment handlers ──
  const handleOpenAllergyAck = async (rx: Prescription) => {
    setAllergyAckPrescription(rx);
    setAllergyAckChecks([]);
    setAllergyAckReasonMap({});
    setShowAllergyAckModal(true);
    setAllergyAckLoading(true);
    try {
      let checks: PrescriptionAllergyCheck[] = [];
      try {
        const r = await hospitalService.getPrescriptionAllergyChecks(rx.prescriptionId);
        checks = r.data || [];
      } catch {
        const r = await hospitalService.checkPrescriptionAllergies(rx.prescriptionId);
        checks = r.data.allergyChecks || [];
      }
      setAllergyAckChecks(checks);
      const initialReasons: Record<string, string> = {};
      checks.forEach(c => {
        initialReasons[c.checkId] = c.overrideReason || '';
      });
      setAllergyAckReasonMap(initialReasons);
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to load allergy checks'));
    } finally {
      setAllergyAckLoading(false);
    }
  };

  const handleAcknowledgeSingleAllergyCheck = async (check: PrescriptionAllergyCheck) => {
    if (!allergyAckPrescription) return;
    const reason = allergyAckReasonMap[check.checkId]?.trim();
    if (!reason) {
      alert('Please enter an override reason before acknowledging this allergy warning.');
      return;
    }
    setAllergyAckSavingId(check.checkId);
    try {
      await hospitalService.acknowledgeAllergyCheck(allergyAckPrescription.prescriptionId, check.checkId, reason);
      setAllergyAckChecks(prev =>
        prev.map(c => c.checkId === check.checkId ? { ...c, isAcknowledged: true, overrideReason: reason } : c)
      );
    } catch (err: any) {
      alert(ehrApiErrorMessage(err, 'Failed to acknowledge allergy check'));
    } finally {
      setAllergyAckSavingId(null);
    }
  };

  const handleConfirmAllergyAck = () => {
    const unacknowledgedInDb = allergyAckChecks.filter(c => !c.isAcknowledged);
    if (unacknowledgedInDb.length > 0) {
      alert(`${unacknowledgedInDb.length} allergy warning(s) are not yet acknowledged. Use the "Acknowledge" button on each one first.`);
      return;
    }
    setShowAllergyAckModal(false);
    if (allergyAckPrescription) {
      setPrescriptionToTransmit(allergyAckPrescription);
      setShowTransmitDialog(true);
    }
  };

  const formatDate = (d?: string) => d ? new Date(d).toLocaleDateString() : '—';

  const templatesFiltered = useMemo(() => {
    const q = templateApplySearch.trim().toLowerCase();
    return templates.filter(t => {
      const matchScope = matchesEpTemplateScope(t, templateFilter, {
        userId: user?.id,
        myDepartmentId: linkedDoctor?.departmentId,
        isDoctorOwned: id => epTemplateService.isDoctorOwned(id),
      });
      if (!matchScope) return false;
      if (!q) return true;
      return (
        t.templateName.toLowerCase().includes(q) ||
        (t.diseaseCategory || '').toLowerCase().includes(q) ||
        (t.clinicalFindings || '').toLowerCase().includes(q)
      );
    });
  }, [templates, templateFilter, templateApplySearch, user?.id, linkedDoctor?.departmentId]);

  if (loading && !prescriptions.length) return <div className="loading">Loading prescriptions…</div>;

  // ── Render: prescription form ──
  const renderForm = () => {
    const favCodes: ICD10Code[] = favoriteIcd10Entries.map(e => ({
      code: e.code,
      description: e.description,
      category: 'Favourite',
    }));
    const showFavIcd10 = !icd10Query.trim() && favCodes.length > 0;

    const advicePickSection = (
      <div className="form-section">
        <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
          Advice
        </h4>
        {adviceItems.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px', marginBottom: '6px' }}>
            {adviceItems.map((a, chipIdx) => (
              <span
                key={`${a}-${chipIdx}`}
                style={{
                  background: 'var(--color-primary, #1d4ed8)',
                  color: '#fff',
                  borderRadius: '10px',
                  fontSize: '11px',
                  padding: '2px 7px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '3px',
                }}
              >
                {a}
                <button
                  type="button"
                  onClick={() => setAdviceItems(p => p.filter((_, j) => j !== chipIdx))}
                  style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '12px', padding: 0, lineHeight: 1 }}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        )}
        <div style={{ display: 'flex', gap: '6px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <Autocomplete
            freeSolo
            options={adviceAutocompleteOptions}
            value={null}
            inputValue={customAdvice}
            onOpen={() => {
              void epAdviceService.getSuggestions({ query: customAdvice.trim(), limit: 120 }).then(setAdviceOptions);
            }}
            onInputChange={(_, v, reason) => {
              if (reason !== 'reset') setCustomAdvice(v);
            }}
            onChange={(_, v) => {
              if (typeof v === 'string' && v.trim()) {
                commitAdviceLine(v);
                setCustomAdvice('');
              }
            }}
            renderOption={(props, option) =>
              renderEpDismissibleOption(props, option, dismissAdviceSuggestion, Boolean(user?.id))
            }
            sx={{ flex: 1, minWidth: '160px' }}
            renderInput={params => (
              <TextField
                {...params}
                size="small"
                placeholder="Common advice first — type to search or add new"
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    if (customAdvice.trim()) {
                      commitAdviceLine(customAdvice);
                      setCustomAdvice('');
                    }
                  }
                }}
              />
            )}
          />
          <button
            type="button"
            className="btn-secondary"
            style={{ fontSize: '12px', padding: '4px 8px', alignSelf: 'center' }}
            onClick={() => {
              if (customAdvice.trim()) {
                commitAdviceLine(customAdvice);
                setCustomAdvice('');
              }
            }}
          >
            Add
          </button>
        </div>
        <p style={{ fontSize: '10px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
          Use × on a dropdown suggestion to remove it from future autocomplete lists.
        </p>
      </div>
    );

    return (
      <div className={quickMode ? 'ep-quick-mode' : 'form-container ep-prescription-form'}>

        {/* ── Left panel (Clinical info) ── */}
        <div className="ep-prescription-form-left">
          {!quickMode && <h3 style={{ marginTop: 0 }}>{editing ? 'Edit' : 'New'} Prescription</h3>}

          {/* Template controls */}
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '14px' }}>
            <button type="button" className="btn-secondary" style={{ fontSize: '12px', padding: '5px 10px' }} onClick={() => setShowTemplateDialog(true)}>
              Apply Template
            </button>
            <button type="button" className="btn-secondary" style={{ fontSize: '12px', padding: '5px 10px' }} onClick={() => setShowCopyPrevious(true)}>
              Copy Previous Rx
            </button>
            {hasDraft && !editing && (
              <button type="button" className="btn-secondary" style={{ fontSize: '12px', padding: '5px 10px', color: '#f59e0b' }} onClick={restoreDraft}>
                Restore Draft
              </button>
            )}
          </div>

          {/* Chief Complaints (EP-4) — pick suggestion to add; ADD only for manual entry */}
          <div className="form-section" style={{ marginBottom: '16px' }}>
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Chief Complaint (C/C)
            </h4>
            {chiefComplaints.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '8px' }}>
                {chiefComplaints.map((c, chipIdx) => (
                  <span
                    key={`${c}-${chipIdx}`}
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                      background: '#1e40af',
                      color: '#fff',
                      borderRadius: '12px',
                      padding: '2px 8px',
                      fontSize: '12px',
                    }}
                  >
                    {c}
                    <button
                      type="button"
                      onClick={() => setChiefComplaints(p => p.filter((_, j) => j !== chipIdx))}
                      style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '12px', padding: 0, lineHeight: 1 }}
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            )}
            <div style={{ display: 'flex', gap: '6px', alignItems: 'flex-start', flexWrap: 'wrap' }}>
              <Autocomplete
                freeSolo
                selectOnFocus
                clearOnBlur={false}
                options={COMMON_COMPLAINTS.filter(c => !chiefComplaints.includes(c))}
                getOptionLabel={(option) => option}
                value={null}
                inputValue={customComplaint}
                onInputChange={(_, v, reason) => {
                  if (reason !== 'reset') setCustomComplaint(v);
                }}
                onChange={(_, v) => {
                  if (typeof v === 'string' && v.trim()) {
                    const trimmed = v.trim();
                    setChiefComplaints(p => (p.includes(trimmed) ? p : [...p, trimmed]));
                    setCustomComplaint('');
                  }
                }}
                sx={{ flex: 1, minWidth: '160px' }}
                renderInput={params => (
                  <TextField
                    {...params}
                    size="small"
                    placeholder="Select a common complaint or type your own"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        if (customComplaint.trim()) {
                          const trimmed = customComplaint.trim();
                          setChiefComplaints((p) => (p.includes(trimmed) ? p : [...p, trimmed]));
                          setCustomComplaint('');
                        }
                      }
                    }}
                  />
                )}
              />
              <button
                type="button"
                className="btn-secondary"
                style={{ fontSize: '12px', padding: '4px 8px', alignSelf: 'center' }}
                onClick={() => {
                  if (customComplaint.trim()) {
                    const trimmed = customComplaint.trim();
                    setChiefComplaints(p => (p.includes(trimmed) ? p : [...p, trimmed]));
                    setCustomComplaint('');
                  }
                }}
              >
                Add
              </button>
            </div>
          </div>

          {/* Diagnosis (ICD-10) (EP-4) */}
          <div className="form-section" style={{ marginBottom: '16px' }}>
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Diagnosis (ICD-10 / ICD-11 / Custom)
            </h4>
            {diagnosisError && (
              <p style={{ color: '#dc2626', fontSize: '12px', margin: '0 0 8px' }}>{diagnosisError}</p>
            )}
            {diagnoses.length > 0 && (
              <div style={{ marginBottom: '8px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                {diagnoses.map((dx, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px' }}>
                    <span style={{ fontSize: '10px', fontWeight: 700, padding: '2px 6px', borderRadius: '8px', background: dx.isPrimary ? '#d1fae5' : '#f3f4f6', color: dx.isPrimary ? '#065f46' : '#374151' }}>
                      {dx.isPrimary ? '1°' : '2°'}
                    </span>
                    <span style={{ flex: 1 }}><strong>{dx.code}</strong> — {dx.description}</span>
                    <button type="button" onClick={() => setDiagnoses(prev => prev.filter((_, j) => j !== i))}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', fontSize: '14px', padding: 0 }}>×</button>
                  </div>
                ))}
              </div>
            )}
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
                onFocus={() => setShowIcd10(true)}
                onBlur={() => setTimeout(() => setShowIcd10(false), 200)}
                placeholder="Search ICD-10 code/name or type custom disease and press Enter…"
                style={{ width: '100%', fontSize: '12px', padding: '5px 8px', boxSizing: 'border-box' }}
              />
              {(showIcd10 && (icd10Results.length > 0 || showFavIcd10 || icd10RemoteLoading)) && (
                <div className="autocomplete-dropdown">
                  {icd10RemoteLoading && icd10Query.trim().length >= 2 && (
                    <div className="autocomplete-item" style={{ color: '#6b7280' }}>Searching ICD-10…</div>
                  )}
                  {showFavIcd10 && (
                    <>
                      <div style={{ fontSize: '10px', color: '#6b7280', padding: '4px 10px', background: '#f9fafb' }}>★ FAVOURITES</div>
                      {favCodes.map(c => (
                        <div key={c.code} className="autocomplete-item" onMouseDown={() => addDiagnosis(c, diagnoses.length === 0)}>
                          <strong>{c.code}</strong> <span style={{ color: '#555' }}>{c.description}</span>
                        </div>
                      ))}
                    </>
                  )}
                  {icd10Results.map(c => (
                    <div key={c.code} className="autocomplete-item" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }} onMouseDown={e => e.preventDefault()}>
                      <span style={{ flex: 1, cursor: 'pointer' }} onClick={() => addDiagnosis(c, diagnoses.length === 0)}>
                        <strong>{c.code}</strong> — {c.description}
                      </span>
                      <button
                        type="button"
                        onClick={e => {
                          e.stopPropagation();
                          toggleFavIcd10(c.code, c.description);
                        }}
                        title={favoriteIcd10Entries.some(e => e.code === c.code) ? 'Remove from favourites' : 'Add to favourites'}
                        style={{ background: 'none', border: 'none', cursor: 'pointer', color: favoriteIcd10Entries.some(e => e.code === c.code) ? '#f59e0b' : '#d1d5db', fontSize: '14px' }}
                      >
                        ★
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
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
            {diagnoses.length > 0 && (
              <div style={{ display: 'flex', gap: '6px', marginTop: '6px', flexWrap: 'wrap' }}>
                {diagnoses.map((dx, i) => (
                  <button key={i} type="button" style={{ fontSize: '10px', padding: '2px 8px', background: 'none', border: `1px solid ${dx.isPrimary ? '#059669' : '#9ca3af'}`, borderRadius: '8px', cursor: 'pointer', color: dx.isPrimary ? '#059669' : '#6b7280' }}
                    onClick={() => setDiagnoses(prev => prev.map((d, j) => ({ ...d, isPrimary: j === i })))}>
                    {dx.code} {dx.isPrimary ? '(Primary)' : '→ Set Primary'}
                  </button>
                ))}
              </div>
            )}
          </div>

          <details style={{ marginTop: '10px', fontSize: '12px', color: 'var(--text-secondary)' }}>
            <summary style={{ cursor: 'pointer', fontWeight: 600 }}>Optional FR-P1.7 context (weight, eGFR, pregnancy)</summary>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: '8px', marginTop: '8px' }}>
              <label style={{ display: 'block' }}>
                <span style={{ display: 'block', marginBottom: '4px' }}>Weight (kg)</span>
                <input type="number" min={0} step="0.1" value={interactionScreeningContext.weightKg}
                  onKeyDown={blockNegativeNumberInput}
                  onChange={e => setInteractionScreeningContext(p => ({ ...p, weightKg: e.target.value }))}
                  style={{ width: '100%', padding: '4px 6px', fontSize: '12px', boxSizing: 'border-box' }} />
              </label>
              <label style={{ display: 'block' }}>
                <span style={{ display: 'block', marginBottom: '4px' }}>eGFR (mL/min)</span>
                <input type="number" min={0} step="0.1" value={interactionScreeningContext.egfrMlMin}
                  onKeyDown={blockNegativeNumberInput}
                  onChange={e => setInteractionScreeningContext(p => ({ ...p, egfrMlMin: e.target.value }))}
                  style={{ width: '100%', padding: '4px 6px', fontSize: '12px', boxSizing: 'border-box' }} />
              </label>
              <label style={{ display: 'block' }}>
                <span style={{ display: 'block', marginBottom: '4px' }}>Creatinine (mg/dL)</span>
                <input type="number" min={0} step="0.01" value={interactionScreeningContext.serumCreatinineMgDl}
                  onKeyDown={blockNegativeNumberInput}
                  onChange={e => setInteractionScreeningContext(p => ({ ...p, serumCreatinineMgDl: e.target.value }))}
                  style={{ width: '100%', padding: '4px 6px', fontSize: '12px', boxSizing: 'border-box' }} />
              </label>
              <label style={{ display: 'block' }}>
                <span style={{ display: 'block', marginBottom: '4px' }}>Pregnancy</span>
                <select value={interactionScreeningContext.pregnancyStatus}
                  onChange={e => setInteractionScreeningContext(p => ({ ...p, pregnancyStatus: e.target.value as '' | PregnancyStatus }))}
                  style={{ width: '100%', padding: '4px 6px', fontSize: '12px', boxSizing: 'border-box' }}>
                  <option value="">Use chart</option>
                  <option value="UNKNOWN">Unknown</option>
                  <option value="NOT_PREGNANT">Not pregnant</option>
                  <option value="POSSIBLE">Possible</option>
                  <option value="PREGNANT">Pregnant</option>
                </select>
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '18px' }}>
                <input type="checkbox" checked={interactionScreeningContext.lactating}
                  onChange={e => setInteractionScreeningContext(p => ({ ...p, lactating: e.target.checked }))} />
                Lactating
              </label>
            </div>
          </details>

          <div className="form-section" style={{ marginBottom: '16px' }}>
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Clinical Findings
            </h4>
            <textarea
              value={clinicalFindings}
              onChange={(e) => setClinicalFindings(e.target.value)}
              placeholder="Examination findings, observations…"
              rows={3}
              style={{ width: '100%', padding: '8px', fontSize: '12px', boxSizing: 'border-box', resize: 'vertical' }}
            />
          </div>

          {/* Investigations / Tests — left column (EHR layout: CC, MH, Dx, Clinical Findings, Investigation) */}
          <div className="form-section" style={{ marginBottom: '16px' }}>
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Investigations / Tests
            </h4>
            {orderedTests.length > 0 && (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px', marginBottom: '6px' }}>
                {orderedTests.map((t, ti) => (
                  <span key={`inv-${ti}-${t.testName}`} style={{ background: '#0d9488', color: '#fff', borderRadius: '10px', fontSize: '11px', padding: '2px 7px', display: 'flex', alignItems: 'center', gap: '3px' }}>
                    {t.testName}
                    {t.isPanel && <span style={{ fontSize: '9px', opacity: 0.9 }}>(panel{t.panelName ? `: ${t.panelName}` : ''})</span>}
                    <button type="button" onClick={() => setOrderedTests((p) => p.filter((_, j) => j !== ti))} style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '12px', padding: 0, lineHeight: 1 }}>×</button>
                  </span>
                ))}
              </div>
            )}
            <div style={{ display: 'flex', gap: '6px', alignItems: 'flex-start' }}>
              <Autocomplete
                freeSolo
                blurOnSelect
                options={investigationAutocompleteOptions}
                value={null}
                inputValue={customTest}
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
                  if (reason !== 'reset') setCustomTest(value);
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
                  if (val != null) appendInvestigationTest(String(val));
                }}
                renderOption={(props, option) =>
                  renderEpDismissibleOption(props, option, dismissInvestigationSuggestion, Boolean(user?.id))
                }
                sx={{ flex: 1 }}
                size="small"
                renderInput={(params) => (
                  <TextField
                    {...params}
                    placeholder="Search clinical chart investigations/tests or type custom…"
                  />
                )}
              />
              <button
                type="button"
                className="btn-secondary"
                style={{ fontSize: '12px', padding: '4px 8px', marginTop: 2 }}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => appendInvestigationTest(customTest)}
              >
                Add
              </button>
            </div>
            <p style={{ fontSize: '10px', color: 'var(--text-secondary)', margin: '6px 0 0' }}>
              Use × on a dropdown suggestion to remove it from future autocomplete lists.
            </p>
            {id && (
              <div style={{ marginTop: '10px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <p style={{ fontSize: '10px', color: 'var(--text-secondary)', margin: 0 }}>
                  Investigations listed here are recorded on the prescription for documentation. To place lab orders in the system, use the patient lab order screen.
                </p>
                <button
                  type="button"
                  className="btn-secondary"
                  style={{ fontSize: '11px', width: '100%' }}
                  onClick={() => {
                    const ret = encodeURIComponent(`${location.pathname}${location.search}`);
                    navigate(`/hospital/patients/${id}/lab-orders?returnTo=${ret}`);
                  }}
                >
                  Open full lab order screen →
                </button>
              </div>
            )}
          </div>

          <div className="form-section" style={{ marginBottom: '16px' }}>
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Medical History
            </h4>
            {medicalHistoryWithValues(medicalHistoryItems).length === 0 ? (
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>No medical history found.</div>
            ) : (
              <div style={{ display: 'grid', gap: '6px', maxHeight: '180px', overflowY: 'auto', paddingRight: '4px' }}>
                {medicalHistoryWithValues(medicalHistoryItems).slice(0, 8).map((mh) => (
                  <div key={mh.historyId} style={{ fontSize: '11px', border: '1px solid var(--border-color, #e5e7eb)', borderRadius: '6px', padding: '6px 8px' }}>
                    {mh.conditionName?.trim() && (
                      <div style={{ fontWeight: 600, marginBottom: '3px' }}>{mh.conditionName}</div>
                    )}
                    {(mh.icd10Code?.trim() || mh.icd11Code?.trim() || mh.severity?.trim()) && (
                      <div style={{ color: 'var(--text-secondary)' }}>
                        {mh.icd10Code?.trim() ? `ICD-10: ${mh.icd10Code}` : ''}
                        {mh.icd11Code?.trim() ? `${mh.icd10Code?.trim() ? ' | ' : ''}ICD-11: ${mh.icd11Code}` : ''}
                        {mh.severity?.trim() ? `${(mh.icd10Code?.trim() || mh.icd11Code?.trim()) ? ' | ' : ''}Severity: ${mh.severity}` : ''}
                      </div>
                    )}
                    {mh.notes?.trim() && (
                      <div style={{ color: 'var(--text-secondary)' }}>Note: {mh.notes}</div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {quickMode && (
            <>
              <div className="form-section">
                <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
                  Follow-up Date
                </h4>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                  <input
                    type="date"
                    min={prescriptionTodayIso()}
                    value={followUpDate}
                    onChange={e => handleFollowUpDateChange(e.target.value)}
                    style={{ width: '170px', padding: '5px 8px', fontSize: '12px', boxSizing: 'border-box' }}
                  />
                  {[3, 5, 7, 14, 30, 60, 90].map(d => (
                    <button
                      key={d}
                      type="button"
                      style={{ fontSize: '11px', padding: '3px 8px', borderRadius: '10px', cursor: 'pointer', border: `1px solid ${followUpDays === d ? 'var(--color-primary)' : 'var(--border-color, #d1d5db)'}`, background: followUpDays === d ? 'var(--color-primary-light, #dbeafe)' : 'transparent', color: followUpDays === d ? 'var(--color-primary)' : 'var(--text-secondary)' }}
                      onClick={() => {
                        setFollowUpDays(d);
                        const dt = new Date(); dt.setDate(dt.getDate() + d);
                        setFollowUpDate(dt.toISOString().split('T')[0]);
                      }}
                    >
                      {d}d
                    </button>
                  ))}
                </div>
              </div>

              <div className="form-section">
                <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
                  Referral
                </h4>
                <textarea
                  value={referral}
                  onChange={e => setReferral(e.target.value)}
                  placeholder="Specialty, facility, or colleague (e.g. Cardiology — City Hospital — Dr. Ahmed)"
                  rows={3}
                  style={{ width: '100%', padding: '8px', fontSize: '12px', boxSizing: 'border-box', resize: 'vertical' }}
                />
              </div>
            </>
          )}

        </div>

        {/* ── Right stack: Medicine Rx, Advice, Notes ── */}
        <div className="ep-prescription-form-right">
        <form onSubmit={handleSubmit} style={quickMode ? {} : undefined} id="rx-form">
          <div className="form-section">
            {quickMode && <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>Medicines</h4>}
            {!quickMode && <h4 className="form-section-title">Medicines</h4>}

            {preferredMedicines.length > 0 && (
              <div style={{ marginBottom: '10px' }}>
                <div style={{ fontSize: '11px', color: 'var(--text-secondary)', marginBottom: '6px' }}>
                  Preferred shortcuts (configure in EP Settings)
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px' }}>
                  {preferredMedicines.map(name => (
                    <button
                      key={name}
                      type="button"
                      className="btn-secondary"
                      style={{ fontSize: '11px', padding: '3px 8px' }}
                      onClick={() => {
                        const line = createDefaultMedicationLine();
                        line.medicationName = name;
                        setMedicationLines(prev => [...prev, line]);
                      }}
                    >
                      + {name}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {doctorConfig.doseFormatNote?.trim() && (
              <div style={{ marginBottom: '10px', padding: '8px 10px', fontSize: '12px', color: '#374151', background: 'var(--bg-elevated, #f0f9ff)', border: '1px solid #bae6fd', borderRadius: '6px' }}>
                <strong style={{ color: '#0369a1' }}>Frequency hint:</strong> {doctorConfig.doseFormatNote.trim()}
              </div>
            )}

            {/* Medication lines */}
            {medicationLines.length === 0 && (
              <p style={{ textAlign: 'center', color: '#888', padding: '20px 0', fontSize: '13px' }}>
                Search below to add medicines
              </p>
            )}

            {medicationLines.map((line, idx) => {
              const isExpanded = expandedLines.has(idx);
              const isCustom = customDurationLines.has(idx);
              const durationVal = isCustom ? 'custom' : getDurationSelectValue(line.durationDays);

              return (
                <div key={line.clientRowId ?? `med-${idx}`} style={{ marginBottom: '10px', padding: '12px 14px', border: '1px solid var(--border-color, #e5e7eb)', borderRadius: '8px', background: 'var(--bg-elevated, #fafafa)' }}>
                  {/* Row 1: form badge | name | duration | remove */}
                  <div style={{ display: 'flex', gap: '6px', alignItems: 'center', flexWrap: 'wrap', marginBottom: '7px' }}>
                    <span style={{ flexShrink: 0, fontSize: '10px', fontWeight: 600, textTransform: 'uppercase', padding: '2px 7px', borderRadius: '10px', background: 'var(--color-primary-light, #dbeafe)', color: 'var(--color-primary, #1d4ed8)' }}>
                      {DOSAGE_FORM_OPTIONS.find(([v]) => v === line.dosageForm)?.[1] ?? line.dosageForm}
                    </span>
                    <span style={{ flex: '1 1 140px', fontSize: '14px', fontWeight: 600 }}>
                      {line.medicationName || <em style={{ color: '#aaa', fontWeight: 400 }}>—</em>}
                      {(line.dosageStrength || line.dosageUnit) && (
                        <span style={{ fontWeight: 400, fontSize: '12px', color: '#555', marginLeft: '4px' }}>
                          {[line.dosageStrength, line.dosageUnit].filter(Boolean).join(' ')}
                        </span>
                      )}
                    </span>
                    <select
                      value={durationVal}
                      onChange={e => {
                        const v = e.target.value;
                        if (v === 'custom') { setCustomDurationLines(prev => { const s = new Set(prev); s.add(idx); return s; }); updateMedicationLine(idx, { durationDays: undefined }); }
                        else { setCustomDurationLines(prev => { const s = new Set(prev); s.delete(idx); return s; }); updateMedicationLine(idx, { durationDays: v === '' ? undefined : parseInt(v) }); }
                      }}
                      style={{ flexShrink: 0, padding: '4px 6px', fontSize: '12px' }}
                    >
                      <option value="">Duration</option>
                      {DURATION_PRESETS.map(p => <option key={p.days} value={String(p.days)}>{p.label}</option>)}
                      <option value="custom">Custom…</option>
                    </select>
                    {isCustom && (
                      <input type="number" min="1" value={line.durationDays ?? ''}
                        onChange={e => updateMedicationLine(idx, { durationDays: e.target.value ? parseInt(e.target.value) : undefined })}
                        placeholder="days" style={{ width: '60px', padding: '4px 6px', fontSize: '12px' }}
                      />
                    )}
                    <button type="button" onClick={() => removeMedicationLine(idx)}
                      style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: '#e53e3e', fontSize: '16px', padding: '2px' }}>×</button>
                  </div>

                  {/* Row 2: frequency (shorthand) | instructions (EP-5) */}
                  <div style={{ display: 'flex', gap: '7px', alignItems: 'flex-start', flexWrap: 'wrap', marginBottom: '4px' }}>
                    <div style={{ position: 'relative', flexShrink: 0 }}>
                      <input
                        type="text"
                        list={`freq-${idx}`}
                        value={line.frequency || ''}
                        onChange={e => updateMedicationLine(idx, { frequency: e.target.value })}
                        placeholder={doctorConfig.doseFrequencyPlaceholder?.trim() || '1-0-1 or BID'}
                        style={{ width: '118px', padding: '4px 6px', fontSize: '13px' }}
                      />
                      <datalist id={`freq-${idx}`}>
                        {FREQUENCY_SUGGESTIONS.map(s => <option key={s} value={s} />)}
                      </datalist>
                      {(() => {
                        const freqHint = describeFrequencyShorthand(line.frequency || '');
                        return freqHint ? (
                          <div style={{ fontSize: '10px', color: '#6b7280', marginTop: '2px', maxWidth: '220px' }}>{freqHint}</div>
                        ) : null;
                      })()}
                    </div>

                    <div style={{ flex: '1 1 140px', minWidth: '120px' }}>
                      <input
                        type="text"
                        list={`instruction-${idx}`}
                        value={line.instructions}
                        onChange={e => updateMedicationLine(idx, { instructions: e.target.value })}
                        placeholder="Type or select instruction"
                        style={{ width: '100%', padding: '4px 6px', fontSize: '12px' }}
                      />
                      <datalist id={`instruction-${idx}`}>
                        {INSTRUCTION_SUGGESTIONS.map(s => <option key={s} value={s} />)}
                      </datalist>
                    </div>
                  </div>

                  {line.frequency && line.durationDays && !isPrnOrStatFrequency(line.frequency) && parseFrequencyDoses(line.frequency) > 0 && (() => {
                    const dpm = parseFrequencyDoses(line.frequency);
                    const dpmLabel = dpm % 1 === 0 ? String(dpm) : dpm.toFixed(2);
                    return (
                      <div style={{ fontSize: '11px', color: '#059669', marginBottom: '4px' }}>
                        Estimated course: {dpmLabel} doses/day × {line.durationDays} days ≈ {autoCalcQuantity(line)} units
                      </div>
                    );
                  })()}

                  {/* Advanced toggle */}
                  <button type="button" onClick={() => setExpandedLines(prev => { const s = new Set(prev); s.has(idx) ? s.delete(idx) : s.add(idx); return s; })}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '11px', color: '#6b7280', padding: 0 }}>
                    {isExpanded ? '▴ Hide advanced' : '▾ Advanced options'}
                  </button>

                  {isExpanded && (
                    <div className="form-grid" style={{ marginTop: '8px', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))' }}>
                      <div className="form-group">
                        <label>Form</label>
                        <select value={line.dosageForm} onChange={e => updateMedicationLine(idx, { dosageForm: e.target.value as any })}>
                          {DOSAGE_FORM_OPTIONS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                        </select>
                      </div>
                      <div className="form-group">
                        <label>Route</label>
                        <select value={line.route} onChange={e => updateMedicationLine(idx, { route: e.target.value as any })}>
                          {['ORAL','IV','IM','SC','TOPICAL','INHALATION','SUBLINGUAL','BUCCAL','RECTAL','OPHTHALMIC','OTIC','NASAL','OTHER'].map(r => <option key={r} value={r}>{r}</option>)}
                        </select>
                      </div>
                      <div className="form-group">
                        <label>Strength</label>
                        <input type="number" value={line.dosageStrength ?? ''} onChange={e => updateMedicationLine(idx, { dosageStrength: e.target.value ? parseFloat(e.target.value) : undefined })} />
                      </div>
                      <div className="form-group">
                        <label>Unit</label>
                        <input type="text" value={line.dosageUnit || ''} onChange={e => updateMedicationLine(idx, { dosageUnit: e.target.value })} placeholder="mg" />
                      </div>
                      <div className="form-group">
                        <label>Start Date</label>
                        <input type="date" value={line.startDate} onChange={e => updateMedicationLine(idx, { startDate: e.target.value })} />
                      </div>
                    </div>
                  )}
                </div>
              );
            })}

            {/* Drug search */}
            <div style={{ position: 'relative', marginTop: '12px' }}>
              <input
                type="text"
                placeholder="Search and add medicine…"
                value={drugSearchQuery}
                onChange={e => { setDrugSearchQuery(e.target.value); debouncedSearchDrugs(e.target.value); }}
                onFocus={() => {
                  if (drugSearchLoading || drugSuggestions.length > 0 || drugSearchQuery.trim().length >= 2) {
                    setShowDrugSuggestions(true);
                  }
                }}
                onBlur={() => setTimeout(() => setShowDrugSuggestions(false), 200)}
                style={{ width: '100%', fontSize: '14px', padding: '9px 12px', boxSizing: 'border-box' }}
              />
              {showDrugSuggestions &&
                (drugSearchLoading || drugSuggestions.length > 0 || drugSearchQuery.trim().length >= 2) && (
                <div className="autocomplete-dropdown">
                  {drugSearchLoading && <div className="autocomplete-item">Searching…</div>}
                  {!drugSearchLoading &&
                    drugSuggestions.map(drug => (
                      <div key={drug.id} className="autocomplete-item" onMouseDown={() => handleAddDrugFromSearch(drug)}>
                        <strong>{drug.brandName || drug.genericName}</strong>
                        {drug.brandName && <span style={{ color: '#666' }}> ({drug.genericName})</span>}
                        {(drug.strength || drug.form) && (
                          <span style={{ color: '#888', fontSize: '12px' }}> — {[drug.strength, drug.form].filter(Boolean).join(', ')}</span>
                        )}
                      </div>
                    ))}
                  {!drugSearchLoading && drugSearchFailed && drugSearchQuery.trim().length >= 2 && (
                    <div className="autocomplete-item" style={{ color: '#dc2626', cursor: 'default' }}>
                      Catalog search unavailable — check hospital-pharmacy-service and Pharmacy Catalog.
                    </div>
                  )}
                  {!drugSearchLoading && !drugSearchFailed && drugSuggestions.length === 0 && drugSearchQuery.trim().length >= 2 && (
                    <div className="autocomplete-item" style={{ color: '#6b7280', cursor: 'default' }}>
                      No matching drugs in catalog
                    </div>
                  )}
                  {canPrescribeRx && (
                    <div
                      className="autocomplete-item"
                      role="button"
                      tabIndex={0}
                      style={{
                        borderTop: '1px solid var(--border-color, #e5e7eb)',
                        fontWeight: 600,
                        color: 'var(--color-primary, #1d4ed8)',
                        background: 'var(--bg-elevated, #f8fafc)',
                        cursor: 'pointer',
                      }}
                      onMouseDown={e => {
                        e.preventDefault();
                        void openAddCatalogDrugModal();
                      }}
                      onKeyDown={e => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          void openAddCatalogDrugModal();
                        }
                      }}
                    >
                      + Add New Medicine…
                    </div>
                  )}
                </div>
              )}
            </div>

            <div style={{ marginTop: '10px', display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              <button type="button" className="btn-secondary" style={{ fontSize: '12px' }} onClick={handleCheckInteractions}>Check Interactions</button>
              <button type="button" className="btn-secondary" style={{ fontSize: '12px' }} onClick={handleCheckAllergies}>Check Allergies</button>
            </div>
          </div>

          {/* Pharmacy info (collapsed in quick mode) */}
          {!quickMode && (
            <div className="form-section">
              <h4 className="form-section-title">Pharmacy & Provider</h4>
              <div className="form-grid">
                {/* FR-P3.5: Pharmacy directory search picker */}
                <div className="form-group" style={{ gridColumn: '1 / -1', position: 'relative' }}>
                  <label>Pharmacy <span style={{ fontWeight: 400, fontSize: '11px', color: 'var(--text-secondary)' }}>(search directory or type manually)</span></label>
                  <input
                    type="text"
                    placeholder="Search by name, city or NPI…"
                    value={pharmacyQuery}
                    onChange={e => {
                      handlePharmacyQueryChange(e.target.value);
                      // Keep formData in sync for manual entry (no directory selection)
                      setFormData(prev => ({ ...prev, pharmacyName: e.target.value }));
                    }}
                    onBlur={() => setTimeout(() => setShowPharmacySuggestions(false), 200)}
                    onFocus={() => { if (pharmacySuggestions.length > 0) setShowPharmacySuggestions(true); }}
                    autoComplete="off"
                  />
                  {pharmacySearchLoading && (
                    <span style={{ position: 'absolute', right: 10, top: 32, fontSize: 11, color: 'var(--text-secondary)' }}>Searching…</span>
                  )}
                  {showPharmacySuggestions && pharmacySuggestions.length > 0 && (
                    <ul className="autocomplete-list" style={{ position: 'absolute', zIndex: 200, top: '100%', left: 0, right: 0, background: 'var(--bg-primary, #fff)', border: '1px solid var(--border-color, #e5e7eb)', borderRadius: 6, boxShadow: '0 4px 12px rgba(0,0,0,0.12)', margin: 0, padding: 0, listStyle: 'none', maxHeight: 260, overflowY: 'auto' }}>
                      {pharmacySuggestions.map(ph => (
                        <li
                          key={ph.id}
                          onMouseDown={() => handleSelectPharmacy(ph)}
                          style={{ padding: '8px 12px', cursor: 'pointer', borderBottom: '1px solid var(--border-color, #f3f4f6)' }}
                        >
                          <div style={{ fontWeight: 600, fontSize: 13 }}>
                            {ph.name}
                            {ph.isEprescribingCapable && (
                              <span style={{ marginLeft: 6, fontSize: 10, background: '#d1fae5', color: '#065f46', borderRadius: 3, padding: '1px 5px' }}>e-Rx</span>
                            )}
                          </div>
                          <div style={{ fontSize: 11, color: 'var(--text-secondary)' }}>
                            {[ph.city, ph.state].filter(Boolean).join(', ')}
                            {ph.npi && <span style={{ marginLeft: 8 }}>NPI: {ph.npi}</span>}
                            {ph.phone && <span style={{ marginLeft: 8 }}>☎ {ph.phone}</span>}
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
                <div className="form-group">
                  <label>Pharmacy NPI</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={formData.pharmacyNpi}
                    onChange={e => setFormData(p => ({ ...p, pharmacyNpi: digitsOnly(e.target.value) }))}
                    placeholder="Auto-filled from directory"
                  />
                </div>
                <div className="form-group">
                  <label>Pharmacy Phone</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={formData.pharmacyPhone}
                    onChange={e => setFormData(p => ({ ...p, pharmacyPhone: digitsOnly(e.target.value) }))}
                    placeholder="Auto-filled from directory"
                  />
                </div>
                <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                  <label>Prescribing Provider</label>
                  <input type="text" readOnly value={formData.prescribingProviderName || formData.prescribingProviderId}
                    style={{ background: 'var(--bg-secondary, #f3f4f6)', cursor: 'default' }} />
                </div>
                <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                  <label>Special Instructions</label>
                  <textarea value={formData.specialInstructions} onChange={e => setFormData(p => ({ ...p, specialInstructions: e.target.value }))} rows={2} />
                </div>
              </div>
            </div>
          )}

          {!quickMode && (
          <div className="form-section">
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Follow-up Date
            </h4>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
              <input
                type="date"
                min={prescriptionTodayIso()}
                value={followUpDate}
                onChange={e => handleFollowUpDateChange(e.target.value)}
                style={{ width: '170px', padding: '5px 8px', fontSize: '12px', boxSizing: 'border-box' }}
              />
              {[3, 5, 7, 14, 30, 60, 90].map(d => (
                <button
                  key={d}
                  type="button"
                  style={{ fontSize: '11px', padding: '3px 8px', borderRadius: '10px', cursor: 'pointer', border: `1px solid ${followUpDays === d ? 'var(--color-primary)' : 'var(--border-color, #d1d5db)'}`, background: followUpDays === d ? 'var(--color-primary-light, #dbeafe)' : 'transparent', color: followUpDays === d ? 'var(--color-primary)' : 'var(--text-secondary)' }}
                  onClick={() => {
                    setFollowUpDays(d);
                    const dt = new Date(); dt.setDate(dt.getDate() + d);
                    setFollowUpDate(dt.toISOString().split('T')[0]);
                  }}
                >
                  {d}d
                </button>
              ))}
            </div>
          </div>
          )}

          {!quickMode && (
          <div className="form-section">
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Referral
            </h4>
            <textarea
              value={referral}
              onChange={e => setReferral(e.target.value)}
              placeholder="Specialty, facility, or colleague (e.g. Cardiology — City Hospital — Dr. Ahmed)"
              rows={3}
              style={{ width: '100%', padding: '8px', fontSize: '12px', boxSizing: 'border-box', resize: 'vertical' }}
            />
          </div>
          )}

          {advicePickSection}

          <div className="form-section">
            <h4 className="form-section-title" style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--text-secondary)' }}>
              Notes
            </h4>
            <textarea
              value={formData.notes}
              onChange={e => setFormData(p => ({ ...p, notes: e.target.value }))}
              placeholder="Add notes"
              rows={3}
              style={{ width: '100%', padding: '8px', fontSize: '12px', boxSizing: 'border-box', resize: 'vertical' }}
            />
          </div>

          {/* Form actions */}
          <div className="form-actions" style={quickMode ? { justifyContent: 'flex-start', flexWrap: 'wrap', gap: '8px' } : {}}>
            <button type="button" className="btn-secondary" onClick={() => { setShowForm(false); setEditing(null); resetForm(); }}>Cancel</button>
            <button type="submit" className="btn-primary">Save Prescription</button>
            <button type="button" className="btn-secondary" onClick={handlePreview}>Preview</button>
            <button type="button" className="btn-secondary" onClick={() => setShowSaveTemplate(true)}>Save as Template</button>
          </div>
        </form>
        </div>
      </div>
    );
  };

  const showRxHistoryPanel = !showForm || !quickMode || showRxListWhilePrescribing;
  const copyPatientShareLink = () => {
    if (!id) return;
    const url = `${window.location.origin}/hospital/patients/${id}`;
    void navigator.clipboard.writeText(url).then(
      () => window.alert('Link copied. Share with care — this opens the patient chart (staff login required).'),
      () => window.alert(url)
    );
  };

  const copyPatientPortalLink = () => {
    if (!id || !doctorConfig.patientPortalBaseUrl?.trim()) return;
    const base = doctorConfig.patientPortalBaseUrl.replace(/\/+$/, '');
    const url = `${base}/patient/${id}/prescriptions`;
    void navigator.clipboard.writeText(url).then(
      () => window.alert('Patient portal link copied. Your portal must implement this URL or redirect.'),
      () => window.alert(url)
    );
  };

  return (
    <div
      className={`hospital-page prescription-page${showForm && quickMode && !showRxListWhilePrescribing ? ' ep-prescribe-focus' : ''}${showForm && quickMode ? ' ep-quick-mode-active' : ''}`}
    >
      {/* Patient header */}
      {patient && (
        <div className="page-header" style={{ marginBottom: '20px' }}>
          <div>
            <h1>{patient.fullName || '—'}</h1>
            <p>
              MRN: {patient.mrn}
              {patient.dateOfBirth && ` | Age: ${formatAge(patient.dateOfBirth)}`}
              {patient.gender && ` | ${formatGenderLabel(patient.gender)}`}
            </p>
          </div>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button className="btn-secondary" onClick={() => navigate('/hospital/doctor-dashboard')}>Doctor Dashboard</button>
            <button
              className="btn-secondary"
              onClick={() => navigate(`/hospital/patients/${id}`, {
                state: { prescriptionReturn: `${location.pathname}${location.search}` },
              })}
            >
              ← Patient
            </button>
            <button type="button" className="btn-secondary" title="Copy URL to clipboard" onClick={copyPatientShareLink}>
              Copy staff chart link
            </button>
            {doctorConfig.patientPortalBaseUrl?.trim() && (
              <button type="button" className="btn-secondary" title="Copy patient portal URL (configure base in EP Settings)" onClick={copyPatientPortalLink}>
                Copy patient portal link
              </button>
            )}
          </div>
        </div>
      )}

      {/* Toolbar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <h3 style={{ margin: 0 }}>Prescriptions</h3>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          {hasDraft && !showForm && (
            <span style={{ fontSize: '12px', color: '#f59e0b', background: '#fffbeb', padding: '3px 10px', borderRadius: '8px', border: '1px solid #fcd34d' }}>
              Unsaved draft —{' '}
              <button type="button" onClick={restoreDraft} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#d97706', fontWeight: 600, fontSize: '12px', padding: 0 }}>
                Restore
              </button>
            </span>
          )}
          {showForm && (
            <>
              <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '13px', cursor: 'pointer', userSelect: 'none' }}>
                <input
                  type="checkbox"
                  checked={quickMode}
                  onChange={e => {
                    const on = e.target.checked;
                    setQuickMode(on);
                    try {
                      localStorage.setItem('ep_quick_mode_default', String(on));
                    } catch {
                      // ignore
                    }
                  }}
                />
                Quick Mode (single-screen / tablet)
              </label>
              {quickMode && !showRxListWhilePrescribing && (
                <button type="button" className="btn-secondary" style={{ fontSize: '12px' }} onClick={() => setShowRxListWhilePrescribing(true)}>
                  Show Rx list
                </button>
              )}
              {quickMode && showRxListWhilePrescribing && showForm && (
                <button type="button" className="btn-secondary" style={{ fontSize: '12px' }} onClick={() => setShowRxListWhilePrescribing(false)}>
                  Hide list (focus prescribe)
                </button>
              )}
              {showForm && lastDraftSavedAt && (
                <span style={{ fontSize: '11px', color: '#6b7280' }} title="Local draft auto-save">
                  Draft saved {new Date(lastDraftSavedAt).toLocaleTimeString()}
                </span>
              )}
              <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '11px', color: '#6b7280', fontWeight: 600 }}>Session</span>
                <div style={{ display: 'inline-flex', borderRadius: '8px', border: '1px solid var(--border-color, #d1d5db)', overflow: 'hidden' }}>
                  <button
                    type="button"
                    className={epSessionMode === 'OPD' ? 'btn-primary' : 'btn-secondary'}
                    style={{ fontSize: '11px', padding: '4px 10px', borderRadius: 0, border: 'none' }}
                    onClick={() => setEpSessionModeAndUrl('OPD')}
                    title="Outpatient prescribing context (EP-1 / EP-11)"
                  >
                    OPD
                  </button>
                  <button
                    type="button"
                    className={epSessionMode === 'IPD' ? 'btn-primary' : 'btn-secondary'}
                    style={{ fontSize: '11px', padding: '4px 10px', borderRadius: 0, border: 'none' }}
                    onClick={() => setEpSessionModeAndUrl('IPD')}
                    title="Inpatient prescribing context (EP-1 / EP-11)"
                  >
                    IPD
                  </button>
                </div>
              </div>
              {showForm && (() => {
                const slaMin = doctorConfig.rxSlaTargetMinutes ?? 2;
                const slaSec = slaMin * 60;
                const over = rxElapsedSec > slaSec;
                return (
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 600,
                      padding: '4px 10px',
                      borderRadius: '8px',
                      border: `1px solid ${over ? '#fca5a5' : 'var(--border-color, #d1d5db)'}`,
                      background: over ? '#fef2f2' : '#f0fdf4',
                      color: over ? '#b91c1c' : '#166534',
                    }}
                    title="Easy Prescription session target (EP-1). Configure in EP Settings."
                  >
                    {epSessionMode} · EP {formatEpMmSs(rxElapsedSec)} · target ≤{slaMin} min
                    {over ? ' · over SLA' : ''}
                  </span>
                );
              })()}
            </>
          )}
          {canPrescribeRx && !showForm && (
            <button
              className="btn-primary"
              onClick={() => {
                resetForm();
                setEditing(null);
                setQuickMode(readQuickModeDefault());
                setShowForm(true);
              }}
            >
              + New Prescription
            </button>
          )}
        </div>
      </div>

      {/* RBAC hints */}
      {!canViewRx && <div className="error-message" role="alert">Not authorized to view prescriptions.</div>}
      {canViewRx && !canPrescribeRx && (
        <div className="rbac-hint-message" role="status">
          View-only access. Assign <strong>HOSPITAL_PRESCRIPTION_PRESCRIBE</strong> to create prescriptions.
        </div>
      )}
      {enableTransmitFeature && canPrescribeRx && !canTransmitRx && (
        <div className="rbac-hint-message" role="status">
          Can create but not transmit. Assign <strong>HOSPITAL_PRESCRIPTION_TRANSMIT</strong> for e-prescribing.
        </div>
      )}
      {error && <div className="error-message">{error}</div>}

      {/* Filter */}
      {showRxHistoryPanel && (
        <div className="filters-section" style={{ marginBottom: '16px' }}>
          <select value={filterStatus} onChange={e => setFilterStatus(e.target.value)} className="filter-select">
            <option value="">All Prescriptions</option>
            <option value="active">Active</option>
            <option value="drafts">Drafts</option>
          </select>
        </div>
      )}

      {/* Prescription form */}
      {showForm && renderForm()}

      {/* ── Modals ── */}

      {/* Template dialog (EP-7) */}
      {showTemplateDialog && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '700px', width: 'min(700px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
              <h3 style={{ margin: 0 }}>Apply Template</h3>
              <button className="btn-secondary" onClick={() => { setShowTemplateDialog(false); setTemplateApplySearch(''); }}>Close</button>
            </div>
            <TextField
              size="small"
              fullWidth
              placeholder="Search templates by name, disease, or findings…"
              value={templateApplySearch}
              onChange={(e) => setTemplateApplySearch(e.target.value)}
              sx={{ mb: 1.5 }}
            />
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '14px' }}>
              {(['mine', 'department', 'all', 'system', 'disease', 'doctor'] as const).map(key => (
                <button
                  key={key}
                  type="button"
                  className="btn-secondary"
                  style={{
                    fontSize: '11px',
                    padding: '4px 10px',
                    background: templateFilter === key ? 'var(--color-primary-light, #dbeafe)' : undefined,
                    borderColor: templateFilter === key ? 'var(--color-primary)' : undefined,
                  }}
                  onClick={() => setTemplateFilter(key)}
                >
                  {key === 'mine'
                    ? 'My templates'
                    : key === 'department'
                      ? 'My department'
                      : key === 'all'
                        ? 'All'
                        : key === 'system'
                          ? 'System'
                          : key === 'disease'
                            ? 'Disease'
                            : 'All custom'}
                </button>
              ))}
            </div>
            {templateFilter === 'department' && !linkedDoctor?.departmentId && (
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '10px' }}>
                Link your account to a doctor profile with a department to filter by department.
              </p>
            )}
            {templatesFiltered.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>No templates in this category.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {templatesFiltered.map(t => (
                  <div key={t.templateId} style={{ border: '1px solid var(--border-color)', borderRadius: '8px', padding: '12px 14px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                      <div>
                        <div style={{ fontWeight: 600, marginBottom: '4px' }}>{t.templateName}</div>
                        <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                          {t.templateType === 'SYSTEM' && <span style={{ color: '#0d9488', marginRight: '6px' }}>System</span>}
                          {t.templateType === 'DISEASE' && <span style={{ color: '#7c3aed', marginRight: '6px' }}>Disease</span>}
                          {t.templateType === 'DOCTOR' && <span style={{ color: '#2563eb', marginRight: '6px' }}>Doctor</span>}
                          {t.diseaseCategory && <span style={{ marginRight: '6px' }}>• {t.diseaseCategory}</span>}
                          <span>{t.medications.length} med(s)</span>
                          {t.tests && t.tests.length > 0 && <span> • {t.tests.length} test(s)</span>}
                          {t.followUpDays && <span> • Follow-up: {t.followUpDays}d</span>}
                        </div>
                        {t.diagnoses && t.diagnoses.length > 0 && (
                          <div style={{ fontSize: '11px', color: '#059669', marginTop: '3px' }}>
                            {t.diagnoses.map(dx => dx.code).join(', ')}
                          </div>
                        )}
                      </div>
                      <button className="btn-primary" style={{ flexShrink: 0, marginLeft: '12px' }} onClick={() => applyTemplate(t)}>
                        Apply
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* Copy previous Rx (EP-7) */}
      {showCopyPrevious && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '650px', width: 'min(650px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0 }}>Copy Previous Prescription</h3>
              <button className="btn-secondary" onClick={() => setShowCopyPrevious(false)}>Close</button>
            </div>
            {prescriptions.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)' }}>No previous prescriptions to copy.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {prescriptions.slice(0, 10).map(rx => (
                  <div key={rx.prescriptionId} style={{ border: '1px solid var(--border-color)', borderRadius: '8px', padding: '10px 14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '13px' }}>{rx.medicationName || (rx.medications?.[0]?.medicationName)}</div>
                      <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                        {rx.prescriptionNumber && `Rx#${rx.prescriptionNumber} · `}
                        {formatDate(rx.createdAt)} · {rx.prescriptionStatus}
                        {(rx.epEncounterMode === 'OPD' || rx.epEncounterMode === 'IPD') && ` · ${rx.epEncounterMode}`}
                        {rx.medications && rx.medications.length > 1 && ` · +${rx.medications.length - 1} more`}
                      </div>
                    </div>
                    <button className="btn-primary" style={{ flexShrink: 0, marginLeft: '12px' }} onClick={() => copyPreviousRx(rx)}>
                      Copy
                    </button>
                  </div>
                ))}
              </div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* Save template dialog (EP-7) */}
      {showSaveTemplate && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '420px', width: 'min(420px, 100%)' }}>
            <div className="modal-body">
            <h3 style={{ marginTop: 0 }}>Save as Template</h3>
            <div className="form-group">
              <label>Template Name *</label>
              <input type="text" value={newTemplateName} onChange={e => setNewTemplateName(e.target.value)} placeholder="e.g. My Hypertension Protocol" autoFocus />
            </div>
            <div className="form-group">
              <label>Type</label>
              <select value={newTemplateKind} onChange={e => setNewTemplateKind(e.target.value as 'DOCTOR' | 'DISEASE')} style={{ width: '100%', padding: '8px' }}>
                <option value="DOCTOR">Doctor (personal protocol)</option>
                <option value="DISEASE">Disease / condition protocol</option>
              </select>
            </div>
            {newTemplateKind === 'DISEASE' && (
              <div className="form-group">
                <label>Disease category (optional)</label>
                <input
                  type="text"
                  value={newTemplateDiseaseCategory}
                  onChange={e => setNewTemplateDiseaseCategory(e.target.value)}
                  placeholder="e.g. Respiratory, Renal"
                  style={{ width: '100%', padding: '8px' }}
                />
              </div>
            )}
            <div className="form-actions">
              <button
                className="btn-secondary"
                onClick={() => {
                  setShowSaveTemplate(false);
                  setNewTemplateName('');
                  setNewTemplateDiseaseCategory('');
                  setNewTemplateKind('DOCTOR');
                }}
              >
                Cancel
              </button>
              <button className="btn-primary" onClick={saveAsTemplate}>Save</button>
            </div>
            </div>
          </div>
        </div>
      )}

      {showAddCatalogDrugModal && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div
            className="modal-content"
            style={{
              maxWidth: '640px',
              width: 'min(640px, 100%)',
              maxHeight: 'min(92vh, 920px)',
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            <div className="modal-body" style={{ overflowY: 'auto', flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px', marginBottom: '14px' }}>
                <div>
                  <h3 style={{ margin: 0 }}>Add New Medicine</h3>
                  <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--text-secondary)' }}>
                    This creates a master medicine record (same list as Pharmacy → Drug Catalog) and adds it to this prescription.
                  </p>
                </div>
                <button type="button" className="btn-secondary" onClick={closeAddCatalogDrugModal}>
                  Close
                </button>
              </div>
              {addCatalogManufacturersLoading && (
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: 0 }}>Loading manufacturers…</p>
              )}
              <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
                <div className="form-group">
                  <label>Generic name *</label>
                  <input
                    type="text"
                    value={addCatalogDrugForm.genericName}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, genericName: e.target.value }))}
                    placeholder="e.g. Paracetamol"
                    autoFocus
                  />
                </div>
                <div className="form-group">
                  <label>Brand name</label>
                  <input
                    type="text"
                    value={addCatalogDrugForm.brandName ?? ''}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, brandName: e.target.value }))}
                    placeholder="Optional"
                  />
                </div>
                <div className="form-group">
                  <label>Strength</label>
                  <input
                    type="text"
                    value={addCatalogDrugForm.strength ?? ''}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, strength: e.target.value }))}
                    placeholder="e.g. 500 mg"
                  />
                </div>
                <div className="form-group">
                  <label>Form</label>
                  <select
                    value={addCatalogDrugFormSelect}
                    onChange={e => {
                      const value = e.target.value;
                      setAddCatalogDrugFormSelect(value);
                      if (value !== '__other__') {
                        setAddCatalogDrugForm(f => ({ ...f, form: value || '' }));
                      }
                    }}
                  >
                    <option value="">Not specified</option>
                    {PHARMACY_DRUG_FORM_PRESET_OPTIONS.map(opt => (
                      <option key={opt} value={opt}>
                        {opt}
                      </option>
                    ))}
                    <option value="__other__">Other (specify)</option>
                  </select>
                </div>
                {(addCatalogDrugFormSelect === '__other__' || (!addCatalogDrugFormSelect && !!addCatalogDrugForm.form)) && (
                  <div className="form-group">
                    <label>Form (custom)</label>
                    <input
                      type="text"
                      value={addCatalogDrugForm.form ?? ''}
                      onChange={e => setAddCatalogDrugForm(f => ({ ...f, form: e.target.value }))}
                      placeholder="Describe dosage form"
                    />
                  </div>
                )}
                <div className="form-group">
                  <label>Route</label>
                  <input
                    type="text"
                    value={addCatalogDrugForm.route ?? ''}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, route: e.target.value }))}
                    placeholder="e.g. ORAL"
                  />
                </div>
                <div className="form-group">
                  <label>Pack size</label>
                  <input
                    type="text"
                    value={addCatalogDrugForm.packSize ?? ''}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, packSize: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label>Unit of measure</label>
                  <input
                    type="text"
                    value={addCatalogDrugForm.unitOfMeasure ?? ''}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, unitOfMeasure: e.target.value }))}
                    placeholder="e.g. tablet, ml"
                  />
                </div>
                <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                  <label>Manufacturer *</label>
                  <select
                    value={addCatalogDrugForm.manufacturerId}
                    onChange={e => setAddCatalogDrugForm(f => ({ ...f, manufacturerId: e.target.value }))}
                    disabled={addCatalogManufacturersLoading}
                  >
                    <option value="">Select manufacturer…</option>
                    {addCatalogManufacturers.map(m => (
                      <option key={m.id} value={m.id}>
                        {m.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div
                  className="form-group"
                  style={{
                    gridColumn: '1 / -1',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
                    alignItems: 'center',
                    gap: '8px 12px',
                  }}
                >
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer', minWidth: 0 }}>
                    <input
                      type="checkbox"
                      checked={!!addCatalogDrugForm.active}
                      onChange={e => setAddCatalogDrugForm(f => ({ ...f, active: e.target.checked }))}
                    />
                    Active
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer', minWidth: 0 }}>
                    <input
                      type="checkbox"
                      checked={!!addCatalogDrugForm.controlledDrugFlag}
                      onChange={e => setAddCatalogDrugForm(f => ({ ...f, controlledDrugFlag: e.target.checked }))}
                    />
                    Controlled drug
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer', minWidth: 0 }}>
                    <input
                      type="checkbox"
                      checked={!!addCatalogDrugForm.batchRequired}
                      onChange={e => setAddCatalogDrugForm(f => ({ ...f, batchRequired: e.target.checked }))}
                    />
                    Batch required
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer', minWidth: 0 }}>
                    <input
                      type="checkbox"
                      checked={!!addCatalogDrugForm.expiryRequired}
                      onChange={e => setAddCatalogDrugForm(f => ({ ...f, expiryRequired: e.target.checked }))}
                    />
                    Expiry required
                  </label>
                </div>
              </div>
              <div className="form-actions" style={{ marginTop: '16px' }}>
                <button type="button" className="btn-secondary" onClick={closeAddCatalogDrugModal} disabled={addCatalogDrugSaving}>
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={() => void saveAddCatalogDrug()}
                  disabled={addCatalogDrugSaving || addCatalogManufacturersLoading}
                >
                  {addCatalogDrugSaving ? 'Saving…' : 'Save to catalog & add to Rx'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Interaction check */}
      {showInteractionCheck && interactionCheck && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '720px', width: 'min(720px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
              <div>
                <h3 style={{ margin: 0 }}>Drug interaction check</h3>
                <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Review alerts below before prescribing.
                </p>
              </div>
              <button className="btn-secondary" onClick={() => { setShowInteractionCheck(false); setInteractionCheck(null); }}>Close</button>
            </div>
            {interactionCheck.summary && (
              <p style={{ fontSize: '13px', marginBottom: '12px', color: 'var(--text-secondary)' }}>{interactionCheck.summary}</p>
            )}
            {interactionCheck.hasInteractions ? (
              <>
                <div className="error-message" style={{ marginBottom: '16px' }}>⚠ Alerts — review before prescribing</div>
                {interactionCheck.interactions.map((ix, i) => (
                  <div key={i} className="info-card" style={{ marginBottom: '12px', borderLeft: `4px solid ${ix.severity === 'MAJOR' || ix.severity === 'CONTRAINDICATED' ? '#ef4444' : '#f59e0b'}` }}>
                    {ix.interactionCategory && (
                      <div className="info-row"><span className="info-label">Category:</span><span className="info-value">{formatClinicalEnumLabel(ix.interactionCategory)}</span></div>
                    )}
                    <div className="info-row"><span className="info-label">Type:</span><span className="info-value">{formatClinicalEnumLabel(ix.interactionType) || '—'}</span></div>
                    <div className="info-row"><span className="info-label">Context:</span><span className="info-value"><strong>{ix.interactingMedication}</strong></span></div>
                    <div className="info-row"><span className="info-label">Severity:</span><span className="info-value">{ix.severity}</span></div>
                    {ix.clinicalSignificanceLevel && (
                      <div className="info-row"><span className="info-label">Clinical significance:</span><span className="info-value">{ix.clinicalSignificanceLevel}</span></div>
                    )}
                    {ix.description && <div className="info-row"><span className="info-label">Description:</span><span className="info-value">{ix.description}</span></div>}
                    {ix.clinicalSignificance && <div className="info-row"><span className="info-label">Risk:</span><span className="info-value">{ix.clinicalSignificance}</span></div>}
                    {ix.managementGuidance && <div className="info-row"><span className="info-label">Guidance:</span><span className="info-value">{ix.managementGuidance}</span></div>}
                    {ix.actionRequired && ix.actionRequired !== ix.managementGuidance && (
                      <div className="info-row"><span className="info-label">Action:</span><span className="info-value">{ix.actionRequired}</span></div>
                    )}
                    {ix.mechanism && <div className="info-row"><span className="info-label">Mechanism:</span><span className="info-value">{ix.mechanism}</span></div>}
                    {ix.evidenceLevel && <div className="info-row"><span className="info-label">Evidence:</span><span className="info-value">{ix.evidenceLevel}</span></div>}
                  </div>
                ))}
              </>
            ) : (
              <div className="info-card" style={{ background: '#d1fae5', borderLeft: '4px solid #10b981' }}>
                <strong>✓ No significant alerts for this check.</strong>
                <p style={{ margin: '8px 0 0', fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Screening still uses your optional context and chart data (problems, labs) when present. Enable an external interaction API in hospital-service config for vendor drug–drug data.
                </p>
              </div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* Allergy check */}
      {showAllergyCheck && allergyCheck && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '700px', width: 'min(700px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3>Allergy Check</h3>
              <button className="btn-secondary" onClick={() => { setShowAllergyCheck(false); setAllergyCheck(null); }}>Close</button>
            </div>
            {allergyCheck.hasAllergies ? (
              <>
                <div className="error-message" style={{ marginBottom: '16px' }}>⚠ Allergy Warnings</div>
                {allergyCheck.allergies.map((a, i) => (
                  <div key={i} className="info-card" style={{ marginBottom: '12px', borderLeft: '4px solid #ef4444' }}>
                    <div className="info-row"><span className="info-label">Allergen:</span><span className="info-value"><strong>{a.allergenName}</strong></span></div>
                    <div className="info-row"><span className="info-label">Severity:</span><span className="info-value">{a.severity.replace('_', ' ')}</span></div>
                    {a.reactionType && <div className="info-row"><span className="info-label">Reaction:</span><span className="info-value">{a.reactionType}</span></div>}
                  </div>
                ))}
              </>
            ) : (
              <div className="info-card" style={{ background: '#d1fae5', borderLeft: '4px solid #10b981' }}>
                <strong>✓ No allergy warnings</strong>
              </div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* Transmit */}
      {cancelDialogId && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '400px', width: 'min(400px, 100%)' }}>
            <div className="modal-body">
            <h3>Cancel Prescription</h3>
            <div className="form-group">
              <label>Cancellation Reason</label>
              <textarea
                rows={3}
                value={cancelReason}
                onChange={e => setCancelReason(e.target.value)}
                placeholder="Enter reason for cancellation…"
                style={{ width: '100%', padding: '8px', fontSize: '13px', boxSizing: 'border-box', resize: 'vertical' }}
                autoFocus
              />
            </div>
            <div className="form-actions">
              <button className="btn-secondary" onClick={() => { setCancelDialogId(null); setCancelReason(''); }}>Back</button>
              <button className="btn-danger" disabled={statusUpdatingId === cancelDialogId} onClick={handleCancelConfirm}>
                {statusUpdatingId === cancelDialogId ? 'Cancelling…' : 'Confirm Cancel'}
              </button>
            </div>
            </div>
          </div>
        </div>
      )}

      {canTransmitRx && showTransmitDialog && prescriptionToTransmit && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z }}>
          <div className="modal-content" style={{ maxWidth: '560px', width: 'min(560px, 100%)' }}>
            <div className="modal-header">
              <h3>Transmit Prescription</h3>
              <button
                className="modal-close"
                onClick={() => { setShowTransmitDialog(false); setPrescriptionToTransmit(null); setShowInlinePdmp(false); setInlinePdmpResult(null); }}
                aria-label="Close"
              >
                ×
              </button>
            </div>
            <div className="modal-body">
            <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '12px' }}>
              Sends to pharmacy routing in this deployment (EP-10). National eRx / external NCPDP connections require separate integration and are not wired here.
            </p>
            <p><strong>
              {prescriptionToTransmit.medicationName ||
                (prescriptionToTransmit.medications && prescriptionToTransmit.medications.length > 0
                  ? prescriptionToTransmit.medications.map(m => m.medicationName).filter(Boolean).join(', ')
                  : 'Unnamed')}
            </strong></p>

            {/* Warnings summary */}
            {prescriptionToTransmit.hasInteractions && (
              <div className="error-message" style={{ marginBottom: '8px' }}>
                ⚠ Drug interactions detected.{' '}
                <button className="btn-link" style={{ fontSize: '12px' }} onClick={() => { setShowTransmitDialog(false); handleOpenInteractionAck(prescriptionToTransmit); }}>
                  Review &amp; Acknowledge →
                </button>
              </div>
            )}
            {prescriptionToTransmit.hasAllergyWarnings && (
              <div className="error-message" style={{ marginBottom: '8px' }}>
                ⚠ Allergy warnings detected.{' '}
                <button className="btn-link" style={{ fontSize: '12px' }} onClick={() => { setShowTransmitDialog(false); handleOpenAllergyAck(prescriptionToTransmit); }}>
                  Review &amp; Acknowledge →
                </button>
              </div>
            )}
            {prescriptionToTransmit.isControlledSubstance && !prescriptionToTransmit.pdmpQueried && (
              <div style={{ background: '#fffbeb', border: '1px solid #f59e0b', borderRadius: '6px', padding: '12px', marginBottom: '8px', fontSize: '12px', color: '#92400e' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong>⚠ PDMP not queried for this controlled substance (Schedule {prescriptionToTransmit.schedule || '?'})</strong>
                  <button
                    className="btn-link"
                    style={{ fontSize: '12px', fontWeight: 600 }}
                    onClick={() => {
                      setShowInlinePdmp(v => !v);
                      if (!showInlinePdmp) {
                        setPdmpQueryState('');
                        setPdmpReason('');
                        setPdmpDeaNumber(prescriptionToTransmit.deaNumber || '');
                        setInlinePdmpResult(null);
                      }
                    }}
                  >
                    {showInlinePdmp ? 'Hide ▲' : 'Query PDMP ▼'}
                  </button>
                </div>
                <p style={{ margin: '4px 0 0' }}>
                  Query below, or check <em>Override PDMP Check</em> with a documented rationale.
                </p>

                {showInlinePdmp && (
                  <div style={{ marginTop: '12px', paddingTop: '12px', borderTop: '1px solid #f59e0b' }}>
                    {inlinePdmpResult ? (
                      /* ── Inline result summary ── */
                      <div>
                        <div style={{
                          display: 'flex', alignItems: 'center', gap: '8px',
                          background: inlinePdmpResult.riskLevel === 'HIGH' || inlinePdmpResult.riskLevel === 'VERY_HIGH'
                            ? '#fef2f2' : inlinePdmpResult.riskLevel === 'MODERATE' ? '#fffbeb' : '#d1fae5',
                          border: `1px solid ${inlinePdmpResult.riskLevel === 'HIGH' || inlinePdmpResult.riskLevel === 'VERY_HIGH'
                            ? '#fca5a5' : inlinePdmpResult.riskLevel === 'MODERATE' ? '#fcd34d' : '#6ee7b7'}`,
                          borderRadius: '6px', padding: '8px 12px', marginBottom: '8px',
                        }}>
                          <span style={{ fontSize: '16px' }}>
                            {inlinePdmpResult.riskLevel === 'HIGH' || inlinePdmpResult.riskLevel === 'VERY_HIGH' ? '🔴' :
                             inlinePdmpResult.riskLevel === 'MODERATE' ? '🟡' : '✅'}
                          </span>
                          <div>
                            <strong>PDMP queried — {inlinePdmpResult.queryState} · {inlinePdmpResult.queryStatus}</strong>
                            {inlinePdmpResult.riskScore != null && (
                              <span style={{ marginLeft: '8px' }}>
                                Risk score: <strong>{inlinePdmpResult.riskScore}</strong> ({inlinePdmpResult.riskLevel})
                              </span>
                            )}
                          </div>
                        </div>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginBottom: '6px' }}>
                          {inlinePdmpResult.totalPrescriptions != null && (
                            <span style={{ background: '#f3f4f6', color: '#374151', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>
                              {inlinePdmpResult.totalPrescriptions} controlled Rx on record
                            </span>
                          )}
                          {inlinePdmpResult.hasDuplicatePrescriptions && <span style={{ background: '#fef2f2', color: '#ef4444', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Duplicate Rx</span>}
                          {inlinePdmpResult.hasOverlappingPrescriptions && <span style={{ background: '#fef2f2', color: '#ef4444', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Overlapping Rx</span>}
                          {inlinePdmpResult.hasEarlyRefills && <span style={{ background: '#fffbeb', color: '#d97706', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Early Refills</span>}
                          {inlinePdmpResult.hasMultiplePrescribers && <span style={{ background: '#fffbeb', color: '#d97706', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Multiple Prescribers</span>}
                          {inlinePdmpResult.hasMultiplePharmacies && <span style={{ background: '#fffbeb', color: '#d97706', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Multiple Pharmacies</span>}
                        </div>
                        {inlinePdmpResult.warnings && (
                          <div style={{ background: '#fffbeb', borderRadius: '6px', padding: '6px 8px', fontSize: '11px', color: '#92400e', marginBottom: '6px' }}>
                            {inlinePdmpResult.warnings}
                          </div>
                        )}
                        <p style={{ margin: 0, color: '#065f46', fontWeight: 600 }}>
                          ✓ PDMP check satisfied — you may now transmit.
                        </p>
                      </div>
                    ) : (
                      /* ── Inline query form ── */
                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '8px' }}>
                        <div className="form-group" style={{ marginBottom: 0 }}>
                          <label style={{ color: '#92400e' }}>Query State *</label>
                          <input
                            value={pdmpQueryState}
                            onChange={e => setPdmpQueryState(e.target.value)}
                            placeholder="e.g. CA, TX, NY"
                            style={{ fontSize: '12px' }}
                          />
                        </div>
                        <div className="form-group" style={{ marginBottom: 0 }}>
                          <label style={{ color: '#92400e' }}>DEA Number</label>
                          <input
                            value={pdmpDeaNumber}
                            onChange={e => setPdmpDeaNumber(e.target.value)}
                            placeholder="DEA#"
                            style={{ fontSize: '12px' }}
                          />
                        </div>
                        <div className="form-group" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
                          <label style={{ color: '#92400e' }}>Query Reason</label>
                          <input
                            value={pdmpReason}
                            onChange={e => setPdmpReason(e.target.value)}
                            placeholder="e.g. Pre-prescribing controlled substance review"
                            style={{ fontSize: '12px' }}
                          />
                        </div>
                        <div style={{ gridColumn: '1 / -1' }}>
                          <button
                            className="btn-primary"
                            style={{ fontSize: '12px', padding: '6px 14px' }}
                            disabled={pdmpLoading}
                            onClick={handleInlinePdmpQuery}
                          >
                            {pdmpLoading ? 'Querying PDMP…' : 'Submit PDMP Query'}
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Pharmacy assignment */}
            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '6px', padding: '10px 12px', marginBottom: '12px', fontSize: '13px' }}>
              <div style={{ fontWeight: 600, marginBottom: '8px', color: '#374151' }}>Pharmacy</div>
              {!(transmitData.pharmacyName || transmitData.pharmacyNpi || transmitData.pharmacyId) && (
                <div style={{ color: '#92400e', fontSize: '12px', marginBottom: '8px' }}>⚠ No pharmacy assigned — enter pharmacy name or NPI to transmit</div>
              )}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '8px' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Pharmacy Name</label>
                  <input type="text" value={transmitData.pharmacyName || ''} onChange={e => setTransmitData(p => ({ ...p, pharmacyName: e.target.value }))} placeholder="e.g. City Pharmacy" style={{ fontSize: '12px' }} />
                </div>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label>Pharmacy NPI</label>
                  <input
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={transmitData.pharmacyNpi || ''}
                    onChange={e => setTransmitData(p => ({ ...p, pharmacyNpi: digitsOnly(e.target.value) }))}
                    placeholder="10-digit NPI"
                    style={{ fontSize: '12px' }}
                  />
                </div>
              </div>
            </div>

            {/* Override checkboxes */}
            <div className="form-group">
              <label><input type="checkbox" checked={transmitData.overrideInteractions} onChange={e => setTransmitData(p => ({ ...p, overrideInteractions: e.target.checked }))} /> Override Interactions</label>
            </div>
            <div className="form-group">
              <label><input type="checkbox" checked={transmitData.overrideAllergies} onChange={e => setTransmitData(p => ({ ...p, overrideAllergies: e.target.checked }))} /> Override Allergies</label>
            </div>
            {prescriptionToTransmit.isControlledSubstance && (
              <div className="form-group">
                <label><input type="checkbox" checked={transmitData.overridePdmpCheck} onChange={e => setTransmitData(p => ({ ...p, overridePdmpCheck: e.target.checked }))} /> Override PDMP Check requirement</label>
              </div>
            )}
            {(transmitData.overrideInteractions || transmitData.overrideAllergies || transmitData.overridePdmpCheck) && (
              <div className="form-group">
                <label>Override Reason *</label>
                <textarea required value={transmitData.overrideReason} onChange={e => setTransmitData(p => ({ ...p, overrideReason: e.target.value }))} rows={3} placeholder="Document clinical rationale for overriding safety checks…" />
              </div>
            )}
            <div className="form-actions">
              <button className="btn-secondary" onClick={() => { setShowTransmitDialog(false); setPrescriptionToTransmit(null); setShowInlinePdmp(false); setInlinePdmpResult(null); }}>Cancel</button>
              <button
                className="btn-primary"
                disabled={
                  !(prescriptionToTransmit.pharmacyId || prescriptionToTransmit.pharmacyNpi || transmitData.pharmacyId || transmitData.pharmacyNpi) ||
                  (prescriptionToTransmit.isControlledSubstance === true &&
                  !prescriptionToTransmit.pdmpQueried &&
                  !transmitData.overridePdmpCheck)
                }
                title={
                  !(prescriptionToTransmit.pharmacyId || prescriptionToTransmit.pharmacyNpi || transmitData.pharmacyId || transmitData.pharmacyNpi)
                    ? 'Enter a Pharmacy NPI above before transmitting'
                    : prescriptionToTransmit.isControlledSubstance === true &&
                      !prescriptionToTransmit.pdmpQueried &&
                      !transmitData.overridePdmpCheck
                    ? 'Query PDMP above or check Override PDMP Check before transmitting a controlled substance'
                    : undefined
                }
                onClick={handleTransmit}
              >
                Transmit
              </button>
            </div>
            </div>
          </div>
        </div>
      )}

      {/* Print preview (EP-8) */}
      {showPrint && printData && (
        <PrintableRx
          data={printData}
          onSaveBeforePrint={undefined}
          onAfterPrint={() => navigate('/hospital/doctor-dashboard')}
          onEdit={() => {
            setShowPrint(false);
            const sourceRx = printData.sourcePrescription;
            setPrintData(null);
            if (sourceRx) {
              handleEdit(sourceRx);
            } else {
              setShowForm(true);
            }
          }}
          onClose={() => { setShowPrint(false); setPrintData(null); }}
        />
      )}

      {/* Prescription list */}
      {!showRxHistoryPanel ? null : prescriptions.length === 0 ? (
        <div className="empty-state"><p>No prescriptions found</p></div>
      ) : (
        <div className="table-container">
          <table className="data-table prescription-list-table">
            <thead>
              <tr>
                <th>Rx #</th>
                <th>EP</th>
                <th>Medication(s)</th>
                <th>Dosage</th>
                <th>Frequency</th>
                <th>Start</th>
                <th>Status</th>
                <th>Warnings</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {prescriptions.map(rx => (
                <tr key={rx.prescriptionId}>
                  <td>{rx.prescriptionNumber || '—'}</td>
                  <td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{rx.epEncounterMode ?? '—'}</td>
                  <td>
                    {(rx.medications && rx.medications.length > 0 ? rx.medications : [{ medicationName: rx.medicationName, dosageStrength: rx.dosageStrength, dosageUnit: rx.dosageUnit, dosageForm: rx.dosageForm, frequency: rx.frequency }]).map((m, i) => (
                      <div key={i}><strong>{m.medicationName}</strong></div>
                    ))}
                  </td>
                  <td>
                    {(rx.medications && rx.medications.length > 0 ? rx.medications : [{ medicationName: rx.medicationName, dosageStrength: rx.dosageStrength, dosageUnit: rx.dosageUnit, dosageForm: rx.dosageForm, frequency: rx.frequency }]).map((m, i) => (
                      <div key={i}>{m.dosageStrength && m.dosageUnit ? `${m.dosageStrength} ${m.dosageUnit}` : '—'} {m.dosageForm}</div>
                    ))}
                  </td>
                  <td>
                    {(rx.medications && rx.medications.length > 0 ? rx.medications : [{ medicationName: rx.medicationName, dosageStrength: rx.dosageStrength, dosageUnit: rx.dosageUnit, dosageForm: rx.dosageForm, frequency: rx.frequency }]).map((m, i) => (
                      <div key={i}>{m.frequency || '—'}</div>
                    ))}
                  </td>
                  <td>{formatDate(rx.startDate)}</td>
                  <td>
                    <div className="rx-status-cell">
                      <span className={`status-badge status-${rx.prescriptionStatus.toLowerCase()}`}>{rx.prescriptionStatus}</span>
                      {canPrescribeRx && getQuickStatusOptions(rx.prescriptionStatus).length > 0 && (
                        <select
                          className="rx-quick-status-select"
                          value=""
                          disabled={statusUpdatingId === rx.prescriptionId}
                          onChange={e => {
                            void handleQuickStatusChange(rx.prescriptionId, rx.prescriptionStatus, e.target.value);
                            e.currentTarget.value = '';
                          }}
                        >
                          <option value="">Change status...</option>
                          {getQuickStatusOptions(rx.prescriptionStatus).map(opt => (
                            <option key={opt.value} value={opt.value}>{opt.label}</option>
                          ))}
                        </select>
                      )}
                    </div>
                  </td>
                  <td>
                    {rx.hasInteractions && (
                      <span style={{ color: '#ef4444', marginRight: '6px', cursor: 'pointer' }} title="Click to review interactions" onClick={() => handleOpenInteractionAck(rx)}>⚠ Int.</span>
                    )}
                    {rx.hasAllergyWarnings && (
                      <span style={{ color: '#ef4444', marginRight: '6px', cursor: 'pointer' }} title="Click to review allergy warnings" onClick={() => handleOpenAllergyAck(rx)}>⚠ Allergy</span>
                    )}
                    {rx.pdmpQueried && <span style={{ color: '#10b981', marginLeft: '4px' }} title="PDMP queried">✓ PDMP</span>}
                    {rx.isControlledSubstance && !rx.pdmpQueried && <span style={{ color: '#f59e0b', marginLeft: '4px' }} title="PDMP not queried for controlled substance">⚠ PDMP</span>}
                    {!rx.hasInteractions && !rx.hasAllergyWarnings && !rx.pdmpQueried && !rx.isControlledSubstance && '—'}
                  </td>
                  <td>
                    <div className="action-buttons">
                      {canViewRx && <button className="btn-link" onClick={() => handlePrintSavedRx(rx.prescriptionId)}>View</button>}
                      {canPrescribeRx && rx.prescriptionStatus === 'DRAFT' && (
                        <>
                          <button className="btn-link" onClick={() => handleEdit(rx)}>Edit</button>
                          <button className="btn-link btn-danger" onClick={() => handleDelete(rx.prescriptionId)}>Delete</button>
                          <button className="btn-link" title="Submit prescription — moves status to Pending" onClick={() => handleValidate(rx.prescriptionId)}>Submit</button>
                        </>
                      )}
                      {canTransmitRx && (rx.prescriptionStatus === 'DRAFT' || rx.prescriptionStatus === 'PENDING') && (
                        <button className="btn-link" onClick={() => {
                          setPrescriptionToTransmit(rx);
                          setTransmitData(prev => ({
                            ...prev,
                            pharmacyId: rx.pharmacyId || undefined,
                            pharmacyNpi: digitsOnly(rx.pharmacyNpi) || undefined,
                            pharmacyName: rx.pharmacyName || undefined,
                          }));
                          setShowTransmitDialog(true);
                        }}>Transmit</button>
                      )}
                      {canPrescribeRx && rx.prescriptionStatus !== 'CANCELLED' && rx.prescriptionStatus !== 'EXPIRED' && (
                        <>
                          <button className="btn-link" onClick={() => handleOpenInteractionAck(rx)} title="Review & acknowledge drug interactions">Int. Ack</button>
                          <button className="btn-link" onClick={() => handleOpenAllergyAck(rx)} title="Review & acknowledge allergy warnings">Allergy Ack</button>
                          {rx.isControlledSubstance && (
                            <button className="btn-link" style={{ color: '#f59e0b' }} onClick={() => handleOpenPdmp(rx)}>PDMP</button>
                          )}
                          {enableFormularyAndPaFeatures && (
                            <>
                              <button className="btn-link" onClick={() => handleOpenFormulary(rx)}>Formulary</button>
                              <button className="btn-link" onClick={() => handleOpenPa(rx)}>Prior Auth</button>
                            </>
                          )}
                          <button className="btn-link btn-danger" onClick={() => handleCancel(rx.prescriptionId)}>Cancel</button>
                        </>
                      )}
                      {canViewRx && (rx.prescriptionStatus === 'SENT' || rx.prescriptionStatus === 'FILLED' || rx.prescriptionStatus === 'PARTIALLY_FILLED') && (
                        <button className="btn-link" onClick={() => handleOpenTransmissions(rx)}>Transmissions</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {/* ── PDMP Modal ── */}
      {showPdmpModal && pdmpPrescription && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '860px', width: 'min(860px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0 }}>PDMP Query — Controlled Substance Monitoring</h3>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>
                  {pdmpPrescription.medicationName} · Schedule {pdmpPrescription.schedule || '?'} · Rx# {pdmpPrescription.prescriptionNumber || '—'}
                </p>
              </div>
              <button className="btn-secondary" onClick={() => setShowPdmpModal(false)}>Close</button>
            </div>

            {/* Query form */}
            <div className="form-section" style={{ background: '#f8fafc', borderRadius: '8px', padding: '16px', marginBottom: '20px' }}>
              <h4 className="form-section-title" style={{ marginBottom: '12px' }}>Submit New PDMP Query</h4>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '12px' }}>
                <div className="form-group">
                  <label>Query State *</label>
                  <input value={pdmpQueryState} onChange={e => setPdmpQueryState(e.target.value)} placeholder="e.g. CA, TX, NY" />
                </div>
                <div className="form-group">
                  <label>DEA Number</label>
                  <input value={pdmpDeaNumber} onChange={e => setPdmpDeaNumber(e.target.value)} placeholder="DEA#" />
                </div>
                <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                  <label>Query Reason</label>
                  <input value={pdmpReason} onChange={e => setPdmpReason(e.target.value)} placeholder="e.g. Pre-prescribing controlled substance review" />
                </div>
              </div>
              <button className="btn-primary" disabled={pdmpLoading} onClick={handleQueryPdmp} style={{ marginTop: '8px' }}>
                {pdmpLoading ? 'Querying PDMP…' : 'Submit PDMP Query'}
              </button>
            </div>

            {/* Latest result */}
            {pdmpQueryResult && (
              <div style={{ marginBottom: '20px' }}>
                <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px' }}>Latest Query Result</h4>
                <div className="info-card" style={{ borderLeft: `4px solid ${pdmpQueryResult.riskLevel === 'HIGH' || pdmpQueryResult.riskLevel === 'VERY_HIGH' ? '#ef4444' : pdmpQueryResult.riskLevel === 'MODERATE' ? '#f59e0b' : '#10b981'}` }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', marginBottom: '12px' }}>
                    <div><span className="info-label">Status:</span> <span className="info-value">{pdmpQueryResult.queryStatus}</span></div>
                    <div><span className="info-label">State:</span> <span className="info-value">{pdmpQueryResult.queryState}</span></div>
                    <div><span className="info-label">Date:</span> <span className="info-value">{pdmpQueryResult.queryDate ? new Date(pdmpQueryResult.queryDate).toLocaleString() : '—'}</span></div>
                    <div><span className="info-label">Total Rx:</span> <span className="info-value">{pdmpQueryResult.totalPrescriptions ?? '—'}</span></div>
                    <div><span className="info-label">Pharmacies:</span> <span className="info-value">{pdmpQueryResult.totalPharmacies ?? '—'}</span></div>
                    <div><span className="info-label">Prescribers:</span> <span className="info-value">{pdmpQueryResult.totalPrescribers ?? '—'}</span></div>
                  </div>
                  {pdmpQueryResult.riskScore != null && (
                    <div style={{ marginBottom: '8px' }}>
                      <span className="info-label">Risk Score:</span>
                      <span style={{ marginLeft: '8px', fontWeight: 700, color: pdmpQueryResult.riskLevel === 'HIGH' || pdmpQueryResult.riskLevel === 'VERY_HIGH' ? '#ef4444' : pdmpQueryResult.riskLevel === 'MODERATE' ? '#f59e0b' : '#10b981' }}>
                        {pdmpQueryResult.riskScore} — {pdmpQueryResult.riskLevel}
                      </span>
                    </div>
                  )}
                  {/* Flags */}
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '8px' }}>
                    {pdmpQueryResult.hasDuplicatePrescriptions && <span style={{ background: '#fef2f2', color: '#ef4444', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Duplicate Rx</span>}
                    {pdmpQueryResult.hasOverlappingPrescriptions && <span style={{ background: '#fef2f2', color: '#ef4444', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Overlapping Rx</span>}
                    {pdmpQueryResult.hasEarlyRefills && <span style={{ background: '#fffbeb', color: '#d97706', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Early Refills</span>}
                    {pdmpQueryResult.hasMultiplePrescribers && <span style={{ background: '#fffbeb', color: '#d97706', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Multiple Prescribers</span>}
                    {pdmpQueryResult.hasMultiplePharmacies && <span style={{ background: '#fffbeb', color: '#d97706', padding: '2px 8px', borderRadius: '12px', fontSize: '11px' }}>⚠ Multiple Pharmacies</span>}
                  </div>
                  {pdmpQueryResult.warnings && (
                    <div style={{ background: '#fffbeb', borderRadius: '6px', padding: '8px', fontSize: '12px', color: '#92400e' }}>{pdmpQueryResult.warnings}</div>
                  )}
                </div>

                {/* Prescription history */}
                {pdmpQueryResult.prescriptionHistory && pdmpQueryResult.prescriptionHistory.length > 0 && (
                  <div style={{ marginTop: '16px' }}>
                    <h5 style={{ fontSize: '13px', fontWeight: 600, marginBottom: '8px' }}>Controlled Substance History ({pdmpQueryResult.prescriptionHistory.length})</h5>
                    <div style={{ maxHeight: '240px', overflow: 'auto', border: '1px solid #e5e7eb', borderRadius: '6px' }}>
                      <table className="data-table" style={{ fontSize: '12px' }}>
                        <thead><tr><th>Medication</th><th>Schedule</th><th>Est. units</th><th>Prescribed</th><th>Filled</th><th>Prescriber</th><th>Pharmacy</th><th>Status</th></tr></thead>
                        <tbody>
                          {pdmpQueryResult.prescriptionHistory.map((item, i) => (
                            <tr key={i}>
                              <td>{item.medicationName}</td>
                              <td>{item.schedule || '—'}</td>
                              <td>{item.quantity}</td>
                              <td>{item.prescribedDate ? new Date(item.prescribedDate).toLocaleDateString() : '—'}</td>
                              <td>{item.filledDate ? new Date(item.filledDate).toLocaleDateString() : '—'}</td>
                              <td>{item.prescriberName}</td>
                              <td>{item.pharmacyName}</td>
                              <td>{item.status}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Previous queries */}
            {pdmpHistory.length > 1 && (
              <div>
                <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '8px' }}>Previous Queries ({pdmpHistory.length - 1})</h4>
                {pdmpHistory.slice(1).map((h, i) => (
                  <div key={i} className="info-card" style={{ marginBottom: '8px', cursor: 'pointer' }} onClick={() => setPdmpQueryResult(h)}>
                    <span style={{ fontSize: '12px' }}>{h.queryDate ? new Date(h.queryDate).toLocaleString() : '—'} · {h.queryState} · {h.queryStatus} · Risk: {h.riskLevel || '—'}</span>
                  </div>
                ))}
              </div>
            )}

            {!pdmpQueryResult && pdmpHistory.length === 0 && !pdmpLoading && (
              <div className="empty-state"><p>No PDMP queries yet. Submit a query above to check patient's controlled substance history.</p></div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* ── Formulary Check Modal ── */}
      {enableFormularyAndPaFeatures && showFormularyModal && formularyPrescription && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '780px', width: 'min(780px, 100%)' }}>
            <div className="modal-header">
              <div>
                <h3 style={{ margin: 0 }}>Formulary & Coverage Check</h3>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>{formularyPrescription.medicationName}</p>
              </div>
              <button className="modal-close" onClick={() => setShowFormularyModal(false)} aria-label="Close">×</button>
            </div>
            <div className="modal-body">

            <div style={{ marginBottom: '16px' }}>
              <button className="btn-primary" disabled={formularyLoading} onClick={handleCheckFormulary}>
                {formularyLoading ? 'Checking…' : 'Run Formulary Check'}
              </button>
              <span style={{ marginLeft: '12px', fontSize: '12px', color: 'var(--text-secondary)' }}>Verifies insurance formulary coverage, tier, copay, and prior authorization requirements</span>
            </div>

            {formularyLoading && !formularyResult && (
              <div className="loading">Checking formulary coverage…</div>
            )}

            {formularyResult && (
              <>
                <div className="info-card" style={{ borderLeft: `4px solid ${formularyResult.coverageStatus === 'COVERED' ? '#10b981' : formularyResult.coverageStatus === 'NOT_COVERED' ? '#ef4444' : '#f59e0b'}`, marginBottom: '16px' }}>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '8px' }}>
                    <div>
                      <span className="info-label">Coverage Status:</span>
                      <strong style={{ marginLeft: '8px', color: formularyResult.coverageStatus === 'COVERED' ? '#10b981' : formularyResult.coverageStatus === 'NOT_COVERED' ? '#ef4444' : '#f59e0b' }}>
                        {formularyResult.coverageStatus?.replace(/_/g, ' ') || '—'}
                      </strong>
                    </div>
                    <div><span className="info-label">Formulary Tier:</span> <span className="info-value">{formularyResult.formularyTier || '—'}</span></div>
                    <div><span className="info-label">Plan / PBM:</span> <span className="info-value">{formularyResult.insuranceCompanyName || formularyResult.pbmName || '—'}</span></div>
                    <div><span className="info-label">Formulary:</span> <span className="info-value">{formularyResult.formularyName || '—'}</span></div>
                    {formularyResult.copayAmount != null && (
                      <div><span className="info-label">Copay:</span> <span className="info-value">${formularyResult.copayAmount.toFixed(2)}</span></div>
                    )}
                    {formularyResult.patientCostEstimate != null && (
                      <div><span className="info-label">Est. Patient Cost:</span> <strong style={{ marginLeft: '8px', color: '#2563eb' }}>${formularyResult.patientCostEstimate.toFixed(2)}</strong></div>
                    )}
                    {formularyResult.insurancePays != null && (
                      <div><span className="info-label">Insurance Pays:</span> <span className="info-value">${formularyResult.insurancePays.toFixed(2)}</span></div>
                    )}
                    {formularyResult.coinsurancePercentage != null && (
                      <div><span className="info-label">Coinsurance:</span> <span className="info-value">{formularyResult.coinsurancePercentage}%</span></div>
                    )}
                    {formularyResult.quantityLimit != null && (
                      <div><span className="info-label">Qty Limit:</span> <span className="info-value">{formularyResult.quantityLimit}</span></div>
                    )}
                    {formularyResult.daysSupplyLimit != null && (
                      <div><span className="info-label">Days Supply Limit:</span> <span className="info-value">{formularyResult.daysSupplyLimit}</span></div>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '12px', flexWrap: 'wrap' }}>
                    {(formularyResult.requiresPriorAuthorization || formularyResult.priorAuthorizationRequired) && (
                      <span style={{ background: '#fef2f2', color: '#ef4444', padding: '3px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 600 }}>⚠ Prior Authorization Required</span>
                    )}
                    {formularyResult.stepTherapyRequired && (
                      <span style={{ background: '#fffbeb', color: '#d97706', padding: '3px 10px', borderRadius: '12px', fontSize: '12px' }}>Step Therapy Required</span>
                    )}
                    {formularyResult.deductibleApplies && (
                      <span style={{ background: '#eff6ff', color: '#2563eb', padding: '3px 10px', borderRadius: '12px', fontSize: '12px' }}>Deductible Applies</span>
                    )}
                  </div>
                  {(formularyResult.requiresPriorAuthorization || formularyResult.priorAuthorizationRequired) && (
                    <div style={{ marginTop: '12px' }}>
                      <button className="btn-primary" style={{ fontSize: '12px' }} onClick={() => { setShowFormularyModal(false); handleOpenPa(formularyPrescription); }}>
                        Submit Prior Authorization Request →
                      </button>
                    </div>
                  )}
                  {formularyResult.checkDate && (
                    <div style={{ marginTop: '8px', fontSize: '11px', color: 'var(--text-secondary)' }}>Checked: {new Date(formularyResult.checkDate).toLocaleString()}</div>
                  )}
                  {formularyResult.errorMessage && (
                    <div className="error-message" style={{ marginTop: '8px' }}>{formularyResult.errorMessage}</div>
                  )}
                </div>

                {/* Formulary alternatives */}
                {formularyResult.alternatives && formularyResult.alternatives.length > 0 && (
                  <div>
                    <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '10px' }}>Formulary Alternatives ({formularyResult.alternatives.length})</h4>
                    {formularyResult.alternatives.map((alt, i) => (
                      <div key={i} className="info-card" style={{ marginBottom: '8px', borderLeft: `4px solid ${alt.coverageStatus === 'COVERED' ? '#10b981' : '#f59e0b'}` }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <div>
                            <strong>{alt.medicationName}</strong>
                            {alt.genericName && <span style={{ fontSize: '12px', color: 'var(--text-secondary)', marginLeft: '8px' }}>({alt.genericName})</span>}
                            {alt.isPreferred && <span style={{ background: '#d1fae5', color: '#065f46', padding: '1px 6px', borderRadius: '10px', fontSize: '11px', marginLeft: '8px' }}>Preferred</span>}
                          </div>
                          <div style={{ textAlign: 'right', fontSize: '12px' }}>
                            <div>Tier {alt.formularyTier || '—'} · {alt.coverageStatus?.replace(/_/g, ' ')}</div>
                            {alt.patientCostEstimate != null && <div style={{ color: '#2563eb', fontWeight: 600 }}>~${alt.patientCostEstimate.toFixed(2)}</div>}
                          </div>
                        </div>
                        {alt.reason && <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>{alt.reason}</div>}
                        {alt.requiresPriorAuthorization && <div style={{ fontSize: '11px', color: '#ef4444', marginTop: '2px' }}>Requires Prior Authorization</div>}
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}

            {!formularyResult && !formularyLoading && (
              <div className="empty-state"><p>No formulary check on record. Click "Run Formulary Check" to verify insurance coverage, tier status, patient cost estimate, and prior authorization requirements.</p></div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* ── Prior Authorization Modal ── */}
      {enableFormularyAndPaFeatures && showPaModal && paPrescription && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '780px', width: 'min(780px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0 }}>Prior Authorization</h3>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>{paPrescription.medicationName} · Rx# {paPrescription.prescriptionNumber || '—'}</p>
              </div>
              <button className="btn-secondary" onClick={() => setShowPaModal(false)}>Close</button>
            </div>

            {/* Submit new PA */}
            <div className="form-section" style={{ background: '#f8fafc', borderRadius: '8px', padding: '16px', marginBottom: '20px' }}>
              <h4 className="form-section-title" style={{ marginBottom: '12px' }}>Submit New Prior Authorization Request</h4>
              <div className="form-group">
                <label>Clinical Justification *</label>
                <textarea
                  rows={4}
                  value={paJustification}
                  onChange={e => setPaJustification(e.target.value)}
                  placeholder="Document medical necessity, failed alternatives, patient condition, diagnosis codes, and why this medication is required…"
                />
              </div>
              <div className="form-group">
                <label>Supporting Documentation References</label>
                <input
                  value={paDocs}
                  onChange={e => setPaDocs(e.target.value)}
                  placeholder="e.g. Lab result IDs, imaging study IDs, previous treatment records…"
                />
              </div>
              <button className="btn-primary" disabled={paLoading} onClick={handleSubmitPa}>
                {paLoading ? 'Submitting…' : 'Submit Prior Authorization'}
              </button>
            </div>

            {/* PA list */}
            {paList.length > 0 ? (
              <div>
                <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '12px' }}>Prior Authorization Requests ({paList.length})</h4>
                {paList.map((pa, i) => (
                  <div key={i} className="info-card" style={{ marginBottom: '12px', borderLeft: `4px solid ${pa.status === 'APPROVED' ? '#10b981' : pa.status === 'DENIED' ? '#ef4444' : '#f59e0b'}` }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px', gap: '8px', flexWrap: 'wrap' }}>
                      <div>
                        <strong style={{ fontSize: '14px' }}>PA #{pa.priorAuthNumber || pa.priorAuthId?.slice(0, 8) || '—'}</strong>
                        <span className={`status-badge status-${(pa.status || 'pending').toLowerCase()}`} style={{ marginLeft: '8px' }}>{pa.status}</span>
                      </div>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button
                          className="btn-secondary"
                          style={{ fontSize: '11px', padding: '3px 10px' }}
                          onClick={() => pa.priorAuthId && handleCheckPaStatus(pa.priorAuthId)}
                          disabled={paLoading}
                        >
                          Refresh Status
                        </button>
                        <button
                          className="btn-secondary"
                          style={{ fontSize: '11px', padding: '3px 10px' }}
                          onClick={() => {
                            if (paManualUpdateId === pa.priorAuthId) {
                              setPaManualUpdateId(null);
                            } else {
                              setPaManualUpdateId(pa.priorAuthId || null);
                              setPaManualStatus(pa.status || '');
                              setPaManualNumber(pa.priorAuthNumber || '');
                              setPaManualExpiry(pa.expirationDate ? String(pa.expirationDate).slice(0, 10) : '');
                              setPaManualDenial(pa.denialReason || '');
                            }
                          }}
                        >
                          {paManualUpdateId === pa.priorAuthId ? 'Cancel Update' : 'Manual Update'}
                        </button>
                      </div>
                    </div>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '6px', fontSize: '12px' }}>
                      <div><span className="info-label">Plan:</span> <span>{pa.insuranceCompanyName || '—'}</span></div>
                      <div><span className="info-label">PBM:</span> <span>{pa.pbmName || '—'}</span></div>
                      <div><span className="info-label">Submitted:</span> <span>{pa.submittedDate ? new Date(pa.submittedDate).toLocaleDateString() : '—'}</span></div>
                      {pa.approvedDate && <div><span className="info-label">Approved:</span> <span style={{ color: '#10b981' }}>{new Date(pa.approvedDate).toLocaleDateString()}</span></div>}
                      {pa.deniedDate && <div><span className="info-label">Denied:</span> <span style={{ color: '#ef4444' }}>{new Date(pa.deniedDate).toLocaleDateString()}</span></div>}
                      {pa.expirationDate && <div><span className="info-label">Expires:</span> <span>{new Date(pa.expirationDate).toLocaleDateString()}</span></div>}
                    </div>
                    {pa.denialReason && (
                      <div style={{ background: '#fef2f2', borderRadius: '6px', padding: '8px', marginTop: '8px', fontSize: '12px', color: '#b91c1c' }}>
                        <strong>Denial Reason:</strong> {pa.denialReason}
                      </div>
                    )}
                    {pa.clinicalJustification && (
                      <div style={{ marginTop: '8px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                        <strong>Justification:</strong> {pa.clinicalJustification}
                      </div>
                    )}
                    {pa.notes && <div style={{ marginTop: '6px', fontSize: '12px' }}><strong>Notes:</strong> {pa.notes}</div>}
                    {paManualUpdateId === pa.priorAuthId && (
                      <div style={{ marginTop: '12px', background: '#f8fafc', borderRadius: '8px', padding: '12px', border: '1px solid #e2e8f0' }}>
                        <p style={{ margin: '0 0 8px', fontSize: '12px', fontWeight: 600, color: '#374151' }}>Manual Status Update — use when payer notifies by phone/fax</p>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '8px', marginBottom: '8px' }}>
                          <div>
                            <label style={{ fontSize: '11px', fontWeight: 600, display: 'block', marginBottom: '2px' }}>New Status *</label>
                            <select value={paManualStatus} onChange={e => setPaManualStatus(e.target.value)} style={{ width: '100%', padding: '4px 6px', fontSize: '12px', borderRadius: '4px', border: '1px solid #d1d5db' }}>
                              <option value="">— select —</option>
                              {['PENDING','SUBMITTED','UNDER_REVIEW','APPROVED','DENIED','APPEALED','CANCELLED','EXPIRED'].map(s => (
                                <option key={s} value={s}>{s}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <label style={{ fontSize: '11px', fontWeight: 600, display: 'block', marginBottom: '2px' }}>PA Number</label>
                            <input value={paManualNumber} onChange={e => setPaManualNumber(e.target.value)} placeholder="e.g. PA-2024-001" style={{ width: '100%', padding: '4px 6px', fontSize: '12px', borderRadius: '4px', border: '1px solid #d1d5db' }} />
                          </div>
                          <div>
                            <label style={{ fontSize: '11px', fontWeight: 600, display: 'block', marginBottom: '2px' }}>Expiry Date</label>
                            <input type="date" value={paManualExpiry} onChange={e => setPaManualExpiry(e.target.value)} style={{ width: '100%', padding: '4px 6px', fontSize: '12px', borderRadius: '4px', border: '1px solid #d1d5db' }} />
                          </div>
                          <div>
                            <label style={{ fontSize: '11px', fontWeight: 600, display: 'block', marginBottom: '2px' }}>Denial Reason</label>
                            <input value={paManualDenial} onChange={e => setPaManualDenial(e.target.value)} placeholder="Required if DENIED" style={{ width: '100%', padding: '4px 6px', fontSize: '12px', borderRadius: '4px', border: '1px solid #d1d5db' }} />
                          </div>
                        </div>
                        <button className="btn-primary" style={{ fontSize: '12px', padding: '5px 14px' }} disabled={!paManualStatus || paManualSaving} onClick={handleManualPaUpdate}>
                          {paManualSaving ? 'Saving…' : 'Save Update'}
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : !paLoading ? (
              <div className="empty-state"><p>No prior authorization requests for this prescription. Submit one above if insurance coverage requires it.</p></div>
            ) : (
              <div className="loading">Loading…</div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* ── Transmission Monitoring Modal ── */}
      {showTransmissionsModal && transmissionsPrescription && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '860px', width: 'min(860px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0 }}>Transmission History</h3>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>{transmissionsPrescription.medicationName} · Rx# {transmissionsPrescription.prescriptionNumber || '—'}</p>
              </div>
              <button className="btn-secondary" onClick={() => setShowTransmissionsModal(false)}>Close</button>
            </div>

            {transmissionsLoading ? (
              <div className="loading">Loading transmissions…</div>
            ) : transmissionsList.length > 0 ? (
              <div>
                {transmissionsList.map((t, i) => {
                  const statusColor = t.transmissionSuccess ? '#10b981' : t.transmissionStatus === 'FAILED' || t.transmissionStatus === 'REJECTED' ? '#ef4444' : '#f59e0b';
                  return (
                    <div key={i} className="info-card" style={{ marginBottom: '14px', borderLeft: `4px solid ${statusColor}` }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                        <div>
                          <strong>{t.transmissionMethod || 'ELECTRONIC'}</strong>
                          <span style={{ marginLeft: '10px', color: statusColor, fontWeight: 600, fontSize: '13px' }}>
                            {t.transmissionStatus}
                          </span>
                          {t.retryCount != null && t.retryCount > 0 && (
                            <span style={{ marginLeft: '8px', fontSize: '11px', color: 'var(--text-secondary)' }}>Attempt #{t.retryCount + 1}</span>
                          )}
                        </div>
                        {(t.transmissionStatus === 'FAILED' || t.transmissionStatus === 'REJECTED') && canTransmitRx && (
                          <button
                            className="btn-primary"
                            style={{ fontSize: '12px', padding: '4px 12px' }}
                            disabled={retryingTransmissionId === t.transmissionId}
                            onClick={() => t.transmissionId && handleRetryTransmission(t.transmissionId)}
                          >
                            {retryingTransmissionId === t.transmissionId ? 'Retrying…' : 'Retry'}
                          </button>
                        )}
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', fontSize: '12px' }}>
                        <div><span className="info-label">Date:</span> <span>{t.transmissionDate ? new Date(t.transmissionDate).toLocaleString() : '—'}</span></div>
                        <div><span className="info-label">Network:</span> <span>{t.networkName || '—'}</span></div>
                        <div><span className="info-label">Transaction ID:</span> <span style={{ fontFamily: 'monospace' }}>{t.networkTransactionId || '—'}</span></div>
                        <div><span className="info-label">Pharmacy:</span> <span>{t.pharmacyName || '—'}</span></div>
                        <div><span className="info-label">Pharmacy NPI:</span> <span>{t.pharmacyNpi || '—'}</span></div>
                        <div><span className="info-label">Phone:</span> <span>{t.pharmacyPhone || '—'}</span></div>
                        {t.confirmationReceived && (
                          <div><span className="info-label">Confirmed:</span> <span style={{ color: '#10b981' }}>{t.confirmationDate ? new Date(t.confirmationDate).toLocaleString() : 'Yes'}</span></div>
                        )}
                        {t.fillStatus && (
                          <div><span className="info-label">Fill Status:</span> <span style={{ fontWeight: 600, color: t.fillStatus === 'FILLED' ? '#10b981' : t.fillStatus === 'NOT_FILLED' || t.fillStatus === 'CANCELLED' ? '#ef4444' : '#f59e0b' }}>{t.fillStatus}</span></div>
                        )}
                        {t.filledDate && (
                          <div><span className="info-label">Filled:</span> <span>{new Date(t.filledDate).toLocaleDateString()}</span></div>
                        )}
                        {t.pickedUpDate && (
                          <div><span className="info-label">Picked Up:</span> <span>{new Date(t.pickedUpDate).toLocaleDateString()}</span></div>
                        )}
                        {t.transmittedByName && (
                          <div><span className="info-label">Sent By:</span> <span>{t.transmittedByName}</span></div>
                        )}
                      </div>

                      {t.errorMessage && (
                        <div style={{ background: '#fef2f2', borderRadius: '6px', padding: '8px', marginTop: '8px', fontSize: '12px', color: '#b91c1c' }}>
                          <strong>Error [{t.errorCode || '—'}]:</strong> {t.errorMessage}
                          {t.maxRetries != null && t.retryCount != null && (
                            <span style={{ marginLeft: '8px', color: '#6b7280' }}>({t.retryCount}/{t.maxRetries} retries used)</span>
                          )}
                        </div>
                      )}
                      {t.confirmationMessage && (
                        <div style={{ background: '#d1fae5', borderRadius: '6px', padding: '8px', marginTop: '8px', fontSize: '12px', color: '#065f46' }}>
                          <strong>Confirmation:</strong> {t.confirmationMessage}
                        </div>
                      )}
                      {t.fillStatusMessage && (
                        <div style={{ background: '#eff6ff', borderRadius: '6px', padding: '8px', marginTop: '8px', fontSize: '12px', color: '#1e40af' }}>
                          <strong>Fill status:</strong> {t.fillStatusMessage}
                        </div>
                      )}
                      {t.cancelledByPharmacy && (
                        <div style={{ background: '#fef2f2', borderRadius: '6px', padding: '8px', marginTop: '8px', fontSize: '12px', color: '#b91c1c' }}>
                          Cancelled by pharmacy: {t.cancellationReason || 'No reason provided'}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="empty-state"><p>No transmission records found for this prescription.</p></div>
            )}
            </div>
          </div>
        </div>
      )}

      {/* ── Interaction Acknowledgment Modal ── */}
      {showInteractionAckModal && ackPrescription && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '860px', width: 'min(860px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0 }}>Drug Interaction Review &amp; Acknowledgment</h3>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>{ackPrescription.medicationName} · Rx# {ackPrescription.prescriptionNumber || '—'}</p>
              </div>
              <button className="btn-secondary" onClick={() => setShowInteractionAckModal(false)}>Close</button>
            </div>

            {ackLoading ? (
              <div className="loading">Loading interactions…</div>
            ) : ackInteractions.length === 0 ? (
              <div className="info-card" style={{ background: '#d1fae5', borderLeft: '4px solid #10b981' }}>
                <strong>✓ No drug interactions recorded for this prescription.</strong>
                <p style={{ margin: '4px 0 0', fontSize: '12px' }}>Run an interaction check first to populate records.</p>
              </div>
            ) : (
              <>
                {ackInteractions.filter(ix => !ix.isAcknowledged).length > 0 ? (
                  <div className="error-message" style={{ marginBottom: '16px' }}>
                    ⚠ {ackInteractions.filter(ix => !ix.isAcknowledged).length} unacknowledged interaction(s) — each must be acknowledged before transmitting without override.
                  </div>
                ) : (
                  <div className="info-card" style={{ background: '#d1fae5', borderLeft: '4px solid #10b981', marginBottom: '16px' }}>
                    <strong>✓ All interactions acknowledged.</strong> You may proceed to transmit.
                  </div>
                )}

                {ackInteractions.map(ix => {
                  const severityColor = ix.severity === 'CONTRAINDICATED' || ix.severity === 'MAJOR' ? '#ef4444' : ix.severity === 'MODERATE' ? '#f59e0b' : '#6b7280';
                  return (
                    <div key={ix.interactionId} className="info-card" style={{ marginBottom: '14px', borderLeft: `4px solid ${severityColor}` }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
                        <div style={{ flex: 1 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                            <strong style={{ fontSize: '14px' }}>{ix.interactingMedication}</strong>
                            <span style={{ background: severityColor, color: '#fff', padding: '1px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 600 }}>{ix.severity}</span>
                            {ix.interactionCategory && <span style={{ fontSize: '11px', color: 'var(--text-secondary)', background: '#f3f4f6', padding: '1px 6px', borderRadius: '8px' }}>{String(ix.interactionCategory).replace('_', '-')}</span>}
                            {ix.isAcknowledged ? (
                              <span style={{ background: '#d1fae5', color: '#065f46', padding: '1px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 600 }}>✓ Acknowledged</span>
                            ) : (
                              <span style={{ background: '#fef2f2', color: '#b91c1c', padding: '1px 8px', borderRadius: '10px', fontSize: '11px' }}>Pending Review</span>
                            )}
                          </div>
                          {ix.description && <p style={{ margin: '0 0 4px', fontSize: '13px' }}>{ix.description}</p>}
                          {ix.clinicalSignificance && <p style={{ margin: '0 0 4px', fontSize: '12px', color: 'var(--text-secondary)' }}><strong>Clinical Significance:</strong> {ix.clinicalSignificance}</p>}
                          {ix.actionRequired && <p style={{ margin: '0 0 4px', fontSize: '12px', color: '#b45309' }}><strong>Action Required:</strong> {ix.actionRequired}</p>}
                          {ix.managementGuidance && <p style={{ margin: '0 0 4px', fontSize: '12px' }}><strong>Management:</strong> {ix.managementGuidance}</p>}
                          {ix.mechanism && <p style={{ margin: '0 0 4px', fontSize: '12px', color: 'var(--text-secondary)' }}><strong>Mechanism:</strong> {ix.mechanism}</p>}
                          {ix.onsetTime && <p style={{ margin: '0 0 2px', fontSize: '11px', color: 'var(--text-secondary)' }}>Onset: {ix.onsetTime}</p>}
                          {ix.evidenceLevel && <p style={{ margin: '0 0 2px', fontSize: '11px', color: 'var(--text-secondary)' }}>Evidence: {ix.evidenceLevel}</p>}
                          {ix.isAcknowledged && ix.overrideReason && (
                            <div style={{ marginTop: '6px', background: '#f0fdf4', borderRadius: '4px', padding: '6px 8px', fontSize: '12px', color: '#166534' }}>
                              <strong>Override reason:</strong> {ix.overrideReason}
                              {ix.acknowledgedDate && <span style={{ marginLeft: '8px', color: '#6b7280' }}>{new Date(ix.acknowledgedDate).toLocaleString()}</span>}
                            </div>
                          )}
                          {!ix.isAcknowledged && (
                            <div style={{ marginTop: '10px' }}>
                              <label style={{ fontSize: '12px', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                                Clinical Override Reason <span style={{ color: '#dc2626' }}>*</span>
                                <span style={{ fontWeight: 400, color: '#6b7280' }}> — saved to audit log</span>
                              </label>
                              <textarea
                                rows={2}
                                placeholder="Document why the benefit outweighs this risk (e.g. patient stable on regimen, no safer alternative, monitoring plan in place)…"
                                value={ackReasonMap[ix.interactionId] || ''}
                                onChange={e => setAckReasonMap(prev => ({ ...prev, [ix.interactionId]: e.target.value }))}
                                style={{ width: '100%', fontSize: '12px', borderRadius: '6px', border: '1px solid #d1d5db', padding: '6px', resize: 'vertical' }}
                              />
                            </div>
                          )}
                        </div>
                        {!ix.isAcknowledged && (
                          <button
                            className="btn-primary"
                            style={{ fontSize: '12px', padding: '6px 14px', whiteSpace: 'nowrap', flexShrink: 0 }}
                            disabled={ackSavingId === ix.interactionId}
                            onClick={() => handleAcknowledgeSingleInteraction(ix)}
                          >
                            {ackSavingId === ix.interactionId ? 'Saving…' : 'Acknowledge'}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}

                <div style={{ background: '#eff6ff', borderRadius: '8px', padding: '12px', marginBottom: '16px', fontSize: '12px', color: '#1e40af' }}>
                  Each acknowledgment is saved to the prescription audit trail with timestamp and provider ID. Once all interactions are acknowledged, transmission proceeds without requiring an override flag.
                </div>

                <div className="form-actions">
                  <button className="btn-secondary" onClick={() => setShowInteractionAckModal(false)}>Close</button>
                  {canTransmitRx && (
                    <button
                      className="btn-primary"
                      disabled={ackInteractions.some(ix => !ix.isAcknowledged)}
                      onClick={handleConfirmInteractionAck}
                      title={ackInteractions.some(ix => !ix.isAcknowledged) ? 'Acknowledge all interactions first' : 'All acknowledged — proceed to transmit'}
                    >
                      Proceed to Transmit
                    </button>
                  )}
                </div>
              </>
            )}
            </div>
          </div>
        </div>
      )}

      {/* ── Allergy Acknowledgment Modal ── */}
      {showAllergyAckModal && allergyAckPrescription && portalLayoutOverlay(
        <div className={`modal-overlay ${LAYOUT_OVERLAY_DETECT_CLASS}`} style={{ zIndex: LAYOUT_OVERLAY_ROOT_Z, background: 'rgba(0,0,0,0.55)' }}>
          <div className="modal-content" style={{ maxWidth: '860px', width: 'min(860px, 100%)' }}>
            <div className="modal-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0 }}>Allergy Warning Review &amp; Acknowledgment</h3>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--text-secondary)' }}>{allergyAckPrescription.medicationName} · Rx# {allergyAckPrescription.prescriptionNumber || '—'}</p>
              </div>
              <button className="btn-secondary" onClick={() => setShowAllergyAckModal(false)}>Close</button>
            </div>

            {allergyAckLoading ? (
              <div className="loading">Loading allergy checks…</div>
            ) : allergyAckChecks.length === 0 ? (
              <div className="info-card" style={{ background: '#d1fae5', borderLeft: '4px solid #10b981' }}>
                <strong>✓ No allergy warnings recorded for this prescription.</strong>
                <p style={{ margin: '4px 0 0', fontSize: '12px' }}>Run an allergy check first to populate records.</p>
              </div>
            ) : (
              <>
                {allergyAckChecks.filter(c => !c.isAcknowledged).length > 0 ? (
                  <div className="error-message" style={{ marginBottom: '16px' }}>
                    ⚠ {allergyAckChecks.filter(c => !c.isAcknowledged).length} unacknowledged allergy warning(s) — each must be acknowledged before transmitting without override.
                  </div>
                ) : (
                  <div className="info-card" style={{ background: '#d1fae5', borderLeft: '4px solid #10b981', marginBottom: '16px' }}>
                    <strong>✓ All allergy warnings acknowledged.</strong> You may proceed to transmit.
                  </div>
                )}

                {allergyAckChecks.map(check => {
                  const sevColor = check.severity === 'LIFE_THREATENING' || check.severity === 'SEVERE' ? '#ef4444' : check.severity === 'MODERATE' ? '#f59e0b' : '#6b7280';
                  return (
                    <div key={check.checkId} className="info-card" style={{ marginBottom: '14px', borderLeft: `4px solid ${sevColor}` }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '12px' }}>
                        <div style={{ flex: 1 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                            <strong style={{ fontSize: '14px' }}>{check.allergenName}</strong>
                            <span style={{ background: sevColor, color: '#fff', padding: '1px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 600 }}>{check.severity.replace('_', ' ')}</span>
                            {check.allergenType && <span style={{ fontSize: '11px', color: 'var(--text-secondary)', background: '#f3f4f6', padding: '1px 6px', borderRadius: '8px' }}>{check.allergenType}</span>}
                            {check.isAcknowledged ? (
                              <span style={{ background: '#d1fae5', color: '#065f46', padding: '1px 8px', borderRadius: '10px', fontSize: '11px', fontWeight: 600 }}>✓ Acknowledged</span>
                            ) : (
                              <span style={{ background: '#fef2f2', color: '#b91c1c', padding: '1px 8px', borderRadius: '10px', fontSize: '11px' }}>Pending Review</span>
                            )}
                          </div>
                          {check.reactionType && <p style={{ margin: '0 0 4px', fontSize: '12px' }}><strong>Reaction:</strong> {check.reactionType}</p>}
                          {check.isAcknowledged && check.overrideReason && (
                            <div style={{ marginTop: '6px', background: '#f0fdf4', borderRadius: '4px', padding: '6px 8px', fontSize: '12px', color: '#166534' }}>
                              <strong>Override reason:</strong> {check.overrideReason}
                              {check.acknowledgedDate && <span style={{ marginLeft: '8px', color: '#6b7280' }}>{new Date(check.acknowledgedDate).toLocaleString()}</span>}
                            </div>
                          )}
                          {!check.isAcknowledged && (
                            <div style={{ marginTop: '10px' }}>
                              <label style={{ fontSize: '12px', fontWeight: 600, display: 'block', marginBottom: '4px' }}>
                                Clinical Override Reason <span style={{ color: '#dc2626' }}>*</span>
                                <span style={{ fontWeight: 400, color: '#6b7280' }}> — saved to audit log</span>
                              </label>
                              <textarea
                                rows={2}
                                placeholder="Document why prescribing is safe despite this allergy (e.g. previous tolerance, cross-reactivity risk assessed, no safer alternative, pre-treatment plan in place)…"
                                value={allergyAckReasonMap[check.checkId] || ''}
                                onChange={e => setAllergyAckReasonMap(prev => ({ ...prev, [check.checkId]: e.target.value }))}
                                style={{ width: '100%', fontSize: '12px', borderRadius: '6px', border: '1px solid #d1d5db', padding: '6px', resize: 'vertical' }}
                              />
                            </div>
                          )}
                        </div>
                        {!check.isAcknowledged && (
                          <button
                            className="btn-primary"
                            style={{ fontSize: '12px', padding: '6px 14px', whiteSpace: 'nowrap', flexShrink: 0 }}
                            disabled={allergyAckSavingId === check.checkId}
                            onClick={() => handleAcknowledgeSingleAllergyCheck(check)}
                          >
                            {allergyAckSavingId === check.checkId ? 'Saving…' : 'Acknowledge'}
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}

                <div style={{ background: '#eff6ff', borderRadius: '8px', padding: '12px', marginBottom: '16px', fontSize: '12px', color: '#1e40af' }}>
                  Each acknowledgment is saved to the prescription audit trail with timestamp, provider ID, and action taken (OVERRIDDEN). Once all warnings are acknowledged, transmission proceeds without requiring an override flag.
                </div>

                <div className="form-actions">
                  <button className="btn-secondary" onClick={() => setShowAllergyAckModal(false)}>Close</button>
                  {canTransmitRx && (
                    <button
                      className="btn-primary"
                      disabled={allergyAckChecks.some(c => !c.isAcknowledged)}
                      onClick={handleConfirmAllergyAck}
                      title={allergyAckChecks.some(c => !c.isAcknowledged) ? 'Acknowledge all allergy warnings first' : 'All acknowledged — proceed to transmit'}
                    >
                      Proceed to Transmit
                    </button>
                  )}
                </div>
              </>
            )}
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default PrescriptionManagementPage;
