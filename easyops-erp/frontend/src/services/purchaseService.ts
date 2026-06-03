import api from './api';

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  poDate: string;
  vendorId: string;
  vendorName: string;
  status: string;
  totalAmount: number;
  currency: string;
  expectedDeliveryDate?: string;
  actualDeliveryDate?: string;
  createdBy: string;
  approvedBy?: string;
  approvedAt?: string;
  notes?: string;
  lines: PurchaseOrderLine[];
}

export interface PurchaseOrderLine {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  receivedQuantity: number;
  pendingQuantity: number;
  status: string;
}

export interface PurchaseReceipt {
  id: string;
  receiptNumber: string;
  receiptDate: string;
  poId: string;
  poNumber: string;
  vendorId: string;
  vendorName: string;
  status: string;
  totalQuantity: number;
  receivedBy: string;
  receivedAt: string;
  notes?: string;
  lines: ReceiptLine[];
}

export interface ReceiptLine {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  orderedQuantity: number;
  receivedQuantity: number;
  unitPrice: number;
  totalPrice: number;
  condition: string;
  notes?: string;
}

export interface PurchaseInvoice {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  poId: string;
  poNumber: string;
  receiptId: string;
  receiptNumber: string;
  vendorId: string;
  vendorName: string;
  status: string;
  totalAmount: number;
  currency: string;
  dueDate: string;
  paidAmount: number;
  balanceAmount: number;
  createdBy: string;
  approvedBy?: string;
  approvedAt?: string;
  notes?: string;
  lines: InvoiceLine[];
}

export interface InvoiceLine {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  orderedQuantity: number;
  receivedQuantity: number;
  invoicedQuantity: number;
  unitPrice: number;
  totalPrice: number;
  variance: number;
  status: string;
}

export interface PurchaseStats {
  totalPOs: number;
  pendingApproval: number;
  approvedPOs: number;
  receivedPOs: number;
  totalValue: number;
  currency: string;
}

export interface VendorPerformance {
  vendorId: string;
  vendorName: string;
  totalPOs: number;
  totalValue: number;
  averageDeliveryDays: number;
  onTimeDeliveryRate: number;
  qualityRating: number;
}

export interface SpendAnalysis {
  category: string;
  totalSpend: number;
  percentage: number;
  poCount: number;
  averageValue: number;
}

class PurchaseService {
  // Purchase Orders
  async getPurchaseOrders(organizationId: string, params?: any): Promise<PurchaseOrder[]> {
    const response = await api.get('/api/purchase/orders', {
      params: { organizationId, ...params }
    });
    return response.data;
  }

  async getPurchaseOrderById(id: string): Promise<PurchaseOrder> {
    const response = await api.get(`/api/purchase/orders/${id}`);
    return response.data;
  }

  async createPurchaseOrder(data: any): Promise<PurchaseOrder> {
    const response = await api.post('/api/purchase/orders', data);
    return response.data;
  }

  async updatePurchaseOrder(id: string, data: any): Promise<PurchaseOrder> {
    const response = await api.put(`/api/purchase/orders/${id}`, data);
    return response.data;
  }

  async approvePurchaseOrder(id: string, data: any): Promise<PurchaseOrder> {
    const response = await api.post(`/api/purchase/orders/${id}/approve`, data);
    return response.data;
  }

  async cancelPurchaseOrder(id: string, data: any): Promise<PurchaseOrder> {
    const response = await api.post(`/api/purchase/orders/${id}/cancel`, data);
    return response.data;
  }

  async deletePurchaseOrder(id: string): Promise<void> {
    await api.delete(`/api/purchase/orders/${id}`);
  }

  // Purchase Receipts
  async getPurchaseReceipts(organizationId: string, params?: any): Promise<PurchaseReceipt[]> {
    const response = await api.get('/api/purchase/receipts', {
      params: { organizationId, ...params }
    });
    return response.data;
  }

  async getPurchaseReceiptById(id: string): Promise<PurchaseReceipt> {
    const response = await api.get(`/api/purchase/receipts/${id}`);
    return response.data;
  }

  async createPurchaseReceipt(data: any): Promise<PurchaseReceipt> {
    const response = await api.post('/api/purchase/receipts', data);
    return response.data;
  }

  async updatePurchaseReceipt(id: string, data: any): Promise<PurchaseReceipt> {
    const response = await api.put(`/api/purchase/receipts/${id}`, data);
    return response.data;
  }

