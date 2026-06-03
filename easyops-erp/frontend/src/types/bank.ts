// Bank & Cash Management Types

export interface BankAccount {
  id: string;
  organizationId: string;
  accountNumber: string;
  accountName: string;
  bankName?: string;
  branchName?: string;
  accountType: BankAccountType;
  currency: string;
  glAccountId: string;
  openingBalance: number;
  currentBalance: number;
  isActive: boolean;
  createdAt: string;
}

export type BankAccountType = 'CHECKING' | 'SAVINGS' | 'CASH';

export interface BankTransaction {
  id: string;
  bankAccountId: string;
  transactionDate: string;
  transactionType: TransactionType;
  referenceNumber?: string;
  description?: string;
  debitAmount: number;
  creditAmount: number;
  balance: number;
  isReconciled: boolean;
  reconciledAt?: string;
  glJournalId?: string;
  sourceType?: string;
  sourceId?: string;
  createdAt: string;
}

export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'FEE' | 'INTEREST';

export interface BankReconciliation {
  id: string;
  bankAccountId: string;
  reconciliationDate: string;
  statementDate: string;
  openingBalance: number;
  closingBalance: number;
  statementBalance: number;
  difference: number;
  status: ReconciliationStatus;
  completedAt?: string;
  completedBy?: string;
  notes?: string;
  createdAt: string;
  createdBy?: string;
}

export type ReconciliationStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface ReconciliationItem {
  id: string;
  reconciliationId: string;
  bankTransactionId?: string;
  statementAmount: number;
  isMatched: boolean;
  createdAt: string;
}

export interface BankAccountRequest {
  organizationId: string;
  accountNumber: string;
  accountName: string;
  bankName?: string;
  branchName?: string;
  accountType: BankAccountType;
  currency?: string;
  glAccountId: string;
  openingBalance?: number;
}

export interface BankTransactionRequest {
  bankAccountId: string;
  transactionDate: string;
  transactionType: TransactionType;
  referenceNumber?: string;
  description?: string;
  debitAmount: number;
  creditAmount: number;
  glJournalId?: string;
}


