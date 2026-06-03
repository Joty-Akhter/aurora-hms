import type { ResourceResponse } from '../services/hospitalSchedulingService';

/** e.g. `Dr. Smith (Cardiology)` when department is known. */
export function doctorNameWithDepartment(doctorName: string, departmentName?: string | null): string {
  const dept = (departmentName ?? '').trim();
  return dept ? `${doctorName} (${dept})` : doctorName;
}

export function doctorDepartmentMap(doctors: { doctorId: string; departmentName?: string | null }[]): Map<string, string> {
  const m = new Map<string, string>();
  for (const d of doctors) {
    if (d.doctorId) m.set(d.doctorId, (d.departmentName ?? '').trim());
  }
  return m;
}

export function schedulingResourceDoctorLabel(
  resource: ResourceResponse,
  departmentByDoctorId: Map<string, string>,
): string {
  const ext = resource.externalReferenceId?.trim();
  const dept = ext ? departmentByDoctorId.get(ext) : undefined;
  return doctorNameWithDepartment(resource.name, dept);
}
