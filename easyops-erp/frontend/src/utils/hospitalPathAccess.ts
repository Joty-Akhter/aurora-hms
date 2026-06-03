import {
  ALL_HOSPITAL_FEATURE_PERMISSION_CODES,
  matchHospitalRoutePermissions,
  normalizePath,
} from '@/config/hospitalRoutePermissions';

/** Aligns with MainLayout hospital menu / route RBAC (feature codes; HOSPITAL_MANAGE bypasses). */
export function userHasHospitalPathAccess(
  hasAnyPermission: (codes: string[]) => boolean,
  path: string,
): boolean {
  const hospitalManage = ['HOSPITAL_MANAGE'];
  const hospitalHub = ['HOSPITAL_VIEW', 'HOSPITAL_MANAGE'];
  const normalized = normalizePath(path);

  if (normalized === '/hospital') {
    return hasAnyPermission([...hospitalHub, ...ALL_HOSPITAL_FEATURE_PERMISSION_CODES]);
  }

  const feature = matchHospitalRoutePermissions(normalized);
  if (feature) {
    return hasAnyPermission([...hospitalManage, ...feature]);
  }

  if (normalized.startsWith('/hospital')) {
    return hasAnyPermission(hospitalManage);
  }

  return false;
}
