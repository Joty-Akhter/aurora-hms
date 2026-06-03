const STORAGE_KEY = 'ep_prescription_return_path';

/** Remember unfinished prescription URL when navigating away mid-compose. */
export function setEpPrescriptionReturnPath(path: string): void {
  try {
    sessionStorage.setItem(STORAGE_KEY, path);
  } catch {
    // ignore quota / private mode
  }
}

export function getEpPrescriptionReturnPath(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function clearEpPrescriptionReturnPath(): void {
  try {
    sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // ignore
  }
}
