import api from './api';

// ========== Type Definitions ==========

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface CreateChargeRequest {
  sourceService: string;
  sourceReferenceId: string;
  patientId: string;
  visitId?: string;
  corporateContractId?: string;
  itemCode: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  discountSource?: string;
  taxAmount?: number;
  idempotencyKey?: string;
}

export interface ChargeResponse {
  id: string;
  sourceService: string;
  sourceReferenceId: string;
  patientId: string;
  visitId?: string;
  corporateContractId?: string;
  itemCode: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
  grossAmount: number;
  discountAmount?: number;
  discountSource?: string;
  taxAmount?: number;
  netAmount: number;
  status: string;
  invoiceId?: string;
}

export interface CreateInvoiceRequest {
  patientId: string;
  visitId?: string;
  payerType: string;
  payerId?: string;
  chargeLineIds: string[];
  groupBy?: 'PATIENT' | 'VISIT';
  dueDate?: string; // ISO date (YYYY-MM-DD)
}

export interface InvoiceResponse {
  id: string;
  invoiceNumber: string;
  patientId: string;
  visitId?: string;
  payerType: string;
  payerId?: string;
  status: string;
  grossAmount: number;
  totalDiscount?: number;
  taxAmount?: number;
  netAmount: number;
  balanceDue: number;
  issuedAt?: string;
  dueDate?: string;
}

export interface PaymentsSummary {
  totalPaid: number;
  lastPaymentAt?: string | null;
}

export interface InvoiceDetailResponse {
  invoice: InvoiceResponse;
  chargeLines: ChargeResponse[];
  paymentsSummary: PaymentsSummary;
}

export interface CreateAdjustmentRequest {
  type: 'WRITE_OFF' | 'CREDIT' | 'ADJUSTMENT';
  amount: number;
  reason?: string;
}

export interface OutstandingInvoiceReportItem {
  invoiceId: string;
  invoiceNumber: string;
  patientId: string;
  payerId?: string;
  payerType: string;
  netAmount: number;
  balanceDue: number;
  issuedAt?: string;
}

export interface CollectedPaymentReportItem {
  paymentId: string;
  invoiceId: string;
  invoiceNumber?: string;
  amount: number;
  paymentMethod: string;
  paymentDate: string;
  receivedByUserId?: string;
}

export interface CreatePaymentRequest {
  amount: number;
  paymentMethod: string;
  paymentReference?: string;
  paymentDate?: string; // ISO datetime
}

export interface PaymentResponse {
  id: string;
  invoiceId: string;
  paymentReference?: string;
  paymentMethod: string;
  amount: number;
  paymentDate: string;
  status: string;
  receivedByUserId?: string;
  createdAt: string;
}

export interface CreateRefundRequest {
  amount: number;
  reason?: string;
}

export interface RefundResponse {
  id: string;
  originalPaymentId: string;
  invoiceId: string;
  amount: number;
  reason?: string;
  processedAt?: string;
  processedByUserId?: string;
}

export interface PaymentDetailResponse {
  payment: PaymentResponse;
  refunds: RefundResponse[];
}

// Estimate and discounts (Phase 3)
export interface EstimateLineItemRequest {
  itemCode: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
}

export interface EstimateRequest {
  lineItems: EstimateLineItemRequest[];
  patientId?: string;
  corporateContractId?: string;
}

export interface EstimateLineResponse {
  itemCode: string;
  itemDescription?: string;
  quantity: number;
  unitPrice: number;
  grossAmount: number;
  discountAmount: number;
  netAmount: number;
}

export interface DiscountLineResponse {
  description: string;
  source: string;
  amount: number;
}

export interface EstimateResponse {
  lines: EstimateLineResponse[];
  discountLines: DiscountLineResponse[];
  totalGross: number;
  totalDiscount: number;
  netPayable: number;
}

// ========== Service ==========

