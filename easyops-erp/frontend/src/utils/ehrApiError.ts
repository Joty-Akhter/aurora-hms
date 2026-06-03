/**
 * Normalises Spring / hospital-service error bodies: `message`, `error`, `detail`
 * (RFC 7807 / ProblemDetail), `errors` as array, or `errors` as field→message map
 * (`MethodArgumentNotValidException`).
 */
export function ehrApiErrorMessage(err: unknown, fallback: string): string {
  const e = err as { response?: { data?: Record<string, unknown> }; message?: string };
  const d = e?.response?.data;
  if (d && typeof d === 'object') {
    if (typeof d.message === 'string' && d.message.trim()) return d.message.trim();
    if (typeof d.error === 'string' && d.error.trim()) return d.error.trim();
    if (typeof d.detail === 'string' && d.detail.trim()) return d.detail.trim();
    if (Array.isArray(d.errors)) {
      const parts = (d.errors as unknown[])
        .map((x) => {
          if (x && typeof x === 'object' && 'defaultMessage' in x) {
            return String((x as { defaultMessage?: string }).defaultMessage);
          }
          if (x && typeof x === 'object' && 'message' in x) {
            return String((x as { message?: string }).message);
          }
          return String(x);
        })
        .filter(Boolean);
      if (parts.length) return parts.join('\n');
    }
    if (d.errors && typeof d.errors === 'object' && !Array.isArray(d.errors)) {
      const errMap = d.errors as Record<string, unknown>;
      const parts = Object.entries(errMap)
        .map(([field, msg]) => {
          const m = typeof msg === 'string' ? msg : msg != null ? String(msg) : '';
          return m.trim() ? `${field}: ${m.trim()}` : '';
        })
        .filter(Boolean);
      if (parts.length) return parts.join('\n');
    }
  }
  if (typeof e?.message === 'string' && e.message.trim()) return e.message.trim();
  return fallback;
}
