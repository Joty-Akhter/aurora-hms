export type WeekdayKey =
  | 'monday'
  | 'tuesday'
  | 'wednesday'
  | 'thursday'
  | 'friday'
  | 'saturday'
  | 'sunday';

export type DayHours = { open: string; close: string; closed: boolean };

export type WeeklyHoursState = Record<WeekdayKey, DayHours>;

export const WEEKDAY_ROWS: { key: WeekdayKey; label: string }[] = [
  { key: 'monday', label: 'Monday' },
  { key: 'tuesday', label: 'Tuesday' },
  { key: 'wednesday', label: 'Wednesday' },
  { key: 'thursday', label: 'Thursday' },
  { key: 'friday', label: 'Friday' },
  { key: 'saturday', label: 'Saturday' },
  { key: 'sunday', label: 'Sunday' },
];

export function defaultWeeklyHours(): WeeklyHoursState {
  const base: DayHours = { open: '09:00', close: '18:00', closed: false };
  return {
    monday: { ...base },
    tuesday: { ...base },
    wednesday: { ...base },
    thursday: { ...base },
    friday: { ...base },
    saturday: { ...base, closed: true },
    sunday: { ...base, closed: true },
  };
}

type StoredOperationalHours = {
  weekly?: Partial<Record<WeekdayKey, Partial<DayHours>>>;
  manualNote?: string;
};

export function parseOperationalHours(raw: string | undefined | null): {
  weekly: WeeklyHoursState;
  manualNote: string;
} {
  const defaults = defaultWeeklyHours();
  if (!raw?.trim()) {
    return { weekly: defaults, manualNote: '' };
  }
  try {
    const parsed = JSON.parse(raw) as StoredOperationalHours;
    if (parsed && typeof parsed === 'object' && parsed.weekly) {
      const weekly = { ...defaults };
      for (const row of WEEKDAY_ROWS) {
        const day = parsed.weekly[row.key];
        if (day) {
          weekly[row.key] = {
            open: day.open || defaults[row.key].open,
            close: day.close || defaults[row.key].close,
            closed: Boolean(day.closed),
          };
        }
      }
      return { weekly, manualNote: parsed.manualNote?.trim() || '' };
    }
  } catch {
    // legacy plain-text value
  }
  return { weekly: defaults, manualNote: raw.trim() };
}

export function serializeOperationalHours(weekly: WeeklyHoursState, manualNote: string): string {
  return JSON.stringify({ weekly, manualNote: manualNote.trim() });
}

export function formatOperationalHoursDisplay(raw: string | undefined | null): string {
  const { weekly, manualNote } = parseOperationalHours(raw);
  const openDays = WEEKDAY_ROWS.filter(({ key }) => !weekly[key].closed);
  if (openDays.length === 0 && !manualNote) return 'Closed';
  const times = openDays.map(({ key, label }) => {
    const d = weekly[key];
    return `${label.slice(0, 3)} ${d.open}–${d.close}`;
  });
  const summary = times.join(', ');
  return manualNote ? `${summary}${summary ? ' · ' : ''}${manualNote}` : summary || manualNote;
}
