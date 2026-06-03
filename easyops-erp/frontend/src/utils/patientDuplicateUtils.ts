import { DuplicatePatientResponse } from '@services/hospitalService';

export function isPhoneRelatedDuplicateReason(matchReason?: string | null): boolean {
  const reason = (matchReason ?? '').toLowerCase();
  return reason.includes('mobile') || reason.includes('phone');
}

export function isPhoneOnlyDuplicateCheck(dup: DuplicatePatientResponse | null | undefined): boolean {
  if (!dup?.hasDuplicates || !dup.matches?.length) {
    return false;
  }
  return dup.matches.every((m) => isPhoneRelatedDuplicateReason(m.matchReason));
}

export function phoneDuplicateHintMessage(dup: DuplicatePatientResponse): string | null {
  const phoneMatches = dup.matches?.filter((m) => isPhoneRelatedDuplicateReason(m.matchReason));
  if (!phoneMatches?.length) {
    return null;
  }
  const first = phoneMatches[0];
  return `Also used by ${first.fullName || 'another patient'} (MRN: ${first.mrn}). Family members may share a mobile.`;
}
