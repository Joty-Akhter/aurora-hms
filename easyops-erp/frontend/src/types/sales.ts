export interface Customer {
  id: string;
  organizationId: string;
  customerCode: string;
  customerName: string;
  email?: string;
  phone?: string;
  contactPerson?: string;
  billingAddress?: string;
  shippingAddress?: string;
  creditLimit: number;
  currentBalance: number;
  paymentTerms?: string;
  taxNumber?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: string;
  organizationId: string;
  productCode: string;
  productName: string;
  description?: string;
  productType: 'GOODS' | 'SERVICE';
  category?: string;
  unitOfMeasure?: string;
  unitPrice: number;
  costPrice?: number;
  taxRate: number;
  revenueAccountId?: string;
  cogsAccountId?: string;
  isActive: boolean;
  trackInventory: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface QuotationLine {
  id?: string;
  lineNumber: number;
  productId?: string;
  productCode?: string;
  productName: string;
  description?: string;
  quantity: number;
  unitOfMeasure?: string;
  unitPrice: number;
  discountPercent: number;
  discountAmount: number;
  taxPercent: number;
  taxAmount: number;
  lineTotal: number;
}

export interface Quotation {
  id: string;
  organizationId: string;
  quotationNumber: string;
  quotationDate: string;
  validUntil: string;
  customerId: string;
  customerName: string;
  customerEmail?: string;
  contactPerson?: string;
  billingAddress?: string;
  shippingAddress?: string;
  subtotal: number;
  discountAmount: number;
  discountPercent: number;
  taxAmount: number;
  totalAmount: number;
  status: 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CONVERTED';
  notes?: string;
  termsAndConditions?: string;
  salesPersonId?: string;
  salesPersonName?: string;
  convertedToOrderId?: string;
  convertedDate?: string;
  lines: QuotationLine[];
  createdBy?: string;
  createdAt: string;
  updatedBy?: string;
  updatedAt: string;
}

export interface SalesOrderLine {
  id?: string;
  lineNumber: number;
  productId?: string;
  productCode?: string;
  productName: string;
  description?: string;
  quantity: number;
  unitOfMeasure?: string;
  unitPrice: number;
  discountPercent: number;
  discountAmount: number;
  taxPercent: number;
  taxAmount: number;
  lineTotal: number;
  deliveredQuantity: number;
  invoicedQuantity: number;
  revenueAccountId?: string;
}

export interface SalesOrder {
  id: string;
  organizationId: string;
  orderNumber: string;
  orderDate: string;
  expectedDeliveryDate?: string;
  customerId: string;
  customerName: string;
  customerEmail?: string;
  contactPerson?: string;
  billingAddress?: string;
  shippingAddress?: string;
  subtotal: number;
  discountAmount: number;
  discountPercent: number;
  taxAmount: number;
  totalAmount: number;
  status: 'DRAFT' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'INVOICED';
  paymentStatus: 'UNPAID' | 'PARTIALLY_PAID' | 'PAID';
  deliveryStatus: 'NOT_DELIVERED' | 'PARTIALLY_DELIVERED' | 'DELIVERED';
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  notes?: string;
  termsAndConditions?: string;
  salesPersonId?: string;
  salesPersonName?: string;
  quotationId?: string;
  convertedToInvoiceId?: string;
  convertedToInvoiceDate?: string;
  approvedBy?: string;
  approvedDate?: string;
  lines: SalesOrderLine[];
  createdBy?: string;
  createdAt: string;
  updatedBy?: string;
  updatedAt: string;
}

export interface QuotationRequest {
  organizationId: string;
  quotationDate: string;
  validUntil: string;
  customerId: string;
  contactPerson?: string;
  billingAddress?: string;
  shippingAddress?: string;
  discountPercent?: number;
  discountAmount?: number;
  notes?: string;
  termsAndConditions?: string;
  salesPersonId?: string;
  salesPersonName?: string;
  lines: {
    lineNumber: number;
    productId?: string;
    productCode?: string;
    productName: string;
    description?: string;
    quantity: number;
    unitOfMeasure?: string;
    unitPrice: number;
    discountPercent?: number;
    taxPercent?: number;
  }[];
}

export interface SalesOrderRequest {
  organizationId: string;
  orderDate: string;
  expectedDeliveryDate?: string;
  customerId: string;
  contactPerson?: string;
  billingAddress?: string;
  shippingAddress?: string;
  discountPercent?: number;
  discountAmount?: number;
  priority?: string;
  notes?: string;
  termsAndConditions?: string;
  salesPersonId?: string;
  salesPersonName?: string;
  quotationId?: string;
  lines: {
    lineNumber: number;
    productId?: string;
    productCode?: string;
    productName: string;
    description?: string;
    quantity: number;
    unitOfMeasure?: string;
    unitPrice: number;
    discountPercent?: number;
    taxPercent?: number;
    revenueAccountId?: string;
  }[];
}