  async deletePurchaseReceipt(id: string): Promise<void> {
    await api.delete(`/api/purchase/receipts/${id}`);
  }

  // Purchase Invoices
  async getPurchaseInvoices(organizationId: string, params?: any): Promise<PurchaseInvoice[]> {
    const response = await api.get('/api/purchase/invoices', {
      params: { organizationId, ...params }
    });
    return response.data;
  }

  async getPurchaseInvoiceById(id: string): Promise<PurchaseInvoice> {
    const response = await api.get(`/api/purchase/invoices/${id}`);
    return response.data;
  }

  async createPurchaseInvoice(data: any): Promise<PurchaseInvoice> {
    const response = await api.post('/api/purchase/invoices', data);
    return response.data;
  }

  async updatePurchaseInvoice(id: string, data: any): Promise<PurchaseInvoice> {
    const response = await api.put(`/api/purchase/invoices/${id}`, data);
    return response.data;
  }

  async approvePurchaseInvoice(id: string, data: any): Promise<PurchaseInvoice> {
    const response = await api.post(`/api/purchase/invoices/${id}/approve`, data);
    return response.data;
  }

  async createBillFromInvoice(id: string): Promise<any> {
    const response = await api.post(`/api/purchase/invoices/${id}/create-bill`);
    return response.data;
  }

  async runThreeWayMatching(id: string): Promise<any> {
    const response = await api.post(`/api/purchase/invoices/${id}/match`);
    return response.data;
  }

  async approveInvoiceVariance(id: string, data: { approvedBy: string; notes: string }): Promise<any> {
    const response = await api.post(`/api/purchase/invoices/${id}/approve-variance`, data);
    return response.data;
  }

  async deletePurchaseInvoice(id: string): Promise<void> {
    await api.delete(`/api/purchase/invoices/${id}`);
  }

  // Dashboard and Reports
  async getPurchaseStats(organizationId: string): Promise<PurchaseStats> {
    const response = await api.get('/api/purchase/dashboard/stats', {
      params: { organizationId }
    });
    return response.data;
  }

  async getRecentPurchaseOrders(organizationId: string, limit: number = 5): Promise<PurchaseOrder[]> {
    const response = await api.get('/api/purchase/orders/recent', {
      params: { organizationId, limit }
    });
    return response.data;
  }

  async getTopVendors(organizationId: string, limit: number = 5): Promise<any[]> {
    const response = await api.get('/api/purchase/dashboard/top-vendors', {
      params: { organizationId, limit }
    });
    return response.data;
  }

  // Reports
  async getPurchaseSummary(organizationId: string, startDate: string, endDate: string): Promise<any> {
    const response = await api.get('/api/purchase/reports/summary', {
      params: { organizationId, startDate, endDate }
    });
    return response.data;
  }

  async getVendorPerformance(organizationId: string, startDate: string, endDate: string): Promise<VendorPerformance[]> {
    const response = await api.get('/api/purchase/reports/vendor-performance', {
      params: { organizationId, startDate, endDate }
    });
    return response.data;
  }

  async getSpendAnalysis(organizationId: string, startDate: string, endDate: string): Promise<SpendAnalysis[]> {
    const response = await api.get('/api/purchase/reports/spend-analysis', {
      params: { organizationId, startDate, endDate }
    });
    return response.data;
  }

  async getTopVendorsReport(organizationId: string, startDate: string, endDate: string, limit: number = 10): Promise<any[]> {
    const response = await api.get('/api/purchase/reports/top-vendors', {
      params: { organizationId, startDate, endDate, limit }
    });
    return response.data;
  }

  // Export functions
  async exportPurchaseOrders(organizationId: string, format: 'excel' | 'pdf' = 'excel'): Promise<Blob> {
    const response = await api.get('/api/purchase/export/orders', {
      params: { organizationId, format },
      responseType: 'blob'
    });
    return response.data;
  }

  async exportPurchaseReport(organizationId: string, startDate: string, endDate: string, format: 'excel' | 'pdf' = 'excel'): Promise<Blob> {
    const response = await api.get('/api/purchase/export/report', {
      params: { organizationId, startDate, endDate, format },
      responseType: 'blob'
    });
    return response.data;
  }
}

export default new PurchaseService();
