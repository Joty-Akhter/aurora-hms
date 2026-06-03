/**
 * Hospital sidebar routes: longest-prefix wins. Each feature maps to its own permission code
 * (seeded in rbac.permissions). Only HOSPITAL_MANAGE bypasses feature checks; HOSPITAL_VIEW opens the hub only.
 * Entries must stay sorted by prefix length descending.
 */
export const HOSPITAL_ROUTE_PERMISSION_ENTRIES: ReadonlyArray<{ prefix: string; permissions: string[] }> = [
  { prefix: '/hospital/prescriptions/templates', permissions: ['HOSPITAL_FEAT_RX_TEMPLATES', 'HOSPITAL_PRESCRIPTION_VIEW'] },
  { prefix: '/hospital/prescriptions/admin', permissions: ['HOSPITAL_FEAT_RX_SETTINGS', 'HOSPITAL_PRESCRIPTION_VIEW'] },
  {
    prefix: '/hospital/prescriptions',
    permissions: ['HOSPITAL_PRESCRIPTION_VIEW', 'HOSPITAL_PRESCRIPTION_PRESCRIBE', 'HOSPITAL_PRESCRIPTION_TRANSMIT'],
  },
  { prefix: '/hospital/doctors/schedule', permissions: ['HOSPITAL_FEAT_DOCTOR_SCHEDULE'] },
  {
    prefix: '/hospital/configurations/clinical-chart',
    permissions: ['HOSPITAL_FEAT_CLINICAL_CHART', 'HOSPITAL_PRESCRIPTION_VIEW'],
  },
  { prefix: '/hospital/medical-codes/icd10', permissions: ['HOSPITAL_FEAT_MEDICAL_CODES_ICD10'] },
  { prefix: '/hospital/medical-codes/icd11', permissions: ['HOSPITAL_FEAT_MEDICAL_CODES_ICD11'] },
  { prefix: '/hospital/corporate-discount/discount-schemes', permissions: ['HOSPITAL_FEAT_CORPORATE_DISCOUNT_SCHEMES'] },
  { prefix: '/hospital/clinical-orders/entry', permissions: ['HOSPITAL_FEAT_CLINICAL_ORDERS_ENTRY'] },
  { prefix: '/hospital/clinical-orders/worklists', permissions: ['HOSPITAL_FEAT_CLINICAL_ORDERS_WORKLISTS'] },
  { prefix: '/hospital/clinical-orders/reports', permissions: ['HOSPITAL_FEAT_CLINICAL_ORDERS_REPORTS'] },
  { prefix: '/hospital/clinical-orders/orders', permissions: ['HOSPITAL_FEAT_CLINICAL_ORDERS_ORDERS'] },
  { prefix: '/hospital/clinical-orders/sets', permissions: ['HOSPITAL_FEAT_CLINICAL_ORDERS_SETS'] },
  { prefix: '/hospital/corporate-discount/corporates', permissions: ['HOSPITAL_FEAT_CORPORATE_CLIENTS'] },
  { prefix: '/hospital/corporate-discount/contracts', permissions: ['HOSPITAL_FEAT_CORPORATE_CONTRACTS'] },
  { prefix: '/hospital/corporate-discount/packages', permissions: ['HOSPITAL_FEAT_CORPORATE_PACKAGES'] },
  { prefix: '/hospital/corporate-discount/decisions', permissions: ['HOSPITAL_FEAT_CORPORATE_DECISIONS'] },
  { prefix: '/hospital/corporate-discount/reports', permissions: ['HOSPITAL_FEAT_CORPORATE_REPORTS'] },
  { prefix: '/hospital/scheduling/planned-admissions', permissions: ['HOSPITAL_FEAT_SCHEDULING_PLANNED_ADMISSIONS'] },
  { prefix: '/hospital/scheduling/expected-admissions', permissions: ['HOSPITAL_FEAT_SCHEDULING_EXPECTED_ADMISSIONS'] },
  { prefix: '/hospital/scheduling/slot-templates', permissions: ['HOSPITAL_FEAT_SCHEDULING_SLOT_TEMPLATES'] },
  { prefix: '/hospital/scheduling/doctor-mappings', permissions: ['HOSPITAL_FEAT_SCHEDULING_DOCTOR_MAPPINGS'] },
  { prefix: '/hospital/billing/charges', permissions: ['HOSPITAL_FEAT_BILLING_CHARGES'] },
  { prefix: '/hospital/billing/invoices', permissions: ['HOSPITAL_FEAT_BILLING_INVOICES'] },
  { prefix: '/hospital/billing/payments', permissions: ['HOSPITAL_FEAT_BILLING_PAYMENTS'] },
  { prefix: '/hospital/billing/reports', permissions: ['HOSPITAL_FEAT_BILLING_REPORTS'] },
  { prefix: '/hospital/pharmacy/catalog', permissions: ['HOSPITAL_FEAT_PHARMACY_CATALOG'] },
  { prefix: '/hospital/pharmacy/locations', permissions: ['HOSPITAL_FEAT_PHARMACY_LOCATIONS'] },
  { prefix: '/hospital/pharmacy/dispense', permissions: ['HOSPITAL_FEAT_PHARMACY_DISPENSE'] },
  { prefix: '/hospital/pharmacy/reports', permissions: ['HOSPITAL_FEAT_PHARMACY_REPORTS'] },
  {
    prefix: '/hospital/doctor-dashboard',
    permissions: ['HOSPITAL_FEAT_DOCTOR_DASHBOARD', 'HOSPITAL_PRESCRIPTION_VIEW'],
  },
  { prefix: '/hospital/doctor-notes', permissions: ['HOSPITAL_FEAT_DOCTOR_NOTES'] },
  { prefix: '/hospital/doctor-departments', permissions: ['HOSPITAL_FEAT_DOCTOR_DEPARTMENTS'] },
  { prefix: '/hospital/notes/templates', permissions: ['HOSPITAL_FEAT_NOTE_TEMPLATES'] },
  { prefix: '/hospital/medical-codes', permissions: ['HOSPITAL_FEAT_MEDICAL_CODES'] },
  { prefix: '/hospital/pharmacy/stock', permissions: ['HOSPITAL_FEAT_PHARMACY_STOCK'] },
  { prefix: '/hospital/scheduling/resources', permissions: ['HOSPITAL_FEAT_SCHEDULING_RESOURCES'] },
  { prefix: '/hospital/scheduling/blackouts', permissions: ['HOSPITAL_FEAT_SCHEDULING_BLACKOUTS'] },
  { prefix: '/hospital/scheduling/availability', permissions: ['HOSPITAL_FEAT_SCHEDULING_AVAILABILITY'] },
  { prefix: '/hospital/scheduling/reservations', permissions: ['HOSPITAL_FEAT_SCHEDULING_RESERVATIONS'] },
  { prefix: '/hospital/scheduling/appointments', permissions: ['HOSPITAL_FEAT_SCHEDULING_APPOINTMENTS'] },
  { prefix: '/hospital/scheduling/roster-blocks', permissions: ['HOSPITAL_FEAT_SCHEDULING_ROSTER_BLOCKS'] },
  { prefix: '/hospital/scheduling/waitlist', permissions: ['HOSPITAL_FEAT_SCHEDULING_WAITLIST'] },
  { prefix: '/hospital/scheduling/reports', permissions: ['HOSPITAL_FEAT_SCHEDULING_REPORTS'] },
  { prefix: '/hospital/scheduling/audit-log', permissions: ['HOSPITAL_FEAT_SCHEDULING_AUDIT_LOG'] },
  { prefix: '/hospital/scheduling/queue', permissions: ['HOSPITAL_FEAT_SCHEDULING_QUEUE'] },
  { prefix: '/hospital/cards/products', permissions: ['HOSPITAL_FEAT_CARD_PRODUCTS'] },
  {
    prefix: '/hospital/patients',
    permissions: [
      'HOSPITAL_FEAT_PATIENTS',
      'HOSPITAL_PRESCRIPTION_VIEW',
      'HOSPITAL_PATIENT_EHR_ADD',
      'HOSPITAL_PATIENT_EHR_EDIT',
      'HOSPITAL_PATIENT_EHR_DELETE',
    ],
  },
  { prefix: '/hospital/doctors', permissions: ['HOSPITAL_FEAT_DOCTORS'] },
  { prefix: '/hospital/pharmacy', permissions: ['HOSPITAL_FEAT_PHARMACY'] },
  { prefix: '/hospital/billing', permissions: ['HOSPITAL_FEAT_BILLING'] },
  { prefix: '/hospital/cards', permissions: ['HOSPITAL_FEAT_CARDS'] },
  { prefix: '/hospital/corporate-discount', permissions: ['HOSPITAL_FEAT_CORPORATE_DISCOUNT'] },
  { prefix: '/hospital/clinical-orders', permissions: ['HOSPITAL_FEAT_CLINICAL_ORDERS'] },
  { prefix: '/hospital/scheduling', permissions: ['HOSPITAL_FEAT_SCHEDULING'] },
  { prefix: '/portal/cards', permissions: ['HOSPITAL_FEAT_PORTAL_MY_CARDS'] },
];

export const ALL_HOSPITAL_FEATURE_PERMISSION_CODES: string[] = Array.from(
  new Set(HOSPITAL_ROUTE_PERMISSION_ENTRIES.flatMap((e) => e.permissions)),
);

export function normalizePath(path: string): string {
  if (!path) return '';
  return path.startsWith('/') ? path : `/${path}`;
}

/** Longest route prefix wins (safe regardless of array declaration order). */
export function matchHospitalRoutePermissions(path: string): string[] | null {
  const n = normalizePath(path);
  const sorted = [...HOSPITAL_ROUTE_PERMISSION_ENTRIES].sort((a, b) => b.prefix.length - a.prefix.length);
  const hit = sorted.find((e) => n.startsWith(e.prefix));
  return hit ? hit.permissions : null;
}
