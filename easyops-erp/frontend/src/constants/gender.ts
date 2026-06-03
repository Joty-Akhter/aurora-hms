/** Gender codes stored on employees → display labels for Aurora HR. */
export const GENDER_LABELS: Record<string, string> = {
  MALE: 'Male',
  FEMALE: 'Female',
  OTHER: 'Others',
  PREFER_NOT_TO_SAY: 'Prefer not to say',
};

export function genderLabel(code?: string | null): string {
  if (!code) return '-';
  return GENDER_LABELS[code] ?? code;
}
