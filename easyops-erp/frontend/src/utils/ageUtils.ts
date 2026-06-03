/**
 * Calculates age in years from a date of birth string.
 * Returns null if the input is falsy or invalid.
 */
export function calculateAge(dateOfBirth?: string | null): number | null {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (isNaN(dob.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    age--;
  }
  return age >= 0 ? age : null;
}

/**
 * Returns a formatted age string like "32 yrs" or "8 mos" for infants under 1 year.
 * Returns null if dateOfBirth is not provided.
 */
export function formatAge(dateOfBirth?: string | null): string | null {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (isNaN(dob.getTime())) return null;
  const today = new Date();
  let years = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    years--;
  }
  if (years < 0) return null;
  if (years < 1) {
    let months = (today.getFullYear() - dob.getFullYear()) * 12 + (today.getMonth() - dob.getMonth());
    if (today.getDate() < dob.getDate()) months--;
    return months <= 1 ? `${months} mo` : `${months} mos`;
  }
  return `${years} yrs`;
}

/**
 * Calculates an approximate date of birth from an age in years.
 * Sets the birth date to today's date/month in the calculated birth year.
 * Returns a date string in "YYYY-MM-DD" format.
 */
export function dobFromAge(age: number): string {
  const today = new Date();
  const birth = new Date(today.getFullYear() - age, today.getMonth(), today.getDate());
  const y = birth.getFullYear();
  const mm = String(birth.getMonth() + 1).padStart(2, '0');
  const dd = String(birth.getDate()).padStart(2, '0');
  return `${y}-${mm}-${dd}`;
}

/**
 * Calculates an approximate date of birth from an age in months.
 * Returns a date string in "YYYY-MM-DD" format.
 */
export function dobFromAgeMonths(months: number): string {
  const today = new Date();
  const birth = new Date(today.getFullYear(), today.getMonth() - months, today.getDate());
  const y = birth.getFullYear();
  const mm = String(birth.getMonth() + 1).padStart(2, '0');
  const dd = String(birth.getDate()).padStart(2, '0');
  return `${y}-${mm}-${dd}`;
}

/**
 * Calculates age in months from a date of birth string.
 * Returns null if the input is falsy or invalid.
 */
export function calculateAgeMonths(dateOfBirth?: string | null): number | null {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (isNaN(dob.getTime())) return null;
  const today = new Date();
  let months =
    (today.getFullYear() - dob.getFullYear()) * 12 + (today.getMonth() - dob.getMonth());
  if (today.getDate() < dob.getDate()) months--;
  return months >= 0 ? months : null;
}