const hospitalBillingService = {
  // Charges
  async createCharges(requests: CreateChargeRequest[]): Promise<ChargeResponse[]> {
    const body = { charges: requests };
    const response = await api.post<ChargeResponse[]>('/api/hospital-billing/charges', body);
    return response.data;
  },

  async getCharge(id: string): Promise<ChargeResponse> {
    const response = await api.get<ChargeResponse>(`/api/hospital-billing/charges/${id}`);
    return response.data;
  },

  async getCharges(params: {
    patientId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<ChargeResponse>> {
    const response = await api.get<PagedResponse<ChargeResponse>>('/api/hospital-billing/charges', {
      params,
    });
    return response.data;
  },

  // Invoices
  async createInvoice(payload: CreateInvoiceRequest): Promise<InvoiceDetailResponse> {
    const response = await api.post<InvoiceDetailResponse>('/api/hospital-billing/invoices', payload);
    return response.data;
  },

  async getInvoice(id: string): Promise<InvoiceDetailResponse> {
    const response = await api.get<InvoiceDetailResponse>(`/api/hospital-billing/invoices/${id}`);
    return response.data;
  },

  async getInvoices(params: {
    patientId?: string;
    visitId?: string;
    page?: number;
    size?: number;
  }): Promise<PagedResponse<InvoiceResponse>> {
    const response = await api.get<PagedResponse<InvoiceResponse>>('/api/hospital-billing/invoices', {
      params,
    });
    return response.data;
  },

  async issueInvoice(id: string): Promise<InvoiceResponse> {
    const response = await api.post<InvoiceResponse>(`/api/hospital-billing/invoices/${id}/issue`);
    return response.data;
  },

  async cancelInvoice(id: string): Promise<InvoiceResponse> {
    const response = await api.post<InvoiceResponse>(`/api/hospital-billing/invoices/${id}/cancel`);
    return response.data;
  },

  async getEstimate(body: EstimateRequest): Promise<EstimateResponse> {
    const response = await api.post<EstimateResponse>('/api/hospital-billing/invoices/estimate', body);
    return response.data;
  },

  async getInvoiceDiscounts(invoiceId: string): Promise<DiscountLineResponse[]> {
    const response = await api.get<DiscountLineResponse[]>(
      `/api/hospital-billing/invoices/${invoiceId}/discounts`,
    );
    return response.data;
  },

  async createAdjustment(
    invoiceId: string,
    body: CreateAdjustmentRequest,
  ): Promise<InvoiceDetailResponse> {
    const response = await api.post<InvoiceDetailResponse>(
      `/api/hospital-billing/invoices/${invoiceId}/adjustments`,
      body,
    );
    return response.data;
  },

  // Payments & refunds
  async createPayment(invoiceId: string, body: CreatePaymentRequest): Promise<PaymentResponse> {
    const response = await api.post<PaymentResponse>(
      `/api/hospital-billing/invoices/${invoiceId}/payments`,
      body,
    );
    return response.data;
  },

  async getPayments(invoiceId: string): Promise<PaymentResponse[]> {
    const response = await api.get<PaymentResponse[]>(
      `/api/hospital-billing/invoices/${invoiceId}/payments`,
    );
    return response.data;
  },

  async getPaymentsGlobal(params: {
    invoiceId?: string;
    from?: string;
    to?: string;
  }): Promise<PaymentResponse[]> {
    const response = await api.get<PaymentResponse[]>('/api/hospital-billing/payments', {
      params,
    });
    return response.data;
  },

  async getPayment(paymentId: string): Promise<PaymentDetailResponse> {
    const response = await api.get<PaymentDetailResponse>(
      `/api/hospital-billing/payments/${paymentId}`,
    );
    return response.data;
  },

  async createRefund(paymentId: string, body: CreateRefundRequest): Promise<RefundResponse> {
    const response = await api.post<RefundResponse>(
      `/api/hospital-billing/payments/${paymentId}/refunds`,
      body,
    );
    return response.data;
  },

  async getOutstandingReports(params: {
    patientId?: string;
    corporateId?: string;
    asOf?: string;
  }): Promise<OutstandingInvoiceReportItem[]> {
    const response = await api.get<OutstandingInvoiceReportItem[]>(
      '/api/hospital-billing/reports/outstanding',
      { params },
    );
    return response.data;
  },

  async getCollectedReports(params: {
    from: string;
    to: string;
  }): Promise<CollectedPaymentReportItem[]> {
    const response = await api.get<CollectedPaymentReportItem[]>(
      '/api/hospital-billing/reports/collected',
      { params },
    );
    return response.data;
  },
};

export default hospitalBillingService;

