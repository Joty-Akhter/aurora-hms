import type { DispenseLine, DispenseOrder } from '../services/hospitalPharmacyService';
import appConfig from '../config';

/** Line amounts captured from the cart at dispense time (server lines have no list price). */
export interface PosReceiptLineSnapshot {
  drugLabel: string;
  quantityDispensed: number;
  unitPrice: number;
  lineTotal: number;
  batchNumber?: string;
}

export interface PosReceiptSnapshot {
  patientName?: string;
  patientPhone?: string;
  lines: PosReceiptLineSnapshot[];
  discount: number;
  paid: number;
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function lineDrugLabel(line: DispenseLine): string {
  const g = line.drugGenericName?.trim() ?? '';
  const b = line.drugBrandName?.trim();
  return b ? `${g} (${b})` : g;
}

/**
 * 80mm-oriented HTML for thermal / POS printers. Uses window.print() from {@link openPharmacyPosReceiptPrint}.
 */
export function buildPharmacyPosReceiptHtml(order: DispenseOrder, snapshot?: PosReceiptSnapshot): string {
  const pharmacyName = order.pharmacyLocationName || 'Pharmacy';
  const orderShort = order.id.slice(0, 8).toUpperCase();
  const when = new Date().toLocaleString();

  let rowsHtml = '';
  if (snapshot?.lines?.length) {
    for (const l of snapshot.lines) {
      rowsHtml += `<tr><td colspan="2" style="font-weight:600;padding-top:6px">${escapeHtml(l.drugLabel)}</td></tr>`;
      rowsHtml += `<tr><td>${l.quantityDispensed} × ${l.unitPrice.toFixed(2)}</td><td style="text-align:right">${l.lineTotal.toFixed(2)}</td></tr>`;
      if (l.batchNumber) {
        rowsHtml += `<tr><td colspan="2" class="muted">Batch ${escapeHtml(l.batchNumber)}</td></tr>`;
      }
    }
  } else {
    for (const line of order.lines) {
      rowsHtml += `<tr><td style="font-weight:600;padding-top:6px">${escapeHtml(lineDrugLabel(line))}</td><td style="text-align:right">${line.quantityDispensed}</td></tr>`;
      if (line.batchNumber) {
        rowsHtml += `<tr><td colspan="2" class="muted">Batch ${escapeHtml(line.batchNumber)}</td></tr>`;
      }
    }
  }

  const subtotal = snapshot?.lines.reduce((s, l) => s + l.lineTotal, 0) ?? 0;
  const disc = snapshot?.discount ?? 0;
  const paid = snapshot?.paid ?? 0;
  const totalAfterDisc = Math.max(0, subtotal - disc);
  const due = Math.max(0, totalAfterDisc - paid);
  const showMoney = Boolean(snapshot?.lines?.length);

  const patientBlock = snapshot?.patientName
    ? `<p class="row"><span>Patient</span><span>${escapeHtml(snapshot.patientName)}</span></p>`
    : '';
  const phoneBlock = snapshot?.patientPhone
    ? `<p class="row"><span>Tel</span><span>${escapeHtml(snapshot.patientPhone)}</span></p>`
    : '';
  const patientRef =
    order.patientId && !snapshot?.patientName
      ? `<p class="muted">Patient ref ${escapeHtml(order.patientId)}</p>`
      : '';

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>Receipt ${orderShort}</title>
  <style>
    @page { size: 80mm auto; margin: 5mm; }
    * { box-sizing: border-box; }
    body {
      font-family: ui-monospace, "Cascadia Code", "Consolas", "Segoe UI Mono", monospace;
      font-size: 12px;
      line-height: 1.35;
      margin: 0;
      padding: 0;
      color: #111;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .wrap { max-width: 72mm; margin: 0 auto; }
    .c { text-align: center; }
    .h { font-weight: 700; font-size: 15px; margin: 0 0 2px; letter-spacing: 0.02em; }
    .sub { font-size: 11px; margin: 0 0 8px; }
    .muted { color: #444; font-size: 10px; margin: 4px 0; }
    table.items { width: 100%; border-collapse: collapse; }
    table.items td { padding: 1px 0; vertical-align: top; }
    .tot { border-top: 1px dashed #222; margin-top: 10px; padding-top: 8px; }
    table.totals { width: 100%; border-collapse: collapse; }
    table.totals td { padding: 3px 0; }
    table.totals td:last-child { text-align: right; font-variant-numeric: tabular-nums; }
    .row { display: flex; justify-content: space-between; gap: 8px; margin: 4px 0; font-size: 11px; }
    .thanks { text-align: center; margin-top: 14px; font-size: 10px; color: #333; }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="c">
      <div class="h">${escapeHtml(pharmacyName)}</div>
      <div class="sub">DISPENSE RECEIPT (POS)</div>
      <div class="muted">${escapeHtml(when)}</div>
    </div>
    <p class="row"><span>Order</span><span>${orderShort}…</span></p>
    ${patientBlock}
    ${phoneBlock}
    ${patientRef}
    <table class="items">${rowsHtml}</table>
    ${
      showMoney
        ? `<div class="tot">
      <table class="totals">
        <tr><td>Subtotal</td><td>${subtotal.toFixed(2)}</td></tr>
        <tr><td>Discount</td><td>${disc.toFixed(2)}</td></tr>
        <tr><td><strong>Total</strong></td><td><strong>${totalAfterDisc.toFixed(2)}</strong></td></tr>
        <tr><td>Paid</td><td>${paid.toFixed(2)}</td></tr>
        <tr><td><strong>Due</strong></td><td><strong>${due.toFixed(2)}</strong></td></tr>
      </table>
    </div>`
        : `<p class="muted" style="margin-top:10px">Pricing: use Billable items / billing when charges are posted.</p>`
    }
    <p class="thanks">Thank you — ${appConfig.appName} Pharmacy</p>
  </div>
</body>
</html>`;
}

/**
 * Opens a narrow receipt window and triggers the browser print dialog (choose a thermal / POS printer in the dialog).
 * @returns false if the browser blocked the popup
 */
export function openPharmacyPosReceiptPrint(order: DispenseOrder, snapshot?: PosReceiptSnapshot): boolean {
  const html = buildPharmacyPosReceiptHtml(order, snapshot);
  const w = window.open('', '_blank', 'noopener,noreferrer,width=400,height=720');
  if (!w) {
    return false;
  }
  w.document.open();
  w.document.write(html);
  w.document.close();
  w.focus();
  const runPrint = () => {
    try {
      w.print();
    } catch {
      /* ignore */
    }
  };
  if (w.document.readyState === 'complete') {
    setTimeout(runPrint, 150);
  } else {
    w.onload = () => setTimeout(runPrint, 150);
  }
  return true;
}
