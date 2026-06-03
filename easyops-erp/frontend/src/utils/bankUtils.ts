/** Parse debit/credit/balance values from bank API (number or decimal string). */
export function toBankAmount(value: unknown): number {
  if (value === undefined || value === null) return 0;
  if (typeof value === 'number' && !Number.isNaN(value)) return value;
  const parsed = parseFloat(String(value));
  return Number.isNaN(parsed) ? 0 : parsed;
}

export function formatBankDebit(txn: { debitAmount?: unknown }): string {
  const amount = toBankAmount(txn.debitAmount);
  return amount > 0 ? amount.toFixed(2) : '';
}

export function formatBankCredit(txn: { creditAmount?: unknown }): string {
  const amount = toBankAmount(txn.creditAmount);
  return amount > 0 ? amount.toFixed(2) : '';
}

export function transactionNetAmount(txn: { debitAmount?: unknown; creditAmount?: unknown }): number {
  return toBankAmount(txn.creditAmount) - toBankAmount(txn.debitAmount);
}
