import type { PatientIdentityCardPrintResponse } from '../services/hospitalService';

/** Opens returned HTML in a new window and triggers the browser print dialog. */
export function openPatientIdentityCardPrintWindow(printPayload: PatientIdentityCardPrintResponse): void {
  const win = window.open('', '_blank', 'noopener,noreferrer,width=460,height=780');
  if (!win) {
    window.alert('Popup blocked. Please allow popups and try printing again.');
    return;
  }
  win.document.open();
  win.document.write(printPayload.html);
  win.document.close();
  win.focus();
  setTimeout(() => {
    win.print();
  }, 200);
}
