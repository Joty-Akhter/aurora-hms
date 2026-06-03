import type { DoctorAppointmentSlot } from '../services/hospitalService';

const DAY_KEYS = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'] as const;

export function slotWindowSignature(startTime: string, endTime: string): string {
  return `${startTime.trim()}|${endTime.trim()}`;
}

/** Reject duplicate start/end windows in doctor schedule rows. */
export function validateUniqueAppointmentSlots(slots: DoctorAppointmentSlot[]): string | null {
  const seen = new Set<string>();
  for (const slot of slots) {
    const start = slot.startTime?.trim();
    const end = slot.endTime?.trim();
    if (!start || !end) continue;
    const sig = slotWindowSignature(start, end);
    if (seen.has(sig)) {
      return `Duplicate appointment window ${start} – ${end}. Each slot time range must be unique.`;
    }
    seen.add(sig);
  }
  return null;
}

export interface SlotAvailabilityLike {
  start: string;
  end: string;
  availableCount: number;
}

/** Merge duplicate bookable instants (e.g. overlapping working-hour rows). */
export function dedupeAvailabilitySlots<T extends SlotAvailabilityLike>(slots: T[]): T[] {
  const byKey = new Map<string, T>();
  for (const s of slots) {
    const key = `${s.start}|${s.end}`;
    const existing = byKey.get(key);
    if (!existing || s.availableCount > existing.availableCount) {
      byKey.set(key, s);
    }
  }
  return [...byKey.values()].sort(
    (a, b) => new Date(a.start).getTime() - new Date(b.start).getTime(),
  );
}

/** Sum maxPatients for slot rows active on the given calendar date (YYYY-MM-DD). */
export function getConfiguredMaxSlotsForDate(
  appointmentSlots: DoctorAppointmentSlot[] | undefined,
  dateYmd: string,
): number | null {
  if (!appointmentSlots?.length || !dateYmd) return null;
  const dayKey = DAY_KEYS[new Date(`${dateYmd}T12:00:00`).getDay()];
  let total = 0;
  for (const slot of appointmentSlots) {
    if (!slot.days?.includes(dayKey)) continue;
    total += slot.maxPatients != null && slot.maxPatients > 0 ? slot.maxPatients : 10;
  }
  return total > 0 ? total : null;
}
