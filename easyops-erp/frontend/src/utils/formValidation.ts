/** Local calendar date as YYYY-MM-DD (for `<input type="date" max>` and comparisons). */
export function isoDateLocal(d: Date = new Date()): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

/** `YYYY-MM-DD` that exists on the local calendar (rejects 2026-02-31, garbage, etc.). */
export function isValidCalendarISODate(value: string): boolean {
  const v = value.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(v)) return false;
  const y = Number(v.slice(0, 4));
  const mo = Number(v.slice(5, 7));
  const d = Number(v.slice(8, 10));
  if (!Number.isFinite(y) || !Number.isFinite(mo) || !Number.isFinite(d)) return false;
  const dt = new Date(y, mo - 1, d);
  return dt.getFullYear() === y && dt.getMonth() === mo - 1 && dt.getDate() === d;
}

/**
 * First 10 characters `YYYY-MM-DD` for `<input type="date" value>` from an ISO datetime or date string.
 * Returns '' if the calendar date is invalid (e.g. 2026-02-31).
 */
export function toDateInputValue(value: string | null | undefined): string {
  if (value == null) return '';
  const s = String(value).trim();
  if (!s) return '';
  const out = s.length >= 10 ? s.slice(0, 10) : s;
  if (out.length === 10 && isValidCalendarISODate(out)) return out;
  return '';
}

/**
 * Optional email: empty is valid. Otherwise require a domain label, dot, and TLD (≥2 chars).
 * Rejects browser-lenient cases like `a@b` or `rr@gmai`.
 */
export function isValidOptionalEmail(value: string): boolean {
  const s = value.trim();
  if (!s) return true;
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(s)) return false;
  const at = s.indexOf('@');
  const domain = s.slice(at + 1);
  const lastDot = domain.lastIndexOf('.');
  if (lastDot <= 0 || lastDot >= domain.length - 1) return false;
  const tld = domain.slice(lastDot + 1);
  return tld.length >= 2 && /^[a-zA-Z0-9]+$/.test(tld);
}

/** Typical max length for international phone digits stored without formatting. */
export const MAX_PHONE_DIGITS = 15;

/** Aligns with backend {@code PatientPhoneNormalization.MIN_DIGITS_FOR_UNIQUENESS}. */
export const MIN_PHONE_DIGITS_FOR_UNIQUENESS = 6;

export function digitsOnlyPhone(value: string): string {
  return value.replace(/\D/g, '').slice(0, MAX_PHONE_DIGITS);
}

/** Empty is allowed; partial numbers (1–5 digits) are rejected for registration. */
export function isValidPhoneDigitLength(phone: string): boolean {
  const digits = digitsOnlyPhone(phone);
  return digits.length === 0 || digits.length >= MIN_PHONE_DIGITS_FOR_UNIQUENESS;
}

/** Keep digits and at most one decimal point (non-negative fee-style). */
export function sanitizeNonNegativeDecimalString(raw: string): string {
  let s = raw.replace(/[^0-9.]/g, '');
  const firstDot = s.indexOf('.');
  if (firstDot !== -1) {
    s = s.slice(0, firstDot + 1) + s.slice(firstDot + 1).replace(/\./g, '');
  }
  return s;
}

/** Non-negative finite number for API payloads; otherwise undefined. */
export function finiteNonNegativeOrUndefined(n: number | undefined | null): number | undefined {
  if (n == null || !Number.isFinite(n) || n < 0) return undefined;
  return n;
}

/** Keep digits only (non-negative integer entry). */
export function sanitizeDigitsOnly(raw: string): string {
  return raw.replace(/\D/g, '');
}

/** Parse optional positive integer; returns undefined when empty. */
export function parseOptionalPositiveInt(raw: string, min = 1): number | undefined {
  const digits = sanitizeDigitsOnly(raw);
  if (!digits) return undefined;
  const n = parseInt(digits, 10);
  if (!Number.isFinite(n)) return undefined;
  return Math.max(min, n);
}

/** Parse optional non-negative integer; returns undefined when empty. */
export function parseOptionalNonNegativeInt(raw: string): number | undefined {
  const digits = sanitizeDigitsOnly(raw);
  if (!digits) return undefined;
  const n = parseInt(digits, 10);
  if (!Number.isFinite(n) || n < 0) return undefined;
  return n;
}

/** Local datetime for `<input type="datetime-local">` (minute precision). */
export function localDateTimeInputMin(d: Date = new Date()): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const h = String(d.getHours()).padStart(2, '0');
  const min = String(d.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${day}T${h}:${min}`;
}

/** Parse positive integer with fallback when empty/invalid. */
export function parseRequiredPositiveInt(raw: string, min = 1, fallback = min): number {
  return parseOptionalPositiveInt(raw, min) ?? fallback;
}

/** Block minus/plus/exponent keys on number inputs. */
export function blockNegativeNumberInput(e: { key: string; preventDefault: () => void }): void {
  if (e.key === '-' || e.key === '+' || e.key === 'e' || e.key === 'E') {
    e.preventDefault();
  }
}

/** Clamp optional integer field for API payloads (undefined when invalid or below min). */
export function finitePositiveIntOrUndefined(n: number | undefined | null, min = 1): number | undefined {
  if (n == null || !Number.isFinite(n) || n < min) return undefined;
  return Math.floor(n);
}
