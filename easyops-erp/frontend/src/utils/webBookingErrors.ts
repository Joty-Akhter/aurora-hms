const WEB_BOOKING_ERROR_MESSAGES: Record<string, string> = {
  doctor_not_found: 'The selected doctor could not be found.',
  doctor_not_available_for_web_booking: 'This doctor is not available for online booking.',
  scheduling_unavailable: 'Appointment scheduling is temporarily unavailable. Please try again later.',
  invalid_appointment_date: 'Please choose a valid appointment date.',
  appointment_date_in_past: 'Appointment date cannot be in the past.',
  appointment_date_outside_booking_window: 'That date is outside the allowed booking window for this doctor.',
  slot_no_longer_available: 'That time slot is no longer available. Please choose another slot.',
  appointment_booking_failed: 'Could not complete the booking. Please try another slot or call the hospital.',
  invalid_date_range: 'The end date must be on or after the start date.',
  slot_date_mismatch: 'The selected slot does not match the chosen appointment date.',
  invalid_slot_start: 'The selected start time is invalid.',
  invalid_slot_end: 'The selected end time is invalid.',
  invalid_slot_range: 'The selected time range is invalid.',
  slot_in_past: 'That time slot has already passed. Please choose a future slot.',
};

function extractErrorCode(data: Record<string, unknown> | undefined): string | null {
  if (!data) return null;
  if (typeof data.message === 'string' && data.message.length > 0) return data.message;
  if (typeof data.error === 'string' && data.error.length > 0) return data.error;
  const errors = data.errors;
  if (errors && typeof errors === 'object' && !Array.isArray(errors)) {
    const first = Object.values(errors as Record<string, unknown>).find((v) => typeof v === 'string');
    if (typeof first === 'string') return first;
  }
  return null;
}

export function webBookingErrorMessage(err: unknown, fallback = 'Something went wrong. Please try again.'): string {
  const data = (err as { response?: { data?: Record<string, unknown> } })?.response?.data;
  const code = extractErrorCode(data);
  if (code && WEB_BOOKING_ERROR_MESSAGES[code]) {
    return WEB_BOOKING_ERROR_MESSAGES[code];
  }
  if (typeof code === 'string' && code.length > 0 && !code.includes('_')) {
    return code;
  }
  return fallback;
}
