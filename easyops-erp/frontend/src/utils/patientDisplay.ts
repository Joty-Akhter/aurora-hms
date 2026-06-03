import { GENDER_LABELS } from '../constants/gender';

/** Human-readable gender for list/detail (API stores enum-style values). */
export function formatGenderLabel(gender: string | undefined | null): string {
  if (!gender) return '';
  const g = gender.trim();
  if (g === 'Prefer_not_to_answer' || /^prefer\s*not\s*to\s*answer$/i.test(g)) {
    return 'Prefer not to answer';
  }
  const normalized = g.toUpperCase().replace(/\s+/g, '_');
  if (GENDER_LABELS[normalized]) return GENDER_LABELS[normalized];
  if (GENDER_LABELS[g]) return GENDER_LABELS[g];
  return g;
}

/** Insurance verification as returned by the API (JPA enum names). */
export type InsuranceVerificationApi = 'Verified' | 'Pending' | 'Not_Verified' | 'Not_Applicable';

export function normalizeInsuranceVerification(raw: string | undefined | null): InsuranceVerificationApi {
  if (!raw) return 'Not_Verified';
  if (raw === 'Not Verified' || raw === 'Not Verify') return 'Not_Verified';
  if (raw === 'Not Applicable') return 'Not_Applicable';
  if (raw === 'Not_Verified' || raw === 'Not_Applicable' || raw === 'Verified' || raw === 'Pending') {
    return raw;
  }
  return 'Not_Verified';
}

export function formatInsuranceVerificationLabel(status: string | undefined | null): string {
  if (!status) return '';
  const n = normalizeInsuranceVerification(status);
  switch (n) {
    case 'Not_Verified':
      return 'Not verified';
    case 'Not_Applicable':
      return 'Not applicable';
    case 'Verified':
      return 'Verified';
    case 'Pending':
      return 'Pending';
    default:
      return status.replace(/_/g, ' ');
  }
}

export function formatProblemStatusLabel(status: string | undefined | null): string {
  if (!status) return '';
  if (status === 'RULED_OUT') return 'Ruled Out';
  return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

/**
 * Human-readable labels for API enums (severity, type, priority, etc.).
 * Splits on underscores and title-cases each segment. Preserves strings that already
 * contain lowercase (e.g. free-text reaction descriptions).
 */
export function formatClinicalEnumLabel(value: string | undefined | null): string {
  if (!value) return '';
  const v = value.trim();
  if (!v) return '';
  if (!/_/.test(v) && /[a-z]/.test(v)) {
    return v;
  }
  return v
    .split(/_+/)
    .filter(Boolean)
    .map((part) => {
      const w = part.trim();
      if (!w) return '';
      return w.charAt(0).toUpperCase() + w.slice(1).toLowerCase();
    })
    .filter(Boolean)
    .join(' ');
}
