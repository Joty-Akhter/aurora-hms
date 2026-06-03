/** Human-readable labels for scheduling appointment / reservation status enums. */
export function formatSchedulingStatusLabel(status: string | undefined | null): string {
  if (!status) return '';
  const normalized = status.trim().replace(/\s+/g, '_').toUpperCase();
  const labels: Record<string, string> = {
    CONFIRMED: 'Confirmed',
    CHECKED_IN: 'Checked in',
    NO_SHOW: 'No show',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
    PENDING: 'Pending',
    RESERVED: 'Reserved',
    EXPIRED: 'Expired',
    CONVERTED: 'Converted',
  };
  if (labels[normalized]) return labels[normalized];
  return normalized
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ');
}

/** Human-readable labels for appointment type enums (NEW, FOLLOW_UP, …). */
export function formatSchedulingAppointmentTypeLabel(type: string | undefined | null): string {
  if (!type) return '';
  const normalized = type.trim().replace(/\s+/g, '_').toUpperCase();
  const labels: Record<string, string> = {
    NEW: 'New',
    FOLLOW_UP: 'Follow up',
    EMERGENCY: 'Emergency',
    ROUTINE: 'Routine',
    REPORT: 'Report',
  };
  if (labels[normalized]) return labels[normalized];
  return formatSchedulingStatusLabel(normalized);
}
