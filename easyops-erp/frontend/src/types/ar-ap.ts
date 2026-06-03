// Accounts Receivable & Accounts Payable Types

// ============ Customers & Vendors ============

export interface Customer {
  id: string;
  organizationId: string;
  customerCode: string;
  customerName: string;
  email?: string;
  phone?: string;
  creditLimit: number;
  paymentTerms: number;
  isActive: boolean;
  createdAt: string;
}

export interface Vendor {
  id: string;
  organizationId: string;
  vendorCode: string;
  vendorName: string;
  email?: string;
  phone?: string;
  paymentTerms: number;
  taxId?: string;
  isActive: boolean;
  createdAt: string;
}

// ============ AR - Accounts Receivable ============

export interface ARInvoice {
  id: string;
  organizationId: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate: string;
  customerId: string;
  periodId: string;
  currency: string;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  status: InvoiceStatus;
  paymentStatus: PaymentStatus;
  glJournalId?: string;
  notes?: string;
  createdAt: string;
  createdBy?: string;
  updatedAt: string;
  lines?: ARInvoiceLine[];
}

export interface ARInvoiceLine {
  id: string;
  invoiceId: string;
  lineNumber: number;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  taxPercent: number;
  lineTotal: number;
  accountId: string;
  createdAt: string;
}

export interface ARReceipt {
  id: string;
  organizationId: string;
  receiptNumber: string;
  receiptDate: string;
  customerId: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  amount: number;
  currency: string;
  bankAccountId?: string;
  glJournalId?: string;
  notes?: string;
  status: string;
  createdAt: string;
  createdBy?: string;
  allocations?: ReceiptAllocation[];
}

export interface ReceiptAllocation {
  id: string;
  receiptId: string;
  invoiceId: string;
  allocatedAmount: number;
  createdAt: string;
}

// ============ AP - Accounts Payable ============

export interface APBill {
  id: string;
  organizationId: string;
  billNumber: string;
  billDate: string;
  dueDate: string;
  vendorId: string;
  periodId: string;
  currency: string;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  status: InvoiceStatus;
  paymentStatus: PaymentStatus;
  glJournalId?: string;
  purchaseOrderRef?: string;
  notes?: string;
  createdAt: string;
  createdBy?: string;
  updatedAt: string;
  lines?: APBillLine[];
}

export interface APBillLine {
  id: string;
  billId: string;
  lineNumber: number;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  taxPercent: number;
  lineTotal: number;
  accountId: string;
  createdAt: string;
}

export interface APPayment {
  id: string;
  organizationId: string;
  paymentNumber: string;
  paymentDate: string;
  vendorId: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  checkNumber?: string;
  amount: number;
  currency: string;
  bankAccountId?: string;
  glJournalId?: string;
  notes?: string;
  status: string;
  createdAt: string;
  createdBy?: string;
  allocations?: PaymentAllocation[];
}

export interface PaymentAllocation {
  id: string;
  paymentId: string;
  billId: string;
  allocatedAmount: number;
  createdAt: string;
}

// ============ Aging Reports ============

export interface AgingReport {
  id: string;
  documentNumber: string;
  documentDate: string;
  dueDate: string;
  partyCode: string;
  partyName: string;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  daysOverdue: number;
  agingBucket: AgingBucket;
}

export type AgingBucket = 'CURRENT' | '1-30 DAYS' | '31-60 DAYS' | '61-90 DAYS' | 'OVER 90 DAYS';

// ============ Common Types ============

export type InvoiceStatus = 'DRAFT' | 'POSTED' | 'PAID' | 'CANCELLED';
export type PaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'OVERDUE';
export type PaymentMethod = 'CASH' | 'CHECK' | 'WIRE' | 'CARD' | 'ACH';

// ============ Request Types ============

export interface ARInvoiceRequest {
  organizationId: string;
  invoiceDate: string;
  customerId: string;
  paymentTerms?: number;
  currency?: string;
  notes?: string;
  lines: InvoiceLineRequest[];
}

export interface InvoiceLineRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  taxPercent?: number;
  accountId: string;
}

export interface APBillRequest {
  organizationId: string;
  billDate: string;
  vendorId: string;
  paymentTerms?: number;
  currency?: string;
  purchaseOrderRef?: string;
  notes?: string;
  lines: BillLineRequest[];
}

export interface BillLineRequest {
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  taxPercent?: number;
  accountId: string;
}

export interface PaymentRequest {
  organizationId: string;
  paymentDate: string;
  partyId: string;
  paymentMethod: PaymentMethod;
  amount: number;
  bankAccountId?: string;
  referenceNumber?: string;
  checkNumber?: string;
  notes?: string;
  allocations: AllocationRequest[];
}

export interface AllocationRequest {
  documentId: string;
  allocatedAmount: number;
}


