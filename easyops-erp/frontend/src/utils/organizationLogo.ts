/**
 * Builds an absolute URL for <img src>. Relative paths (from uploaded logos) are prefixed with the API base URL.
 */
export function resolveOrganizationLogoUrl(logo: string | null | undefined): string | undefined {
  if (!logo) return undefined;
  const trimmed = logo.trim();
  if (!trimmed) return undefined;
  if (/^https?:\/\//i.test(trimmed) || trimmed.startsWith('data:')) {
    return trimmed;
  }
  const base = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
  const path = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return base ? `${base}${path}` : path;
}
