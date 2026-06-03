/**
 * Post-login home when the user has no DASHBOARD_VIEW (e.g. Call Center / Doctor narrow roles).
 * Reads `userPermissions` from localStorage (written by AuthContext sync).
 */
export function getDefaultHomePathFromStoredPermissions(): string {
  try {
    const raw = localStorage.getItem('userPermissions');
    const list = raw ? JSON.parse(raw) : [];
    const codes = new Set(
      Array.isArray(list)
        ? list
            .map((p: { code?: string }) => (typeof p?.code === 'string' ? p.code : null))
            .filter((c: string | null): c is string => Boolean(c))
        : [],
    );
    if (codes.has('DASHBOARD_VIEW')) {
      return '/dashboard';
    }
    if (codes.has('HOSPITAL_FEAT_DOCTOR_DASHBOARD')) {
      return '/hospital/doctor-dashboard';
    }
    if (codes.has('HOSPITAL_FEAT_SCHEDULING_APPOINTMENTS')) {
      return '/hospital/scheduling/appointments';
    }
  } catch {
    /* ignore */
  }
  return '/dashboard';
}
