/** Normalized per-document aging row (AR invoice or AP bill). */
export interface AgingReportRow {
  invoiceId?: string;
  invoiceNumber?: string;
  billId?: string;
  billNumber?: string;
  customerId?: string;
  customerName?: string;
  vendorId?: string;
  vendorName?: string;
  invoiceDate?: string;
  dueDate?: string;
  balanceDue?: number | string;
  daysOverdue?: number;
  current?: number | string;
  days1To30?: number | string;
  days31To60?: number | string;
  days61To90?: number | string;
  days90Plus?: number | string;
}

export const AGING_BUCKET_FIELDS = [
  'current',
  'days1To30',
  'days31To60',
  'days61To90',
  'days90Plus',
] as const;

export function toNumber(value: number | string | undefined): number {
  if (value === undefined || value === null) return 0;
  if (typeof value === 'number') return value;
  const parsed = parseFloat(value);
  return Number.isNaN(parsed) ? 0 : parsed;
}

export function normalizeAgingRows(data: unknown): AgingReportRow[] {
  if (!Array.isArray(data)) return [];
  return data.map((raw: Record<string, unknown>) => ({
    invoiceId: raw.invoiceId as string | undefined,
    invoiceNumber: raw.invoiceNumber as string | undefined,
    billId: raw.billId as string | undefined,
    billNumber: raw.billNumber as string | undefined,
    customerId: raw.customerId as string | undefined,
    customerName: raw.customerName as string | undefined,
    vendorId: raw.vendorId as string | undefined,
    vendorName: raw.vendorName as string | undefined,
    invoiceDate: raw.invoiceDate as string | undefined,
    dueDate: raw.dueDate as string | undefined,
    balanceDue: raw.balanceDue as number | string | undefined,
    daysOverdue:
      raw.daysOverdue != null ? Number(raw.daysOverdue) : undefined,
    current: raw.current as number | string | undefined,
    days1To30: raw.days1To30 as number | string | undefined,
    days31To60: raw.days31To60 as number | string | undefined,
    days61To90: raw.days61To90 as number | string | undefined,
    days90Plus: raw.days90Plus as number | string | undefined,
  }));
}

export function getPartyLabel(row: AgingReportRow, isAR: boolean): string {
  return isAR ? row.customerName ?? '—' : row.vendorName ?? '—';
}

export function getDocumentLabel(row: AgingReportRow, isAR: boolean): string {
  return isAR ? row.invoiceNumber ?? '—' : row.billNumber ?? '—';
}

export function getRowKey(row: AgingReportRow, index: number): string {
  return row.invoiceId ?? row.billId ?? `row-${index}`;
}

export function sumBalanceDue(rows: AgingReportRow[]): number {
  return rows.reduce((sum, row) => sum + toNumber(row.balanceDue), 0);
}

export function sumBucket(
  rows: AgingReportRow[],
  field: (typeof AGING_BUCKET_FIELDS)[number]
): number {
  return rows.reduce((sum, row) => sum + toNumber(row[field]), 0);
}

/** Aggregated aging row for summary views (per customer or vendor). */
export interface AggregatedAgingRow {
  partyId: string;
  partyName: string;
  totalOutstanding: number;
  current: number;
  days1To30: number;
  days31To60: number;
  days61To90: number;
  over90Days: number;
}

export function aggregateCustomerAging(lines: AgingReportRow[]): AggregatedAgingRow[] {
  const byCustomer = new Map<string, AggregatedAgingRow>();

  for (const line of lines) {
    const partyId = line.customerId ?? 'unknown';
    const partyName = line.customerName ?? 'Unknown';
    const existing = byCustomer.get(partyId) ?? emptyAggregate(partyId, partyName);

    existing.totalOutstanding += toNumber(line.balanceDue);
    existing.current += toNumber(line.current);
    existing.days1To30 += toNumber(line.days1To30);
    existing.days31To60 += toNumber(line.days31To60);
    existing.days61To90 += toNumber(line.days61To90);
    existing.over90Days += toNumber(line.days90Plus);

    byCustomer.set(partyId, existing);
  }

  return Array.from(byCustomer.values()).sort((a, b) =>
    a.partyName.localeCompare(b.partyName)
  );
}

export function aggregateVendorAging(lines: AgingReportRow[]): AggregatedAgingRow[] {
  const byVendor = new Map<string, AggregatedAgingRow>();

  for (const line of lines) {
    const partyId = line.vendorId ?? 'unknown';
    const partyName = line.vendorName ?? 'Unknown';
    const existing = byVendor.get(partyId) ?? emptyAggregate(partyId, partyName);

    existing.totalOutstanding += toNumber(line.balanceDue);
    existing.current += toNumber(line.current);
    existing.days1To30 += toNumber(line.days1To30);
    existing.days31To60 += toNumber(line.days31To60);
    existing.days61To90 += toNumber(line.days61To90);
    existing.over90Days += toNumber(line.days90Plus);

    byVendor.set(partyId, existing);
  }

  return Array.from(byVendor.values()).sort((a, b) =>
    a.partyName.localeCompare(b.partyName)
  );
}

function emptyAggregate(partyId: string, partyName: string): AggregatedAgingRow {
  return {
    partyId,
    partyName,
    totalOutstanding: 0,
    current: 0,
    days1To30: 0,
    days31To60: 0,
    days61To90: 0,
    over90Days: 0,
  };
}
