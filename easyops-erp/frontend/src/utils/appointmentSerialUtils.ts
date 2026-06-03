import { parseISO } from 'date-fns';
import type { AppointmentResponse } from '../services/hospitalSchedulingService';

const INACTIVE = new Set(['CANCELLED', 'NO_SHOW']);

/** Compare slot instants; API strings may differ in offset notation. */
export function slotInstantEqual(a: string, b: string): boolean {
  if (!a && !b) return true;
  if (!a || !b) return false;
  if (a === b) return true;
  const ta = parseISO(a).getTime();
  const tb = parseISO(b).getTime();
  return !isNaN(ta) && !isNaN(tb) && ta === tb;
}

function activeAppointments(apps: AppointmentResponse[]): AppointmentResponse[] {
  return apps.filter((a) => a.status && !INACTIVE.has(a.status.toUpperCase()));
}

/**
 * Next serial for a new booking: per-slot increment, otherwise day order by slot start
 * (earlier slots get lower serial numbers).
 */
export function computeNextAppointmentSerial(
  appointments: AppointmentResponse[],
  serialStartFrom: number | undefined,
  slotStart?: string,
  slotEnd?: string,
): number {
  const base = serialStartFrom != null && serialStartFrom > 0 ? serialStartFrom : 1;
  const active = activeAppointments(appointments);

  if (slotStart?.trim() && slotEnd?.trim()) {
    const sameSlot = active.filter(
      (a) => slotInstantEqual(a.slotStart, slotStart) && slotInstantEqual(a.slotEnd, slotEnd),
    );
    if (sameSlot.length > 0) {
      const maxTok = sameSlot.reduce((m, a) => Math.max(m, a.tokenNumber ?? 0), 0);
      return maxTok > 0 ? maxTok + 1 : base;
    }
    const slotMs = parseISO(slotStart).getTime();
    const earlier = !isNaN(slotMs)
      ? active.filter((a) => {
          const t = parseISO(a.slotStart).getTime();
          return !isNaN(t) && t < slotMs;
        }).length
      : 0;
    return base + earlier;
  }

  const maxDay = active.reduce((m, a) => Math.max(m, a.tokenNumber ?? 0), 0);
  return maxDay > 0 ? maxDay + 1 : base;
}

/** Label shown on slot picker: next serial if this slot were booked now. */
export function projectedSerialForSlot(
  appointments: AppointmentResponse[],
  serialStartFrom: number | undefined,
  slotStart: string,
  slotEnd: string,
): number {
  return computeNextAppointmentSerial(appointments, serialStartFrom, slotStart, slotEnd);
}
